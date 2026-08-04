package com.ruoyi.ai.controller;

import java.util.List;
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
     * 新增Prompt模板
     */
    @PreAuthorize("@ss.hasPermi('system:template:add')")
    @Log(title = "Prompt模板", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody AiPromptTemplate aiPromptTemplate)
    {
        return toAjax(aiPromptTemplateService.insertAiPromptTemplate(aiPromptTemplate));
    }

    /**
     * 修改Prompt模板
     */
    @PreAuthorize("@ss.hasPermi('system:template:edit')")
    @Log(title = "Prompt模板", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody AiPromptTemplate aiPromptTemplate)
    {
        return toAjax(aiPromptTemplateService.updateAiPromptTemplate(aiPromptTemplate));
    }

    /**
     * 删除Prompt模板
     */
    @PreAuthorize("@ss.hasPermi('system:template:remove')")
    @Log(title = "Prompt模板", businessType = BusinessType.DELETE)
	@DeleteMapping("/{templateIds}")
    public AjaxResult remove(@PathVariable Long[] templateIds)
    {
        return toAjax(aiPromptTemplateService.deleteAiPromptTemplateByTemplateIds(templateIds));
    }
}
