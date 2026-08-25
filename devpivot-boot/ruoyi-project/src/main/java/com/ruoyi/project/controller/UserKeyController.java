package com.ruoyi.project.controller;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.ai.domain.AiUserApiKey;
import com.ruoyi.ai.service.IAiUserApiKeyService;

/**
 * 门户·我的模型 API Key 管理（/portal/userkey，仅登录态，数据严格限定当前用户）
 * 仅操作本人的 Key（强制 userId=当前登录用户，防伪造）；admin 的全局模型配置与用户 Key 管理见后台 /system/key。
 * 说明：门户「我的模型」配置的 Key 会在生成时优先于全局配置使用（AiModelClient 按 provider 匹配覆盖）。
 * @author devpivot
 */
@RestController
@Validated
@RequestMapping("/portal/userkey")
public class UserKeyController extends BaseController
{
    private static final Logger log = LoggerFactory.getLogger(UserKeyController.class);

    @Autowired
    private IAiUserApiKeyService userApiKeyService;

    /** 我的 Key 列表（仅脱敏展示：maskedApiKey） */
    @GetMapping("/my")
    public AjaxResult my()
    {
        AiUserApiKey query = new AiUserApiKey();
        query.setUserId(getUserId());
        List<AiUserApiKey> list = userApiKeyService.selectAiUserApiKeyList(query);
        return success(list);
    }

    /** 新增我的 Key（userId 强制为当前用户，忽略客户端伪造） */
    @PostMapping
    public AjaxResult add(@RequestBody AiUserApiKey key)
    {
        key.setKeyId(null);
        key.setUserId(getUserId());
        return toAjax(userApiKeyService.insertAiUserApiKey(key));
    }

    /** 修改我的 Key（校验归属） */
    @PutMapping
    public AjaxResult edit(@RequestBody AiUserApiKey key)
    {
        if (key.getKeyId() == null || !isMine(key.getKeyId()))
        {
            return error("无权操作该密钥");
        }
        key.setUserId(getUserId());
        return toAjax(userApiKeyService.updateAiUserApiKey(key));
    }

    /** 删除我的 Key（校验归属） */
    @DeleteMapping("/{keyId}")
    public AjaxResult remove(@PathVariable Long keyId)
    {
        if (keyId == null || !isMine(keyId))
        {
            return error("无权操作该密钥");
        }
        return toAjax(userApiKeyService.deleteAiUserApiKeyByKeyId(keyId));
    }

    private boolean isMine(Long keyId)
    {
        AiUserApiKey existing = userApiKeyService.selectAiUserApiKeyByKeyId(keyId);
        return existing != null && getUserId().equals(existing.getUserId());
    }
}
