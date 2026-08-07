package com.ruoyi.project.mapper;

import java.util.List;
import com.ruoyi.project.domain.AiProtoVersion;

/**
 * 原型历史版本Mapper接口
 *
 * @author devpivot
 * @date 2026-08-07
 */
public interface AiProtoVersionMapper
{
    /**
     * 新增原型历史版本
     *
     * @param aiProtoVersion 原型历史版本
     * @return 结果
     */
    public int insertAiProtoVersion(AiProtoVersion aiProtoVersion);

    /**
     * 按项目查询历史版本列表（按版本ID倒序）
     *
     * @param projectId 项目ID
     * @return 版本集合
     */
    public List<AiProtoVersion> selectAiProtoVersionByProjectId(Long projectId);

    /**
     * 按版本ID查询单条（含快照）
     *
     * @param versionId 版本ID
     * @return 历史版本
     */
    public AiProtoVersion selectAiProtoVersionByVersionId(Long versionId);
}
