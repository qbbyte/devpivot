package com.ruoyi.project.controller;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.utils.ParamValidator;
import com.ruoyi.project.domain.AiArtifactVersion;
import com.ruoyi.project.service.IAiArtifactVersionService;

/**
 * 结果物版本 · 数据接口（/portal/version）
 * 仅承载版本数据的读写/发布/还原/删除/对比（纯数据，无 AI/流式）。
 * 鉴权在 service 层（ProjectAccessService：Reader 查看 / Writer 保存还原发布 / Manager 删除）。
 *
 * @author devpivot
 * @date 2026-08-26
 */
@RestController
@RequestMapping("/portal/version")
public class VersionController extends BaseController
{
    @Autowired
    private IAiArtifactVersionService versionService;

    /**
     * 版本列表（不含快照），分页
     */
    @GetMapping("/list")
    public TableDataInfo list(@RequestParam Long projectId, String stage, String status)
    {
        ParamValidator.projectId(projectId);
        startPage();
        List<AiArtifactVersion> list = versionService.selectVersionList(projectId, stage, status);
        return getDataTable(list);
    }

    /**
     * 版本详情（含快照）
     */
    @GetMapping("/detail/{versionId}")
    public AjaxResult detail(@PathVariable Long versionId)
    {
        return success(versionService.selectVersionDetail(versionId));
    }

    /**
     * 两版本结构化 diff
     */
    @GetMapping("/diff")
    public AjaxResult diff(@RequestParam Long fromId, @RequestParam Long toId)
    {
        return success(versionService.diffVersions(fromId, toId));
    }

    /**
     * 保存新版本
     * body: { stage, artifactType, versionName, snapshot, sourceType, sourceModel, changeRemark }
     */
    @PostMapping("/save/{projectId}")
    public AjaxResult save(@PathVariable Long projectId, @RequestBody Map<String, Object> body)
    {
        ParamValidator.projectId(projectId);
        return success(versionService.saveVersion(projectId, body));
    }

    /**
     * 发布版本（DRAFT -> RELEASED）
     */
    @PostMapping("/release/{versionId}")
    public AjaxResult release(@PathVariable Long versionId)
    {
        return success(versionService.releaseVersion(versionId));
    }

    /**
     * 还原版本（快照写回业务表并生成新版本）
     */
    @PostMapping("/restore/{versionId}")
    public AjaxResult restore(@PathVariable Long versionId)
    {
        return success(versionService.restoreVersion(versionId));
    }

    /**
     * 删除版本（仅 Manager）
     */
    @DeleteMapping("/{versionId}")
    public AjaxResult remove(@PathVariable Long versionId)
    {
        return toAjax(versionService.deleteVersion(versionId));
    }
}
