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
import com.ruoyi.project.domain.AiArchDoc;
import com.ruoyi.project.service.IAiArchDocService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 系统架构设计文档Controller
 *
 * @author devpivot
 * @date 2026-08-30
 */
@RestController
@RequestMapping("/system/archdoc")
public class AiArchDocController extends BaseController
{
    @Autowired
    private IAiArchDocService aiArchDocService;

    /**
     * 查询系统架构设计文档列表
     */
    @PreAuthorize("@ss.hasPermi('system:archdoc:list')")
    @GetMapping("/list")
    public TableDataInfo list(AiArchDoc aiArchDoc)
    {
        startPage();
        List<AiArchDoc> list = aiArchDocService.selectAiArchDocList(aiArchDoc);
        return getDataTable(list);
    }

    /**
     * 导出系统架构设计文档列表
     */
    @PreAuthorize("@ss.hasPermi('system:archdoc:export')")
    @Log(title = "系统架构设计文档", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, AiArchDoc aiArchDoc)
    {
        List<AiArchDoc> list = aiArchDocService.selectAiArchDocList(aiArchDoc);
        ExcelUtil<AiArchDoc> util = new ExcelUtil<AiArchDoc>(AiArchDoc.class);
        util.exportExcel(response, list, "系统架构设计文档数据");
    }

    /**
     * 获取系统架构设计文档详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:archdoc:query')")
    @GetMapping(value = "/{docId}")
    public AjaxResult getInfo(@PathVariable("docId") Long docId)
    {
        return success(aiArchDocService.selectAiArchDocByDocId(docId));
    }

    /**
     * 新增系统架构设计文档
     */
    @PreAuthorize("@ss.hasPermi('system:archdoc:add')")
    @Log(title = "系统架构设计文档", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody AiArchDoc aiArchDoc)
    {
        return toAjax(aiArchDocService.insertAiArchDoc(aiArchDoc));
    }

    /**
     * 修改系统架构设计文档
     */
    @PreAuthorize("@ss.hasPermi('system:archdoc:edit')")
    @Log(title = "系统架构设计文档", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody AiArchDoc aiArchDoc)
    {
        return toAjax(aiArchDocService.updateAiArchDoc(aiArchDoc));
    }

    /**
     * 删除系统架构设计文档
     */
    @PreAuthorize("@ss.hasPermi('system:archdoc:remove')")
    @Log(title = "系统架构设计文档", businessType = BusinessType.DELETE)
	@DeleteMapping("/{docIds}")
    public AjaxResult remove(@PathVariable Long[] docIds)
    {
        return toAjax(aiArchDocService.deleteAiArchDocByDocIds(docIds));
    }
}
