package com.ruoyi.ai.service;

import java.util.List;
import com.ruoyi.ai.domain.AiKbChunk;
import com.ruoyi.ai.domain.AiKbDoc;

/**
 * 知识库检索服务（AI 引擎层，纯引擎能力，不依赖业务模块）。
 *
 * <p>设计：按 (projectId, stage) 分区检索知识库切片，拼接为提示词上下文。
 * 底层当前为 MySQL InnoDB FULLTEXT（ngram 中文分词）+ stage 过滤 + top-k + token 预算；
 * 检索实现被本接口抽象，未来升级向量库时上层（阶段控制器）零改动。
 *
 * @author devpivot
 * @date 2026-08-12
 */
public interface IKnowledgeRetrievalService
{
    /** 组织级共享知识库专用 projectId（与业务项目 id 不冲突，负数保留） */
    long SHARED_PROJECT_ID = -1L;

    /** 检索切片（原始列表，供调参/预览） */
    List<AiKbChunk> retrieve(Long projectId, String stage, String query, int topK);

    /**
     * 检索并格式化为可直接注入提示词的字符串（无命中返回空串，保证模板占位符安全）。
     * query 建议传「上游文档摘要 + 当前生成意图」。
     */
    String retrieveAsContext(Long projectId, String stage, String query);

    /**
     * 带 modelId 的重载：生成的模型标识会随检索日志持久化，便于效果回溯。
     */
    String retrieveAsContext(Long projectId, String stage, String query, String modelId);

    /** 索引一篇文档：切片（约 500 字/段，seq 保序）+ 落库 */
    void indexDocument(Long projectId, String stage, String title, String content, String sourceType);

    /**
     * 流水线产物自动索引：先删除本项目该阶段旧的 pipeline 文档与切片，再写入新文档，避免 KB 无限膨胀。
     */
    void autoIndexPipelineProduct(Long projectId, String stage, String content);

    /** 文档列表（按 projectId + 可选 stage 过滤） */
    List<AiKbDoc> listDocs(Long projectId, String stage);

    /** 文档总览列表（管理员后台用：跨项目 + 含组织共享库，可选 stage 过滤） */
    List<AiKbDoc> listAllDocs(String stage);

    /** 删除文档（级联删除其切片） */
    int deleteDoc(Long docId);

    /** 清理超过保留天数的检索日志，返回删除行数 */
    int cleanupRetrievalLog();
}
