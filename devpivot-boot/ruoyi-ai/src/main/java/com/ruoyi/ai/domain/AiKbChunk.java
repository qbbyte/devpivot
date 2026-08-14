package com.ruoyi.ai.domain;

import java.util.Date;
import com.ruoyi.common.core.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 知识库切片对象 ai_kb_chunk
 *
 * @author devpivot
 * @date 2026-08-12
 */
public class AiKbChunk extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 切片ID */
    private Long chunkId;

    /** 文档ID */
    private Long docId;

    /** 项目ID（分区键） */
    private Long projectId;

    /** 作用域：NULL=全局；REQ/CLARIFY/PRD/PROTO/TECH/DB */
    private String stage;

    /** 顺序（同一文档内保序） */
    private Integer seq;

    /** 切片内容 */
    private String content;

    /** 关键词（FULLTEXT 加权） */
    private String tags;

    /** 预估 token 数（控噪用） */
    private Integer tokens;

    /** 状态（0正常 1停用） */
    private String status;

    public void setChunkId(Long chunkId) { this.chunkId = chunkId; }
    public Long getChunkId() { return chunkId; }

    public void setDocId(Long docId) { this.docId = docId; }
    public Long getDocId() { return docId; }

    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public Long getProjectId() { return projectId; }

    public void setStage(String stage) { this.stage = stage; }
    public String getStage() { return stage; }

    public void setSeq(Integer seq) { this.seq = seq; }
    public Integer getSeq() { return seq; }

    public void setContent(String content) { this.content = content; }
    public String getContent() { return content; }

    public void setTags(String tags) { this.tags = tags; }
    public String getTags() { return tags; }

    public void setTokens(Integer tokens) { this.tokens = tokens; }
    public Integer getTokens() { return tokens; }

    public void setStatus(String status) { this.status = status; }
    public String getStatus() { return status; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("chunkId", getChunkId())
                .append("docId", getDocId())
                .append("projectId", getProjectId())
                .append("stage", getStage())
                .append("seq", getSeq())
                .append("content", getContent())
                .append("tokens", getTokens())
                .append("status", getStatus())
                .toString();
    }
}
