package com.ruoyi.ai.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.ai.mapper.AiDbColumnMapper;
import com.ruoyi.ai.domain.AiDbColumn;
import com.ruoyi.ai.service.IAiDbColumnService;

/**
 * 数据库字段定义Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-08-04
 */
@Service
public class AiDbColumnServiceImpl implements IAiDbColumnService 
{
    @Autowired
    private AiDbColumnMapper aiDbColumnMapper;

    /**
     * 查询数据库字段定义
     * 
     * @param columnId 数据库字段定义主键
     * @return 数据库字段定义
     */
    @Override
    public AiDbColumn selectAiDbColumnByColumnId(Long columnId)
    {
        return aiDbColumnMapper.selectAiDbColumnByColumnId(columnId);
    }

    /**
     * 查询数据库字段定义列表
     * 
     * @param aiDbColumn 数据库字段定义
     * @return 数据库字段定义
     */
    @Override
    public List<AiDbColumn> selectAiDbColumnList(AiDbColumn aiDbColumn)
    {
        return aiDbColumnMapper.selectAiDbColumnList(aiDbColumn);
    }

    /**
     * 新增数据库字段定义
     * 
     * @param aiDbColumn 数据库字段定义
     * @return 结果
     */
    @Override
    public int insertAiDbColumn(AiDbColumn aiDbColumn)
    {
        aiDbColumn.setCreateTime(DateUtils.getNowDate());
        return aiDbColumnMapper.insertAiDbColumn(aiDbColumn);
    }

    /**
     * 修改数据库字段定义
     * 
     * @param aiDbColumn 数据库字段定义
     * @return 结果
     */
    @Override
    public int updateAiDbColumn(AiDbColumn aiDbColumn)
    {
        aiDbColumn.setUpdateTime(DateUtils.getNowDate());
        return aiDbColumnMapper.updateAiDbColumn(aiDbColumn);
    }

    /**
     * 批量删除数据库字段定义
     * 
     * @param columnIds 需要删除的数据库字段定义主键
     * @return 结果
     */
    @Override
    public int deleteAiDbColumnByColumnIds(Long[] columnIds)
    {
        return aiDbColumnMapper.deleteAiDbColumnByColumnIds(columnIds);
    }

    /**
     * 删除数据库字段定义信息
     * 
     * @param columnId 数据库字段定义主键
     * @return 结果
     */
    @Override
    public int deleteAiDbColumnByColumnId(Long columnId)
    {
        return aiDbColumnMapper.deleteAiDbColumnByColumnId(columnId);
    }
}
