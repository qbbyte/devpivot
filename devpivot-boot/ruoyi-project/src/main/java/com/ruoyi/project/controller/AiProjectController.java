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
import com.ruoyi.project.domain.AiProject;
import com.ruoyi.project.service.IAiProjectService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * AI项目Controller
 * 
 * @author ruoyi
 * @date 2026-08-04
 */
@RestController
@RequestMapping("/system/project")
public class AiProjectController extends BaseController
{
    @Autowired
    private IAiProjectService aiProjectService;

    /**
     * 查询AI项目列表
     */
    @PreAuthorize("@ss.hasPermi('system:project:list')")
    @GetMapping("/list")
    public TableDataInfo list(AiProject aiProject)
    {
        startPage();
        List<AiProject> list = aiProjectService.selectAiProjectList(aiProject);
        return getDataTable(list);
    }

    /**
     * 导出AI项目列表
     */
    @PreAuthorize("@ss.hasPermi('system:project:export')")
    @Log(title = "AI项目", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, AiProject aiProject)
    {
        List<AiProject> list = aiProjectService.selectAiProjectList(aiProject);
        ExcelUtil<AiProject> util = new ExcelUtil<AiProject>(AiProject.class);
        util.exportExcel(response, list, "AI项目数据");
    }

    /**
     * 获取AI项目详细信息
     * 注：门户项目总览页(/portal/project/:id)也依赖此接口，故放开后台权限，仅要求登录态即可访问
     */
    @GetMapping(value = "/{projectId}")
    public AjaxResult getInfo(@PathVariable("projectId") Long projectId)
    {
        return success(aiProjectService.selectAiProjectByProjectId(projectId));
    }

    /**
     * 项目阶段概览（含每阶段状态与实现人）
     * 门户团队页点击项目后以弹窗展示，放开后台权限、仅要求登录态
     */
    @GetMapping("/{projectId}/phases")
    public AjaxResult phases(@PathVariable("projectId") Long projectId)
    {
        return success(aiProjectService.getProjectPhases(projectId));
    }

    /**
     * 项目产物概览（聚合各阶段产物文本，门户团队页「产物」按钮调用）
     * 放开后台权限、仅要求登录态
     */
    @GetMapping("/{projectId}/artifacts")
    public AjaxResult artifacts(@PathVariable("projectId") Long projectId)
    {
        return success(aiProjectService.getProjectArtifacts(projectId));
    }

    /**
     * 新增AI项目
     */
    @PreAuthorize("@ss.hasPermi('system:project:add')")
    @Log(title = "AI项目", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody AiProject aiProject)
    {
        return toAjax(aiProjectService.insertAiProject(aiProject));
    }

    /**
     * 修改AI项目
     */
    @PreAuthorize("@ss.hasPermi('system:project:edit')")
    @Log(title = "AI项目", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody AiProject aiProject)
    {
        return toAjax(aiProjectService.updateAiProject(aiProject));
    }

    /**
     * 删除AI项目
     */
    @PreAuthorize("@ss.hasPermi('system:project:remove')")
    @Log(title = "AI项目", businessType = BusinessType.DELETE)
	@DeleteMapping("/{projectIds}")
    public AjaxResult remove(@PathVariable Long[] projectIds)
    {
        return toAjax(aiProjectService.deleteAiProjectByProjectIds(projectIds));
    }
}
