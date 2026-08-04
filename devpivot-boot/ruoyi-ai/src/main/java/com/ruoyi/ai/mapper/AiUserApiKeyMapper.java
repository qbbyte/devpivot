package com.ruoyi.ai.mapper;

import java.util.List;
import com.ruoyi.ai.domain.AiUserApiKey;

/**
 * 用户API Key配置Mapper接口
 * 
 * @author ruoyi
 * @date 2026-08-04
 */
public interface AiUserApiKeyMapper 
{
    /**
     * 查询用户API Key配置
     * 
     * @param keyId 用户API Key配置主键
     * @return 用户API Key配置
     */
    public AiUserApiKey selectAiUserApiKeyByKeyId(Long keyId);

    /**
     * 查询用户API Key配置列表
     * 
     * @param aiUserApiKey 用户API Key配置
     * @return 用户API Key配置集合
     */
    public List<AiUserApiKey> selectAiUserApiKeyList(AiUserApiKey aiUserApiKey);

    /**
     * 新增用户API Key配置
     * 
     * @param aiUserApiKey 用户API Key配置
     * @return 结果
     */
    public int insertAiUserApiKey(AiUserApiKey aiUserApiKey);

    /**
     * 修改用户API Key配置
     * 
     * @param aiUserApiKey 用户API Key配置
     * @return 结果
     */
    public int updateAiUserApiKey(AiUserApiKey aiUserApiKey);

    /**
     * 删除用户API Key配置
     * 
     * @param keyId 用户API Key配置主键
     * @return 结果
     */
    public int deleteAiUserApiKeyByKeyId(Long keyId);

    /**
     * 批量删除用户API Key配置
     * 
     * @param keyIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteAiUserApiKeyByKeyIds(Long[] keyIds);
}
