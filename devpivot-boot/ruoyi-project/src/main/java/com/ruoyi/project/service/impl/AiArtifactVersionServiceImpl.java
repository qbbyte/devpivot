package com.ruoyi.project.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.project.domain.AiArtifactVersion;
import com.ruoyi.project.domain.AiProtoComponent;
import com.ruoyi.project.domain.AiProtoPage;
import com.ruoyi.project.mapper.AiArtifactVersionMapper;
import com.ruoyi.project.service.IAiArtifactVersionService;
import com.ruoyi.project.service.IAiProtoComponentService;
import com.ruoyi.project.service.IAiProtoPageService;
import com.ruoyi.project.service.ProjectAccessService;
import com.ruoyi.project.support.EditHistoryRecorder;
import com.ruoyi.project.support.JsonDiffUtil;

/**
 * 结果物版本Service业务层处理
 * <p>
 * 版本级写操作全部在此收口（保存/发布/还原/删除），并统一埋点成员修改记录。
 * 还原（P0）：PROTO 阶段快照写回 ai_proto_page/component；其余阶段暂不支持，P1 扩展。
 *
 * @author devpivot
 * @date 2026-08-26
 */
@Service
public class AiArtifactVersionServiceImpl implements IAiArtifactVersionService
{
    private static final Logger log = LoggerFactory.getLogger(AiArtifactVersionServiceImpl.class);

    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_RELEASED = "RELEASED";

    @Autowired
    private AiArtifactVersionMapper versionMapper;

    @Autowired
    private ProjectAccessService access;

    @Autowired
    private EditHistoryRecorder recorder;

    @Autowired
    private IAiProtoPageService aiProtoPageService;

    @Autowired
    private IAiProtoComponentService aiProtoComponentService;

    /* ============================ 版本写操作（收口） ============================ */

    @Override
    @Transactional
    public AiArtifactVersion saveVersion(Long projectId, Map<String, Object> body)
    {
        return createVersion(projectId, body, true);
    }

    @Override
    @Transactional
    public AiArtifactVersion releaseVersion(Long versionId)
    {
        AiArtifactVersion v = requireVersion(versionId);
        access.assertWriter(v.getProjectId());
        if (STATUS_RELEASED.equals(v.getStatus()))
        {
            return v;
        }
        versionMapper.updateStatus(versionId, STATUS_RELEASED, SecurityUtils.getUsername(), DateUtils.getNowDate());
        v.setStatus(STATUS_RELEASED);
        recorder.recordVersion(v, "RELEASE", "发布了版本 " + v.getVersionNo());
        return v;
    }

    @Override
    @Transactional
    public AiArtifactVersion restoreVersion(Long versionId)
    {
        AiArtifactVersion src = requireVersion(versionId);
        access.assertWriter(src.getProjectId());
        if (!STATUS_RELEASED.equals(src.getStatus()))
        {
            throw new ServiceException("仅正式版本可还原");
        }
        writeBackToBiz(src);
        // 还原自动生成新版本（版本链）：跳过判重，还原动作本身即有效建档
        Map<String, Object> cmd = new HashMap<>(6);
        cmd.put("stage", src.getStage());
        cmd.put("artifactType", src.getArtifactType());
        cmd.put("versionName", "还原自 " + src.getVersionNo());
        cmd.put("snapshot", src.getSnapshot());
        cmd.put("sourceType", "RESTORE");
        cmd.put("sourceModel", src.getSourceModel());
        cmd.put("parentVersionId", src.getVersionId());
        AiArtifactVersion next = createVersion(src.getProjectId(), cmd, false);
        recorder.recordVersion(next, "RESTORE", "将 " + src.getVersionNo() + " 还原为 " + next.getVersionNo());
        return next;
    }

    @Override
    @Transactional
    public int deleteVersion(Long versionId)
    {
        AiArtifactVersion v = requireVersion(versionId);
        access.assertManager(v.getProjectId());
        recorder.recordVersion(v, "DELETE", "删除了版本 " + v.getVersionNo());
        return versionMapper.deleteAiArtifactVersionByVersionId(versionId);
    }

    /* ============================ 查询 ============================ */

    @Override
    public List<AiArtifactVersion> selectVersionList(Long projectId, String stage, String status)
    {
        access.assertReader(projectId);
        AiArtifactVersion query = new AiArtifactVersion();
        query.setProjectId(projectId);
        query.setStage(stage);
        query.setStatus(status);
        return versionMapper.selectAiArtifactVersionList(query);
    }

    @Override
    public AiArtifactVersion selectVersionDetail(Long versionId)
    {
        AiArtifactVersion v = requireVersion(versionId);
        access.assertReader(v.getProjectId());
        return v;
    }

    @Override
    public Map<String, Object> diffVersions(Long fromId, Long toId)
    {
        AiArtifactVersion from = requireVersion(fromId);
        AiArtifactVersion to = requireVersion(toId);
        access.assertReader(from.getProjectId());
        return JsonDiffUtil.diff(from.getSnapshot(), to.getSnapshot());
    }

    /* ============================ 内部方法 ============================ */

    /**
     * 版本建档（保存/还原共用）
     *
     * @param dedupe 是否与最近 RELEASED 快照判重（保存=true，还原=false）
     */
    private AiArtifactVersion createVersion(Long projectId, Map<String, Object> body, boolean dedupe)
    {
        access.assertWriter(projectId);
        String stage = str(body.get("stage"));
        String snapshot = str(body.get("snapshot"));
        if (stage == null || stage.isEmpty())
        {
            throw new ServiceException("阶段不能为空");
        }
        if (snapshot == null || snapshot.isEmpty())
        {
            throw new ServiceException("快照内容不能为空");
        }
        String hash = DigestUtils.md5DigestAsHex(snapshot.getBytes());
        if (dedupe)
        {
            AiArtifactVersion latest = versionMapper.selectLatestReleased(projectId, stage);
            if (latest != null && hash.equals(latest.getSnapshotHash()))
            {
                throw new ServiceException("内容未变化，无需新建版本");
            }
        }
        AiArtifactVersion v = new AiArtifactVersion();
        v.setProjectId(projectId);
        v.setStage(stage);
        v.setArtifactType(str(body.get("artifactType")) == null ? stage : str(body.get("artifactType")));
        v.setArtifactId(toLong(body.get("artifactId")));
        v.setVersionNo(nextVersionNo(projectId, stage));
        String name = str(body.get("versionName"));
        v.setVersionName(name == null || name.isEmpty() ? "版本 " + DateUtils.dateTimeNow("yyyy-MM-dd HH:mm") : name);
        v.setSnapshot(snapshot);
        v.setSnapshotHash(hash);
        v.setParentVersionId(toLong(body.get("parentVersionId")));
        v.setSourceType(str(body.get("sourceType")) == null ? "MANUAL" : str(body.get("sourceType")));
        v.setSourceModel(str(body.get("sourceModel")));
        v.setStatus(STATUS_DRAFT);
        v.setChangeRemark(str(body.get("changeRemark")));
        v.setCreateBy(SecurityUtils.getUsername());
        v.setCreateTime(DateUtils.getNowDate());
        versionMapper.insertAiArtifactVersion(v);
        recorder.recordVersion(v, "CREATE", "创建了版本 " + v.getVersionNo());
        return v;
    }

    /** 版本号分配：行锁取最新版本，V 号 +1；无历史则为 V1。唯一索引兜底并发冲突。 */
    private String nextVersionNo(Long projectId, String stage)
    {
        AiArtifactVersion last = versionMapper.selectLastForUpdate(projectId, stage);
        int next = 1;
        if (last != null && last.getVersionNo() != null)
        {
            String no = last.getVersionNo().replaceAll("[^0-9]", "");
            if (!no.isEmpty())
            {
                next = Integer.parseInt(no) + 1;
            }
        }
        return "V" + next;
    }

    private AiArtifactVersion requireVersion(Long versionId)
    {
        if (versionId == null)
        {
            throw new ServiceException("版本ID不能为空");
        }
        AiArtifactVersion v = versionMapper.selectAiArtifactVersionByVersionId(versionId);
        if (v == null)
        {
            throw new ServiceException("版本不存在");
        }
        return v;
    }

    /** 快照写回对应阶段业务表（P0 支持 PROTO，其余阶段 P1 扩展） */
    private void writeBackToBiz(AiArtifactVersion v)
    {
        switch (v.getStage())
        {
            case "PROTO":
                writeBackProto(v);
                break;
            default:
                throw new ServiceException("该阶段暂不支持还原，敬请期待");
        }
    }

    /** PROTO：快照 pages 数组 → 删除旧页面/组件并插入（与 AiProtoController.persistPages 同构） */
    @SuppressWarnings("unchecked")
    private void writeBackProto(AiArtifactVersion v)
    {
        Long projectId = v.getProjectId();
        Object parsed = JSON.parse(v.getSnapshot());
        if (!(parsed instanceof JSONArray))
        {
            throw new ServiceException("原型快照格式不正确");
        }
        JSONArray arr = (JSONArray) parsed;
        aiProtoComponentService.deleteAiProtoComponentByProjectId(projectId);
        aiProtoPageService.deleteAiProtoPageByProjectId(projectId);
        String creator = SecurityUtils.getUsername();
        Date now = DateUtils.getNowDate();
        List<AiProtoComponent> allComps = new ArrayList<>();
        for (int i = 0; i < arr.size(); i++)
        {
            Object o = arr.get(i);
            if (!(o instanceof JSONObject))
            {
                continue;
            }
            JSONObject pm = (JSONObject) o;
            AiProtoPage page = new AiProtoPage();
            page.setProjectId(projectId);
            page.setPageName(str(pm.get("pageName")));
            page.setPageDesc(str(pm.get("pageDesc")));
            page.setStatus(str(pm.get("status")) == null ? "0" : str(pm.get("status")));
            page.setDeviceType(str(pm.get("deviceType")) == null ? "WEB" : str(pm.get("deviceType")));
            page.setSourceModel("历史还原");
            page.setCreateBy(creator);
            page.setCreateTime(now);
            aiProtoPageService.insertAiProtoPage(page);
            Object compsObj = pm.get("components");
            if (compsObj instanceof JSONArray)
            {
                JSONArray comps = (JSONArray) compsObj;
                for (int j = 0; j < comps.size(); j++)
                {
                    Object co = comps.get(j);
                    if (!(co instanceof JSONObject))
                    {
                        continue;
                    }
                    allComps.add(toComponent(page.getPageId(), (JSONObject) co, creator, now, j));
                }
            }
        }
        aiProtoComponentService.batchInsertAiProtoComponent(allComps);
    }

    private AiProtoComponent toComponent(Long pageId, JSONObject cm, String creator, Date now, int sort)
    {
        AiProtoComponent c = new AiProtoComponent();
        c.setPageId(pageId);
        c.setType(str(cm.get("type")));
        c.setCompType(str(cm.get("compType")));
        c.setCompName(str(cm.get("compName")));
        c.setFieldName(str(cm.get("fieldName")));
        c.setFieldType(str(cm.get("fieldType")));
        c.setRequired(str(cm.get("required")) == null ? "N" : str(cm.get("required")));
        Object ws = cm.get("widthSpan");
        c.setWidthSpan(ws instanceof Number ? ((Number) ws).longValue() : 12L);
        c.setBizDesc(str(cm.get("bizDesc")));
        c.setInteractDesc(str(cm.get("interactDesc")));
        Object pid = cm.get("parentId");
        c.setParentId(pid instanceof Number ? ((Number) pid).longValue() : 0L);
        c.setSort((long) sort);
        c.setProps(jsonStr(cm.get("props")));
        c.setStyle(jsonStr(cm.get("style")));
        c.setInteraction(jsonStr(cm.get("interaction")));
        Map<String, Object> meta = new HashMap<>(3);
        meta.put("ep", cm.get("ep"));
        meta.put("epProps", cm.get("epProps"));
        meta.put("epText", cm.get("epText"));
        c.setMeta(JSON.toJSONString(meta));
        c.setCreateBy(creator);
        c.setCreateTime(now);
        return c;
    }

    private static String str(Object o)
    {
        return o == null ? null : String.valueOf(o);
    }

    private static Long toLong(Object o)
    {
        if (o == null)
        {
            return null;
        }
        if (o instanceof Number)
        {
            return ((Number) o).longValue();
        }
        try
        {
            return Long.parseLong(String.valueOf(o).trim());
        }
        catch (Exception e)
        {
            return null;
        }
    }

    private static String jsonStr(Object o)
    {
        if (o == null)
        {
            return null;
        }
        if (o instanceof String)
        {
            return (String) o;
        }
        return JSON.toJSONString(o);
    }
}
