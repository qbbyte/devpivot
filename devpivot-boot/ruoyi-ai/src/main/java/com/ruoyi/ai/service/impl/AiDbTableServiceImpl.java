package com.ruoyi.ai.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.ai.mapper.AiDbTableMapper;
import com.ruoyi.ai.domain.AiDbTable;
import com.ruoyi.ai.service.IAiDbTableService;

/**
 * 数据库结构Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-08-04
 */
@Service
public class AiDbTableServiceImpl implements IAiDbTableService 
{
    @Autowired
    private AiDbTableMapper aiDbTableMapper;

    /**
     * 查询数据库结构
     * 
     * @param tableId 数据库结构主键
     * @return 数据库结构
     */
    @Override
    public AiDbTable selectAiDbTableByTableId(Long tableId)
    {
        return aiDbTableMapper.selectAiDbTableByTableId(tableId);
    }

    /**
     * 查询数据库结构列表
     * 
     * @param aiDbTable 数据库结构
     * @return 数据库结构
     */
    @Override
    public List<AiDbTable> selectAiDbTableList(AiDbTable aiDbTable)
    {
        return aiDbTableMapper.selectAiDbTableList(aiDbTable);
    }

    /**
     * 新增数据库结构
     * 
     * @param aiDbTable 数据库结构
     * @return 结果
     */
    @Override
    public int insertAiDbTable(AiDbTable aiDbTable)
    {
        aiDbTable.setCreateTime(DateUtils.getNowDate());
        return aiDbTableMapper.insertAiDbTable(aiDbTable);
    }

    /**
     * 修改数据库结构
     * 
     * @param aiDbTable 数据库结构
     * @return 结果
     */
    @Override
    public int updateAiDbTable(AiDbTable aiDbTable)
    {
        aiDbTable.setUpdateTime(DateUtils.getNowDate());
        return aiDbTableMapper.updateAiDbTable(aiDbTable);
    }

    /**
     * 批量删除数据库结构
     * 
     * @param tableIds 需要删除的数据库结构主键
     * @return 结果
     */
    @Override
    public int deleteAiDbTableByTableIds(Long[] tableIds)
    {
        return aiDbTableMapper.deleteAiDbTableByTableIds(tableIds);
    }

    /**
     * 删除数据库结构信息
     * 
     * @param tableId 数据库结构主键
     * @return 结果
     */
    @Override
    public int deleteAiDbTableByTableId(Long tableId)
    {
        return aiDbTableMapper.deleteAiDbTableByTableId(tableId);
    }
}
