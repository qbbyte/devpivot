package com.ruoyi.project.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.project.domain.AiTeam;
import com.ruoyi.project.domain.AiTeamMessage;
import com.ruoyi.project.domain.AiTeamMember;
import com.ruoyi.project.domain.AiTeamProject;
import com.ruoyi.project.service.IAiTeamService;

/**
 * 团队模块Controller(门户侧,仅登录即可访问)
 * 
 * @author devpivot
 * @date 2026-08-09
 */
@RestController
@RequestMapping("/team")
public class AiTeamController extends BaseController
{
    @Autowired
    private IAiTeamService teamService;

    /** 我的团队列表 */
    @GetMapping("/mine")
    public AjaxResult mine()
    {
        Long userId = SecurityUtils.getUserId();
        return success(teamService.listMyTeams(userId));
    }

    /** 团队详情 */
    @GetMapping("/{teamId}")
    public AjaxResult detail(@PathVariable("teamId") Long teamId)
    {
        Long userId = SecurityUtils.getUserId();
        return success(teamService.getTeamDetail(teamId, userId));
    }

    /** 团队成员分页列表(若依分页范式: startPage + getDataTable) */
    @GetMapping("/{teamId}/members")
    public TableDataInfo members(@PathVariable("teamId") Long teamId)
    {
        Long userId = SecurityUtils.getUserId();
        startPage();
        List<AiTeamMember> list = teamService.listMembers(teamId, userId);
        return getDataTable(list);
    }

    /** 团队关联项目分页列表 */
    @GetMapping("/{teamId}/projects")
    public TableDataInfo projects(@PathVariable("teamId") Long teamId)
    {
        Long userId = SecurityUtils.getUserId();
        startPage();
        List<AiTeamProject> list = teamService.listProjects(teamId, userId);
        return getDataTable(list);
    }

    /** 创建团队 */
    @PostMapping
    public AjaxResult create(@RequestBody AiTeam team)
    {
        Long userId = SecurityUtils.getUserId();
        Long teamId = teamService.createTeam(team, userId, SecurityUtils.getUsername());
        return success(teamService.getTeamDetail(teamId, userId));
    }

    /** 编辑团队 */
    @PutMapping
    public AjaxResult edit(@RequestBody AiTeam team)
    {
        Long userId = SecurityUtils.getUserId();
        teamService.updateTeam(team, userId);
        return success();
    }

    /** 解散团队 */
    @DeleteMapping("/{teamId}")
    public AjaxResult dissolve(@PathVariable("teamId") Long teamId)
    {
        Long userId = SecurityUtils.getUserId();
        teamService.dissolveTeam(teamId, userId);
        return success();
    }

    /** 添加成员 */
    @PostMapping("/{teamId}/member")
    public AjaxResult addMember(@PathVariable("teamId") Long teamId,
                                @RequestParam("userId") Long userId,
                                @RequestParam(value = "role", required = false) String role)
    {
        Long operatorId = SecurityUtils.getUserId();
        teamService.addMember(teamId, userId, role, operatorId);
        return success();
    }

    /** 移除成员 */
    @DeleteMapping("/{teamId}/member/{userId}")
    public AjaxResult removeMember(@PathVariable("teamId") Long teamId,
                                   @PathVariable("userId") Long userId)
    {
        Long operatorId = SecurityUtils.getUserId();
        teamService.removeMember(teamId, userId, operatorId);
        return success();
    }

    /** 修改成员角色 */
    @PutMapping("/{teamId}/member/{userId}/role")
    public AjaxResult changeRole(@PathVariable("teamId") Long teamId,
                                 @PathVariable("userId") Long userId,
                                 @RequestParam("role") String role)
    {
        Long operatorId = SecurityUtils.getUserId();
        teamService.changeMemberRole(teamId, userId, role, operatorId);
        return success();
    }

    /** 关联项目 */
    @PostMapping("/{teamId}/project")
    public AjaxResult bindProject(@PathVariable("teamId") Long teamId,
                                  @RequestParam("projectId") Long projectId)
    {
        Long operatorId = SecurityUtils.getUserId();
        teamService.bindProject(teamId, projectId, operatorId);
        return success();
    }

    /** 解绑项目 */
    @DeleteMapping("/{teamId}/project/{projectId}")
    public AjaxResult unbindProject(@PathVariable("teamId") Long teamId,
                                    @PathVariable("projectId") Long projectId)
    {
        Long operatorId = SecurityUtils.getUserId();
        teamService.unbindProject(teamId, projectId, operatorId);
        return success();
    }

    /** 发送讨论消息 */
    @PostMapping("/{teamId}/message")
    public AjaxResult sendMessage(@PathVariable("teamId") Long teamId,
                                  @RequestParam("content") String content)
    {
        Long userId = SecurityUtils.getUserId();
        AiTeamMessage msg = teamService.sendMessage(teamId, userId, content);
        return success(msg);
    }

    /** 标记已读(不传 msgIds 则标记全部未读) */
    @PostMapping("/{teamId}/message/read")
    public AjaxResult markRead(@PathVariable("teamId") Long teamId,
                               @RequestBody(required = false) List<Long> msgIds)
    {
        Long userId = SecurityUtils.getUserId();
        teamService.markRead(teamId, userId, msgIds);
        return success();
    }

    /** 检索平台用户目录 */
    @GetMapping("/user-search")
    public AjaxResult userSearch(@RequestParam(value = "keyword", required = false) String keyword)
    {
        return success(teamService.searchUsers(keyword));
    }

    /** 项目下拉选项(供关联项目选择器) */
    @GetMapping("/project-options")
    public AjaxResult projectOptions()
    {
        return success(teamService.listProjectOptions());
    }

    /** 轻量拉取团队消息(供讨论区轮询刷新) */
    @GetMapping("/{teamId}/messages")
    public AjaxResult messages(@PathVariable("teamId") Long teamId)
    {
        Long userId = SecurityUtils.getUserId();
        return success(teamService.listMessages(teamId, userId));
    }

    /** 退出团队(非创建者主动退出) */
    @DeleteMapping("/{teamId}/leave")
    public AjaxResult leave(@PathVariable("teamId") Long teamId)
    {
        Long userId = SecurityUtils.getUserId();
        teamService.leaveTeam(teamId, userId);
        return success();
    }
}
