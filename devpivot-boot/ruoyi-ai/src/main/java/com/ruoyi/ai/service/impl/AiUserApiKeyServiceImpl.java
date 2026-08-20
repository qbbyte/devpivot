package com.ruoyi.ai.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.AesGcmCrypto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.ai.mapper.AiUserApiKeyMapper;
import com.ruoyi.ai.domain.AiUserApiKey;
import com.ruoyi.ai.service.IAiUserApiKeyService;

/**
 * 用户API Key配置Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-08-04
 */
@Service
public class AiUserApiKeyServiceImpl implements IAiUserApiKeyService 
{
    @Autowired
    private AiUserApiKeyMapper aiUserApiKeyMapper;

    @Autowired
    private AesGcmCrypto aesGcmCrypto;

    /**
     * 查询用户API Key配置
     * 
     * @param keyId 用户API Key配置主键
     * @return 用户API Key配置
     */
    @Override
    public AiUserApiKey selectAiUserApiKeyByKeyId(Long keyId)
    {
        AiUserApiKey e = aiUserApiKeyMapper.selectAiUserApiKeyByKeyId(keyId);
        decorate(e);
        return e;
    }

    /**
     * 查询用户API Key配置列表
     * 
     * @param aiUserApiKey 用户API Key配置
     * @return 用户API Key配置
     */
    @Override
    public List<AiUserApiKey> selectAiUserApiKeyList(AiUserApiKey aiUserApiKey)
    {
        List<AiUserApiKey> list = aiUserApiKeyMapper.selectAiUserApiKeyList(aiUserApiKey);
        if (list != null)
        {
            for (AiUserApiKey e : list)
            {
                decorate(e);
            }
        }
        return list;
    }

    /**
     * 新增用户API Key配置
     * 
     * @param aiUserApiKey 用户API Key配置
     * @return 结果
     */
    @Override
    public int insertAiUserApiKey(AiUserApiKey aiUserApiKey)
    {
        String incoming = aiUserApiKey.getApiKey();
        if (incoming != null && !incoming.isEmpty() && !incoming.startsWith("ENC:"))
        {
            aiUserApiKey.setApiKey(aesGcmCrypto.encrypt(incoming));
        }
        aiUserApiKey.setCreateTime(DateUtils.getNowDate());
        return aiUserApiKeyMapper.insertAiUserApiKey(aiUserApiKey);
    }

    /**
     * 修改用户API Key配置
     * 
     * @param aiUserApiKey 用户API Key配置
     * @return 结果
     */
    @Override
    public int updateAiUserApiKey(AiUserApiKey aiUserApiKey)
    {
        String incoming = aiUserApiKey.getApiKey();
        // 留空或仍为脱敏占位(****)时，保留库中已有的加密值，避免误覆盖密钥
        if (incoming == null || incoming.isEmpty() || incoming.startsWith("*"))
        {
            AiUserApiKey existing = aiUserApiKeyMapper.selectAiUserApiKeyByKeyId(aiUserApiKey.getKeyId());
            aiUserApiKey.setApiKey(existing == null ? null : existing.getApiKey());
        }
        else if (!incoming.startsWith("ENC:"))
        {
            aiUserApiKey.setApiKey(aesGcmCrypto.encrypt(incoming));
        }
        aiUserApiKey.setUpdateTime(DateUtils.getNowDate());
        return aiUserApiKeyMapper.updateAiUserApiKey(aiUserApiKey);
    }

    /** 解密入库密钥为明文(供内部调用)并填充脱敏字段(供返回) */
    private void decorate(AiUserApiKey e)
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
     * 批量删除用户API Key配置
     * 
     * @param keyIds 需要删除的用户API Key配置主键
     * @return 结果
     */
    @Override
    public int deleteAiUserApiKeyByKeyIds(Long[] keyIds)
    {
        return aiUserApiKeyMapper.deleteAiUserApiKeyByKeyIds(keyIds);
    }

    /**
     * 删除用户API Key配置信息
     * 
     * @param keyId 用户API Key配置主键
     * @return 结果
     */
    @Override
    public int deleteAiUserApiKeyByKeyId(Long keyId)
    {
        return aiUserApiKeyMapper.deleteAiUserApiKeyByKeyId(keyId);
    }
}
