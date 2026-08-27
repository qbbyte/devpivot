package com.ruoyi.project.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.alibaba.fastjson2.JSON;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.project.domain.AiArtifactVersion;
import com.ruoyi.project.domain.AiEditHistory;
import com.ruoyi.project.service.IAiEditHistoryService;

/**
 * 成员修改记录埋点门面
 * <p>
 * 两条采集链路统一走这里：
 * <ul>
 *   <li>版本级：VersionService 内写操作后调用 recordVersion（收口，零遗漏）</li>
 *   <li>内容级：各域 Save/Confirm 端点调用 record（按接入点清单显式埋点）</li>
 * </ul>
 * 与业务写操作同事务（由调用方保证 @Transactional）；自身异常降级不阻断主流程。
 *
 * @author devpivot
 * @date 2026-08-26
 */
@Component
public class EditHistoryRecorder
{
    private static final Logger log = LoggerFactory.getLogger(EditHistoryRecorder.class);

    @Autowired
    private IAiEditHistoryService historyService;

    /**
     * 通用埋点（内容级编辑等）
     *
     * @param projectId 项目ID
     * @param stage 阶段
     * @param action 动作（CREATE/UPDATE/DELETE/RESTORE/RELEASE/EXPORT/ROLLBACK）
     * @param targetLabel 目标对象标签
     * @param actionDesc 人类可读描述
     * @param summary 变更摘要（可空）
     * @param detail 变更详情（可空）
     */
    public void record(Long projectId, String stage, String action, String targetLabel,
                       String actionDesc, Object summary, String detail)
    {
        try
        {
            AiEditHistory h = new AiEditHistory();
            h.setProjectId(projectId);
            h.setStage(stage);
            h.setArtifactType(stage);
            h.setAction(action);
            h.setActionDesc(actionDesc);
            h.setOperatorId(SecurityUtils.getUserId());
            h.setOperatorName(SecurityUtils.getUsername());
            h.setTargetLabel(targetLabel);
            h.setChangeSummary(summary == null ? null : JSON.toJSONString(summary));
            h.setChangeDetail(detail);
            h.setCreateTime(DateUtils.getNowDate());
            historyService.insertAiEditHistory(h);
        }
        catch (Exception e)
        {
            log.warn("[history] 埋点失败(降级不阻断) projectId={} action={} err={}", projectId, action, e.getMessage());
        }
    }

    /**
     * 版本级埋点：由 VersionService 写操作后调用
     */
    public void recordVersion(AiArtifactVersion v, String action, String actionDesc)
    {
        try
        {
            AiEditHistory h = new AiEditHistory();
            h.setProjectId(v.getProjectId());
            h.setStage(v.getStage());
            h.setArtifactType(v.getArtifactType() == null ? v.getStage() : v.getArtifactType());
            h.setVersionId(v.getVersionId());
            h.setVersionNo(v.getVersionNo());
            h.setAction(action);
            h.setActionDesc(actionDesc);
            h.setOperatorId(SecurityUtils.getUserId());
            h.setOperatorName(SecurityUtils.getUsername());
            h.setTargetLabel(stageLabel(v.getStage()));
            h.setChangeSummary(buildVersionSummary(v));
            h.setCreateTime(DateUtils.getNowDate());
            historyService.insertAiEditHistory(h);
        }
        catch (Exception e)
        {
            log.warn("[history] 版本埋点失败(降级不阻断) versionId={} err={}", v.getVersionId(), e.getMessage());
        }
    }

    private String buildVersionSummary(AiArtifactVersion v)
    {
        java.util.Map<String, Object> m = new java.util.HashMap<>(3);
        m.put("versionNo", v.getVersionNo());
        m.put("sourceType", v.getSourceType());
        if (v.getChangeRemark() != null)
        {
            m.put("remark", v.getChangeRemark());
        }
        return JSON.toJSONString(m);
    }

    /** 阶段 → 目标标签 */
    public static String stageLabel(String stage)
    {
        if (stage == null)
        {
            return "结果物";
        }
        switch (stage)
        {
            case "CLARIFY": return "澄清记录";
            case "REQ":     return "需求基线";
            case "PRD":     return "PRD 文档";
            case "PROTO":   return "原型设计";
            case "TECH":    return "技术方案";
            case "DB":      return "数据库设计";
            default:        return stage;
        }
    }
}
