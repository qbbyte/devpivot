package com.ruoyi.project.controller;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.project.domain.AiProtoComponent;
import com.ruoyi.project.service.IAiProtoComponentService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 原型组件清单Controller
 * 
 * @author ruoyi
 * @date 2026-08-04
 */
@RestController
@RequestMapping("/system/component")
public class AiProtoComponentController extends BaseController
{
    @Autowired
    private IAiProtoComponentService aiProtoComponentService;

    /**
     * 查询原型组件清单列表
     */
    @PreAuthorize("@ss.hasPermi('system:component:list')")
    @GetMapping("/list")
    public TableDataInfo list(AiProtoComponent aiProtoComponent)
    {
        startPage();
        List<AiProtoComponent> list = aiProtoComponentService.selectAiProtoComponentList(aiProtoComponent);
        return getDataTable(list);
    }

    /**
     * 导出原型组件清单列表
     */
    @PreAuthorize("@ss.hasPermi('system:component:export')")
    @Log(title = "原型组件清单", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, AiProtoComponent aiProtoComponent)
    {
        List<AiProtoComponent> list = aiProtoComponentService.selectAiProtoComponentList(aiProtoComponent);
        ExcelUtil<AiProtoComponent> util = new ExcelUtil<AiProtoComponent>(AiProtoComponent.class);
        util.exportExcel(response, list, "原型组件清单数据");
    }

    /**
     * 获取原型组件清单详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:component:query')")
    @GetMapping(value = "/{compId}")
    public AjaxResult getInfo(@PathVariable("compId") Long compId)
    {
        return success(aiProtoComponentService.selectAiProtoComponentByCompId(compId));
    }

    /**
     * 新增原型组件清单
     */
    @PreAuthorize("@ss.hasPermi('system:component:add')")
    @Log(title = "原型组件清单", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody AiProtoComponent aiProtoComponent)
    {
        return toAjax(aiProtoComponentService.insertAiProtoComponent(aiProtoComponent));
    }

    /**
     * 修改原型组件清单
     */
    @PreAuthorize("@ss.hasPermi('system:component:edit')")
    @Log(title = "原型组件清单", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody AiProtoComponent aiProtoComponent)
    {
        return toAjax(aiProtoComponentService.updateAiProtoComponent(aiProtoComponent));
    }

    /**
     * 删除原型组件清单
     */
    @PreAuthorize("@ss.hasPermi('system:component:remove')")
    @Log(title = "原型组件清单", businessType = BusinessType.DELETE)
	@DeleteMapping("/{compIds}")
    public AjaxResult remove(@PathVariable Long[] compIds)
    {
        return toAjax(aiProtoComponentService.deleteAiProtoComponentByCompIds(compIds));
    }
}
