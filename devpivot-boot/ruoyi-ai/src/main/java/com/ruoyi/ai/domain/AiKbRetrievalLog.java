package com.ruoyi.ai.domain;

import java.util.Date;
import com.ruoyi.common.core.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 知识库检索日志对象 ai_kb_retrieval_log（调参/可观测用）
 *
 * @author devpivot
 * @date 2026-08-12
 */
public class AiKbRetrievalLog extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 日志ID */
    private Long logId;

    /** 项目ID */
    private Long projectId;

    /** 作用域 */
    private String stage;

    /** 查询文本 */
    private String queryText;

    /** 命中的切片ID（逗号分隔） */
    private String chunkIds;

    /** 模型ID */
    private String modelId;

    public void setLogId(Long logId) { this.logId = logId; }
    public Long getLogId() { return logId; }

    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public Long getProjectId() { return projectId; }

    public void setStage(String stage) { this.stage = stage; }
    public String getStage() { return stage; }

    public void setQueryText(String queryText) { this.queryText = queryText; }
    public String getQueryText() { return queryText; }

    public void setChunkIds(String chunkIds) { this.chunkIds = chunkIds; }
    public String getChunkIds() { return chunkIds; }

    public void setModelId(String modelId) { this.modelId = modelId; }
    public String getModelId() { return modelId; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("logId", getLogId())
                .append("projectId", getProjectId())
                .append("stage", getStage())
                .append("queryText", getQueryText())
                .append("chunkIds", getChunkIds())
                .append("modelId", getModelId())
                .toString();
    }
}
