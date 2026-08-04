package com.ruoyi.ai.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * AI模型配置对象 ai_model_config
 * 
 * @author ruoyi
 * @date 2026-08-04
 */
public class AiModelConfig extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 模型ID */
    private Long modelId;

    /** 模型标识 */
    @Excel(name = "模型标识")
    private String modelCode;

    /** 模型名称 */
    @Excel(name = "模型名称")
    private String modelName;

    /** 供应商(OpenAI/DeepSeek/Qwen等) */
    @Excel(name = "供应商(OpenAI/DeepSeek/Qwen等)")
    private String provider;

    /** 接口地址 */
    @Excel(name = "接口地址")
    private String baseUrl;

    /** API密钥(加密存储) */
    @Excel(name = "API密钥(加密存储)")
    private String apiKey;

    /** 路由类型(GENERAL通用/STRUCT结构化/ENGINEER工程/LIGHT轻量) */
    @Excel(name = "路由类型(GENERAL通用/STRUCT结构化/ENGINEER工程/LIGHT轻量)")
    private String modelType;

    /** 上下文长度 */
    @Excel(name = "上下文长度")
    private Long contextLength;

    /** 是否启用(0启用 1停用) */
    @Excel(name = "是否启用(0启用 1停用)")
    private String isEnabled;

    /** 默认参数(JSON: temperature/top_p) */
    @Excel(name = "默认参数(JSON: temperature/top_p)")
    private String defaultParams;

    /** 排序 */
    @Excel(name = "排序")
    private Long sort;

    public void setModelId(Long modelId) 
    {
        this.modelId = modelId;
    }

    public Long getModelId() 
    {
        return modelId;
    }

    public void setModelCode(String modelCode) 
    {
        this.modelCode = modelCode;
    }

    public String getModelCode() 
    {
        return modelCode;
    }

    public void setModelName(String modelName) 
    {
        this.modelName = modelName;
    }

    public String getModelName() 
    {
        return modelName;
    }

    public void setProvider(String provider) 
    {
        this.provider = provider;
    }

    public String getProvider() 
    {
        return provider;
    }

    public void setBaseUrl(String baseUrl) 
    {
        this.baseUrl = baseUrl;
    }

    public String getBaseUrl() 
    {
        return baseUrl;
    }

    public void setApiKey(String apiKey) 
    {
        this.apiKey = apiKey;
    }

    public String getApiKey() 
    {
        return apiKey;
    }

    public void setModelType(String modelType) 
    {
        this.modelType = modelType;
    }

    public String getModelType() 
    {
        return modelType;
    }

    public void setContextLength(Long contextLength) 
    {
        this.contextLength = contextLength;
    }

    public Long getContextLength() 
    {
        return contextLength;
    }

    public void setIsEnabled(String isEnabled) 
    {
        this.isEnabled = isEnabled;
    }

    public String getIsEnabled() 
    {
        return isEnabled;
    }

    public void setDefaultParams(String defaultParams) 
    {
        this.defaultParams = defaultParams;
    }

    public String getDefaultParams() 
    {
        return defaultParams;
    }

    public void setSort(Long sort) 
    {
        this.sort = sort;
    }

    public Long getSort() 
    {
        return sort;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("modelId", getModelId())
            .append("modelCode", getModelCode())
            .append("modelName", getModelName())
            .append("provider", getProvider())
            .append("baseUrl", getBaseUrl())
            .append("apiKey", getApiKey())
            .append("modelType", getModelType())
            .append("contextLength", getContextLength())
            .append("isEnabled", getIsEnabled())
            .append("defaultParams", getDefaultParams())
            .append("sort", getSort())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
