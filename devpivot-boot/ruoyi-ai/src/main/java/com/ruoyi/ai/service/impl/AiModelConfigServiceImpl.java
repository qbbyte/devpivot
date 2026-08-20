package com.ruoyi.ai.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.AesGcmCrypto;
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

    @Autowired
    private AesGcmCrypto aesGcmCrypto;

    /**
     * 查询AI模型配置
     * 
     * @param modelId AI模型配置主键
     * @return AI模型配置
     */
    @Override
    public AiModelConfig selectAiModelConfigByModelId(Long modelId)
    {
        AiModelConfig e = aiModelConfigMapper.selectAiModelConfigByModelId(modelId);
        decorate(e);
        return e;
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
        List<AiModelConfig> list = aiModelConfigMapper.selectAiModelConfigList(aiModelConfig);
        if (list != null)
        {
            for (AiModelConfig e : list)
            {
                decorate(e);
            }
        }
        return list;
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
        String incoming = aiModelConfig.getApiKey();
        if (incoming != null && !incoming.isEmpty() && !incoming.startsWith("ENC:"))
        {
            aiModelConfig.setApiKey(aesGcmCrypto.encrypt(incoming));
        }
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
        String incoming = aiModelConfig.getApiKey();
        // 留空或仍为脱敏占位(****)时，保留库中已有的加密值，避免误覆盖密钥
        if (incoming == null || incoming.isEmpty() || incoming.startsWith("*"))
        {
            AiModelConfig existing = aiModelConfigMapper.selectAiModelConfigByModelId(aiModelConfig.getModelId());
            aiModelConfig.setApiKey(existing == null ? null : existing.getApiKey());
        }
        else if (!incoming.startsWith("ENC:"))
        {
            aiModelConfig.setApiKey(aesGcmCrypto.encrypt(incoming));
        }
        aiModelConfig.setUpdateTime(DateUtils.getNowDate());
        return aiModelConfigMapper.updateAiModelConfig(aiModelConfig);
    }

    /** 解密入库密钥为明文(供内部调用)并填充脱敏字段(供返回) */
    private void decorate(AiModelConfig e)
    {
        if (e == null)
        {
            return;
        }
        String plain = aesGcmCrypto.decrypt(e.getApiKey());
        e.setApiKey(plain);
        e.setMaskedApiKey(AesGcmCrypto.maskKey(plain));
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
