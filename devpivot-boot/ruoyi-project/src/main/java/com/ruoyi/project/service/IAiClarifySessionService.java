package com.ruoyi.project.service;

import com.ruoyi.project.domain.AiClarifySession;

/**
 * AI澄清会话Service接口
 * 
 * @author devpivot
 * @date 2026-08-05
 */
public interface IAiClarifySessionService 
{
    /**
     * 获取或创建项目的澄清会话
     * 
     * @param projectId 项目ID
     * @return 澄清会话（不存在则新建并返回空会话）
     */
    public AiClarifySession getOrCreateSession(Long projectId);

    /**
     * 保存会话的对话/采纳/保留数据（按项目维度 upsert）
     * 
     * @param session 会话数据
     * @return 结果
     */
    public int saveSession(AiClarifySession session);

    /**
     * 提交澄清结论：持久化结论快照、标记会话已提交，并将项目阶段推进到 PRD
     * 
     * @param projectId 项目ID
     * @param conclusionJson 结论 JSON 字符串（含 conversation/adopted/retained 等）
     * @return 结果
     */
    public int submitSession(Long projectId, String conclusionJson);
}
