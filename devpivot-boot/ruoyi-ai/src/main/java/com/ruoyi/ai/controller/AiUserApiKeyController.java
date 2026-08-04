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
import com.ruoyi.ai.domain.AiUserApiKey;
import com.ruoyi.ai.service.IAiUserApiKeyService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 用户API Key配置Controller
 * 
 * @author ruoyi
 * @date 2026-08-04
 */
@RestController
@RequestMapping("/system/key")
public class AiUserApiKeyController extends BaseController
{
    @Autowired
    private IAiUserApiKeyService aiUserApiKeyService;

    /**
     * 查询用户API Key配置列表
     */
    @PreAuthorize("@ss.hasPermi('system:key:list')")
    @GetMapping("/list")
    public TableDataInfo list(AiUserApiKey aiUserApiKey)
    {
        startPage();
        List<AiUserApiKey> list = aiUserApiKeyService.selectAiUserApiKeyList(aiUserApiKey);
        return getDataTable(list);
    }

    /**
     * 导出用户API Key配置列表
     */
    @PreAuthorize("@ss.hasPermi('system:key:export')")
    @Log(title = "用户API Key配置", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, AiUserApiKey aiUserApiKey)
    {
        List<AiUserApiKey> list = aiUserApiKeyService.selectAiUserApiKeyList(aiUserApiKey);
        ExcelUtil<AiUserApiKey> util = new ExcelUtil<AiUserApiKey>(AiUserApiKey.class);
        util.exportExcel(response, list, "用户API Key配置数据");
    }

    /**
     * 获取用户API Key配置详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:key:query')")
    @GetMapping(value = "/{keyId}")
    public AjaxResult getInfo(@PathVariable("keyId") Long keyId)
    {
        return success(aiUserApiKeyService.selectAiUserApiKeyByKeyId(keyId));
    }

    /**
     * 新增用户API Key配置
     */
    @PreAuthorize("@ss.hasPermi('system:key:add')")
    @Log(title = "用户API Key配置", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody AiUserApiKey aiUserApiKey)
    {
        return toAjax(aiUserApiKeyService.insertAiUserApiKey(aiUserApiKey));
    }

    /**
     * 修改用户API Key配置
     */
    @PreAuthorize("@ss.hasPermi('system:key:edit')")
    @Log(title = "用户API Key配置", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody AiUserApiKey aiUserApiKey)
    {
        return toAjax(aiUserApiKeyService.updateAiUserApiKey(aiUserApiKey));
    }

    /**
     * 删除用户API Key配置
     */
    @PreAuthorize("@ss.hasPermi('system:key:remove')")
    @Log(title = "用户API Key配置", businessType = BusinessType.DELETE)
	@DeleteMapping("/{keyIds}")
    public AjaxResult remove(@PathVariable Long[] keyIds)
    {
        return toAjax(aiUserApiKeyService.deleteAiUserApiKeyByKeyIds(keyIds));
    }
}
