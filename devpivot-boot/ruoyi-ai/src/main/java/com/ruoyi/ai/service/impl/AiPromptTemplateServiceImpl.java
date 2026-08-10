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

    @Override
    public int setDefault(Long templateId)
    {
        AiPromptTemplate target = selectAiPromptTemplateByTemplateId(templateId);
        if (target == null)
        {
            return 0;
        }
        // 同场景其他「默认」模板置为非默认（保证互斥）
        AiPromptTemplate q = new AiPromptTemplate();
        q.setSceneType(target.getSceneType());
        List<AiPromptTemplate> list = selectAiPromptTemplateList(q);
        for (AiPromptTemplate o : list)
        {
            if (!o.getTemplateId().equals(templateId) && "Y".equals(o.getIsDefault()))
            {
                o.setIsDefault("N");
                updateAiPromptTemplate(o);
            }
        }
        target.setIsDefault("Y");
        target.setIsEnabled("0");
        return updateAiPromptTemplate(target);
    }

    @Override
    public Long clone(Long templateId)
    {
        AiPromptTemplate src = selectAiPromptTemplateByTemplateId(templateId);
        if (src == null)
        {
            return null;
        }
        src.setTemplateId(null);
        String suffix = "_v" + (System.currentTimeMillis() % 100000L);
        src.setTemplateCode((src.getTemplateCode() == null ? "T" : src.getTemplateCode()) + suffix);
        src.setTemplateName((src.getTemplateName() == null ? "模板" : src.getTemplateName()) + " 副本");
        src.setIsDefault("N");
        src.setIsEnabled("1"); // 默认停用，避免误用
        src.setCreateBy(null);
        src.setCreateTime(null);
        src.setUpdateBy(null);
        src.setUpdateTime(null);
        src.setRemark("克隆自模板#" + templateId);
        insertAiPromptTemplate(src);
        return src.getTemplateId();
    }
}
