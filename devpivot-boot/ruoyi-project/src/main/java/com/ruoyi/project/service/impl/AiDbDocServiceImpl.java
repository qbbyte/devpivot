package com.ruoyi.project.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.project.mapper.AiDbDocMapper;
import com.ruoyi.project.domain.AiDbDoc;
import com.ruoyi.project.service.IAiDbDocService;

/**
 * 数据库设计文档Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-08-08
 */
@Service
public class AiDbDocServiceImpl implements IAiDbDocService 
{
    @Autowired
    private AiDbDocMapper aiDbDocMapper;

    @Override
    public AiDbDoc selectAiDbDocByDocId(Long docId)
    {
        return aiDbDocMapper.selectAiDbDocByDocId(docId);
    }

    @Override
    public List<AiDbDoc> selectAiDbDocList(AiDbDoc aiDbDoc)
    {
        return aiDbDocMapper.selectAiDbDocList(aiDbDoc);
    }

    @Override
    public int insertAiDbDoc(AiDbDoc aiDbDoc)
    {
        aiDbDoc.setCreateTime(DateUtils.getNowDate());
        return aiDbDocMapper.insertAiDbDoc(aiDbDoc);
    }

    @Override
    public int updateAiDbDoc(AiDbDoc aiDbDoc)
    {
        aiDbDoc.setUpdateTime(DateUtils.getNowDate());
        return aiDbDocMapper.updateAiDbDoc(aiDbDoc);
    }

    @Override
    public int deleteAiDbDocByDocIds(Long[] docIds)
    {
        return aiDbDocMapper.deleteAiDbDocByDocIds(docIds);
    }

    @Override
    public int deleteAiDbDocByDocId(Long docId)
    {
        return aiDbDocMapper.deleteAiDbDocByDocId(docId);
    }
}
