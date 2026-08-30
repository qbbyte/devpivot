package com.ruoyi.project.controller;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.project.domain.AiArtifactVersion;
import com.ruoyi.project.service.IAiArtifactVersionService;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 产物版本管理Controller
 *
 * @author devpivot
 * @date 2026-08-30
 */
@RestController
@RequestMapping("/system/artifact")
public class AiArtifactVersionController extends BaseController
{
    @Autowired
    private IAiArtifactVersionService aiArtifactVersionService;

    /**
     * 查询产物版本列表
     */
    @PreAuthorize("@ss.hasPermi('system:artifact:list')")
    @GetMapping("/list")
    public TableDataInfo list(@RequestParam(value = "projectId", required = false) Long projectId,
                              @RequestParam(value = "stage", required = false) String stage,
                              @RequestParam(value = "status", required = false) String status)
    {
        startPage();
        List<AiArtifactVersion> list = aiArtifactVersionService.selectAdminVersionList(projectId, stage, status);
        return getDataTable(list);
    }

    /**
     * 获取产物版本详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:artifact:query')")
    @GetMapping(value = "/{versionId}")
    public AjaxResult getInfo(@PathVariable("versionId") Long versionId)
    {
        return success(aiArtifactVersionService.selectAdminVersionDetail(versionId));
    }

    /**
     * 发布产物版本
     */
    @PreAuthorize("@ss.hasPermi('system:artifact:release')")
    @Log(title = "产物版本", businessType = BusinessType.UPDATE)
    @PutMapping("/{versionId}/release")
    public AjaxResult release(@PathVariable("versionId") Long versionId)
    {
        return success(aiArtifactVersionService.releaseAdminVersion(versionId));
    }

    /**
     * 恢复产物版本
     */
    @PreAuthorize("@ss.hasPermi('system:artifact:restore')")
    @Log(title = "产物版本", businessType = BusinessType.UPDATE)
    @PutMapping("/{versionId}/restore")
    public AjaxResult restore(@PathVariable("versionId") Long versionId)
    {
        return success(aiArtifactVersionService.restoreAdminVersion(versionId));
    }

    /**
     * 删除产物版本
     */
    @PreAuthorize("@ss.hasPermi('system:artifact:remove')")
    @Log(title = "产物版本", businessType = BusinessType.DELETE)
	@DeleteMapping("/{versionId}")
    public AjaxResult remove(@PathVariable Long versionId)
    {
        return toAjax(aiArtifactVersionService.deleteAdminVersion(versionId));
    }
}
