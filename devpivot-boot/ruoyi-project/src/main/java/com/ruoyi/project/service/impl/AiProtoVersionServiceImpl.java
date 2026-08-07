package com.ruoyi.project.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.project.mapper.AiProtoVersionMapper;
import com.ruoyi.project.domain.AiProtoVersion;
import com.ruoyi.project.service.IAiProtoVersionService;

/**
 * 原型历史版本Service业务层处理
 *
 * @author devpivot
 * @date 2026-08-07
 */
@Service
public class AiProtoVersionServiceImpl implements IAiProtoVersionService
{
    @Autowired
    private AiProtoVersionMapper aiProtoVersionMapper;

    @Override
    public int insertAiProtoVersion(AiProtoVersion aiProtoVersion)
    {
        aiProtoVersion.setCreateTime(DateUtils.getNowDate());
        return aiProtoVersionMapper.insertAiProtoVersion(aiProtoVersion);
    }

    @Override
    public List<AiProtoVersion> selectAiProtoVersionByProjectId(Long projectId)
    {
        return aiProtoVersionMapper.selectAiProtoVersionByProjectId(projectId);
    }

    @Override
    public AiProtoVersion selectAiProtoVersionByVersionId(Long versionId)
    {
        return aiProtoVersionMapper.selectAiProtoVersionByVersionId(versionId);
    }
}
