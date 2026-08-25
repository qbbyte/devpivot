package com.ruoyi.project.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.utils.ParamValidator;
import com.ruoyi.project.domain.AiReqBaseline;
import com.ruoyi.project.service.IAiReqBaselineService;

/**
 * 门户·需求基线数据接口（/portal/baseline，仅登录态）
 * 承载门户需求采集页的按项目读取与保存（按 projectId upsert）。
 * 后台管理 CRUD（list/export/getInfo/add/edit/remove，带权限）见同包 AiReqBaselineController（/system/baseline）。
 * @author devpivot
 */
@RestController
@Validated
@RequestMapping("/portal/baseline")
public class BaselineController extends BaseController
{
    private static final Logger log = LoggerFactory.getLogger(BaselineController.class);

    @Autowired
    private IAiReqBaselineService aiReqBaselineService;

    /**
     * 按项目ID获取需求基线（门户需求采集页 REQ 步骤依赖，仅登录态）
     */
    @GetMapping(value = "/byProject/{projectId}")
    public AjaxResult getByProject(@PathVariable("projectId") Long projectId)
    {
        return success(aiReqBaselineService.selectAiReqBaselineByProjectId(projectId));
    }

    /**
     * 保存（新增或更新）需求基线，按 projectId 做 upsert（门户保存草稿/提交需求，仅登录态）
     */
    @PostMapping("/save")
    public AjaxResult save(@RequestBody AiReqBaseline aiReqBaseline)
    {
        ParamValidator.projectId(aiReqBaseline.getProjectId());
        AiReqBaseline existing = aiReqBaselineService.selectAiReqBaselineByProjectId(aiReqBaseline.getProjectId());
        if (existing != null)
        {
            aiReqBaseline.setBaselineId(existing.getBaselineId());
            aiReqBaselineService.updateAiReqBaseline(aiReqBaseline);
        }
        else
        {
            aiReqBaselineService.insertAiReqBaseline(aiReqBaseline);
        }
        return success();
    }
}
