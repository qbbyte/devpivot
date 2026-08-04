package com.ruoyi.ai.service;

import java.util.List;
import com.ruoyi.ai.domain.AiTechDoc;

/**
 * 技术方案文档Service接口
 * 
 * @author ruoyi
 * @date 2026-08-04
 */
public interface IAiTechDocService 
{
    /**
     * 查询技术方案文档
     * 
     * @param docId 技术方案文档主键
     * @return 技术方案文档
     */
    public AiTechDoc selectAiTechDocByDocId(Long docId);

    /**
     * 查询技术方案文档列表
     * 
     * @param aiTechDoc 技术方案文档
     * @return 技术方案文档集合
     */
    public List<AiTechDoc> selectAiTechDocList(AiTechDoc aiTechDoc);

    /**
     * 新增技术方案文档
     * 
     * @param aiTechDoc 技术方案文档
     * @return 结果
     */
    public int insertAiTechDoc(AiTechDoc aiTechDoc);

    /**
     * 修改技术方案文档
     * 
     * @param aiTechDoc 技术方案文档
     * @return 结果
     */
    public int updateAiTechDoc(AiTechDoc aiTechDoc);

    /**
     * 批量删除技术方案文档
     * 
     * @param docIds 需要删除的技术方案文档主键集合
     * @return 结果
     */
    public int deleteAiTechDocByDocIds(Long[] docIds);

    /**
     * 删除技术方案文档信息
     * 
     * @param docId 技术方案文档主键
     * @return 结果
     */
    public int deleteAiTechDocByDocId(Long docId);
}
