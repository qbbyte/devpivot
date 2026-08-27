package com.ruoyi.project.service;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.project.domain.AiProject;
import com.ruoyi.project.domain.AiTeamMember;
import com.ruoyi.project.mapper.AiTeamMapper;

/**
 * 项目级访问控制（service 层三级鉴权，延续团队模块约定，Controller 不挂 @PreAuthorize）
 * <p>
 * 角色判定链：ai_project.create_by（OWNER）→ ai_team_project → ai_team_member.role（ADMIN/MEMBER）
 * <ul>
 *   <li>Reader：项目创建者或团队成员 —— 查看版本/历史</li>
 *   <li>Writer：同 Reader（协作编辑语义） —— 保存版本/还原/发布/内容编辑</li>
 *   <li>Manager：项目创建者或团队 ADMIN/OWNER —— 删除版本/归档</li>
 * </ul>
 *
 * @author devpivot
 * @date 2026-08-26
 */
@Service
public class ProjectAccessService
{
    private static final Logger log = LoggerFactory.getLogger(ProjectAccessService.class);

    /** 团队内管理员角色（OWNER 团队创建者 / ADMIN 团队管理员） */
    private static final String ROLE_OWNER = "OWNER";
    private static final String ROLE_ADMIN = "ADMIN";

    @Autowired
    private IAiProjectService projectService;

    @Autowired
    private AiTeamMapper teamMapper;

    /**
     * 解析当前登录用户在项目的角色
     *
     * @return OWNER（项目创建者）/ ADMIN / MEMBER / null（无权）
     */
    public String resolveRole(Long projectId)
    {
        AiProject project = projectService.selectAiProjectByProjectId(projectId);
        if (project == null)
        {
            throw new ServiceException("项目不存在");
        }
        Long userId = SecurityUtils.getUserId();
        String username = SecurityUtils.getUsername();
        // 项目创建者 = 最高权限
        if (username != null && username.equals(project.getCreateBy()))
        {
            return ROLE_OWNER;
        }
        // 按项目反查绑定团队，命中任一团队成员即有权
        List<Long> teamIds = teamMapper.selectTeamIdsByProjectId(projectId);
        if (teamIds != null)
        {
            for (Long teamId : teamIds)
            {
                try
                {
                    AiTeamMember me = teamMapper.selectMember(teamId, userId);
                    if (me != null)
                    {
                        return me.getRole();
                    }
                }
                catch (Exception e)
                {
                    log.warn("[access] 查询团队成员失败 teamId={} userId={}", teamId, userId, e);
                }
            }
        }
        return null;
    }

    /** 可读：项目创建者或团队成员 */
    public void assertReader(Long projectId)
    {
        if (resolveRole(projectId) == null)
        {
            throw new ServiceException("无权访问该项目");
        }
    }

    /** 可写：同 Reader（协作编辑语义） */
    public void assertWriter(Long projectId)
    {
        assertReader(projectId);
    }

    /** 管理：项目创建者或团队管理员（ADMIN/OWNER） */
    public void assertManager(Long projectId)
    {
        String role = resolveRole(projectId);
        if (!ROLE_OWNER.equals(role) && !ROLE_ADMIN.equals(role))
        {
            throw new ServiceException("仅项目负责人或团队管理员可执行该操作");
        }
    }
}
