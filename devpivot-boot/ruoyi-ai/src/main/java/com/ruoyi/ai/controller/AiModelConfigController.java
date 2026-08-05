package com.ruoyi.ai.controller;

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
import com.ruoyi.ai.domain.AiModelConfig;
import com.ruoyi.ai.service.AiModelClient;
import com.ruoyi.ai.service.IAiModelConfigService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * AI模型配置Controller
 * 
 * @author ruoyi
 * @date 2026-08-04
 */
@RestController
@RequestMapping("/system/aiconfig")
public class AiModelConfigController extends BaseController
{
    @Autowired
    private IAiModelConfigService aiModelConfigService;

    @Autowired
    private AiModelClient aiModelClient;

    /**
     * 查询AI模型配置列表
     */
    @PreAuthorize("@ss.hasPermi('system:aiconfig:list')")
    @GetMapping("/list")
    public TableDataInfo list(AiModelConfig aiModelConfig)
    {
        startPage();
        List<AiModelConfig> list = aiModelConfigService.selectAiModelConfigList(aiModelConfig);
        return getDataTable(list);
    }

    /**
     * 导出AI模型配置列表
     */
    @PreAuthorize("@ss.hasPermi('system:aiconfig:export')")
    @Log(title = "AI模型配置", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, AiModelConfig aiModelConfig)
    {
        List<AiModelConfig> list = aiModelConfigService.selectAiModelConfigList(aiModelConfig);
        ExcelUtil<AiModelConfig> util = new ExcelUtil<AiModelConfig>(AiModelConfig.class);
        util.exportExcel(response, list, "AI模型配置数据");
    }

    /**
     * 获取AI模型配置详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:aiconfig:query')")
    @GetMapping(value = "/{modelId}")
    public AjaxResult getInfo(@PathVariable("modelId") Long modelId)
    {
        return success(aiModelConfigService.selectAiModelConfigByModelId(modelId));
    }

    /**
     * 测试AI模型配置是否可用
     */
    @PreAuthorize("@ss.hasPermi('system:aiconfig:query')")
    @GetMapping("/test/{modelId}")
    public AjaxResult testModel(@PathVariable("modelId") Long modelId)
    {
        AiModelConfig cfg = aiModelConfigService.selectAiModelConfigByModelId(modelId);
        if (cfg == null)
        {
            return error("模型配置不存在");
        }
        return success(aiModelClient.testModel(cfg));
    }

    /**
     * 新增AI模型配置
     */
    @PreAuthorize("@ss.hasPermi('system:aiconfig:add')")
    @Log(title = "AI模型配置", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody AiModelConfig aiModelConfig)
    {
        return toAjax(aiModelConfigService.insertAiModelConfig(aiModelConfig));
    }

    /**
     * 修改AI模型配置
     */
    @PreAuthorize("@ss.hasPermi('system:aiconfig:edit')")
    @Log(title = "AI模型配置", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody AiModelConfig aiModelConfig)
    {
        return toAjax(aiModelConfigService.updateAiModelConfig(aiModelConfig));
    }

    /**
     * 删除AI模型配置
     */
    @PreAuthorize("@ss.hasPermi('system:aiconfig:remove')")
    @Log(title = "AI模型配置", businessType = BusinessType.DELETE)
	@DeleteMapping("/{modelIds}")
    public AjaxResult remove(@PathVariable Long[] modelIds)
    {
        return toAjax(aiModelConfigService.deleteAiModelConfigByModelIds(modelIds));
    }
}
