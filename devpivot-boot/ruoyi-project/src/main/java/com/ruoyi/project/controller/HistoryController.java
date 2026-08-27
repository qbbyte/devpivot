package com.ruoyi.project.controller;

import java.util.Date;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.ParamValidator;
import com.ruoyi.project.domain.AiEditHistory;
import com.ruoyi.project.service.IAiEditHistoryService;

/**
 * 成员修改记录 · 数据接口（/portal/history）
 * 腾讯文档式操作轨迹时间线：按项目/阶段/版本/操作人/时间范围查询，成员贡献聚合，最近记录（头像组入口）。
 * 鉴权在 service 层（ProjectAccessService.assertReader）。
 *
 * @author devpivot
 * @date 2026-08-26
 */
@RestController
@RequestMapping("/portal/history")
public class HistoryController extends BaseController
{
    @Autowired
    private IAiEditHistoryService historyService;

    /**
     * 修改记录时间线（分页，按时间倒序）
     * 参数：projectId, stage, versionId, operatorId, startTime, endTime
     */
    @GetMapping("/list")
    public TableDataInfo list(AiEditHistory query, Date startTime, Date endTime)
    {
        ParamValidator.projectId(query.getProjectId());
        startPage();
        List<AiEditHistory> list = historyService.selectHistoryList(query, startTime, endTime);
        return getDataTable(list);
    }

    /**
     * 最近 N 条记录（历史入口头像组数据源）
     */
    @GetMapping("/recent")
    public AjaxResult recent(@RequestParam Long projectId)
    {
        ParamValidator.projectId(projectId);
        return success(historyService.selectRecent(projectId, 3));
    }

    /**
     * 成员贡献聚合（按人：操作次数/版本操作/最近活跃）
     */
    @GetMapping("/aggregate")
    public AjaxResult aggregate(@RequestParam Long projectId, Integer days)
    {
        ParamValidator.projectId(projectId);
        Date startTime = null;
        if (days != null && days > 0)
        {
            startTime = DateUtils.addDays(DateUtils.getNowDate(), -days);
        }
        return success(historyService.aggregateByOperator(projectId, startTime));
    }
}
