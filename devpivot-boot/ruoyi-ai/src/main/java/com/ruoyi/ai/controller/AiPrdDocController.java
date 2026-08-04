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
import com.ruoyi.ai.domain.AiPrdDoc;
import com.ruoyi.ai.service.IAiPrdDocService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * PRD需求文档Controller
 * 
 * @author ruoyi
 * @date 2026-08-04
 */
@RestController
@RequestMapping("/system/doc")
public class AiPrdDocController extends BaseController
{
    @Autowired
    private IAiPrdDocService aiPrdDocService;

    /**
     * 查询PRD需求文档列表
     */
    @PreAuthorize("@ss.hasPermi('system:doc:list')")
    @GetMapping("/list")
    public TableDataInfo list(AiPrdDoc aiPrdDoc)
    {
        startPage();
        List<AiPrdDoc> list = aiPrdDocService.selectAiPrdDocList(aiPrdDoc);
        return getDataTable(list);
    }

    /**
     * 导出PRD需求文档列表
     */
    @PreAuthorize("@ss.hasPermi('system:doc:export')")
    @Log(title = "PRD需求文档", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, AiPrdDoc aiPrdDoc)
    {
        List<AiPrdDoc> list = aiPrdDocService.selectAiPrdDocList(aiPrdDoc);
        ExcelUtil<AiPrdDoc> util = new ExcelUtil<AiPrdDoc>(AiPrdDoc.class);
        util.exportExcel(response, list, "PRD需求文档数据");
    }

    /**
     * 获取PRD需求文档详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:doc:query')")
    @GetMapping(value = "/{docId}")
    public AjaxResult getInfo(@PathVariable("docId") Long docId)
    {
        return success(aiPrdDocService.selectAiPrdDocByDocId(docId));
    }

    /**
     * 新增PRD需求文档
     */
    @PreAuthorize("@ss.hasPermi('system:doc:add')")
    @Log(title = "PRD需求文档", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody AiPrdDoc aiPrdDoc)
    {
        return toAjax(aiPrdDocService.insertAiPrdDoc(aiPrdDoc));
    }

    /**
     * 修改PRD需求文档
     */
    @PreAuthorize("@ss.hasPermi('system:doc:edit')")
    @Log(title = "PRD需求文档", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody AiPrdDoc aiPrdDoc)
    {
        return toAjax(aiPrdDocService.updateAiPrdDoc(aiPrdDoc));
    }

    /**
     * 删除PRD需求文档
     */
    @PreAuthorize("@ss.hasPermi('system:doc:remove')")
    @Log(title = "PRD需求文档", businessType = BusinessType.DELETE)
	@DeleteMapping("/{docIds}")
    public AjaxResult remove(@PathVariable Long[] docIds)
    {
        return toAjax(aiPrdDocService.deleteAiPrdDocByDocIds(docIds));
    }
}
