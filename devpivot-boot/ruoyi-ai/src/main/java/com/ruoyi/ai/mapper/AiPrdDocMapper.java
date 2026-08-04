package com.ruoyi.ai.mapper;

import java.util.List;
import com.ruoyi.ai.domain.AiPrdDoc;

/**
 * PRD需求文档Mapper接口
 * 
 * @author ruoyi
 * @date 2026-08-04
 */
public interface AiPrdDocMapper 
{
    /**
     * 查询PRD需求文档
     * 
     * @param docId PRD需求文档主键
     * @return PRD需求文档
     */
    public AiPrdDoc selectAiPrdDocByDocId(Long docId);

    /**
     * 查询PRD需求文档列表
     * 
     * @param aiPrdDoc PRD需求文档
     * @return PRD需求文档集合
     */
    public List<AiPrdDoc> selectAiPrdDocList(AiPrdDoc aiPrdDoc);

    /**
     * 新增PRD需求文档
     * 
     * @param aiPrdDoc PRD需求文档
     * @return 结果
     */
    public int insertAiPrdDoc(AiPrdDoc aiPrdDoc);

    /**
     * 修改PRD需求文档
     * 
     * @param aiPrdDoc PRD需求文档
     * @return 结果
     */
    public int updateAiPrdDoc(AiPrdDoc aiPrdDoc);

    /**
     * 删除PRD需求文档
     * 
     * @param docId PRD需求文档主键
     * @return 结果
     */
    public int deleteAiPrdDocByDocId(Long docId);

    /**
     * 批量删除PRD需求文档
     * 
     * @param docIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteAiPrdDocByDocIds(Long[] docIds);
}
