package com.ruoyi.project.controller;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.project.domain.AiTeam;
import com.ruoyi.project.domain.AiTeamMember;
import com.ruoyi.project.domain.AiTeamProject;
import com.ruoyi.project.mapper.AiTeamMapper;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 团队管理Controller（管理端）
 *
 * 管理端只做审阅与治理：查看全量团队、成员构成、关联项目，必要时解散违规团队。
 * 团队的日常自助操作（建团/邀请/绑定项目等）走门户 /team 接口。
 *
 * @author devpivot
 * @date 2026-08-30
 */
@RestController
@RequestMapping("/system/team")
public class AiTeamAdminController extends BaseController
{
    @Autowired
    private AiTeamMapper aiTeamMapper;

    /**
     * 查询全量团队列表
     */
    @PreAuthorize("@ss.hasPermi('system:team:list')")
    @GetMapping("/list")
    public TableDataInfo list(AiTeam aiTeam)
    {
        startPage();
        List<AiTeam> list = aiTeamMapper.selectAllTeams(aiTeam);
        return getDataTable(list);
    }

    /**
     * 查询团队成员列表
     */
    @PreAuthorize("@ss.hasPermi('system:team:query')")
    @GetMapping("/{teamId}/members")
    public TableDataInfo members(@PathVariable("teamId") Long teamId)
    {
        List<AiTeamMember> list = aiTeamMapper.selectMembersByTeamId(teamId);
        return getDataTable(list);
    }

    /**
     * 查询团队关联项目列表
     */
    @PreAuthorize("@ss.hasPermi('system:team:query')")
    @GetMapping("/{teamId}/projects")
    public TableDataInfo projects(@PathVariable("teamId") Long teamId)
    {
        List<AiTeamProject> list = aiTeamMapper.selectProjectsByTeamId(teamId);
        return getDataTable(list);
    }

    /**
     * 解散团队（治理操作，软删除）
     */
    @PreAuthorize("@ss.hasPermi('system:team:remove')")
    @Log(title = "团队管理", businessType = BusinessType.DELETE)
	@DeleteMapping("/{teamId}")
    public AjaxResult dissolve(@PathVariable Long teamId)
    {
        return toAjax(aiTeamMapper.dissolveTeam(teamId));
    }
}
