package com.ruoyi.ai.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.ai.domain.AiPromptTemplate;
import com.ruoyi.ai.service.IAiPromptTemplateService;
import com.ruoyi.ai.service.AiModelClient;
import com.ruoyi.ai.prompt.PromptTemplateService;
import com.ruoyi.ai.prompt.RenderedPrompt;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * Prompt模板Controller
 * 
 * @author ruoyi
 * @date 2026-08-04
 */
@RestController
@RequestMapping("/system/template")
public class AiPromptTemplateController extends BaseController
{
    @Autowired
    private IAiPromptTemplateService aiPromptTemplateService;

    @Autowired
    private PromptTemplateService promptTemplateService;

    @Autowired
    private AiModelClient aiModelClient;

    /**
     * 查询Prompt模板列表
     */
    @PreAuthorize("@ss.hasPermi('system:template:list')")
    @GetMapping("/list")
    public TableDataInfo list(AiPromptTemplate aiPromptTemplate)
    {
        startPage();
        List<AiPromptTemplate> list = aiPromptTemplateService.selectAiPromptTemplateList(aiPromptTemplate);
        return getDataTable(list);
    }

    /**
     * 导出Prompt模板列表
     */
    @PreAuthorize("@ss.hasPermi('system:template:export')")
    @Log(title = "Prompt模板", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, AiPromptTemplate aiPromptTemplate)
    {
        List<AiPromptTemplate> list = aiPromptTemplateService.selectAiPromptTemplateList(aiPromptTemplate);
        ExcelUtil<AiPromptTemplate> util = new ExcelUtil<AiPromptTemplate>(AiPromptTemplate.class);
        util.exportExcel(response, list, "Prompt模板数据");
    }

    /**
     * 获取Prompt模板详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:template:query')")
    @GetMapping(value = "/{templateId}")
    public AjaxResult getInfo(@PathVariable("templateId") Long templateId)
    {
        return success(aiPromptTemplateService.selectAiPromptTemplateByTemplateId(templateId));
    }

    /**
     * 新增Prompt模板（新增后清空渲染缓存，使新提示词可立即生效）
     */
    @PreAuthorize("@ss.hasPermi('system:template:add')")
    @Log(title = "Prompt模板", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody AiPromptTemplate aiPromptTemplate)
    {
        int n = aiPromptTemplateService.insertAiPromptTemplate(aiPromptTemplate);
        promptTemplateService.clearCache();
        return toAjax(n);
    }

    /**
     * 修改Prompt模板（修改后清空渲染缓存，使新提示词立即生效）
     */
    @PreAuthorize("@ss.hasPermi('system:template:edit')")
    @Log(title = "Prompt模板", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody AiPromptTemplate aiPromptTemplate)
    {
        int n = aiPromptTemplateService.updateAiPromptTemplate(aiPromptTemplate);
        promptTemplateService.clearCache();
        return toAjax(n);
    }

    /**
     * 删除Prompt模板（删除后清空渲染缓存）
     */
    @PreAuthorize("@ss.hasPermi('system:template:remove')")
    @Log(title = "Prompt模板", businessType = BusinessType.DELETE)
    @DeleteMapping("/{templateIds}")
    public AjaxResult remove(@PathVariable Long[] templateIds)
    {
        int n = aiPromptTemplateService.deleteAiPromptTemplateByTemplateIds(templateIds);
        promptTemplateService.clearCache();
        return toAjax(n);
    }

    /**
     * 主动刷新提示词渲染缓存（后台编辑后无需等待 5 分钟 TTL 即生效）
     */
    @PreAuthorize("@ss.hasPermi('system:template:edit')")
    @PostMapping("/clearCache")
    public AjaxResult clearCache()
    {
        promptTemplateService.clearCache();
        return AjaxResult.success("缓存已刷新，模板将立即生效");
    }

    /**
     * 试跑：用当前模板渲染后的 system/user 直接调用模型，返回输出（dry-run，便于验证提示词效果）。
     */
    @PreAuthorize("@ss.hasPermi('system:template:query')")
    @PostMapping("/tryRun")
    public AjaxResult tryRun(@RequestBody TryRunBody body)
    {
        if (body == null)
        {
            return AjaxResult.error("请求体缺失");
        }
        if (body.modelCode == null || body.modelCode.isBlank())
        {
            return AjaxResult.error("请选择试跑模型");
        }
        Map<String, Object> vars = body.vars == null ? new HashMap<>(2) : body.vars;

        RenderedPrompt rp;
        if (body.templateCode != null && !body.templateCode.isBlank())
        {
            rp = promptTemplateService.renderByCode(body.templateCode, body.modelCode, vars);
        }
        else if (body.sceneType != null && !body.sceneType.isBlank())
        {
            rp = promptTemplateService.render(body.sceneType, body.modelCode, vars);
        }
        else
        {
            return AjaxResult.error("请指定 sceneType 或 templateCode");
        }

        String user = rp.getUserPrompt();
        if (body.userInput != null && !body.userInput.isBlank())
        {
            user = user + "\n\n" + body.userInput;
        }

        String output = aiModelClient.chat(body.modelCode, rp.getSystemPrompt(), user);

        Map<String, Object> data = new HashMap<>(4);
        data.put("output", output);
        data.put("source", rp.getSource());
        data.put("systemPrompt", rp.getSystemPrompt());
        data.put("userPrompt", user);
        return AjaxResult.success(data);
    }

    /**
     * 克隆模板为新版本：复制一份「非默认 + 停用」的副本，便于在线上生效模板之外试验新提示词。
     */
    @PreAuthorize("@ss.hasPermi('system:template:add')")
    @Log(title = "Prompt模板", businessType = BusinessType.INSERT)
    @PostMapping("/clone")
    public AjaxResult clone(@RequestBody Map<String, Object> body)
    {
        Object idObj = body == null ? null : body.get("templateId");
        if (idObj == null)
        {
            return AjaxResult.error("缺少 templateId");
        }
        Long templateId = Long.valueOf(String.valueOf(idObj));
        Long newId = aiPromptTemplateService.clone(templateId);
        if (newId == null)
        {
            return AjaxResult.error("克隆失败：源模板不存在");
        }
        return AjaxResult.success(newId);
    }

    /**
     * 互斥设为默认（版本回滚）：同场景其他模板置非默认，本模板置默认 + 启用，并立即清空缓存。
     */
    @PreAuthorize("@ss.hasPermi('system:template:edit')")
    @Log(title = "Prompt模板", businessType = BusinessType.UPDATE)
    @PostMapping("/setDefault")
    public AjaxResult setDefault(@RequestBody Map<String, Object> body)
    {
        Object idObj = body == null ? null : body.get("templateId");
        if (idObj == null)
        {
            return AjaxResult.error("缺少 templateId");
        }
        Long templateId = Long.valueOf(String.valueOf(idObj));
        int n = aiPromptTemplateService.setDefault(templateId);
        promptTemplateService.clearCache();
        return toAjax(n);
    }

    /** 试跑请求体 */
    public static class TryRunBody
    {
        public String sceneType;
        public String templateCode;
        public String modelCode;
        public String userInput;
        public Map<String, Object> vars;
    }
}
