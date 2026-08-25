package com.ruoyi.ai.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.ai.domain.AiKbChunk;
import com.ruoyi.ai.domain.AiKbDoc;
import com.ruoyi.ai.domain.AiKbRetrievalLog;
import com.ruoyi.ai.mapper.AiKbMapper;
import com.ruoyi.ai.service.IKnowledgeRetrievalService;
import com.ruoyi.common.utils.SecurityUtils;

/**
 * 知识库检索服务实现（MySQL FULLTEXT 版）
 *
 * @author devpivot
 * @date 2026-08-12
 */
@Service
public class KnowledgeRetrievalServiceImpl implements IKnowledgeRetrievalService
{
    @Autowired
    private AiKbMapper aiKbMapper;

    /** 检索日志开关：关闭后不再写 ai_kb_retrieval_log（避免表无限膨胀）。默认开启。 */
    @Value("${kb.retrieval-log.enabled:true}")
    private boolean retrievalLogEnabled;

    /** 检索日志保留天数：cleanupRetrievalLog() 清理该天数之前的记录。默认 30 天。 */
    @Value("${kb.retrieval-log.keep-days:30}")
    private int retrievalLogKeepDays;

    private static final int TOP_K = 8;
    private static final int MAX_CONTEXT_CHARS = 4000; // 约 2000 token 预算

    @Override
    public List<AiKbChunk> retrieve(Long projectId, String stage, String query, int topK)
    {
        if (projectId == null || query == null || query.isBlank())
        {
            return new ArrayList<>();
        }
        String q = cleanQuery(query);
        if (q.isBlank())
        {
            return new ArrayList<>();
        }
        return aiKbMapper.selectChunksForRetrieve(projectId, stage, q, topK <= 0 ? TOP_K : topK);
    }

    @Override
    public String retrieveAsContext(Long projectId, String stage, String query)
    {
        return retrieveAsContext(projectId, stage, query, null);
    }

    /**
     * 检索并格式化为可注入提示词的字符串。带 modelId 的重载，便于检索日志回溯使用的模型。
     * 日志写入受 {@code kb.retrieval-log.enabled} 开关控制，关闭则不写表（避免无限膨胀）。
     */
    @Override
    public String retrieveAsContext(Long projectId, String stage, String query, String modelId)
    {
        List<AiKbChunk> chunks = retrieve(projectId, stage, query, TOP_K);
        if (chunks == null || chunks.isEmpty())
        {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        int used = 0;
        List<Long> hitIds = new ArrayList<>();
        for (AiKbChunk c : chunks)
        {
            String content = c.getContent() == null ? "" : c.getContent();
            if (used > 0 && used + content.length() > MAX_CONTEXT_CHARS)
            {
                break;
            }
            used += content.length();
            hitIds.add(c.getChunkId());
            sb.append("【参考资料-").append(c.getStage() == null ? "全局" : c.getStage()).append("】\n")
              .append(content).append("\n---\n");
        }
        if (retrievalLogEnabled)
        {
            try
            {
                AiKbRetrievalLog log = new AiKbRetrievalLog();
                log.setProjectId(projectId);
                log.setStage(stage);
                log.setQueryText(query.length() > 1000 ? query.substring(0, 1000) : query);
                log.setChunkIds(hitIds.stream().map(String::valueOf).collect(Collectors.joining(",")));
                log.setModelId(modelId);
                aiKbMapper.insertAiKbRetrievalLog(log);
            }
            catch (Exception ignored)
            {
                // 检索日志失败不影响主流程
            }
        }
        return sb.toString().trim();
    }

    /**
     * 清理超过保留天数的检索日志（管理员手动触发；项目未启用 @EnableScheduling，故不自动跑定时任务）。
     * 返回被删除的行数。开关关闭时直接返回 0。
     */
    @Override
    public int cleanupRetrievalLog()
    {
        if (!retrievalLogEnabled)
        {
            return 0;
        }
        Date before = new Date(System.currentTimeMillis() - (long) retrievalLogKeepDays * 86400000L);
        return aiKbMapper.deleteRetrievalLogBefore(before);
    }

    @Override
    public List<AiKbRetrievalLog> listRetrievalLogs(Long projectId, String stage, int limit)
    {
        int safeLimit = (limit <= 0 || limit > 500) ? 100 : limit;
        return aiKbMapper.selectAiKbRetrievalLogList(projectId, stage, safeLimit);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void indexDocument(Long projectId, String stage, String title, String content, String sourceType)
    {
        if (projectId == null || content == null || content.isBlank())
        {
            return;
        }
        String docTitle = title == null || title.isBlank() ? "未命名文档" : title;
        // 上传来源去重：同 (projectId, stage, title) 视为同一文档，覆盖式更新（先删旧文档+切片再插入），避免同名重复入库
        if ("upload".equals(sourceType))
        {
            List<AiKbDoc> exist = aiKbMapper.selectAiKbDocByTitle(projectId, stage, docTitle);
            for (AiKbDoc old : exist)
            {
                aiKbMapper.deleteAiKbChunkByDocId(old.getDocId());
                aiKbMapper.deleteAiKbDocByDocId(old.getDocId());
            }
        }
        AiKbDoc doc = new AiKbDoc();
        doc.setProjectId(projectId);
        doc.setStage(stage);
        doc.setTitle(docTitle);
        doc.setSourceType(sourceType == null ? "upload" : sourceType);
        doc.setOriginalText(content);
        doc.setChunkCount(0);
        doc.setStatus("0");
        doc.setCreateBy(SecurityUtils.getUsername());
        aiKbMapper.insertAiKbDoc(doc);

        String docTags = buildTags(docTitle, stage);
        List<String> parts = splitContent(content, 500);
        List<AiKbChunk> chunks = new ArrayList<>(parts.size());
        int seq = 0;
        int total = 0;
        for (String p : parts)
        {
            if (p.isBlank())
            {
                continue;
            }
            AiKbChunk ch = new AiKbChunk();
            ch.setDocId(doc.getDocId());
            ch.setProjectId(projectId);
            ch.setStage(stage);
            ch.setSeq(seq++);
            ch.setContent(p.trim());
            ch.setTags(docTags);
            ch.setTokens(estimateTokens(p));
            ch.setStatus("0");
            chunks.add(ch);
            total++;
        }
        if (!chunks.isEmpty())
        {
            aiKbMapper.insertAiKbChunkBatch(chunks);
        }
        doc.setChunkCount(total);
        aiKbMapper.updateAiKbDocChunkCount(doc);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void autoIndexPipelineProduct(Long projectId, String stage, String content)
    {
        if (projectId == null || content == null || content.isBlank())
        {
            return;
        }
        aiKbMapper.deleteAiKbChunkByProjectStage(projectId, stage);
        aiKbMapper.deletePipelineDoc(projectId, stage);
        indexDocument(projectId, stage, stage + " 阶段产物自动索引", content, "pipeline");
    }

    @Override
    public List<AiKbDoc> listDocs(Long projectId, String stage)
    {
        if (projectId == null)
        {
            return new ArrayList<>();
        }
        return aiKbMapper.selectAiKbDocList(projectId, stage);
    }

    @Override
    public List<AiKbDoc> listAllDocs(String stage)
    {
        return aiKbMapper.selectAiKbDocAll(stage);
    }

    @Override
    public AiKbDoc getDocById(Long docId)
    {
        if (docId == null)
        {
            return null;
        }
        return aiKbMapper.selectAiKbDocByDocId(docId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteDoc(Long docId)
    {
        if (docId == null)
        {
            return 0;
        }
        aiKbMapper.deleteAiKbChunkByDocId(docId);
        return aiKbMapper.deleteAiKbDocByDocId(docId);
    }

    /**
     * 由标题 + 阶段抽取检索标签（逗号分隔），写入切片 tags 让 FULLTEXT(tags) 真正生效。
     * 规则：提取长度≥2 的中英连续词，去重，上限 200 字符；附带阶段值（如 PRD）。
     */
    private String buildTags(String title, String stage)
    {
        if (title == null)
        {
            title = "";
        }
        List<String> tokens = new ArrayList<>();
        if (stage != null && !stage.isBlank())
        {
            tokens.add(stage);
        }
        Matcher m = TAG_TOKEN_PATTERN.matcher(title);
        Set<String> seen = new HashSet<>();
        while (m.find())
        {
            String t = m.group().toLowerCase();
            if (seen.add(t))
            {
                tokens.add(t);
            }
        }
        String joined = String.join(",", tokens);
        return joined.length() > 200 ? joined.substring(0, 200) : joined;
    }

    private static final Pattern TAG_TOKEN_PATTERN = Pattern.compile("[\\p{L}\\p{N}]{2,}");

    /** 清洗检索 query：去除 FULLTEXT BOOLEAN 模式特殊字符，保留中英文数字空白 */
    private String cleanQuery(String q)
    {
        if (q == null)
        {
            return "";
        }
        String s = q.replaceAll("[^\\p{L}\\p{N}\\s]", " ").replaceAll("\\s+", " ").trim();
        return s.length() > 2000 ? s.substring(0, 2000) : s;
    }

    /** 切片：先按换行分段，超长段再按 maxLen 硬切 */
    private List<String> splitContent(String text, int maxLen)
    {
        List<String> result = new ArrayList<>();
        String[] paras = text.split("\\r?\\n");
        for (String para : paras)
        {
            para = para.trim();
            if (para.isEmpty())
            {
                continue;
            }
            if (para.length() <= maxLen)
            {
                result.add(para);
            }
            else
            {
                for (int i = 0; i < para.length(); i += maxLen)
                {
                    result.add(para.substring(i, Math.min(para.length(), i + maxLen)));
                }
            }
        }
        return result;
    }

    private int estimateTokens(String text)
    {
        return Math.max(1, (int) (text.length() * 0.6));
    }
}
