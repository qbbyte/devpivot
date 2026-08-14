package com.ruoyi.ai.mapper;

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

    int insertAiKbChunk(AiKbChunk chunk);

    List<AiKbChunk> selectChunksForRetrieve(Long projectId, String stage, String query, int topK);

    int insertAiKbRetrievalLog(AiKbRetrievalLog log);
}
