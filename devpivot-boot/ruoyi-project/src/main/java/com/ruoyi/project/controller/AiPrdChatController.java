package com.ruoyi.project.controller;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.ruoyi.ai.domain.AiModelConfig;
import com.ruoyi.ai.service.AiModelClient;
import com.ruoyi.ai.service.IAiModelConfigService;
import com.ruoyi.common.core.controller.BaseController;

/**
 * 门户·PRD 润色对话流式接口（/ai/chat）
 *
 * 服务于门户 PRD 步骤页的右侧对话区：用户针对当前 PRD 文档提问或引用其中段落，
 * 后端将「问题 + 当前文档内容 + 引用片段」作为上下文调用大模型，逐 token 通过 SSE 推送。
 * 与现有后台管理接口 /system/chat（预留 CRUD）完全独立，互不影响。
 *
 * @author devpivot
 * @date 2026-08-06
 */
@RestController
@RequestMapping("/ai/chat")
public class AiPrdChatController extends BaseController
{
    @Autowired
    private AiModelClient aiModelClient;

    @Autowired
    private IAiModelConfigService modelConfigService;

    private static final ExecutorService STREAM_POOL = Executors.newCachedThreadPool();

    private static final Logger log = LoggerFactory.getLogger(AiPrdChatController.class);

    /**
     * 发送对话消息（流式 SSE）
     *
     * 事件约定（data 为 JSON）：
     *  - {type:"start"}
     *  - {type:"token", delta}
     *  - {type:"done"}
     *  - {type:"error", content}
     */
    @PostMapping("/send")
    public SseEmitter send(@RequestBody Map<String, Object> body, HttpServletResponse response)
    {
        response.setHeader("Cache-Control", "no-cache, no-transform");
        response.setHeader("X-Accel-Buffering", "no");
        log.info("[prd-chat] 收到请求 projectId={}", body == null ? null : body.get("projectId"));

        SseEmitter emitter = new SseEmitter(180000L);
        emitter.onTimeout(() -> {
            try
            {
                emitter.send(SseEmitter.event().name("error").data(mapOf("type", "error", "content", "（请求超时）")));
            }
            catch (IOException ignored) { }
            emitter.complete();
        });
        emitter.onError(e -> emitter.completeWithError(e));

        String rawModel = body.get("model") == null ? null : String.valueOf(body.get("model")).trim();
        final String model = (rawModel == null || rawModel.isEmpty()) ? defaultModelCode() : rawModel;
        String question = body.get("question") == null ? "" : String.valueOf(body.get("question"));
        String docContent = body.get("docContent") == null ? "" : String.valueOf(body.get("docContent"));
        List<Object> quotesObj = body.get("quotes") instanceof List ? (List<Object>) body.get("quotes") : null;

        String systemPrompt = "你是一名资深产品经理助手，正在帮助用户审阅、修改与完善 PRD 文档。"
                + "请结合用户提供的当前 PRD 文档内容与引用片段，给出专业、可落地的建议或修订后的内容。"
                + "如涉及具体章节改写，请直接给出改写后的 Markdown 片段，便于用户复制回文档。使用中文，重点突出。";

        String userPrompt = buildUserPrompt(question, docContent, quotesObj);

        try
        {
            emitter.send(SseEmitter.event().name("start").data(mapOf("type", "start")));
        }
        catch (IOException e)
        {
            emitter.completeWithError(e);
            return emitter;
        }

        STREAM_POOL.submit(() -> {
            try
            {
                aiModelClient.chatStream(model, systemPrompt, userPrompt, delta -> {
                    try
                    {
                        emitter.send(SseEmitter.event().name("token").data(mapOf("type", "token", "delta", delta)));
                    }
                    catch (IOException ignored) { }
                });
                emitter.send(SseEmitter.event().name("done").data(mapOf("type", "done")));
                emitter.complete();
            }
            catch (Exception e)
            {
                log.error("[prd-chat] 对话异常", e);
                try
                {
                    emitter.send(SseEmitter.event().name("error").data(mapOf("type", "error", "content", e.getMessage())));
                }
                catch (IOException ignored) { }
                emitter.complete();
            }
        });
        return emitter;
    }

    private String buildUserPrompt(String question, String docContent, List<Object> quotesObj)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("【当前 PRD 文档内容】\n");
        sb.append(docContent.isEmpty() ? "（文档尚未生成）" : docContent).append("\n\n");
        if (quotesObj != null && !quotesObj.isEmpty())
        {
            sb.append("【用户引用的 PRD 片段】\n");
            for (int i = 0; i < quotesObj.size(); i++)
            {
                sb.append((i + 1)).append(". ").append(quotesObj.get(i)).append("\n");
            }
            sb.append("\n");
        }
        sb.append("【用户的问题/要求】\n").append(question.isEmpty() ? "（请针对以上引用的 PRD 内容给出修改建议）" : question);
        return sb.toString();
    }

    /** 取第一个启用模型的 modelCode，无配置时回退 "deepseek" */
    private String defaultModelCode()
    {
        try
        {
            AiModelConfig query = new AiModelConfig();
            query.setIsEnabled("0");
            List<AiModelConfig> list = modelConfigService.selectAiModelConfigList(query);
            if (list != null && !list.isEmpty())
            {
                for (AiModelConfig c : list)
                {
                    if (c.getModelCode() != null && !c.getModelCode().isEmpty())
                    {
                        return c.getModelCode();
                    }
                }
            }
        }
        catch (Exception e) { }
        return "deepseek";
    }

    private void writeError(SseEmitter emitter, String msg)
    {
        try
        {
            emitter.send(SseEmitter.event().name("error").data(mapOf("type", "error", "content", msg)));
            emitter.complete();
        }
        catch (IOException e)
        {
            emitter.completeWithError(e);
        }
    }

    /** 简化的不可变 Map 构造（值不允许为 null） */
    private static Map<String, Object> mapOf(Object... kv)
    {
        Map<String, Object> m = new java.util.HashMap<>(kv.length / 2 + 1);
        for (int i = 0; i + 1 < kv.length; i += 2)
        {
            m.put(String.valueOf(kv[i]), kv[i + 1]);
        }
        return m;
    }
}
