package com.ruoyi.project.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.project.mapper.AiProtoComponentMapper;
import com.ruoyi.project.domain.AiProtoComponent;
import com.ruoyi.project.service.IAiProtoComponentService;

/**
 * 原型组件清单Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-08-04
 */
@Service
public class AiProtoComponentServiceImpl implements IAiProtoComponentService 
{
    @Autowired
    private AiProtoComponentMapper aiProtoComponentMapper;

    /**
     * 查询原型组件清单
     * 
     * @param compId 原型组件清单主键
     * @return 原型组件清单
     */
    @Override
    public AiProtoComponent selectAiProtoComponentByCompId(Long compId)
    {
        return aiProtoComponentMapper.selectAiProtoComponentByCompId(compId);
    }

    /**
     * 查询原型组件清单列表
     * 
     * @param aiProtoComponent 原型组件清单
     * @return 原型组件清单
     */
    @Override
    public List<AiProtoComponent> selectAiProtoComponentList(AiProtoComponent aiProtoComponent)
    {
        return aiProtoComponentMapper.selectAiProtoComponentList(aiProtoComponent);
    }

    /**
     * 新增原型组件清单
     * 
     * @param aiProtoComponent 原型组件清单
     * @return 结果
     */
    @Override
    public int insertAiProtoComponent(AiProtoComponent aiProtoComponent)
    {
        aiProtoComponent.setCreateTime(DateUtils.getNowDate());
        return aiProtoComponentMapper.insertAiProtoComponent(aiProtoComponent);
    }

    /**
     * 修改原型组件清单
     * 
     * @param aiProtoComponent 原型组件清单
     * @return 结果
     */
    @Override
    public int updateAiProtoComponent(AiProtoComponent aiProtoComponent)
    {
        aiProtoComponent.setUpdateTime(DateUtils.getNowDate());
        return aiProtoComponentMapper.updateAiProtoComponent(aiProtoComponent);
    }

    /**
     * 批量删除原型组件清单
     * 
     * @param compIds 需要删除的原型组件清单主键
     * @return 结果
     */
    @Override
    public int deleteAiProtoComponentByCompIds(Long[] compIds)
    {
        return aiProtoComponentMapper.deleteAiProtoComponentByCompIds(compIds);
    }

    /**
     * 删除原型组件清单信息
     * 
     * @param compId 原型组件清单主键
     * @return 结果
     */
    @Override
    public int deleteAiProtoComponentByCompId(Long compId)
    {
        return aiProtoComponentMapper.deleteAiProtoComponentByCompId(compId);
    }

    @Override
    public List<AiProtoComponent> selectAiProtoComponentByPageId(Long pageId)
    {
        return aiProtoComponentMapper.selectAiProtoComponentByPageId(pageId);
    }

    @Override
    public int deleteAiProtoComponentByProjectId(Long projectId)
    {
        return aiProtoComponentMapper.deleteAiProtoComponentByProjectId(projectId);
    }

    @Override
    public int batchInsertAiProtoComponent(List<AiProtoComponent> list)
    {
        if (list == null || list.isEmpty())
        {
            return 0;
        }
        return aiProtoComponentMapper.batchInsertAiProtoComponent(list);
    }
}
