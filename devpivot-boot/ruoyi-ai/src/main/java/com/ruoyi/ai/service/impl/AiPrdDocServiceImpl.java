package com.ruoyi.ai.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.ai.mapper.AiPrdDocMapper;
import com.ruoyi.ai.domain.AiPrdDoc;
import com.ruoyi.ai.service.IAiPrdDocService;

/**
 * PRD需求文档Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-08-04
 */
@Service
public class AiPrdDocServiceImpl implements IAiPrdDocService 
{
    @Autowired
    private AiPrdDocMapper aiPrdDocMapper;

    /**
     * 查询PRD需求文档
     * 
     * @param docId PRD需求文档主键
     * @return PRD需求文档
     */
    @Override
    public AiPrdDoc selectAiPrdDocByDocId(Long docId)
    {
        return aiPrdDocMapper.selectAiPrdDocByDocId(docId);
    }

    /**
     * 查询PRD需求文档列表
     * 
     * @param aiPrdDoc PRD需求文档
     * @return PRD需求文档
     */
    @Override
    public List<AiPrdDoc> selectAiPrdDocList(AiPrdDoc aiPrdDoc)
    {
        return aiPrdDocMapper.selectAiPrdDocList(aiPrdDoc);
    }

    /**
     * 新增PRD需求文档
     * 
     * @param aiPrdDoc PRD需求文档
     * @return 结果
     */
    @Override
    public int insertAiPrdDoc(AiPrdDoc aiPrdDoc)
    {
        aiPrdDoc.setCreateTime(DateUtils.getNowDate());
        return aiPrdDocMapper.insertAiPrdDoc(aiPrdDoc);
    }

    /**
     * 修改PRD需求文档
     * 
     * @param aiPrdDoc PRD需求文档
     * @return 结果
     */
    @Override
    public int updateAiPrdDoc(AiPrdDoc aiPrdDoc)
    {
        aiPrdDoc.setUpdateTime(DateUtils.getNowDate());
        return aiPrdDocMapper.updateAiPrdDoc(aiPrdDoc);
    }

    /**
     * 批量删除PRD需求文档
     * 
     * @param docIds 需要删除的PRD需求文档主键
     * @return 结果
     */
    @Override
    public int deleteAiPrdDocByDocIds(Long[] docIds)
    {
        return aiPrdDocMapper.deleteAiPrdDocByDocIds(docIds);
    }

    /**
     * 删除PRD需求文档信息
     * 
     * @param docId PRD需求文档主键
     * @return 结果
     */
    @Override
    public int deleteAiPrdDocByDocId(Long docId)
    {
        return aiPrdDocMapper.deleteAiPrdDocByDocId(docId);
    }
}
