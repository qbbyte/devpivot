package com.ruoyi.project.service;

import java.util.List;
import com.ruoyi.project.domain.AiProject;

/**
 * AI项目Service接口
 * 
 * @author ruoyi
 * @date 2026-08-04
 */
public interface IAiProjectService 
{
    /**
     * 查询AI项目
     * 
     * @param projectId AI项目主键
     * @return AI项目
     */
    public AiProject selectAiProjectByProjectId(Long projectId);

    /**
     * 查询AI项目列表
     * 
     * @param aiProject AI项目
     * @return AI项目集合
     */
    public List<AiProject> selectAiProjectList(AiProject aiProject);

    /**
     * 新增AI项目
     * 
     * @param aiProject AI项目
     * @return 结果
     */
    public int insertAiProject(AiProject aiProject);

    /**
     * 修改AI项目
     * 
     * @param aiProject AI项目
     * @return 结果
     */
    public int updateAiProject(AiProject aiProject);

    /**
     * 批量删除AI项目
     * 
     * @param projectIds 需要删除的AI项目主键集合
     * @return 结果
     */
    public int deleteAiProjectByProjectIds(Long[] projectIds);

    /**
     * 删除AI项目信息
     * 
     * @param projectId AI项目主键
     * @return 结果
     */
    public int deleteAiProjectByProjectId(Long projectId);
}
