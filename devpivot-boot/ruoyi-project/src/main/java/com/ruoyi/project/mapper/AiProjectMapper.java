package com.ruoyi.project.mapper;

import java.util.List;
import com.ruoyi.project.domain.AiProject;

/**
 * AI项目Mapper接口
 * 
 * @author ruoyi
 * @date 2026-08-04
 */
public interface AiProjectMapper 
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
     * 删除AI项目
     * 
     * @param projectId AI项目主键
     * @return 结果
     */
    public int deleteAiProjectByProjectId(Long projectId);

    /**
     * 批量删除AI项目
     * 
     * @param projectIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteAiProjectByProjectIds(Long[] projectIds);
}
