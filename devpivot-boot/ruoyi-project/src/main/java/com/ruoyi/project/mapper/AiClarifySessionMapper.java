package com.ruoyi.project.mapper;

import java.util.List;
import com.ruoyi.project.domain.AiClarifySession;

/**
 * AI澄清会话Mapper接口
 * 
 * @author devpivot
 * @date 2026-08-05
 */
public interface AiClarifySessionMapper 
{
    /**
     * 根据项目ID查询澄清会话
     *
     * @param projectId 项目ID
     * @return 澄清会话
     */
    public AiClarifySession selectAiClarifySessionByProjectId(Long projectId);

    /**
     * 查询澄清会话列表（管理端，按项目/状态过滤）
     *
     * @param aiClarifySession 查询条件
     * @return 澄清会话集合
     */
    public List<AiClarifySession> selectAiClarifySessionList(AiClarifySession aiClarifySession);

    /**
     * 根据会话ID查询澄清会话
     *
     * @param sessionId 会话ID
     * @return 澄清会话
     */
    public AiClarifySession selectAiClarifySessionBySessionId(Long sessionId);

    /**
     * 新增AI澄清会话
     * 
     * @param aiClarifySession AI澄清会话
     * @return 结果
     */
    public int insertAiClarifySession(AiClarifySession aiClarifySession);

    /**
     * 修改AI澄清会话
     * 
     * @param aiClarifySession AI澄清会话
     * @return 结果
     */
    public int updateAiClarifySession(AiClarifySession aiClarifySession);

    /**
     * 根据项目ID修改AI澄清会话（按项目维度更新，便于 getOrCreate 后保存）
     * 
     * @param aiClarifySession AI澄清会话
     * @return 结果
     */
    public int updateAiClarifySessionByProjectId(AiClarifySession aiClarifySession);
}
