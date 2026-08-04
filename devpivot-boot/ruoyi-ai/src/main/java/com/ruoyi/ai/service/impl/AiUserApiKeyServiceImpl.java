package com.ruoyi.ai.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
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

    /**
     * 查询用户API Key配置
     * 
     * @param keyId 用户API Key配置主键
     * @return 用户API Key配置
     */
    @Override
    public AiUserApiKey selectAiUserApiKeyByKeyId(Long keyId)
    {
        return aiUserApiKeyMapper.selectAiUserApiKeyByKeyId(keyId);
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
        return aiUserApiKeyMapper.selectAiUserApiKeyList(aiUserApiKey);
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
        aiUserApiKey.setUpdateTime(DateUtils.getNowDate());
        return aiUserApiKeyMapper.updateAiUserApiKey(aiUserApiKey);
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
