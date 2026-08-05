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
import com.ruoyi.project.domain.AiReqBaseline;
import com.ruoyi.project.service.IAiReqBaselineService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 需求基线Controller
 * 
 * @author ruoyi
 * @date 2026-08-04
 */
@RestController
@RequestMapping("/system/baseline")
public class AiReqBaselineController extends BaseController
{
    @Autowired
    private IAiReqBaselineService aiReqBaselineService;

    /**
     * 查询需求基线列表
     */
    @PreAuthorize("@ss.hasPermi('system:baseline:list')")
    @GetMapping("/list")
    public TableDataInfo list(AiReqBaseline aiReqBaseline)
    {
        startPage();
        List<AiReqBaseline> list = aiReqBaselineService.selectAiReqBaselineList(aiReqBaseline);
        return getDataTable(list);
    }

    /**
     * 导出需求基线列表
     */
    @PreAuthorize("@ss.hasPermi('system:baseline:export')")
    @Log(title = "需求基线", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, AiReqBaseline aiReqBaseline)
    {
        List<AiReqBaseline> list = aiReqBaselineService.selectAiReqBaselineList(aiReqBaseline);
        ExcelUtil<AiReqBaseline> util = new ExcelUtil<AiReqBaseline>(AiReqBaseline.class);
        util.exportExcel(response, list, "需求基线数据");
    }

    /**
     * 获取需求基线详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:baseline:query')")
    @GetMapping(value = "/{baselineId}")
    public AjaxResult getInfo(@PathVariable("baselineId") Long baselineId)
    {
        return success(aiReqBaselineService.selectAiReqBaselineByBaselineId(baselineId));
    }

    /**
     * 新增需求基线
     */
    @PreAuthorize("@ss.hasPermi('system:baseline:add')")
    @Log(title = "需求基线", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody AiReqBaseline aiReqBaseline)
    {
        return toAjax(aiReqBaselineService.insertAiReqBaseline(aiReqBaseline));
    }

    /**
     * 修改需求基线
     */
    @PreAuthorize("@ss.hasPermi('system:baseline:edit')")
    @Log(title = "需求基线", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody AiReqBaseline aiReqBaseline)
    {
        return toAjax(aiReqBaselineService.updateAiReqBaseline(aiReqBaseline));
    }

    /**
     * 删除需求基线
     */
    @PreAuthorize("@ss.hasPermi('system:baseline:remove')")
    @Log(title = "需求基线", businessType = BusinessType.DELETE)
	@DeleteMapping("/{baselineIds}")
    public AjaxResult remove(@PathVariable Long[] baselineIds)
    {
        return toAjax(aiReqBaselineService.deleteAiReqBaselineByBaselineIds(baselineIds));
    }
}
