package com.ruoyi.web.controller.system;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.ruoyi.common.annotation.Anonymous;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.model.RegisterBody;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.framework.web.service.SysRegisterService;
import com.ruoyi.system.service.ISysConfigService;

/**
 * 注册验证
 * 
 * @author ruoyi
 */
@RestController
@RequestMapping("/register")
public class SysRegisterController extends BaseController
{
    @Autowired
    private SysRegisterService registerService;

    @Autowired
    private ISysConfigService configService;

    @PostMapping
    public AjaxResult register(@RequestBody RegisterBody user)
    {
        if (!("true".equals(configService.selectConfigByKey("sys.account.registerUser"))))
        {
            return error("当前系统没有开启注册功能！");
        }
        String msg = registerService.register(user);
        return StringUtils.isEmpty(msg) ? success() : error(msg);
    }

    /**
     * 查询是否开放注册（免登录，供登录页据此显隐注册入口）
     * 由后台参数 sys.account.registerUser 控制，默认 false
     *
     * 注意：类级 @RequestMapping("/register") + 本方法 /enabled = 实际路径 /register/enabled，
     * 与前端 api/login.js 的 getRegisterEnabled() 保持一致；
     * 此前无类级前缀导致实际路径是 /enabled，前端请求 /register/enabled 落空，
     * 被 Spring Security 判为未认证返回 401，触发全局「重新登录」弹窗死循环。
     */
    @Anonymous
    @GetMapping("/enabled")
    public AjaxResult registerEnabled()
    {
        return success("true".equals(configService.selectConfigByKey("sys.account.registerUser")));
    }
}
