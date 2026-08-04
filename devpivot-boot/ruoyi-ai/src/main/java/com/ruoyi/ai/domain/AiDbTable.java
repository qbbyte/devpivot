package com.ruoyi.ai.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 数据库结构对象 ai_db_table
 * 
 * @author ruoyi
 * @date 2026-08-04
 */
public class AiDbTable extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 表结构ID */
    private Long tableId;

    /** 项目ID */
    @Excel(name = "项目ID")
    private Long projectId;

    /** 表名 */
    @Excel(name = "表名")
    private String tableName;

    /** 表说明 */
    @Excel(name = "表说明")
    private String tableComment;

    /** 数据库类型(MySQL/PostgreSQL) */
    @Excel(name = "数据库类型(MySQL/PostgreSQL)")
    private String dbType;

    /** 表关系说明(JSON) */
    @Excel(name = "表关系说明(JSON)")
    private String relationDesc;

    /** 完整DDL脚本 */
    @Excel(name = "完整DDL脚本")
    private String ddlSql;

    /** 规范校验结果(JSON) */
    @Excel(name = "规范校验结果(JSON)")
    private String checkReport;

    /** 状态(0草稿 1已确认) */
    @Excel(name = "状态(0草稿 1已确认)")
    private String status;

    public void setTableId(Long tableId) 
    {
        this.tableId = tableId;
    }

    public Long getTableId() 
    {
        return tableId;
    }

    public void setProjectId(Long projectId) 
    {
        this.projectId = projectId;
    }

    public Long getProjectId() 
    {
        return projectId;
    }

    public void setTableName(String tableName) 
    {
        this.tableName = tableName;
    }

    public String getTableName() 
    {
        return tableName;
    }

    public void setTableComment(String tableComment) 
    {
        this.tableComment = tableComment;
    }

    public String getTableComment() 
    {
        return tableComment;
    }

    public void setDbType(String dbType) 
    {
        this.dbType = dbType;
    }

    public String getDbType() 
    {
        return dbType;
    }

    public void setRelationDesc(String relationDesc) 
    {
        this.relationDesc = relationDesc;
    }

    public String getRelationDesc() 
    {
        return relationDesc;
    }

    public void setDdlSql(String ddlSql) 
    {
        this.ddlSql = ddlSql;
    }

    public String getDdlSql() 
    {
        return ddlSql;
    }

    public void setCheckReport(String checkReport) 
    {
        this.checkReport = checkReport;
    }

    public String getCheckReport() 
    {
        return checkReport;
    }

    public void setStatus(String status) 
    {
        this.status = status;
    }

    public String getStatus() 
    {
        return status;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("tableId", getTableId())
            .append("projectId", getProjectId())
            .append("tableName", getTableName())
            .append("tableComment", getTableComment())
            .append("dbType", getDbType())
            .append("relationDesc", getRelationDesc())
            .append("ddlSql", getDdlSql())
            .append("checkReport", getCheckReport())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
