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
import com.ruoyi.project.domain.AiTechDoc;
import com.ruoyi.project.service.IAiTechDocService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 技术方案文档Controller
 * 
 * @author ruoyi
 * @date 2026-08-04
 */
@RestController
@RequestMapping("/system/techdoc")
public class AiTechDocController extends BaseController
{
    @Autowired
    private IAiTechDocService aiTechDocService;

    /**
     * 查询技术方案文档列表
     */
    @PreAuthorize("@ss.hasPermi('system:techdoc:list')")
    @GetMapping("/list")
    public TableDataInfo list(AiTechDoc aiTechDoc)
    {
        startPage();
        List<AiTechDoc> list = aiTechDocService.selectAiTechDocList(aiTechDoc);
        return getDataTable(list);
    }

    /**
     * 导出技术方案文档列表
     */
    @PreAuthorize("@ss.hasPermi('system:techdoc:export')")
    @Log(title = "技术方案文档", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, AiTechDoc aiTechDoc)
    {
        List<AiTechDoc> list = aiTechDocService.selectAiTechDocList(aiTechDoc);
        ExcelUtil<AiTechDoc> util = new ExcelUtil<AiTechDoc>(AiTechDoc.class);
        util.exportExcel(response, list, "技术方案文档数据");
    }

    /**
     * 获取技术方案文档详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:techdoc:query')")
    @GetMapping(value = "/{docId}")
    public AjaxResult getInfo(@PathVariable("docId") Long docId)
    {
        return success(aiTechDocService.selectAiTechDocByDocId(docId));
    }

    /**
     * 新增技术方案文档
     */
    @PreAuthorize("@ss.hasPermi('system:techdoc:add')")
    @Log(title = "技术方案文档", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody AiTechDoc aiTechDoc)
    {
        return toAjax(aiTechDocService.insertAiTechDoc(aiTechDoc));
    }

    /**
     * 修改技术方案文档
     */
    @PreAuthorize("@ss.hasPermi('system:techdoc:edit')")
    @Log(title = "技术方案文档", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody AiTechDoc aiTechDoc)
    {
        return toAjax(aiTechDocService.updateAiTechDoc(aiTechDoc));
    }

    /**
     * 删除技术方案文档
     */
    @PreAuthorize("@ss.hasPermi('system:techdoc:remove')")
    @Log(title = "技术方案文档", businessType = BusinessType.DELETE)
	@DeleteMapping("/{docIds}")
    public AjaxResult remove(@PathVariable Long[] docIds)
    {
        return toAjax(aiTechDocService.deleteAiTechDocByDocIds(docIds));
    }
}
