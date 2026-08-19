package com.ruoyi.ai.mapper;

import java.util.Date;
import java.util.List;
import com.ruoyi.ai.domain.AiKbChunk;
import com.ruoyi.ai.domain.AiKbDoc;
import com.ruoyi.ai.domain.AiKbRetrievalLog;

/**
 * 知识库 Mapper（文档/切片/检索日志聚合）
 *
 * @author devpivot
 * @date 2026-08-12
 */
public interface AiKbMapper
{
    int insertAiKbDoc(AiKbDoc doc);

    int updateAiKbDocChunkCount(AiKbDoc doc);

    AiKbDoc selectAiKbDocByDocId(Long docId);

    int deleteAiKbDocByDocId(Long docId);

    int deleteAiKbChunkByDocId(Long docId);

    List<AiKbDoc> selectAiKbDocList(Long projectId, String stage);

    List<AiKbDoc> selectAiKbDocAll(String stage);

    int deletePipelineDoc(Long projectId, String stage);

    int deleteAiKbChunkByProjectStage(Long projectId, String stage);

    int insertAiKbChunkBatch(List<AiKbChunk> chunks);

    List<AiKbChunk> selectChunksForRetrieve(Long projectId, String stage, String query, int topK);

    int insertAiKbRetrievalLog(AiKbRetrievalLog log);

    /**
     * 查询检索日志（按时间倒序，最近 limit 条；projectId/stage 可选过滤）
     * @param projectId 项目ID（null=全部项目）
     * @param stage 阶段（null=全部阶段）
     * @param limit 返回条数上限
     */
    List<AiKbRetrievalLog> selectAiKbRetrievalLogList(Long projectId, String stage, int limit);

    int deleteRetrievalLogBefore(Date before);

    /** 按 (projectId, stage, title) 查询已存在文档（用于上传去重/覆盖更新） */
    List<AiKbDoc> selectAiKbDocByTitle(Long projectId, String stage, String title);
}
