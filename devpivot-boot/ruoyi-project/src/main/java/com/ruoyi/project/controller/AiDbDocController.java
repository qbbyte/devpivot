package com.ruoyi.project.controller;

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
import com.ruoyi.project.domain.AiDbDoc;
import com.ruoyi.project.service.IAiDbDocService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 数据库设计文档Controller
 *
 * @author devpivot
 * @date 2026-08-30
 */
@RestController
@RequestMapping("/system/dbdoc")
public class AiDbDocController extends BaseController
{
    @Autowired
    private IAiDbDocService aiDbDocService;

    /**
     * 查询数据库设计文档列表
     */
    @PreAuthorize("@ss.hasPermi('system:dbdoc:list')")
    @GetMapping("/list")
    public TableDataInfo list(AiDbDoc aiDbDoc)
    {
        startPage();
        List<AiDbDoc> list = aiDbDocService.selectAiDbDocList(aiDbDoc);
        return getDataTable(list);
    }

    /**
     * 导出数据库设计文档列表
     */
    @PreAuthorize("@ss.hasPermi('system:dbdoc:export')")
    @Log(title = "数据库设计文档", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, AiDbDoc aiDbDoc)
    {
        List<AiDbDoc> list = aiDbDocService.selectAiDbDocList(aiDbDoc);
        ExcelUtil<AiDbDoc> util = new ExcelUtil<AiDbDoc>(AiDbDoc.class);
        util.exportExcel(response, list, "数据库设计文档数据");
    }

    /**
     * 获取数据库设计文档详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:dbdoc:query')")
    @GetMapping(value = "/{docId}")
    public AjaxResult getInfo(@PathVariable("docId") Long docId)
    {
        return success(aiDbDocService.selectAiDbDocByDocId(docId));
    }

    /**
     * 新增数据库设计文档
     */
    @PreAuthorize("@ss.hasPermi('system:dbdoc:add')")
    @Log(title = "数据库设计文档", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody AiDbDoc aiDbDoc)
    {
        return toAjax(aiDbDocService.insertAiDbDoc(aiDbDoc));
    }

    /**
     * 修改数据库设计文档
     */
    @PreAuthorize("@ss.hasPermi('system:dbdoc:edit')")
    @Log(title = "数据库设计文档", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody AiDbDoc aiDbDoc)
    {
        return toAjax(aiDbDocService.updateAiDbDoc(aiDbDoc));
    }

    /**
     * 删除数据库设计文档
     */
    @PreAuthorize("@ss.hasPermi('system:dbdoc:remove')")
    @Log(title = "数据库设计文档", businessType = BusinessType.DELETE)
	@DeleteMapping("/{docIds}")
    public AjaxResult remove(@PathVariable Long[] docIds)
    {
        return toAjax(aiDbDocService.deleteAiDbDocByDocIds(docIds));
    }
}
