package com.ruoyi.project.service;

import java.util.List;
import java.util.Map;
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
     * 项目阶段概览（含每阶段状态与实现人）
     * 门户团队页点击项目后以弹窗展示，故放开后台权限、仅要求登录态
     *
     * @param projectId 项目ID
     * @return { projectId, projectName, step, assigneeName, phases:[{step,label,status,implementer}] }
     */
    public Map<String, Object> getProjectPhases(Long projectId);

    /**
     * 项目产物概览：聚合各阶段已生成的产物文本（需求基线/澄清记录/PRD/原型/技术方案/数据库设计）
     * 门户团队页点击「产物」后以弹窗展示，供用户按需下载
     *
     * @param projectId 项目ID
     * @return { projectId, projectName, artifacts:[{ step,label,type,fileName,content,hasData }] }
     */
    public Map<String, Object> getProjectArtifacts(Long projectId);

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
