package com.ruoyi.ai.prompt;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.ai.domain.AiPromptTemplate;
import com.ruoyi.ai.service.IAiPromptTemplateService;

/**
 * 提示词模板渲染服务（引擎层，纯 AI 引擎，不依赖业务模块）。
 *
 * <p>职责：把分散在各业务 Controller 里的硬编码 system / user 提示词，统一改为
 * 「从 ai_prompt_template 读取 → 变量替换 → 按模型差异化」的可运营方式。
 *
 * <p>渲染链路（三级兜底，保证零回归）：
 * <ol>
 *   <li>DB：按 sceneType 取 is_default='Y' 且 is_enabled='0' 的模板；</li>
 *   <li>model_specific：若模板的 model_specific(JSON) 含当前 modelCode，则用其 system/user 覆盖；</li>
 *   <li>BUILTIN：DB 缺失时回退 {@link BuiltinPrompt} 内置常量（与原硬编码一致）；</li>
 *   <li>若两者皆无，标记 source=FALLBACK，返回空串，由 AiModelClient 现有兜底文案接住。</li>
 * </ol>
 *
 * <p>缓存：按 sceneType 缓存解析后的模板（默认 5 分钟 TTL），后台改词后最多 5 分钟生效。
 * 零新增 maven 依赖（变量替换用轻量正则，JSON 解析复用项目已有的 fastjson2）。
 *
 * @author devpivot
 * @date 2026-08-10
 */
@Service
public class PromptTemplateService
{
    private static final Logger log = LoggerFactory.getLogger(PromptTemplateService.class);

    /** 缓存 TTL：5 分钟 */
    private static final long CACHE_TTL_MS = 5 * 60 * 1000L;

    /** 匹配 {{var}} 或 {{ var }} */
    private static final Pattern VAR_PATTERN = Pattern.compile("\\{\\{\\s*([\\w.]+)\\s*\\}\\}");

    @Autowired
    private IAiPromptTemplateService aiPromptTemplateService;

    /** sceneType -> 缓存条目 */
    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

    /**
     * 按场景渲染提示词（使用默认模型时 modelCode 可传 null）。
     *
     * @param sceneType 场景类型（PRD / CLARIFY / TECH / DB / CHECK / POLISH）
     * @param modelCode 模型标识（用于 model_specific 差异化），可为 null
     * @param vars      模板变量（键对应 {{key}}）
     */
    public RenderedPrompt render(String sceneType, String modelCode, Map<String, Object> vars)
    {
        Resolved r = resolve(sceneType);
        String sys = r.baseSystem;
        String usr = r.baseUser;

        if (modelCode != null && r.modelSpecific != null)
        {
            JSONObject ms = r.modelSpecific.get(modelCode);
            if (ms != null)
            {
                String s = ms.getString("system");
                String u = ms.getString("user");
                if (s != null) sys = s;
                if (u != null) usr = u;
            }
        }

        return new RenderedPrompt(renderVars(sys, vars), renderVars(usr, vars), sceneType, r.source.name());
    }

    /**
     * 主动失效渲染缓存（后台「保存/设为默认」后调用，使新提示词立即生效，无需等 5 分钟 TTL）。
     */
    public void clearCache()
    {
        cache.clear();
        log.info("[prompt] 渲染缓存已主动清空");
    }

    /**
     * 按模板编码渲染（适用于同一场景存在多套命名模板时显式指定）。
     */
    public RenderedPrompt renderByCode(String templateCode, String modelCode, Map<String, Object> vars)
    {
        AiPromptTemplate tpl = aiPromptTemplateService.selectAiPromptTemplateList(
                new AiPromptTemplate() { { setTemplateCode(templateCode); } })
                .stream().findFirst().orElse(null);
        if (tpl == null)
        {
            // DB 缺失：按 templateCode 回退内置常量（同一场景多套提示词，如 PROTO_CHAT/GEN/PATCH）
            String[] builtin = BuiltinPrompt.get(templateCode);
            if (builtin != null)
            {
                log.warn("[prompt] 未找到模板编码={}，回退内置常量", templateCode);
                return new RenderedPrompt(renderVars(builtin[0], vars), renderVars(builtin[1], vars),
                        null, "BUILTIN");
            }
            log.warn("[prompt] 未找到模板编码={} 且无内置常量，标记 FALLBACK", templateCode);
            return new RenderedPrompt("", "", null, "FALLBACK");
        }
        String sys = tpl.getTemplateContent() == null ? "" : tpl.getTemplateContent();
        String usr = tpl.getUserTemplate() == null ? "" : tpl.getUserTemplate();
        if (modelCode != null && tpl.getModelSpecific() != null)
        {
            try
            {
                JSONObject root = JSON.parseObject(tpl.getModelSpecific());
                JSONObject ms = root.getJSONObject(modelCode);
                if (ms != null)
                {
                    if (ms.getString("system") != null) sys = ms.getString("system");
                    if (ms.getString("user") != null) usr = ms.getString("user");
                }
            }
            catch (Exception e)
            {
                log.warn("[prompt] 模板编码={} 的 model_specific 解析失败，忽略", templateCode);
            }
        }
        return new RenderedPrompt(renderVars(sys, vars), renderVars(usr, vars), tpl.getSceneType(), "DB");
    }

    /* ----------------------------- 内部实现 ----------------------------- */

    private Resolved resolve(String sceneType)
    {
        long now = System.currentTimeMillis();
        CacheEntry e = cache.get(sceneType);
        if (e != null && e.expireAt > now)
        {
            return e.resolved;
        }
        Resolved r = doResolve(sceneType);
        cache.put(sceneType, new CacheEntry(r, now + CACHE_TTL_MS));
        return r;
    }

    private Resolved doResolve(String sceneType)
    {
        try
        {
            AiPromptTemplate q = new AiPromptTemplate();
            q.setSceneType(sceneType);
            q.setIsDefault("Y");
            q.setIsEnabled("0");
            var list = aiPromptTemplateService.selectAiPromptTemplateList(q);
            if (list != null && !list.isEmpty())
            {
                AiPromptTemplate tpl = list.get(0);
                Map<String, JSONObject> modelSpecific = null;
                if (tpl.getModelSpecific() != null && !tpl.getModelSpecific().isEmpty())
                {
                    try
                    {
                        JSONObject root = JSON.parseObject(tpl.getModelSpecific());
                        modelSpecific = new HashMap<>();
                        for (Map.Entry<String, Object> en : root.entrySet())
                        {
                            if (en.getValue() instanceof JSONObject)
                            {
                                modelSpecific.put(en.getKey(), (JSONObject) en.getValue());
                            }
                        }
                    }
                    catch (Exception ex)
                    {
                        log.warn("[prompt] 场景={} 的 model_specific 解析失败，忽略按模型差异化", sceneType);
                    }
                }
                Resolved r = new Resolved();
                r.sceneType = sceneType;
                r.baseSystem = tpl.getTemplateContent() == null ? "" : tpl.getTemplateContent();
                r.baseUser = tpl.getUserTemplate() == null ? "" : tpl.getUserTemplate();
                r.modelSpecific = modelSpecific;
                r.source = Source.DB;
                return r;
            }
        }
        catch (Exception ex)
        {
            log.warn("[prompt] 场景={} 查询数据库模板异常，回退内置常量", sceneType, ex);
        }

        // 二级兜底：内置常量
        String[] builtin = BuiltinPrompt.get(sceneType);
        if (builtin != null)
        {
            Resolved r = new Resolved();
            r.sceneType = sceneType;
            r.baseSystem = builtin[0] == null ? "" : builtin[0];
            r.baseUser = builtin[1] == null ? "" : builtin[1];
            r.modelSpecific = null;
            r.source = Source.BUILTIN;
            return r;
        }

        // 三级兜底：无可用模板
        Resolved r = new Resolved();
        r.sceneType = sceneType;
        r.baseSystem = "";
        r.baseUser = "";
        r.modelSpecific = null;
        r.source = Source.FALLBACK;
        return r;
    }

    private String renderVars(String template, Map<String, Object> vars)
    {
        if (template == null || template.isEmpty())
        {
            return "";
        }
        Matcher m = VAR_PATTERN.matcher(template);
        StringBuffer sb = new StringBuffer();
        while (m.find())
        {
            String key = m.group(1);
            Object val = (vars == null) ? null : vars.get(key);
            if (val == null)
            {
                log.warn("[prompt] 模板变量 {{}} 未提供值，原样保留", key);
                m.appendReplacement(sb, Matcher.quoteReplacement(m.group(0)));
            }
            else
            {
                m.appendReplacement(sb, Matcher.quoteReplacement(String.valueOf(val)));
            }
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private enum Source
    {
        DB, BUILTIN, FALLBACK
    }

    private static class Resolved
    {
        String sceneType;
        String baseSystem;
        String baseUser;
        Map<String, JSONObject> modelSpecific;
        Source source;
    }

    private static class CacheEntry
    {
        final Resolved resolved;
        final long expireAt;

        CacheEntry(Resolved resolved, long expireAt)
        {
            this.resolved = resolved;
            this.expireAt = expireAt;
        }
    }
}
