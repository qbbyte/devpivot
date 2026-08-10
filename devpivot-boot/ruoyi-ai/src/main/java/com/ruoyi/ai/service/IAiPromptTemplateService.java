package com.ruoyi.ai.service;

import java.util.List;
import com.ruoyi.ai.domain.AiPromptTemplate;

/**
 * Prompt模板Service接口
 * 
 * @author ruoyi
 * @date 2026-08-04
 */
public interface IAiPromptTemplateService 
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
     * 批量删除Prompt模板
     * 
     * @param templateIds 需要删除的Prompt模板主键集合
     * @return 结果
     */
    public int deleteAiPromptTemplateByTemplateIds(Long[] templateIds);

    /**
     * 删除Prompt模板信息
     * 
     * @param templateId Prompt模板主键
     * @return 结果
     */
    public int deleteAiPromptTemplateByTemplateId(Long templateId);

    /**
     * 互斥设为默认模板：同场景其他模板置为「非默认」，本模板置为「默认 + 启用」。
     * 用于版本回滚（把某历史版本重新设为生效模板）。
     *
     * @param templateId 目标模板主键
     * @return 结果
     */
    public int setDefault(Long templateId);

    /**
     * 克隆模板为新版本：复制除主键外的全部字段，新模板 code 带时间戳后缀、默认「非默认 + 停用」，
     * 便于在不影响线上生效模板的前提下试验新提示词。返回新模板主键。
     *
     * @param templateId 源模板主键
     * @return 新模板主键
     */
    public Long clone(Long templateId);
}
