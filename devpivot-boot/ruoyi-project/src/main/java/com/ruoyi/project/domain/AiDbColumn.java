package com.ruoyi.project.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 数据库字段定义对象 ai_db_column
 * 
 * @author ruoyi
 * @date 2026-08-04
 */
public class AiDbColumn extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 字段ID */
    private Long columnId;

    /** 所属表结构ID */
    @Excel(name = "所属表结构ID")
    private Long tableId;

    /** 字段名 */
    @Excel(name = "字段名")
    private String columnName;

    /** 字段注释 */
    @Excel(name = "字段注释")
    private String columnComment;

    /** 字段类型 */
    @Excel(name = "字段类型")
    private String columnType;

    /** 长度 */
    @Excel(name = "长度")
    private Long columnLength;

    /** 是否为空(Y/N) */
    @Excel(name = "是否为空(Y/N)")
    private String nullable;

    /** 默认值 */
    @Excel(name = "默认值")
    private String defaultValue;

    /** 是否主键(Y/N) */
    @Excel(name = "是否主键(Y/N)")
    private String isPk;

    /** 外键关联表 */
    @Excel(name = "外键关联表")
    private String fkTable;

    /** 外键关联字段 */
    @Excel(name = "外键关联字段")
    private String fkColumn;

    /** 是否唯一约束(Y/N) */
    @Excel(name = "是否唯一约束(Y/N)")
    private String isUnique;

    /** 索引类型(NORMAL普通/UNIQUE唯一/UNION联合) */
    @Excel(name = "索引类型(NORMAL普通/UNIQUE唯一/UNION联合)")
    private String indexType;

    /** 排序 */
    @Excel(name = "排序")
    private Long sort;

    public void setColumnId(Long columnId) 
    {
        this.columnId = columnId;
    }

    public Long getColumnId() 
    {
        return columnId;
    }

    public void setTableId(Long tableId) 
    {
        this.tableId = tableId;
    }

    public Long getTableId() 
    {
        return tableId;
    }

    public void setColumnName(String columnName) 
    {
        this.columnName = columnName;
    }

    public String getColumnName() 
    {
        return columnName;
    }

    public void setColumnComment(String columnComment) 
    {
        this.columnComment = columnComment;
    }

    public String getColumnComment() 
    {
        return columnComment;
    }

    public void setColumnType(String columnType) 
    {
        this.columnType = columnType;
    }

    public String getColumnType() 
    {
        return columnType;
    }

    public void setColumnLength(Long columnLength) 
    {
        this.columnLength = columnLength;
    }

    public Long getColumnLength() 
    {
        return columnLength;
    }

    public void setNullable(String nullable) 
    {
        this.nullable = nullable;
    }

    public String getNullable() 
    {
        return nullable;
    }

    public void setDefaultValue(String defaultValue) 
    {
        this.defaultValue = defaultValue;
    }

    public String getDefaultValue() 
    {
        return defaultValue;
    }

    public void setIsPk(String isPk) 
    {
        this.isPk = isPk;
    }

    public String getIsPk() 
    {
        return isPk;
    }

    public void setFkTable(String fkTable) 
    {
        this.fkTable = fkTable;
    }

    public String getFkTable() 
    {
        return fkTable;
    }

    public void setFkColumn(String fkColumn) 
    {
        this.fkColumn = fkColumn;
    }

    public String getFkColumn() 
    {
        return fkColumn;
    }

    public void setIsUnique(String isUnique) 
    {
        this.isUnique = isUnique;
    }

    public String getIsUnique() 
    {
        return isUnique;
    }

    public void setIndexType(String indexType) 
    {
        this.indexType = indexType;
    }

    public String getIndexType() 
    {
        return indexType;
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
            .append("columnId", getColumnId())
            .append("tableId", getTableId())
            .append("columnName", getColumnName())
            .append("columnComment", getColumnComment())
            .append("columnType", getColumnType())
            .append("columnLength", getColumnLength())
            .append("nullable", getNullable())
            .append("defaultValue", getDefaultValue())
            .append("isPk", getIsPk())
            .append("fkTable", getFkTable())
            .append("fkColumn", getFkColumn())
            .append("isUnique", getIsUnique())
            .append("indexType", getIndexType())
            .append("sort", getSort())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
