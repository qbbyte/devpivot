package com.ruoyi.project.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 原型组件清单对象 ai_proto_component
 * 
 * @author ruoyi
 * @date 2026-08-04
 */
public class AiProtoComponent extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 组件ID */
    private Long compId;

    /** 所属页面ID */
    @Excel(name = "所属页面ID")
    private Long pageId;

    /** 组件类型(LAYOUT布局/NAV导航/FORM表单/VIEW展示/BASE基础) */
    @Excel(name = "组件类型(LAYOUT布局/NAV导航/FORM表单/VIEW展示/BASE基础)")
    private String compType;

    /** 组件显示名称 */
    @Excel(name = "组件显示名称")
    private String compName;

    /** 绑定字段名 */
    @Excel(name = "绑定字段名")
    private String fieldName;

    /** 字段类型(STRING/NUMBER/DATE/.../JSON) */
    @Excel(name = "字段类型(STRING/NUMBER/DATE/.../JSON)")
    private String fieldType;

    /** 是否必填(Y/N) */
    @Excel(name = "是否必填(Y/N)")
    private String required;

    /** 默认值 */
    @Excel(name = "默认值")
    private String defaultValue;

    /** 校验规则(JSON) */
    @Excel(name = "校验规则(JSON)")
    private String validateRule;

    /** 栅格宽度占比(1-12) */
    @Excel(name = "栅格宽度占比(1-12)")
    private Long widthSpan;

    /** 业务说明 */
    @Excel(name = "业务说明")
    private String bizDesc;

    /** 交互说明 */
    @Excel(name = "交互说明")
    private String interactDesc;

    /** 组件细类(渲染器键：nav/table/button/ep-tag 等，区别于 comp_type 大类) */
    @Excel(name = "组件细类(渲染器键)")
    private String type;

    /** 业务参数(JSON: columns/menus/text/options 等) */
    @Excel(name = "业务参数(JSON)")
    private String props;

    /** 视觉样式(JSON: 对齐/尺寸/圆角/填充/阴影等) */
    @Excel(name = "视觉样式(JSON)")
    private String style;

    /** 交互配置(JSON: action/jumpTo 等) */
    @Excel(name = "交互配置(JSON)")
    private String interaction;

    /** 杂项元数据(JSON: ep/epProps/epText 等渲染扩展信息) */
    @Excel(name = "杂项元数据(JSON)")
    private String meta;

    /** 父组件ID(支持嵌套) */
    @Excel(name = "父组件ID(支持嵌套)")
    private Long parentId;

    /** 排序 */
    @Excel(name = "排序")
    private Long sort;

    public void setCompId(Long compId) 
    {
        this.compId = compId;
    }

    public Long getCompId() 
    {
        return compId;
    }

    public void setPageId(Long pageId) 
    {
        this.pageId = pageId;
    }

    public Long getPageId() 
    {
        return pageId;
    }

    public void setCompType(String compType) 
    {
        this.compType = compType;
    }

    public String getCompType() 
    {
        return compType;
    }

    public void setCompName(String compName) 
    {
        this.compName = compName;
    }

    public String getCompName() 
    {
        return compName;
    }

    public void setFieldName(String fieldName) 
    {
        this.fieldName = fieldName;
    }

    public String getFieldName() 
    {
        return fieldName;
    }

    public void setFieldType(String fieldType) 
    {
        this.fieldType = fieldType;
    }

    public String getFieldType() 
    {
        return fieldType;
    }

    public void setRequired(String required) 
    {
        this.required = required;
    }

    public String getRequired() 
    {
        return required;
    }

    public void setDefaultValue(String defaultValue) 
    {
        this.defaultValue = defaultValue;
    }

    public String getDefaultValue() 
    {
        return defaultValue;
    }

    public void setValidateRule(String validateRule) 
    {
        this.validateRule = validateRule;
    }

    public String getValidateRule() 
    {
        return validateRule;
    }

    public void setWidthSpan(Long widthSpan) 
    {
        this.widthSpan = widthSpan;
    }

    public Long getWidthSpan() 
    {
        return widthSpan;
    }

    public void setBizDesc(String bizDesc) 
    {
        this.bizDesc = bizDesc;
    }

    public String getBizDesc() 
    {
        return bizDesc;
    }

    public void setInteractDesc(String interactDesc) 
    {
        this.interactDesc = interactDesc;
    }

    public String getInteractDesc() 
    {
        return interactDesc;
    }

    public void setType(String type) 
    {
        this.type = type;
    }

    public String getType() 
    {
        return type;
    }

    public void setProps(String props) 
    {
        this.props = props;
    }

    public String getProps() 
    {
        return props;
    }

    public void setStyle(String style) 
    {
        this.style = style;
    }

    public String getStyle() 
    {
        return style;
    }

    public void setInteraction(String interaction) 
    {
        this.interaction = interaction;
    }

    public String getInteraction() 
    {
        return interaction;
    }

    public void setMeta(String meta) 
    {
        this.meta = meta;
    }

    public String getMeta() 
    {
        return meta;
    }

    public void setParentId(Long parentId) 
    {
        this.parentId = parentId;
    }

    public Long getParentId() 
    {
        return parentId;
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
            .append("compId", getCompId())
            .append("pageId", getPageId())
            .append("compType", getCompType())
            .append("compName", getCompName())
            .append("fieldName", getFieldName())
            .append("fieldType", getFieldType())
            .append("required", getRequired())
            .append("defaultValue", getDefaultValue())
            .append("validateRule", getValidateRule())
            .append("widthSpan", getWidthSpan())
            .append("bizDesc", getBizDesc())
            .append("interactDesc", getInteractDesc())
            .append("type", getType())
            .append("props", getProps())
            .append("style", getStyle())
            .append("interaction", getInteraction())
            .append("meta", getMeta())
            .append("parentId", getParentId())
            .append("sort", getSort())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
