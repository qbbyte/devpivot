package com.ruoyi.ai.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.fasterxml.jackson.annotation.JsonProperty;
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

    /** API密钥(加密存储；落库为 AES/GCM 密文，JSON 响应不回传明文) */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String apiKey;

    /** API密钥脱敏展示(仅后4位)，由 Service 层填充，不入库 */
    @Excel(name = "API密钥(脱敏)")
    private String maskedApiKey;

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

    public void setMaskedApiKey(String maskedApiKey) 
    {
        this.maskedApiKey = maskedApiKey;
    }

    public String getMaskedApiKey() 
    {
        return maskedApiKey;
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
