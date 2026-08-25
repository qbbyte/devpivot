package com.ruoyi.project.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.core.redis.RedisCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.project.mapper.AiProjectMapper;
import com.ruoyi.project.mapper.AiClarifySessionMapper;
import com.ruoyi.project.domain.AiProject;
import com.ruoyi.project.domain.AiReqBaseline;
import com.ruoyi.project.domain.AiPrdDoc;
import com.ruoyi.project.domain.AiTechDoc;
import com.ruoyi.project.domain.AiDbDoc;
import com.ruoyi.project.domain.AiClarifySession;
import com.ruoyi.project.domain.AiClarifyRecord;
import com.ruoyi.project.domain.AiProtoPage;
import com.ruoyi.project.service.IAiProjectService;
import com.ruoyi.project.service.IAiReqBaselineService;
import com.ruoyi.project.service.IAiPrdDocService;
import com.ruoyi.project.service.IAiTechDocService;
import com.ruoyi.project.service.IAiDbDocService;
import com.ruoyi.project.service.IAiProtoPageService;
import com.ruoyi.project.service.IAiClarifyRecordService;

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
    @Autowired
    private IAiReqBaselineService aiReqBaselineService;
    @Autowired
    private IAiPrdDocService aiPrdDocService;
    @Autowired
    private IAiTechDocService aiTechDocService;
    @Autowired
    private IAiDbDocService aiDbDocService;
    @Autowired
    private IAiProtoPageService aiProtoPageService;
    @Autowired
    private IAiClarifyRecordService aiClarifyRecordService;
    @Autowired
    private AiClarifySessionMapper aiClarifySessionMapper;
    @Autowired
    private RedisCache redisCache;

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
     * 项目阶段概览：基于 project.step 计算各阶段状态，并从各阶段产物文档的 createBy 取实现人
     */
    @Override
    public Map<String, Object> getProjectPhases(Long projectId)
    {
        Map<String, Object> result = new HashMap<>();
        AiProject project = selectAiProjectByProjectId(projectId);
        if (project == null)
        {
            result.put("phases", Collections.emptyList());
            return result;
        }
        // 仅 6 个实际工作阶段；项目结束(DONE)不再单列“完成”汇总阶段，而是 6 阶段全标记为已完成
        String[] order = { "REQ", "CLARIFY", "PRD", "PROTO", "TECH", "DB" };
        String[] labels = { "需求采集", "AI 澄清", "PRD 文档", "原型设计", "技术方案", "数据库设计" };
        String curStep = project.getStep() == null ? "REQ" : project.getStep();
        int curIdx;
        if ("DONE".equals(curStep))
        {
            // 项目已结束：6 个实际阶段全部视为已完成
            curIdx = order.length;
        }
        else
        {
            curIdx = Arrays.asList(order).indexOf(curStep);
            if (curIdx < 0)
            {
                curIdx = 0;
            }
        }

        List<Map<String, Object>> phases = new ArrayList<>();
        for (int i = 0; i < order.length; i++)
        {
            Map<String, Object> phase = new HashMap<>();
            phase.put("step", order[i]);
            phase.put("label", labels[i]);
            if (i < curIdx)
            {
                phase.put("status", "done");
            }
            else if (i == curIdx)
            {
                phase.put("status", "current");
            }
            else
            {
                phase.put("status", "todo");
            }
            phase.put("implementer", getPhaseImplementer(order[i], projectId));
            phases.add(phase);
        }

        result.put("projectId", projectId);
        result.put("projectName", project.getProjectName());
        result.put("step", project.getStep());
        result.put("assigneeName", project.getAssigneeName());
        result.put("phases", phases);
        return result;
    }

    /** 取某阶段产物文档的创建人(实现人登录名)；未产出文档的阶段返回 null */
    private String getPhaseImplementer(String step, Long projectId)
    {
        switch (step)
        {
            case "REQ":
            {
                AiReqBaseline q = new AiReqBaseline();
                q.setProjectId(projectId);
                List<AiReqBaseline> list = aiReqBaselineService.selectAiReqBaselineList(q);
                return list.isEmpty() ? null : list.get(0).getCreateBy();
            }
            case "CLARIFY":
            {
                AiClarifySession s = aiClarifySessionMapper.selectAiClarifySessionByProjectId(projectId);
                return s == null ? null : s.getCreateBy();
            }
            case "PRD":
            {
                AiPrdDoc q = new AiPrdDoc();
                q.setProjectId(projectId);
                List<AiPrdDoc> list = aiPrdDocService.selectAiPrdDocList(q);
                return list.isEmpty() ? null : list.get(0).getCreateBy();
            }
            case "PROTO":
            {
                List<AiProtoPage> list = aiProtoPageService.selectAiProtoPageByProjectId(projectId);
                return list.isEmpty() ? null : list.get(0).getCreateBy();
            }
            case "TECH":
            {
                AiTechDoc q = new AiTechDoc();
                q.setProjectId(projectId);
                List<AiTechDoc> list = aiTechDocService.selectAiTechDocList(q);
                return list.isEmpty() ? null : list.get(0).getCreateBy();
            }
            case "DB":
            {
                AiDbDoc q = new AiDbDoc();
                q.setProjectId(projectId);
                List<AiDbDoc> list = aiDbDocService.selectAiDbDocList(q);
                return list.isEmpty() ? null : list.get(0).getCreateBy();
            }
            default:
                return null;
        }
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
     * 查询当前用户可见的AI项目列表（我创建的 ∪ 我参与团队关联的项目）
     * 门户首页依赖此接口，仅按登录态隔离数据，不要求后台管理权限
     */
    @Override
    public List<AiProject> selectMyProjectList(AiProject aiProject, Long userId, String userName)
    {
        return aiProjectMapper.selectMyProjectList(aiProject, userId, userName);
    }

    /**
     * 项目产物概览：聚合各阶段产物文本（库内存储，均非落盘文件，下载由前端按需生成文件）
     */
    @Override
    public Map<String, Object> getProjectArtifacts(Long projectId)
    {
        Map<String, Object> result = new HashMap<>();
        AiProject project = selectAiProjectByProjectId(projectId);
        if (project == null)
        {
            result.put("artifacts", Collections.emptyList());
            return result;
        }

        List<Map<String, Object>> artifacts = new ArrayList<>();
        // 顺序与阶段一致：需求采集 / AI 澄清 / PRD 文档 / 原型设计 / 技术方案 / 数据库设计
        collectReqBaseline(projectId, artifacts);
        collectClarify(projectId, artifacts);
        collectPrd(projectId, artifacts);
        collectProto(projectId, artifacts);
        collectTech(projectId, artifacts);
        collectDb(projectId, artifacts);

        result.put("projectId", projectId);
        result.put("projectName", project.getProjectName());
        result.put("artifacts", artifacts);
        return result;
    }

    /** 导出 token 在 Redis 中的键前缀 */
    private static final String EXPORT_TOKEN_PREFIX = "dev:export:token:";
    /** 导出 token 有效期：24 小时（秒） */
    private static final int EXPORT_TOKEN_TTL_SECONDS = 24 * 60 * 60;

    /**
     * 生成短期只读导出 token：存入 Redis，键=dev:export:token:{token}，值=projectId，24h 过期
     */
    @Override
    public String createExportToken(Long projectId)
    {
        String token = java.util.UUID.randomUUID().toString().replace("-", "");
        redisCache.setCacheObject(EXPORT_TOKEN_PREFIX + token, projectId, EXPORT_TOKEN_TTL_SECONDS, java.util.concurrent.TimeUnit.SECONDS);
        return token;
    }

    /**
     * 校验导出 token 是否合法且归属该项目；非法返回 null（调用方据此返回 401）
     * 注意：Redis 值序列化器为 FastJson2(Object.class)，小数字会被反序列化为 Integer，
     * 故用 Number 兼容，避免 Long 强转 ClassCastException
     */
    public Long verifyExportToken(String token)
    {
        if (token == null || token.isEmpty())
        {
            return null;
        }
        Object val = redisCache.getCacheObject(EXPORT_TOKEN_PREFIX + token);
        if (val == null)
        {
            return null;
        }
        if (val instanceof Number)
        {
            return ((Number) val).longValue();
        }
        try
        {
            return Long.parseLong(val.toString());
        }
        catch (NumberFormatException e)
        {
            return null;
        }
    }

    /**
     * 按目标格式渲染项目上下文原始文本（AGENTS.md 及其镜像），与前端 exportDevContext.js 同源
     */
    @Override
    public String getProjectContextText(Long projectId, String fmt)
    {
        AiProject project = selectAiProjectByProjectId(projectId);
        if (project == null)
        {
            return "# 项目不存在\n";
        }
        String body = buildAgentsMd(project);
        if ("cursor".equalsIgnoreCase(fmt))
        {
            String name = project.getProjectName() == null ? "未命名项目" : project.getProjectName();
            return "---\ndescription: devPivot 项目「" + name + "」开发上下文\nalwaysApply: true\n---\n\n" + body;
        }
        return body;
    }

    /** 构建 AGENTS.md 正文（项目宪法：概览 + 约束 + 产物索引），与前端 buildAgentsMd 保持一致 */
    private String buildAgentsMd(AiProject project)
    {
        String name = project.getProjectName() == null ? "未命名项目" : project.getProjectName();
        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(name).append("\n\n");
        sb.append("> 由 devPivot 协同研发平台导出 · 项目开发上下文\n\n");
        sb.append("## 项目概览\n");
        sb.append("- 行业分类：").append(nvl(project.getIndustryType())).append("\n");
        sb.append("- 目标用户：").append(nvl(project.getTargetUser())).append("\n");
        sb.append("- 当前阶段：").append(nvl(project.getStep())).append("\n");
        sb.append("- 负责人：").append(nvl(project.getAssigneeName())).append("\n\n");
        sb.append("## 技术约束\n");
        sb.append("- 严格按照下方各阶段产物文档进行开发，不要偏离已确认的需求与设计\n");
        sb.append("- 修改代码前，先阅读对应阶段的产物文档\n");
        sb.append("- 不要引入未在「技术方案」中列出的新技术栈或依赖\n\n");
        sb.append("## 阶段产物（位于仓库根目录）\n");
        sb.append("- 需求基线 → `requirements.md`\n");
        sb.append("- AI 澄清记录 → `clarify.md`\n");
        sb.append("- PRD 文档 → `prd.md`\n");
        sb.append("- 原型设计 → `proto.json`\n");
        sb.append("- 技术方案 → `tech.md`\n");
        sb.append("- 数据库设计 → `db.md`\n\n");
        sb.append("## 协作约定\n");
        sb.append("- 数据库变更必须同步更新 `db.md`\n");
        sb.append("- 接口变更必须同步更新 `prd.md` 与 `tech.md`\n");
        return sb.toString();
    }

    /** 需求基线：content 为 Markdown 文本 */
    private void collectReqBaseline(Long projectId, List<Map<String, Object>> artifacts)
    {
        AiReqBaseline q = new AiReqBaseline();
        q.setProjectId(projectId);
        List<AiReqBaseline> list = aiReqBaselineService.selectAiReqBaselineList(q);
        String content = list.isEmpty() ? null : list.get(0).getContent();
        Map<String, Object> a = new HashMap<>();
        a.put("step", "REQ");
        a.put("label", "需求基线");
        a.put("type", "markdown");
        a.put("fileName", "需求基线.md");
        a.put("content", content);
        a.put("hasData", content != null && !content.isEmpty());
        artifacts.add(a);
    }

    /** AI 澄清：聚合所有澄清记录 Q&A 为可读文本 */
    private void collectClarify(Long projectId, List<Map<String, Object>> artifacts)
    {
        AiClarifyRecord q = new AiClarifyRecord();
        q.setProjectId(projectId);
        List<AiClarifyRecord> list = aiClarifyRecordService.selectAiClarifyRecordList(q);
        StringBuilder sb = new StringBuilder();
        if (!list.isEmpty())
        {
            sb.append("# AI 澄清记录\n\n");
            int idx = 1;
            for (AiClarifyRecord r : list)
            {
                sb.append("## 问题 ").append(idx++).append("\n");
                sb.append("**分类**：").append(nvl(r.getCategory())).append("\n\n");
                sb.append("**问题**：").append(nvl(r.getQuestion())).append("\n\n");
                sb.append("**回答**：\n").append(nvl(r.getAnswer())).append("\n\n");
                sb.append("---\n\n");
            }
        }
        String content = sb.length() == 0 ? null : sb.toString();
        Map<String, Object> a = new HashMap<>();
        a.put("step", "CLARIFY");
        a.put("label", "AI 澄清记录");
        a.put("type", "markdown");
        a.put("fileName", "AI澄清记录.md");
        a.put("content", content);
        a.put("hasData", content != null);
        artifacts.add(a);
    }

    /** PRD 文档：content 为 Markdown 文本 */
    private void collectPrd(Long projectId, List<Map<String, Object>> artifacts)
    {
        AiPrdDoc q = new AiPrdDoc();
        q.setProjectId(projectId);
        List<AiPrdDoc> list = aiPrdDocService.selectAiPrdDocList(q);
        String content = list.isEmpty() ? null : list.get(0).getContent();
        Map<String, Object> a = new HashMap<>();
        a.put("step", "PRD");
        a.put("label", "PRD 文档");
        a.put("type", "markdown");
        a.put("fileName", "PRD文档.md");
        a.put("content", content);
        a.put("hasData", content != null && !content.isEmpty());
        artifacts.add(a);
    }

    /** 原型设计：layout 为布局 JSON，直接导出 JSON */
    private void collectProto(Long projectId, List<Map<String, Object>> artifacts)
    {
        List<AiProtoPage> list = aiProtoPageService.selectAiProtoPageByProjectId(projectId);
        StringBuilder sb = new StringBuilder();
        boolean has = !list.isEmpty();
        if (has)
        {
            sb.append("[\n");
            for (int i = 0; i < list.size(); i++)
            {
                AiProtoPage p = list.get(i);
                sb.append("  {\n");
                sb.append("    \"pageName\": ").append(jsonStr(p.getPageName())).append(",\n");
                sb.append("    \"pageDesc\": ").append(jsonStr(p.getPageDesc())).append(",\n");
                sb.append("    \"deviceType\": ").append(jsonStr(p.getDeviceType())).append(",\n");
                sb.append("    \"layout\": ").append(nvl(p.getLayout())).append("\n");
                sb.append("  }").append(i < list.size() - 1 ? "," : "").append("\n");
            }
            sb.append("]");
        }
        String content = has ? sb.toString() : null;
        Map<String, Object> a = new HashMap<>();
        a.put("step", "PROTO");
        a.put("label", "原型设计");
        a.put("type", "json");
        a.put("fileName", "原型设计.json");
        a.put("content", content);
        a.put("hasData", has);
        artifacts.add(a);
    }

    /** 技术方案：content 为 Markdown 文本 */
    private void collectTech(Long projectId, List<Map<String, Object>> artifacts)
    {
        AiTechDoc q = new AiTechDoc();
        q.setProjectId(projectId);
        List<AiTechDoc> list = aiTechDocService.selectAiTechDocList(q);
        String content = list.isEmpty() ? null : list.get(0).getContent();
        Map<String, Object> a = new HashMap<>();
        a.put("step", "TECH");
        a.put("label", "技术方案");
        a.put("type", "markdown");
        a.put("fileName", "技术方案.md");
        a.put("content", content);
        a.put("hasData", content != null && !content.isEmpty());
        artifacts.add(a);
    }

    /** 数据库设计：content 为 Markdown 文本 */
    private void collectDb(Long projectId, List<Map<String, Object>> artifacts)
    {
        AiDbDoc q = new AiDbDoc();
        q.setProjectId(projectId);
        List<AiDbDoc> list = aiDbDocService.selectAiDbDocList(q);
        String content = list.isEmpty() ? null : list.get(0).getContent();
        Map<String, Object> a = new HashMap<>();
        a.put("step", "DB");
        a.put("label", "数据库设计");
        a.put("type", "markdown");
        a.put("fileName", "数据库设计.md");
        a.put("content", content);
        a.put("hasData", content != null && !content.isEmpty());
        artifacts.add(a);
    }

    private static String nvl(String s)
    {
        return s == null ? "" : s;
    }

    private static String jsonStr(String s)
    {
        if (s == null)
        {
            return "null";
        }
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "") + "\"";
    }

    /**
     * 判断当前用户是否为该项目可写者（创建者 / 负责人 / 「我的项目」成员）
     */
    @Override
    public boolean isProjectWriter(Long projectId, Long userId, String userName)
    {
        if (projectId == null)
        {
            return false;
        }
        AiProject project = selectAiProjectByProjectId(projectId);
        if (project == null)
        {
            return false;
        }
        // 项目创建者本人
        if (userName != null && userName.equals(project.getCreateBy()))
        {
            return true;
        }
        // 项目负责人（门户创建时 assigneeId=当前用户）
        if (userId != null && userId.equals(project.getAssigneeId()))
        {
            return true;
        }
        // 我参与团队关联的项目（复用「我的项目」查询逻辑）
        List<AiProject> mine = aiProjectMapper.selectMyProjectList(new AiProject(), userId, userName);
        if (mine != null)
        {
            for (AiProject p : mine)
            {
                if (projectId.equals(p.getProjectId()))
                {
                    return true;
                }
            }
        }
        return false;
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
