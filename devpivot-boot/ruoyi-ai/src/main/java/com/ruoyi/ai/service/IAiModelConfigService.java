package com.ruoyi.ai.service;

import java.util.List;
import com.ruoyi.ai.domain.AiModelConfig;

/**
 * AI模型配置Service接口
 * 
 * @author ruoyi
 * @date 2026-08-04
 */
public interface IAiModelConfigService 
{
    /**
     * 查询AI模型配置
     * 
     * @param modelId AI模型配置主键
     * @return AI模型配置
     */
    public AiModelConfig selectAiModelConfigByModelId(Long modelId);

    /**
     * 查询AI模型配置列表
     * 
     * @param aiModelConfig AI模型配置
     * @return AI模型配置集合
     */
    public List<AiModelConfig> selectAiModelConfigList(AiModelConfig aiModelConfig);

    /**
     * 新增AI模型配置
     * 
     * @param aiModelConfig AI模型配置
     * @return 结果
     */
    public int insertAiModelConfig(AiModelConfig aiModelConfig);

    /**
     * 修改AI模型配置
     * 
     * @param aiModelConfig AI模型配置
     * @return 结果
     */
    public int updateAiModelConfig(AiModelConfig aiModelConfig);

    /**
     * 批量删除AI模型配置
     * 
     * @param modelIds 需要删除的AI模型配置主键集合
     * @return 结果
     */
    public int deleteAiModelConfigByModelIds(Long[] modelIds);

    /**
     * 删除AI模型配置信息
     * 
     * @param modelId AI模型配置主键
     * @return 结果
     */
    public int deleteAiModelConfigByModelId(Long modelId);
}
