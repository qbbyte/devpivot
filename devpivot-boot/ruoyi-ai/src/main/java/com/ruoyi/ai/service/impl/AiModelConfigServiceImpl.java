package com.ruoyi.ai.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.ai.mapper.AiModelConfigMapper;
import com.ruoyi.ai.domain.AiModelConfig;
import com.ruoyi.ai.service.IAiModelConfigService;

/**
 * AI模型配置Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-08-04
 */
@Service
public class AiModelConfigServiceImpl implements IAiModelConfigService 
{
    @Autowired
    private AiModelConfigMapper aiModelConfigMapper;

    /**
     * 查询AI模型配置
     * 
     * @param modelId AI模型配置主键
     * @return AI模型配置
     */
    @Override
    public AiModelConfig selectAiModelConfigByModelId(Long modelId)
    {
        return aiModelConfigMapper.selectAiModelConfigByModelId(modelId);
    }

    /**
     * 查询AI模型配置列表
     * 
     * @param aiModelConfig AI模型配置
     * @return AI模型配置
     */
    @Override
    public List<AiModelConfig> selectAiModelConfigList(AiModelConfig aiModelConfig)
    {
        return aiModelConfigMapper.selectAiModelConfigList(aiModelConfig);
    }

    /**
     * 新增AI模型配置
     * 
     * @param aiModelConfig AI模型配置
     * @return 结果
     */
    @Override
    public int insertAiModelConfig(AiModelConfig aiModelConfig)
    {
        aiModelConfig.setCreateTime(DateUtils.getNowDate());
        return aiModelConfigMapper.insertAiModelConfig(aiModelConfig);
    }

    /**
     * 修改AI模型配置
     * 
     * @param aiModelConfig AI模型配置
     * @return 结果
     */
    @Override
    public int updateAiModelConfig(AiModelConfig aiModelConfig)
    {
        aiModelConfig.setUpdateTime(DateUtils.getNowDate());
        return aiModelConfigMapper.updateAiModelConfig(aiModelConfig);
    }

    /**
     * 批量删除AI模型配置
     * 
     * @param modelIds 需要删除的AI模型配置主键
     * @return 结果
     */
    @Override
    public int deleteAiModelConfigByModelIds(Long[] modelIds)
    {
        return aiModelConfigMapper.deleteAiModelConfigByModelIds(modelIds);
    }

    /**
     * 删除AI模型配置信息
     * 
     * @param modelId AI模型配置主键
     * @return 结果
     */
    @Override
    public int deleteAiModelConfigByModelId(Long modelId)
    {
        return aiModelConfigMapper.deleteAiModelConfigByModelId(modelId);
    }
}
