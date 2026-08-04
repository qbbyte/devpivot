package com.ruoyi.ai.mapper;

import java.util.List;
import com.ruoyi.ai.domain.AiPromptTemplate;

/**
 * Prompt模板Mapper接口
 * 
 * @author ruoyi
 * @date 2026-08-04
 */
public interface AiPromptTemplateMapper 
{
    /**
     * 查询Prompt模板
     * 
     * @param templateId Prompt模板主键
     * @return Prompt模板
     */
    public AiPromptTemplate selectAiPromptTemplateByTemplateId(Long templateId);

    /**
     * 查询Prompt模板列表
     * 
     * @param aiPromptTemplate Prompt模板
     * @return Prompt模板集合
     */
    public List<AiPromptTemplate> selectAiPromptTemplateList(AiPromptTemplate aiPromptTemplate);

    /**
     * 新增Prompt模板
     * 
     * @param aiPromptTemplate Prompt模板
     * @return 结果
     */
    public int insertAiPromptTemplate(AiPromptTemplate aiPromptTemplate);

    /**
     * 修改Prompt模板
     * 
     * @param aiPromptTemplate Prompt模板
     * @return 结果
     */
    public int updateAiPromptTemplate(AiPromptTemplate aiPromptTemplate);

    /**
     * 删除Prompt模板
     * 
     * @param templateId Prompt模板主键
     * @return 结果
     */
    public int deleteAiPromptTemplateByTemplateId(Long templateId);

    /**
     * 批量删除Prompt模板
     * 
     * @param templateIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteAiPromptTemplateByTemplateIds(Long[] templateIds);
}
