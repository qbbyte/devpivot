package com.ruoyi.ai.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.ai.mapper.AiTechDocMapper;
import com.ruoyi.ai.domain.AiTechDoc;
import com.ruoyi.ai.service.IAiTechDocService;

/**
 * 技术方案文档Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-08-04
 */
@Service
public class AiTechDocServiceImpl implements IAiTechDocService 
{
    @Autowired
    private AiTechDocMapper aiTechDocMapper;

    /**
     * 查询技术方案文档
     * 
     * @param docId 技术方案文档主键
     * @return 技术方案文档
     */
    @Override
    public AiTechDoc selectAiTechDocByDocId(Long docId)
    {
        return aiTechDocMapper.selectAiTechDocByDocId(docId);
    }

    /**
     * 查询技术方案文档列表
     * 
     * @param aiTechDoc 技术方案文档
     * @return 技术方案文档
     */
    @Override
    public List<AiTechDoc> selectAiTechDocList(AiTechDoc aiTechDoc)
    {
        return aiTechDocMapper.selectAiTechDocList(aiTechDoc);
    }

    /**
     * 新增技术方案文档
     * 
     * @param aiTechDoc 技术方案文档
     * @return 结果
     */
    @Override
    public int insertAiTechDoc(AiTechDoc aiTechDoc)
    {
        aiTechDoc.setCreateTime(DateUtils.getNowDate());
        return aiTechDocMapper.insertAiTechDoc(aiTechDoc);
    }

    /**
     * 修改技术方案文档
     * 
     * @param aiTechDoc 技术方案文档
     * @return 结果
     */
    @Override
    public int updateAiTechDoc(AiTechDoc aiTechDoc)
    {
        aiTechDoc.setUpdateTime(DateUtils.getNowDate());
        return aiTechDocMapper.updateAiTechDoc(aiTechDoc);
    }

    /**
     * 批量删除技术方案文档
     * 
     * @param docIds 需要删除的技术方案文档主键
     * @return 结果
     */
    @Override
    public int deleteAiTechDocByDocIds(Long[] docIds)
    {
        return aiTechDocMapper.deleteAiTechDocByDocIds(docIds);
    }

    /**
     * 删除技术方案文档信息
     * 
     * @param docId 技术方案文档主键
     * @return 结果
     */
    @Override
    public int deleteAiTechDocByDocId(Long docId)
    {
        return aiTechDocMapper.deleteAiTechDocByDocId(docId);
    }
}
