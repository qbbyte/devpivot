package com.ruoyi.project.service.impl;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.text.SimpleDateFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.project.domain.AiTeam;
import com.ruoyi.project.domain.AiTeamMember;
import com.ruoyi.project.domain.AiTeamMessage;
import com.ruoyi.project.domain.AiTeamMessageRead;
import com.ruoyi.project.domain.AiTeamProject;
import com.ruoyi.project.mapper.AiTeamMapper;
import com.ruoyi.project.service.IAiTeamService;
import com.ruoyi.project.websocket.TeamReadEvent;

/**
 * 团队模块业务层实现
 * 
 * @author devpivot
 * @date 2026-08-09
 */
@Service
public class AiTeamServiceImpl implements IAiTeamService
{
    private static final String ROLE_OWNER = "OWNER";
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_MEMBER = "MEMBER";

    @Autowired
    private AiTeamMapper teamMapper;

    /** WebSocket 广播模板(团队讨论区实时推送) */
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Override
    public List<AiTeam> listMyTeams(Long userId)
    {
        return teamMapper.selectMyTeams(userId);
    }

    @Override
    public AiTeam getTeamDetail(Long teamId, Long userId)
    {
        AiTeam team = teamMapper.selectTeamDetail(teamId, userId);
        if (team == null)
        {
            throw new ServiceException("团队不存在或已解散");
        }
        List<AiTeamMember> members = teamMapper.selectMembersByTeamId(teamId);
        List<AiTeamProject> projects = teamMapper.selectProjectsByTeamId(teamId);
        List<AiTeamMessage> messages = teamMapper.selectMessagesByTeamId(teamId);
        for (AiTeamMessage msg : messages)
        {
            msg.setTime(formatTime(msg.getCreateTime()));
            msg.setReadUsers(new ArrayList<>());
        }
        // 聚合已读人(排除当前用户)
        List<Map<String, Object>> readers = teamMapper.selectReadersByTeamId(teamId, userId);
        Map<Long, List<Map<String, Object>>> readerMap = new HashMap<>();
        for (Map<String, Object> r : readers)
        {
            Long msgId = ((Number) r.get("msgId")).longValue();
            readerMap.computeIfAbsent(msgId, k -> new ArrayList<>()).add(r);
        }
        for (AiTeamMessage msg : messages)
        {
            List<Map<String, Object>> list = readerMap.get(msg.getMsgId());
            if (list != null)
            {
                msg.setReadUsers(list);
            }
        }
        team.setMembers(members);
        team.setProjects(projects);
        team.setMessages(messages);
        return team;
    }

    @Override
    public List<AiTeamMember> listMembers(Long teamId, Long userId)
    {
        // 复用详情中的成员查询;Controller 通过 startPage() 触发 PageHelper 分页
        return teamMapper.selectMembersByTeamId(teamId);
    }

    @Override
    public List<AiTeamProject> listProjects(Long teamId, Long userId)
    {
        return teamMapper.selectProjectsByTeamId(teamId);
    }

    @Override
    public Long createTeam(AiTeam team, Long userId, String username)
    {
        if (team.getTeamName() == null || team.getTeamName().trim().isEmpty())
        {
            throw new ServiceException("团队名称不能为空");
        }
        Date now = DateUtils.getNowDate();
        team.setTeamName(team.getTeamName().trim());
        if (team.getDescription() == null)
        {
            team.setDescription("");
        }
        team.setOwnerId(userId);
        team.setStatus("0");
        team.setDelFlag("0");
        team.setInviteCode(genUniqueInviteCode());
        team.setCreateBy(username);
        team.setCreateTime(now);
        teamMapper.insertTeam(team);
        Long teamId = team.getTeamId();
        AiTeamMember owner = new AiTeamMember();
        owner.setTeamId(teamId);
        owner.setUserId(userId);
        owner.setRole(ROLE_OWNER);
        owner.setCreateBy(username);
        owner.setCreateTime(now);
        teamMapper.insertMember(owner);
        return teamId;
    }

    @Override
    public void updateTeam(AiTeam team, Long userId)
    {
        assertManager(team.getTeamId(), userId);
        if (team.getTeamName() == null || team.getTeamName().trim().isEmpty())
        {
            throw new ServiceException("团队名称不能为空");
        }
        team.setTeamName(team.getTeamName().trim());
        if (team.getDescription() == null)
        {
            team.setDescription("");
        }
        team.setUpdateBy(SecurityUtils.getUsername());
        team.setUpdateTime(DateUtils.getNowDate());
        teamMapper.updateTeam(team);
    }

    @Override
    public void dissolveTeam(Long teamId, Long userId)
    {
        AiTeamMember me = assertMember(teamId, userId);
        if (!ROLE_OWNER.equals(me.getRole()))
        {
            throw new ServiceException("仅团队创建者可解散团队");
        }
        teamMapper.dissolveTeam(teamId);
    }

    @Override
    public void addMember(Long teamId, Long targetUserId, String role, Long operatorId)
    {
        assertManager(teamId, operatorId);
        if (targetUserId == null)
        {
            throw new ServiceException("成员用户ID不能为空");
        }
        if (teamMapper.selectMember(teamId, targetUserId) != null)
        {
            throw new ServiceException("该用户已是团队成员");
        }
        String realRole = (role == null || !isValidRole(role)) ? ROLE_MEMBER : role;
        AiTeamMember member = new AiTeamMember();
        member.setTeamId(teamId);
        member.setUserId(targetUserId);
        member.setRole(realRole);
        member.setCreateBy(SecurityUtils.getUsername());
        member.setCreateTime(DateUtils.getNowDate());
        teamMapper.insertMember(member);
    }

    @Override
    public void removeMember(Long teamId, Long targetUserId, Long operatorId)
    {
        assertManager(teamId, operatorId);
        if (targetUserId.equals(operatorId))
        {
            throw new ServiceException("不能移除自己");
        }
        AiTeamMember target = teamMapper.selectMember(teamId, targetUserId);
        if (target == null)
        {
            throw new ServiceException("成员不存在");
        }
        if (ROLE_OWNER.equals(target.getRole()))
        {
            throw new ServiceException("不能移除团队创建者");
        }
        teamMapper.deleteMember(teamId, targetUserId);
    }

    @Override
    public void changeMemberRole(Long teamId, Long targetUserId, String role, Long operatorId)
    {
        assertManager(teamId, operatorId);
        if (!isValidRole(role))
        {
            throw new ServiceException("角色不合法");
        }
        if (targetUserId.equals(operatorId))
        {
            throw new ServiceException("不能修改自己的角色");
        }
        AiTeamMember target = teamMapper.selectMember(teamId, targetUserId);
        if (target == null)
        {
            throw new ServiceException("成员不存在");
        }
        if (ROLE_OWNER.equals(target.getRole()))
        {
            throw new ServiceException("不能修改创建者角色");
        }
        teamMapper.updateMemberRole(teamId, targetUserId, role, SecurityUtils.getUsername(), DateUtils.getNowDate());
    }

    @Override
    public void bindProject(Long teamId, Long projectId, Long operatorId)
    {
        assertManager(teamId, operatorId);
        if (projectId == null)
        {
            throw new ServiceException("项目ID不能为空");
        }
        if (teamMapper.existsProject(teamId, projectId) > 0)
        {
            throw new ServiceException("该项目已关联本团队");
        }
        AiTeamProject project = new AiTeamProject();
        project.setTeamId(teamId);
        project.setProjectId(projectId);
        project.setCreateBy(SecurityUtils.getUsername());
        project.setCreateTime(DateUtils.getNowDate());
        teamMapper.insertProject(project);
    }

    @Override
    public void unbindProject(Long teamId, Long projectId, Long operatorId)
    {
        assertManager(teamId, operatorId);
        teamMapper.deleteProject(teamId, projectId);
    }

    @Override
    public AiTeamMessage sendMessage(Long teamId, Long userId, String content)
    {
        assertMember(teamId, userId);
        if (content == null || (content = content.trim()).isEmpty())
        {
            throw new ServiceException("消息内容不能为空");
        }
        if (content.length() > 500)
        {
            content = content.substring(0, 500);
        }
        Date now = DateUtils.getNowDate();
        AiTeamMessage msg = new AiTeamMessage();
        msg.setTeamId(teamId);
        msg.setUserId(userId);
        msg.setContent(content);
        msg.setCreateBy(SecurityUtils.getUsername());
        msg.setCreateTime(now);
        msg.setTime(formatTime(now));
        msg.setReadUsers(new ArrayList<>());
        // 补全发送者昵称/头像，避免前端手补；广播时所有订阅者拿到一致数据
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser != null && loginUser.getUser() != null)
        {
            msg.setNickName(loginUser.getUser().getNickName());
            msg.setAvatar(loginUser.getUser().getAvatar());
        }
        teamMapper.insertMessage(msg);
        // 实时推送：广播到该团队频道，所有在线成员即时收到
        messagingTemplate.convertAndSend("/topic/team/" + teamId, msg);
        return msg;
    }

    @Override
    public void markRead(Long teamId, Long userId, List<Long> msgIds)
    {
        List<Long> unread = teamMapper.selectUnreadMessageIds(teamId, userId, msgIds);
        if (unread == null || unread.isEmpty())
        {
            return;
        }
        Date now = DateUtils.getNowDate();
        List<AiTeamMessageRead> list = new ArrayList<>(unread.size());
        for (Long msgId : unread)
        {
            AiTeamMessageRead read = new AiTeamMessageRead();
            read.setMsgId(msgId);
            read.setTeamId(teamId);
            read.setUserId(userId);
            read.setCreateTime(now);
            list.add(read);
        }
        teamMapper.insertReadIgnore(list);
        // 实时推送已读事件：让其他成员即时看到"已读 N 人"更新
        LoginUser loginUser = SecurityUtils.getLoginUser();
        String readerNick = loginUser != null && loginUser.getUser() != null ? loginUser.getUser().getNickName() : null;
        TeamReadEvent event = new TeamReadEvent(teamId, unread, userId, readerNick);
        messagingTemplate.convertAndSend("/topic/team/" + teamId + "/read", event);
    }

    @Override
    public List<Map<String, Object>> searchUsers(String keyword)
    {
        return teamMapper.searchSysUser(keyword);
    }

    @Override
    public void leaveTeam(Long teamId, Long userId)
    {
        AiTeamMember me = assertMember(teamId, userId);
        if (ROLE_OWNER.equals(me.getRole()))
        {
            throw new ServiceException("创建者不能直接退出，请先转移所有权或解散团队");
        }
        teamMapper.deleteMember(teamId, userId);
    }

    @Override
    public String joinByInviteCode(String inviteCode, Long userId, String username)
    {
        if (inviteCode == null || inviteCode.trim().isEmpty())
        {
            throw new ServiceException("邀请码不能为空");
        }
        AiTeam team = teamMapper.selectByInviteCode(inviteCode.trim().toUpperCase());
        if (team == null)
        {
            throw new ServiceException("邀请码无效或团队已解散");
        }
        if ("1".equals(team.getStatus()))
        {
            throw new ServiceException("团队已解散，无法加入");
        }
        if (teamMapper.selectMember(team.getTeamId(), userId) != null)
        {
            throw new ServiceException("您已经是该团队成员");
        }
        Date now = DateUtils.getNowDate();
        AiTeamMember member = new AiTeamMember();
        member.setTeamId(team.getTeamId());
        member.setUserId(userId);
        member.setRole(ROLE_MEMBER);
        member.setCreateBy(username);
        member.setCreateTime(now);
        teamMapper.insertMember(member);
        return team.getTeamName();
    }

    @Override
    public Map<String, Object> getInviteInfo(String inviteCode, Long userId)
    {
        if (inviteCode == null || inviteCode.trim().isEmpty())
        {
            return null;
        }
        AiTeam team = teamMapper.selectByInviteCode(inviteCode.trim().toUpperCase());
        if (team == null || "1".equals(team.getStatus()))
        {
            return null;
        }
        Map<String, Object> info = new HashMap<>(4);
        info.put("teamId", team.getTeamId());
        info.put("teamName", team.getTeamName());
        info.put("joined", teamMapper.selectMember(team.getTeamId(), userId) != null);
        return info;
    }

    @Override
    public String refreshInviteCode(Long teamId, Long operatorId)
    {
        assertManager(teamId, operatorId);
        String code = genUniqueInviteCode();
        teamMapper.updateInviteCode(teamId, code, SecurityUtils.getUsername(), DateUtils.getNowDate());
        return code;
    }

    /** 生成唯一邀请码(8位大写, 先查库避免唯一索引冲突) */
    private String genUniqueInviteCode()
    {
        for (int i = 0; i < 5; i++)
        {
            String code = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
            if (teamMapper.selectByInviteCode(code) == null)
            {
                return code;
            }
        }
        // 极端兜底: 追加时间戳片避免碰撞
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase()
                + (System.currentTimeMillis() % 1000);
    }

    @Override
    public List<Map<String, Object>> listProjectOptions()
    {
        return teamMapper.selectProjectOptions();
    }

    @Override
    public List<AiTeamMessage> listMessages(Long teamId, Long userId)
    {
        assertMember(teamId, userId);
        List<AiTeamMessage> messages = teamMapper.selectMessagesByTeamId(teamId);
        for (AiTeamMessage msg : messages)
        {
            msg.setTime(formatTime(msg.getCreateTime()));
            msg.setReadUsers(new ArrayList<>());
        }
        // 聚合已读人(排除当前用户)
        List<Map<String, Object>> readers = teamMapper.selectReadersByTeamId(teamId, userId);
        Map<Long, List<Map<String, Object>>> readerMap = new HashMap<>();
        for (Map<String, Object> r : readers)
        {
            Long msgId = ((Number) r.get("msgId")).longValue();
            readerMap.computeIfAbsent(msgId, k -> new ArrayList<>()).add(r);
        }
        for (AiTeamMessage msg : messages)
        {
            List<Map<String, Object>> list = readerMap.get(msg.getMsgId());
            if (list != null)
            {
                msg.setReadUsers(list);
            }
        }
        return messages;
    }

    /** 校验当前用户是否为团队成员,返回其成员记录 */
    private AiTeamMember assertMember(Long teamId, Long userId)
    {
        AiTeamMember me = teamMapper.selectMember(teamId, userId);
        if (me == null)
        {
            throw new ServiceException("您不是该团队成员");
        }
        return me;
    }

    /** 校验当前用户是否为管理员(OWNER/ADMIN) */
    private void assertManager(Long teamId, Long userId)
    {
        AiTeamMember me = assertMember(teamId, userId);
        if (!ROLE_OWNER.equals(me.getRole()) && !ROLE_ADMIN.equals(me.getRole()))
        {
            throw new ServiceException("无操作权限(仅管理员/创建者可操作)");
        }
    }

    private boolean isValidRole(String role)
    {
        return ROLE_OWNER.equals(role) || ROLE_ADMIN.equals(role) || ROLE_MEMBER.equals(role);
    }

    private String formatTime(Date date)
    {
        if (date == null)
        {
            return "";
        }
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        Calendar now = Calendar.getInstance();
        SimpleDateFormat sdf;
        if (c.get(Calendar.YEAR) == now.get(Calendar.YEAR)
                && c.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR))
        {
            sdf = new SimpleDateFormat("HH:mm");
        }
        else
        {
            sdf = new SimpleDateFormat("MM-dd HH:mm");
        }
        return sdf.format(date);
    }
}
