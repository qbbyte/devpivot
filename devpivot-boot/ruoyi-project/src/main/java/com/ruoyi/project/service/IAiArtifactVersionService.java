package com.ruoyi.project.service;

import java.util.List;
import java.util.Map;
import com.ruoyi.project.domain.AiArtifactVersion;

/**
 * 结果物版本Service接口
 * 版本级写操作在此收口，并统一埋点成员修改记录。
 *
 * @author devpivot
 * @date 2026-08-26
 */
public interface IAiArtifactVersionService
{
    /**
     * 保存新版本（保存即建档，V 号阶段内自增；与最近 RELEASED 快照 MD5 相同则拒绝）
     *
     * @param projectId 项目ID
     * @param body 含 stage/artifactType/versionName/snapshot/sourceType/sourceModel/changeRemark
     * @return 新版本
     */
    AiArtifactVersion saveVersion(Long projectId, Map<String, Object> body);

    /**
     * 发布版本（DRAFT -> RELEASED）
     */
    AiArtifactVersion releaseVersion(Long versionId);

    /**
     * 还原版本：快照写回业务表并自动生成新版本（source_type=RESTORE，parent_version_id=来源）
     */
    AiArtifactVersion restoreVersion(Long versionId);

    /**
     * 删除版本（仅 Manager，软约束由调用方鉴权）
     */
    int deleteVersion(Long versionId);

    /**
     * 版本列表（不含快照）
     */
    List<AiArtifactVersion> selectVersionList(Long projectId, String stage, String status);

    /**
     * 版本详情（含快照）
     */
    AiArtifactVersion selectVersionDetail(Long versionId);

    /**
     * 两个版本快照的结构化 diff
     *
     * @return {summary:{added,removed,modified}, detail:[{path,op,oldValue,newValue}]}
     */
    Map<String, Object> diffVersions(Long fromId, Long toId);

    /* ============================ 管理端（功能级鉴权，不走项目级 ProjectAccessService） ============================ */

    /**
     * 管理端版本列表：projectId 可空（全局查询），不做项目成员校验
     */
    List<AiArtifactVersion> selectAdminVersionList(Long projectId, String stage, String status);

    /**
     * 管理端版本详情（含快照）
     */
    AiArtifactVersion selectAdminVersionDetail(Long versionId);

    /**
     * 管理端发布版本（DRAFT -> RELEASED）
     */
    AiArtifactVersion releaseAdminVersion(Long versionId);

    /**
     * 管理端还原版本：快照写回业务表并自动生成新版本
     */
    AiArtifactVersion restoreAdminVersion(Long versionId);

    /**
     * 管理端删除版本
     */
    int deleteAdminVersion(Long versionId);
}
