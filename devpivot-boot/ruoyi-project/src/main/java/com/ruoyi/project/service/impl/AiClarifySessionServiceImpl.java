package com.ruoyi.project.service.impl;

import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.project.mapper.AiClarifySessionMapper;
import com.ruoyi.project.domain.AiClarifySession;
import com.ruoyi.project.domain.AiProject;
import com.ruoyi.project.service.IAiClarifySessionService;
import com.ruoyi.project.service.IAiProjectService;

/**
 * AI澄清会话Service业务层处理
 * 
 * @author devpivot
 * @date 2026-08-05
 */
@Service
public class AiClarifySessionServiceImpl implements IAiClarifySessionService 
{
    @Autowired
    private AiClarifySessionMapper aiClarifySessionMapper;

    @Autowired
    private IAiProjectService aiProjectService;

    @Override
    public AiClarifySession getOrCreateSession(Long projectId)
    {
        AiClarifySession existing = aiClarifySessionMapper.selectAiClarifySessionByProjectId(projectId);
        if (existing != null)
        {
            return existing;
        }
        AiClarifySession session = new AiClarifySession();
        session.setProjectId(projectId);
        session.setStatus("0");
        session.setCreateBy(SecurityUtils.getUsername());
        session.setCreateTime(DateUtils.getNowDate());
        aiClarifySessionMapper.insertAiClarifySession(session);
        return session;
    }

    @Override
    public List<AiClarifySession> selectAiClarifySessionList(AiClarifySession aiClarifySession)
    {
        return aiClarifySessionMapper.selectAiClarifySessionList(aiClarifySession);
    }

    @Override
    public AiClarifySession selectAiClarifySessionBySessionId(Long sessionId)
    {
        return aiClarifySessionMapper.selectAiClarifySessionBySessionId(sessionId);
    }

    @Override
    public int saveSession(AiClarifySession session)
    {
        AiClarifySession existing = aiClarifySessionMapper.selectAiClarifySessionByProjectId(session.getProjectId());
        if (existing == null)
        {
            session.setCreateBy(SecurityUtils.getUsername());
            session.setCreateTime(DateUtils.getNowDate());
            return aiClarifySessionMapper.insertAiClarifySession(session);
        }
        session.setSessionId(existing.getSessionId());
        session.setUpdateBy(SecurityUtils.getUsername());
        session.setUpdateTime(DateUtils.getNowDate());
        return aiClarifySessionMapper.updateAiClarifySessionByProjectId(session);
    }

    @Override
    @Transactional
    public int submitSession(Long projectId, String conclusionJson)
    {
        AiClarifySession existing = aiClarifySessionMapper.selectAiClarifySessionByProjectId(projectId);
        AiClarifySession session;
        if (existing == null)
        {
            session = new AiClarifySession();
            session.setProjectId(projectId);
            session.setCreateBy(SecurityUtils.getUsername());
            session.setCreateTime(DateUtils.getNowDate());
        }
        else
        {
            session = existing;
        }
        session.setConclusion(conclusionJson);
        session.setStatus("1");
        session.setSubmitTime(new Date());
        session.setUpdateBy(SecurityUtils.getUsername());
        session.setUpdateTime(DateUtils.getNowDate());

        int rows;
        if (existing == null)
        {
            rows = aiClarifySessionMapper.insertAiClarifySession(session);
        }
        else
        {
            rows = aiClarifySessionMapper.updateAiClarifySession(session);
        }

        // 推进项目阶段到 PRD（仅更新 step 字段，不影响其他项目信息）
        AiProject project = aiProjectService.selectAiProjectByProjectId(projectId);
        if (project != null)
        {
            project.setStep("PRD");
            project.setUpdateBy(SecurityUtils.getUsername());
            project.setUpdateTime(DateUtils.getNowDate());
            aiProjectService.updateAiProject(project);
        }
        return rows;
    }
}
