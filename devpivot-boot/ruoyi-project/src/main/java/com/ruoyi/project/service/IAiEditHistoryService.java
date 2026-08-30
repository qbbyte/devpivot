package com.ruoyi.project.service;

import java.util.Date;
import java.util.List;
import java.util.Map;
import com.ruoyi.project.domain.AiEditHistory;

/**
 * 成员修改记录Service接口
 *
 * @author devpivot
 * @date 2026-08-26
 */
public interface IAiEditHistoryService
{
    /**
     * 新增修改记录（与业务写同事务，由调用方保证 @Transactional）
     */
    int insertAiEditHistory(AiEditHistory aiEditHistory);

    /**
     * 查询修改记录列表（分页由 Controller startPage 处理）
     */
    List<AiEditHistory> selectHistoryList(AiEditHistory query, Date startTime, Date endTime);

    /**
     * 最近 N 条（头像组入口数据源）
     */
    List<AiEditHistory> selectRecent(Long projectId, int limit);

    /**
     * 成员贡献聚合（按人：操作次数/版本操作/最近活跃）
     */
    List<Map<String, Object>> aggregateByOperator(Long projectId, Date startTime);

    /**
     * 管理端查询修改记录列表：不做项目成员校验（功能级鉴权由 Controller @PreAuthorize 兜底）
     */
    List<AiEditHistory> selectAdminHistoryList(AiEditHistory query, Date startTime, Date endTime);
}
