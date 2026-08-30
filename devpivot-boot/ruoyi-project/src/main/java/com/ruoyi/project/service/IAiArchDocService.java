package com.ruoyi.project.service;

import java.util.List;
import com.ruoyi.project.domain.AiArchDoc;

/**
 * 系统架构设计文档Service接口
 *
 * @author devpivot
 * @date 2026-08-26
 */
public interface IAiArchDocService
{
    /**
     * 查询系统架构设计文档
     *
     * @param docId 文档主键
     * @return 系统架构设计文档
     */
    public AiArchDoc selectAiArchDocByDocId(Long docId);

    /**
     * 查询系统架构设计文档列表
     *
     * @param aiArchDoc 系统架构设计文档
     * @return 系统架构设计文档集合
     */
    public List<AiArchDoc> selectAiArchDocList(AiArchDoc aiArchDoc);

    /**
     * 新增系统架构设计文档
     *
     * @param aiArchDoc 系统架构设计文档
     * @return 结果
     */
    public int insertAiArchDoc(AiArchDoc aiArchDoc);

    /**
     * 修改系统架构设计文档
     *
     * @param aiArchDoc 系统架构设计文档
     * @return 结果
     */
    public int updateAiArchDoc(AiArchDoc aiArchDoc);

    /**
     * 删除系统架构设计文档
     *
     * @param docId 文档主键
     * @return 结果
     */
    public int deleteAiArchDocByDocId(Long docId);

    /**
     * 批量删除系统架构设计文档
     *
     * @param docIds 需要删除的文档主键集合
     * @return 结果
     */
    public int deleteAiArchDocByDocIds(Long[] docIds);
}
