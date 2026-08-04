package com.ruoyi.ai.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 用户API Key配置对象 ai_user_api_key
 * 
 * @author ruoyi
 * @date 2026-08-04
 */
public class AiUserApiKey extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 密钥ID */
    private Long keyId;

    /** 用户ID(sys_user) */
    @Excel(name = "用户ID(sys_user)")
    private Long userId;

    /** 供应商 */
    @Excel(name = "供应商")
    private String provider;

    /** API密钥(加密存储) */
    @Excel(name = "API密钥(加密存储)")
    private String apiKey;

    /** 是否启用(Y/N) */
    @Excel(name = "是否启用(Y/N)")
    private String isActive;

    /** 每日调用次数上限 */
    @Excel(name = "每日调用次数上限")
    private Long dailyQuota;

    /** 累计消耗token */
    @Excel(name = "累计消耗token")
    private Long usedTokens;

    public void setKeyId(Long keyId) 
    {
        this.keyId = keyId;
    }

    public Long getKeyId() 
    {
        return keyId;
    }

    public void setUserId(Long userId) 
    {
        this.userId = userId;
    }

    public Long getUserId() 
    {
        return userId;
    }

    public void setProvider(String provider) 
    {
        this.provider = provider;
    }

    public String getProvider() 
    {
        return provider;
    }

    public void setApiKey(String apiKey) 
    {
        this.apiKey = apiKey;
    }

    public String getApiKey() 
    {
        return apiKey;
    }

    public void setIsActive(String isActive) 
    {
        this.isActive = isActive;
    }

    public String getIsActive() 
    {
        return isActive;
    }

    public void setDailyQuota(Long dailyQuota) 
    {
        this.dailyQuota = dailyQuota;
    }

    public Long getDailyQuota() 
    {
        return dailyQuota;
    }

    public void setUsedTokens(Long usedTokens) 
    {
        this.usedTokens = usedTokens;
    }

    public Long getUsedTokens() 
    {
        return usedTokens;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("keyId", getKeyId())
            .append("userId", getUserId())
            .append("provider", getProvider())
            .append("apiKey", getApiKey())
            .append("isActive", getIsActive())
            .append("dailyQuota", getDailyQuota())
            .append("usedTokens", getUsedTokens())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
