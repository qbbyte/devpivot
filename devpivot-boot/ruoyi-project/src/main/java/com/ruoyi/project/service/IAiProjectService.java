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
     * 生成项目上下文导出用的短期只读 token（24h 有效），供服务器终端 curl 拉取约定文件时使用
     * token 存入 Redis（键 dev:export:token:{token} = projectId），避免服务器终端携带 JWT 的不便
     *
     * @param projectId 项目ID
     * @return 一次性只读 token 字符串
     */
    public String createExportToken(Long projectId);

    /**
     * 按目标工具格式渲染项目上下文的原始文本（AGENTS.md 及其镜像），供 curl 端点直接吐出
     *
     * @param projectId 项目ID
     * @param fmt 目标格式：agents|claude|cursor|trae|vscode
     * @return 原始 Markdown 文本（cursor 含 frontmatter）
     */
    public String getProjectContextText(Long projectId, String fmt);

    /**
     * 校验导出 token 是否合法且归属指定项目；非法/过期返回 null（调用方据此返回 401）
     *
     * @param token 导出 token
     * @return 命中的 projectId，或 null
     */
    public Long verifyExportToken(String token);

    /**
     * 查询AI项目列表
     * 
     * @param aiProject AI项目
     * @return AI项目集合
     */
    public List<AiProject> selectAiProjectList(AiProject aiProject);

    /**
     * 查询当前用户可见的AI项目列表（我创建的 ∪ 我参与团队关联的项目）
     * 门户首页依赖此接口，仅按登录态隔离数据，不要求后台管理权限
     * 
     * @param aiProject AI项目过滤条件
     * @param userId 当前登录用户ID
     * @param userName 当前登录用户名
     * @return AI项目集合
     */
    public List<AiProject> selectMyProjectList(AiProject aiProject, Long userId, String userName);

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
