package com.ruoyi.project.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.project.domain.AiArtifactVersion;

/**
 * 结果物版本Mapper接口
 *
 * @author devpivot
 * @date 2026-08-26
 */
public interface AiArtifactVersionMapper
{
    /**
     * 查询结果物版本
     *
     * @param versionId 版本ID
     * @return 结果物版本（含快照）
     */
    public AiArtifactVersion selectAiArtifactVersionByVersionId(Long versionId);

    /**
     * 查询结果物版本列表（不含快照大字段）
     *
     * @param aiArtifactVersion 查询条件
     * @return 结果物版本集合
     */
    public List<AiArtifactVersion> selectAiArtifactVersionList(AiArtifactVersion aiArtifactVersion);

    /**
     * 新增结果物版本
     *
     * @param aiArtifactVersion 结果物版本
     * @return 结果
     */
    public int insertAiArtifactVersion(AiArtifactVersion aiArtifactVersion);

    /**
     * 更新版本状态（发布/归档）
     *
     * @param versionId 版本ID
     * @param status 目标状态
     * @param updateBy 更新者
     * @param updateTime 更新时间
     * @return 结果
     */
    public int updateStatus(@Param("versionId") Long versionId, @Param("status") String status,
                            @Param("updateBy") String updateBy, @Param("updateTime") java.util.Date updateTime);

    /**
     * 删除结果物版本
     *
     * @param versionId 版本ID
     * @return 结果
     */
    public int deleteAiArtifactVersionByVersionId(Long versionId);

    /**
     * 查询某项目某阶段最新一个 RELEASED 版本（用于快照判重）
     */
    public AiArtifactVersion selectLatestReleased(@Param("projectId") Long projectId, @Param("stage") String stage);

    /**
     * 查询某项目某阶段最新一个版本（任意状态，行锁 FOR UPDATE，用于版本号分配）
     */
    public AiArtifactVersion selectLastForUpdate(@Param("projectId") Long projectId, @Param("stage") String stage);
}
