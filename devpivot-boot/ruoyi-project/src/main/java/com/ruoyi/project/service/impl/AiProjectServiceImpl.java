package com.ruoyi.project.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.ruoyi.common.utils.DateUtils;
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
