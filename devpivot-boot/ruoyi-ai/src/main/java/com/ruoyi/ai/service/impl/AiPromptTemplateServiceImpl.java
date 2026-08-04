package com.ruoyi.ai.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.ai.mapper.AiPromptTemplateMapper;
import com.ruoyi.ai.domain.AiPromptTemplate;
import com.ruoyi.ai.service.IAiPromptTemplateService;

/**
 * Prompt模板Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-08-04
 */
@Service
public class AiPromptTemplateServiceImpl implements IAiPromptTemplateService 
{
    @Autowired
    private AiPromptTemplateMapper aiPromptTemplateMapper;

    /**
     * 查询Prompt模板
     * 
     * @param templateId Prompt模板主键
     * @return Prompt模板
     */
    @Override
    public AiPromptTemplate selectAiPromptTemplateByTemplateId(Long templateId)
    {
        return aiPromptTemplateMapper.selectAiPromptTemplateByTemplateId(templateId);
    }

    /**
     * 查询Prompt模板列表
     * 
     * @param aiPromptTemplate Prompt模板
     * @return Prompt模板
     */
    @Override
    public List<AiPromptTemplate> selectAiPromptTemplateList(AiPromptTemplate aiPromptTemplate)
    {
        return aiPromptTemplateMapper.selectAiPromptTemplateList(aiPromptTemplate);
    }

    /**
     * 新增Prompt模板
     * 
     * @param aiPromptTemplate Prompt模板
     * @return 结果
     */
    @Override
    public int insertAiPromptTemplate(AiPromptTemplate aiPromptTemplate)
    {
        aiPromptTemplate.setCreateTime(DateUtils.getNowDate());
        return aiPromptTemplateMapper.insertAiPromptTemplate(aiPromptTemplate);
    }

    /**
     * 修改Prompt模板
     * 
     * @param aiPromptTemplate Prompt模板
     * @return 结果
     */
    @Override
    public int updateAiPromptTemplate(AiPromptTemplate aiPromptTemplate)
    {
        aiPromptTemplate.setUpdateTime(DateUtils.getNowDate());
        return aiPromptTemplateMapper.updateAiPromptTemplate(aiPromptTemplate);
    }

    /**
     * 批量删除Prompt模板
     * 
     * @param templateIds 需要删除的Prompt模板主键
     * @return 结果
     */
    @Override
    public int deleteAiPromptTemplateByTemplateIds(Long[] templateIds)
    {
        return aiPromptTemplateMapper.deleteAiPromptTemplateByTemplateIds(templateIds);
    }

    /**
     * 删除Prompt模板信息
     * 
     * @param templateId Prompt模板主键
     * @return 结果
     */
    @Override
    public int deleteAiPromptTemplateByTemplateId(Long templateId)
    {
        return aiPromptTemplateMapper.deleteAiPromptTemplateByTemplateId(templateId);
    }
}
