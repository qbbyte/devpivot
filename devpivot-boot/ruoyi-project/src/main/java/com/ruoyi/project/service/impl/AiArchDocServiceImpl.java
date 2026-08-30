package com.ruoyi.project.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.project.mapper.AiArchDocMapper;
import com.ruoyi.project.domain.AiArchDoc;
import com.ruoyi.project.service.IAiArchDocService;

/**
 * 系统架构设计文档Service业务层处理
 *
 * @author devpivot
 * @date 2026-08-26
 */
@Service
public class AiArchDocServiceImpl implements IAiArchDocService
{
    @Autowired
    private AiArchDocMapper aiArchDocMapper;

    @Override
    public AiArchDoc selectAiArchDocByDocId(Long docId)
    {
        return aiArchDocMapper.selectAiArchDocByDocId(docId);
    }

    @Override
    public List<AiArchDoc> selectAiArchDocList(AiArchDoc aiArchDoc)
    {
        return aiArchDocMapper.selectAiArchDocList(aiArchDoc);
    }

    @Override
    public int insertAiArchDoc(AiArchDoc aiArchDoc)
    {
        aiArchDoc.setCreateTime(DateUtils.getNowDate());
        return aiArchDocMapper.insertAiArchDoc(aiArchDoc);
    }

    @Override
    public int updateAiArchDoc(AiArchDoc aiArchDoc)
    {
        aiArchDoc.setUpdateTime(DateUtils.getNowDate());
        return aiArchDocMapper.updateAiArchDoc(aiArchDoc);
    }

    @Override
    public int deleteAiArchDocByDocId(Long docId)
    {
        return aiArchDocMapper.deleteAiArchDocByDocId(docId);
    }

    @Override
    public int deleteAiArchDocByDocIds(Long[] docIds)
    {
        return aiArchDocMapper.deleteAiArchDocByDocIds(docIds);
    }
}
