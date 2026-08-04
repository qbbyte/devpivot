package com.ruoyi.ai.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * Prompt模板对象 ai_prompt_template
 * 
 * @author ruoyi
 * @date 2026-08-04
 */
public class AiPromptTemplate extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 模板ID */
    private Long templateId;

    /** 模板编码 */
    @Excel(name = "模板编码")
    private String templateCode;

    /** 场景类型(CLARIFY/PRD/TECH/DB/CHECK/POLISH) */
    @Excel(name = "场景类型(CLARIFY/PRD/TECH/DB/CHECK/POLISH)")
    private String sceneType;

    /** 模板名称 */
    @Excel(name = "模板名称")
    private String templateName;

    /** 模板内容 */
    @Excel(name = "模板内容")
    private String templateContent;

    /** 多模型差异化Prompt(JSON) */
    @Excel(name = "多模型差异化Prompt(JSON)")
    private String modelSpecific;

    /** 是否默认(Y/N) */
    @Excel(name = "是否默认(Y/N)")
    private String isDefault;

    /** 是否启用(0启用 1停用) */
    @Excel(name = "是否启用(0启用 1停用)")
    private String isEnabled;

    public void setTemplateId(Long templateId) 
    {
        this.templateId = templateId;
    }

    public Long getTemplateId() 
    {
        return templateId;
    }

    public void setTemplateCode(String templateCode) 
    {
        this.templateCode = templateCode;
    }

    public String getTemplateCode() 
    {
        return templateCode;
    }

    public void setSceneType(String sceneType) 
    {
        this.sceneType = sceneType;
    }

    public String getSceneType() 
    {
        return sceneType;
    }

    public void setTemplateName(String templateName) 
    {
        this.templateName = templateName;
    }

    public String getTemplateName() 
    {
        return templateName;
    }

    public void setTemplateContent(String templateContent) 
    {
        this.templateContent = templateContent;
    }

    public String getTemplateContent() 
    {
        return templateContent;
    }

    public void setModelSpecific(String modelSpecific) 
    {
        this.modelSpecific = modelSpecific;
    }

    public String getModelSpecific() 
    {
        return modelSpecific;
    }

    public void setIsDefault(String isDefault) 
    {
        this.isDefault = isDefault;
    }

    public String getIsDefault() 
    {
        return isDefault;
    }

    public void setIsEnabled(String isEnabled) 
    {
        this.isEnabled = isEnabled;
    }

    public String getIsEnabled() 
    {
        return isEnabled;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("templateId", getTemplateId())
            .append("templateCode", getTemplateCode())
            .append("sceneType", getSceneType())
            .append("templateName", getTemplateName())
            .append("templateContent", getTemplateContent())
            .append("modelSpecific", getModelSpecific())
            .append("isDefault", getIsDefault())
            .append("isEnabled", getIsEnabled())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
