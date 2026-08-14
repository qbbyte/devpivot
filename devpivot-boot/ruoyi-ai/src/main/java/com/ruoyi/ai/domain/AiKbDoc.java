package com.ruoyi.ai.domain;

import java.util.Date;
import com.ruoyi.common.core.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 知识库文档对象 ai_kb_doc
 *
 * @author devpivot
 * @date 2026-08-12
 */
public class AiKbDoc extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 文档ID */
    private Long docId;

    /** 项目ID（仅作分区键，引擎不反向依赖业务） */
    private Long projectId;

    /** 作用域：NULL=全局；REQ/CLARIFY/PRD/PROTO/TECH/DB */
    private String stage;

    /** 标题 */
    private String title;

    /** 来源：upload=用户上传；pipeline=流水线产物自动索引 */
    private String sourceType;

    /** 原文 */
    private String originalText;

    /** 切片数 */
    private Integer chunkCount;

    /** 状态（0正常 1停用） */
    private String status;

    public void setDocId(Long docId) { this.docId = docId; }
    public Long getDocId() { return docId; }

    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public Long getProjectId() { return projectId; }

    public void setStage(String stage) { this.stage = stage; }
    public String getStage() { return stage; }

    public void setTitle(String title) { this.title = title; }
    public String getTitle() { return title; }

    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public String getSourceType() { return sourceType; }

    public void setOriginalText(String originalText) { this.originalText = originalText; }
    public String getOriginalText() { return originalText; }

    public void setChunkCount(Integer chunkCount) { this.chunkCount = chunkCount; }
    public Integer getChunkCount() { return chunkCount; }

    public void setStatus(String status) { this.status = status; }
    public String getStatus() { return status; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("docId", getDocId())
                .append("projectId", getProjectId())
                .append("stage", getStage())
                .append("title", getTitle())
                .append("sourceType", getSourceType())
                .append("chunkCount", getChunkCount())
                .append("status", getStatus())
                .append("createBy", getCreateBy())
                .append("createTime", getCreateTime())
                .toString();
    }
}
