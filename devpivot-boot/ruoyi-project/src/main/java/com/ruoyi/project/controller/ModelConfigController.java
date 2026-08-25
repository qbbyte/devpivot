package com.ruoyi.project.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.ai.domain.AiModelConfig;
import com.ruoyi.ai.service.IAiModelConfigService;

/**
 * 门户·系统全局模型配置只读接口（/portal/model，仅登录态）
 * 展示平台已启用的全局模型（apiKey 不回传、maskedApiKey 脱敏），供「我的模型」页对照：
 * 全局已配 Key 的模型可直接使用；未配 Key 的供应商可由用户自行配置 Key，生成时按供应商覆盖。
 * @author devpivot
 */
@RestController
@RequestMapping("/portal/model")
public class ModelConfigController extends BaseController
{
    private static final Logger log = LoggerFactory.getLogger(ModelConfigController.class);

    @Autowired
    private IAiModelConfigService modelConfigService;

    /**
     * 已启用的全局模型列表（仅暴露必要字段：模型名称/标识/供应商；
     * 接口地址与平台密钥属系统内部配置，不对门户用户返回）
     */
    @GetMapping("/global")
    public AjaxResult global()
    {
        AiModelConfig query = new AiModelConfig();
        query.setIsEnabled("0");
        List<AiModelConfig> list = modelConfigService.selectAiModelConfigList(query);
        List<Map<String, Object>> result = new ArrayList<>();
        if (list != null)
        {
            for (AiModelConfig c : list)
            {
                Map<String, Object> m = new HashMap<>(4);
                m.put("modelId", c.getModelId());
                m.put("modelName", c.getModelName());
                m.put("modelCode", c.getModelCode());
                m.put("provider", c.getProvider());
                result.add(m);
            }
        }
        return success(result);
    }
}
