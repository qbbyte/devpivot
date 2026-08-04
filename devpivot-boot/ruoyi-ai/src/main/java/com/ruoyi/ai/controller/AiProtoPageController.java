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
import com.ruoyi.ai.domain.AiProtoPage;
import com.ruoyi.ai.service.IAiProtoPageService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 原型页面Controller
 * 
 * @author ruoyi
 * @date 2026-08-04
 */
@RestController
@RequestMapping("/system/page")
public class AiProtoPageController extends BaseController
{
    @Autowired
    private IAiProtoPageService aiProtoPageService;

    /**
     * 查询原型页面列表
     */
    @PreAuthorize("@ss.hasPermi('system:page:list')")
    @GetMapping("/list")
    public TableDataInfo list(AiProtoPage aiProtoPage)
    {
        startPage();
        List<AiProtoPage> list = aiProtoPageService.selectAiProtoPageList(aiProtoPage);
        return getDataTable(list);
    }

    /**
     * 导出原型页面列表
     */
    @PreAuthorize("@ss.hasPermi('system:page:export')")
    @Log(title = "原型页面", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, AiProtoPage aiProtoPage)
    {
        List<AiProtoPage> list = aiProtoPageService.selectAiProtoPageList(aiProtoPage);
        ExcelUtil<AiProtoPage> util = new ExcelUtil<AiProtoPage>(AiProtoPage.class);
        util.exportExcel(response, list, "原型页面数据");
    }

    /**
     * 获取原型页面详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:page:query')")
    @GetMapping(value = "/{pageId}")
    public AjaxResult getInfo(@PathVariable("pageId") Long pageId)
    {
        return success(aiProtoPageService.selectAiProtoPageByPageId(pageId));
    }

    /**
     * 新增原型页面
     */
    @PreAuthorize("@ss.hasPermi('system:page:add')")
    @Log(title = "原型页面", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody AiProtoPage aiProtoPage)
    {
        return toAjax(aiProtoPageService.insertAiProtoPage(aiProtoPage));
    }

    /**
     * 修改原型页面
     */
    @PreAuthorize("@ss.hasPermi('system:page:edit')")
    @Log(title = "原型页面", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody AiProtoPage aiProtoPage)
    {
        return toAjax(aiProtoPageService.updateAiProtoPage(aiProtoPage));
    }

    /**
     * 删除原型页面
     */
    @PreAuthorize("@ss.hasPermi('system:page:remove')")
    @Log(title = "原型页面", businessType = BusinessType.DELETE)
	@DeleteMapping("/{pageIds}")
    public AjaxResult remove(@PathVariable Long[] pageIds)
    {
        return toAjax(aiProtoPageService.deleteAiProtoPageByPageIds(pageIds));
    }
}
