package com.ruoyi.ai.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
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
        try
        {
            AiKbRetrievalLog log = new AiKbRetrievalLog();
            log.setProjectId(projectId);
            log.setStage(stage);
            log.setQueryText(query.length() > 1000 ? query.substring(0, 1000) : query);
            log.setChunkIds(hitIds.stream().map(String::valueOf).collect(Collectors.joining(",")));
            aiKbMapper.insertAiKbRetrievalLog(log);
        }
        catch (Exception ignored)
        {
            // 检索日志失败不影响主流程
        }
        return sb.toString().trim();
    }

    @Override
    public void indexDocument(Long projectId, String stage, String title, String content, String sourceType)
    {
        if (projectId == null || content == null || content.isBlank())
        {
            return;
        }
        AiKbDoc doc = new AiKbDoc();
        doc.setProjectId(projectId);
        doc.setStage(stage);
        doc.setTitle(title == null || title.isBlank() ? "未命名文档" : title);
        doc.setSourceType(sourceType == null ? "upload" : sourceType);
        doc.setOriginalText(content);
        doc.setChunkCount(0);
        doc.setStatus("0");
        doc.setCreateBy(SecurityUtils.getUsername());
        aiKbMapper.insertAiKbDoc(doc);

        List<String> parts = splitContent(content, 500);
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
            ch.setTokens(estimateTokens(p));
            ch.setStatus("0");
            aiKbMapper.insertAiKbChunk(ch);
            total++;
        }
        doc.setChunkCount(total);
        aiKbMapper.updateAiKbDocChunkCount(doc);
    }

    @Override
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
    public int deleteDoc(Long docId)
    {
        if (docId == null)
        {
            return 0;
        }
        aiKbMapper.deleteAiKbChunkByDocId(docId);
        return aiKbMapper.deleteAiKbDocByDocId(docId);
    }

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
