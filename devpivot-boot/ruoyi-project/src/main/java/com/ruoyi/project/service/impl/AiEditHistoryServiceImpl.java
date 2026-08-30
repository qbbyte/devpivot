package com.ruoyi.project.service.impl;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.project.domain.AiEditHistory;
import com.ruoyi.project.mapper.AiEditHistoryMapper;
import com.ruoyi.project.service.IAiEditHistoryService;
import com.ruoyi.project.service.ProjectAccessService;

/**
 * 成员修改记录Service业务层处理
 *
 * @author devpivot
 * @date 2026-08-26
 */
@Service
public class AiEditHistoryServiceImpl implements IAiEditHistoryService
{
    @Autowired
    private AiEditHistoryMapper aiEditHistoryMapper;

    @Autowired
    private ProjectAccessService access;

    @Override
    public int insertAiEditHistory(AiEditHistory aiEditHistory)
    {
        if (aiEditHistory.getCreateTime() == null)
        {
            aiEditHistory.setCreateTime(DateUtils.getNowDate());
        }
        return aiEditHistoryMapper.insertAiEditHistory(aiEditHistory);
    }

    @Override
    public List<AiEditHistory> selectHistoryList(AiEditHistory query, Date startTime, Date endTime)
    {
        if (query.getProjectId() != null)
        {
            access.assertReader(query.getProjectId());
        }
        return doSelectHistoryList(query, startTime, endTime);
    }

    @Override
    public List<AiEditHistory> selectAdminHistoryList(AiEditHistory query, Date startTime, Date endTime)
    {
        return doSelectHistoryList(query, startTime, endTime);
    }

    private List<AiEditHistory> doSelectHistoryList(AiEditHistory query, Date startTime, Date endTime)
    {
        Map<String, Object> params = query.getParams();
        if (params == null)
        {
            params = new HashMap<>(4);
        }
        if (startTime != null)
        {
            params.put("startTime", DateUtils.parseDateToStr("yyyy-MM-dd HH:mm:ss", startTime));
        }
        if (endTime != null)
        {
            params.put("endTime", DateUtils.parseDateToStr("yyyy-MM-dd HH:mm:ss", endTime));
        }
        query.setParams(params);
        return aiEditHistoryMapper.selectAiEditHistoryList(query);
    }

    @Override
    public List<AiEditHistory> selectRecent(Long projectId, int limit)
    {
        access.assertReader(projectId);
        return aiEditHistoryMapper.selectRecent(projectId, limit);
    }

    @Override
    public List<Map<String, Object>> aggregateByOperator(Long projectId, Date startTime)
    {
        access.assertReader(projectId);
        return aiEditHistoryMapper.selectAggregateByOperator(projectId, startTime);
    }
}
