package com.ruoyi.project.controller;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.io.IOException;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.utils.ParamValidator;
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
@Validated
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
     * 查询「我的项目」列表（我创建的 ∪ 我参与团队关联的项目）
     * 门户首页依赖此接口，故放开后台权限、仅要求登录态；数据按当前用户隔离
     */
    @GetMapping("/my")
    public TableDataInfo my(AiProject aiProject)
    {
        startPage();
        List<AiProject> list = aiProjectService.selectMyProjectList(aiProject,
                getUserId(), getUsername());
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
     * 生成项目上下文导出 token（24h 只读），供服务器终端 curl 拉取约定文件使用
     * 放开后台权限、仅要求登录态
     */
    @PostMapping("/{projectId}/export-token")
    public AjaxResult exportToken(@PathVariable("projectId") Long projectId)
    {
        String token = aiProjectService.createExportToken(projectId);
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("expireSeconds", 24 * 60 * 60);
        return success(data);
    }

    /**
     * 按目标工具格式输出项目上下文原始文本（AGENTS.md 及其镜像），供终端 curl 一行拉取
     * 该端点对匿名开放（permitAll），改由导出 token 做只读鉴权，避免服务器终端携带 JWT 的不便
     */
    @GetMapping("/{projectId}/context")
    public void devContext(@PathVariable("projectId") Long projectId,
                           @RequestParam(value = "fmt", defaultValue = "agents") String fmt,
                           @RequestParam(value = "token", required = false) String token,
                           HttpServletResponse response) throws IOException
    {
        Long validProjectId = aiProjectService.verifyExportToken(token);
        if (validProjectId == null || !validProjectId.equals(projectId))
        {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("text/plain;charset=utf-8");
            response.getWriter().write("导出 token 无效或已过期，请在平台「导出到开发工具」中重新生成。");
            return;
        }
        String text = aiProjectService.getProjectContextText(projectId, fmt);
        String filename = contextFileName(fmt);
        response.setContentType("text/markdown;charset=utf-8");
        response.setHeader("Content-Disposition", "inline; filename=\"" + filename + "\"");
        response.getWriter().write(text);
    }

    /** fmt -> 约定文件名（与前端 exportDevContext.js 及菜单映射保持一致） */
    private String contextFileName(String fmt)
    {
        if ("claude".equalsIgnoreCase(fmt)) return "CLAUDE.md";
        if ("cursor".equalsIgnoreCase(fmt)) return "devpivot.mdc";
        if ("trae".equalsIgnoreCase(fmt)) return "project_rules.md";
        if ("vscode".equalsIgnoreCase(fmt)) return "copilot-instructions.md";
        return "AGENTS.md";
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
