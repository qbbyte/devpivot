package com.ruoyi.project.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.project.mapper.AiProjectMapper;
import com.ruoyi.project.domain.AiProject;
import com.ruoyi.project.service.IAiProjectService;

/**
 * AI项目Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-08-04
 */
@Service
public class AiProjectServiceImpl implements IAiProjectService 
{
    @Autowired
    private AiProjectMapper aiProjectMapper;

    /**
     * 查询AI项目
     * 
     * @param projectId AI项目主键
     * @return AI项目
     */
    @Override
    public AiProject selectAiProjectByProjectId(Long projectId)
    {
        return aiProjectMapper.selectAiProjectByProjectId(projectId);
    }

    /**
     * 查询AI项目列表
     * 
     * @param aiProject AI项目
     * @return AI项目
     */
    @Override
    public List<AiProject> selectAiProjectList(AiProject aiProject)
    {
        return aiProjectMapper.selectAiProjectList(aiProject);
    }

    /**
     * 新增AI项目
     * 
     * @param aiProject AI项目
     * @return 结果
     */
    @Override
    public int insertAiProject(AiProject aiProject)
    {
        aiProject.setCreateTime(DateUtils.getNowDate());
        return aiProjectMapper.insertAiProject(aiProject);
    }

    /**
     * 修改AI项目
     * 
     * @param aiProject AI项目
     * @return 结果
     */
    @Override
    public int updateAiProject(AiProject aiProject)
    {
        aiProject.setUpdateTime(DateUtils.getNowDate());
        return aiProjectMapper.updateAiProject(aiProject);
    }

    /**
     * 批量删除AI项目
     * 
     * @param projectIds 需要删除的AI项目主键
     * @return 结果
     */
    @Override
    public int deleteAiProjectByProjectIds(Long[] projectIds)
    {
        return aiProjectMapper.deleteAiProjectByProjectIds(projectIds);
    }

    /**
     * 删除AI项目信息
     * 
     * @param projectId AI项目主键
     * @return 结果
     */
    @Override
    public int deleteAiProjectByProjectId(Long projectId)
    {
        return aiProjectMapper.deleteAiProjectByProjectId(projectId);
    }
}
