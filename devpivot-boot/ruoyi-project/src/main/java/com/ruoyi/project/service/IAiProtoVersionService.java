package com.ruoyi.project.service;

import java.util.List;
import com.ruoyi.project.domain.AiProtoVersion;

/**
 * 原型历史版本Service接口
 *
 * @author devpivot
 * @date 2026-08-07
 */
public interface IAiProtoVersionService
{
    /**
     * 新增原型历史版本
     *
     * @param aiProtoVersion 原型历史版本
     * @return 结果
     */
    public int insertAiProtoVersion(AiProtoVersion aiProtoVersion);

    /**
     * 按项目查询历史版本列表（倒序）
     *
     * @param projectId 项目ID
     * @return 版本集合
     */
    public List<AiProtoVersion> selectAiProtoVersionByProjectId(Long projectId);

    /**
     * 按版本ID查询单条
     *
     * @param versionId 版本ID
     * @return 历史版本
     */
    public AiProtoVersion selectAiProtoVersionByVersionId(Long versionId);
}
