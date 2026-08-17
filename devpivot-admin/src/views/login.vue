<template>
  <div class="login">
    <!-- 极光背景层（纯 CSS 光晕叠加，无图片依赖） -->
    <div class="login-bg" aria-hidden="true">
      <span class="glow g1"></span>
      <span class="glow g2"></span>
      <span class="glow g3"></span>
      <span class="glow g4"></span>
      <span class="glow g5"></span>
      <span class="glow g6"></span>
    </div>

    <!-- 顶部 logo -->
    <div class="login-header">
      <img :src="logo" class="header-logo" alt="logo" />
      <div class="header-title">
        <span class="header-name">{{ title }}</span>
        <span class="header-sub">devPivot</span>
      </div>
    </div>

    <!-- 左侧文案 -->
    <div class="login-brand">
      <h1 class="brand-slogan">在同一个地方，构思、需求、<br />协作，让团队研发更高效</h1>
      <ul class="brand-feats">
        <li>永久免费试用</li>
        <li>实时在线协作</li>
        <li>需求 / PRD / 原型 / 代码 一体化</li>
      </ul>
    </div>

    <!-- 右侧白卡 -->
    <div class="login-main">
      <div class="login-card">
        <h3 class="title">登录你的 devPivot 账户</h3>
        <p class="subtitle" v-if="registerVisible">
          还没有账户？<router-link class="link-type reg-link" to="/register">免费注册</router-link>
        </p>
        <el-form ref="loginRef" :model="loginForm" :rules="loginRules" class="login-form">
          <el-form-item prop="username">
            <el-input
              v-model="loginForm.username"
              type="text"
              size="large"
              auto-complete="off"
              placeholder="账号"
            >
              <template #prefix><svg-icon icon-class="user" class="el-input__icon input-icon" /></template>
            </el-input>
          </el-form-item>
          <el-form-item prop="password">
            <el-input
              v-model="loginForm.password"
              type="password"
              size="large"
              auto-complete="off"
              placeholder="密码"
              @keyup.enter="handleLogin"
            >
              <template #prefix><svg-icon icon-class="password" class="el-input__icon input-icon" /></template>
            </el-input>
          </el-form-item>
          <el-form-item prop="code" v-if="captchaEnabled">
            <el-input
              v-model="loginForm.code"
              size="large"
              auto-complete="off"
              placeholder="验证码"
              style="width: 63%"
              @keyup.enter="handleLogin"
            >
              <template #prefix><svg-icon icon-class="validCode" class="el-input__icon input-icon" /></template>
            </el-input>
            <div class="login-code">
              <img :src="codeUrl" @click="getCode" class="login-code-img"/>
            </div>
          </el-form-item>
          <div class="login-options">
            <el-checkbox v-model="loginForm.rememberMe">记住密码</el-checkbox>
            <span class="link-type forgot-link" @click="notSupported">忘记密码</span>
          </div>
          <el-form-item style="width:100%;">
            <el-button
              :loading="loading"
              size="large"
              type="primary"
              style="width:100%;"
              @click.prevent="handleLogin"
            >
              <span v-if="!loading">登 录</span>
              <span v-else>登 录 中...</span>
            </el-button>
          </el-form-item>
        </el-form>

        <div class="oauth-divider"><span>其他方式</span></div>
        <div class="oauth-row">
          <button class="oauth-btn" type="button" @click="notSupported">
            <span class="oauth-icon wx">微</span>
            <span>微信登录</span>
          </button>
          <button class="oauth-btn" type="button" @click="notSupported">
            <span class="oauth-icon gh">G</span>
          </button>
        </div>
      </div>
    </div>

    <!-- 底部 -->
    <div class="el-login-footer">
      <span>{{ footerContent }}</span>
      <span class="footer-link" @click="notSupported">服务条款</span>
      <span class="footer-link" @click="notSupported">隐私协议</span>
    </div>
  </div>
</template>

<script setup>
import { getCodeImg, getRegisterEnabled } from "@/api/login"
import Cookies from "js-cookie"
import { encrypt, decrypt } from "@/utils/jsencrypt"
import useUserStore from '@/store/modules/user'
import defaultSettings from '@/settings'
import logo from '@/assets/logo/logo.png'

const title = import.meta.env.VITE_APP_TITLE
const footerContent = defaultSettings.footerContent
const userStore = useUserStore()
const route = useRoute()
const router = useRouter()
const { proxy } = getCurrentInstance()

const loginForm = ref({
  username: "admin",
  password: "admin123",
  rememberMe: false,
  code: "",
  uuid: ""
})

const loginRules = {
  username: [{ required: true, trigger: "blur", message: "请输入您的账号" }],
  password: [{ required: true, trigger: "blur", message: "请输入您的密码" }],
  code: [{ required: true, trigger: "change", message: "请输入验证码" }]
}

const codeUrl = ref("")
const loading = ref(false)
// 验证码开关
const captchaEnabled = ref(true)
// 注册入口开关：由后台 sys.account.registerUser 控制（免登录接口 /register/enabled）
const registerVisible = ref(false)
const redirect = ref(undefined)

watch(route, (newRoute) => {
    redirect.value = newRoute.query && newRoute.query.redirect
}, { immediate: true })

function handleLogin() {
  proxy.$refs.loginRef.validate(valid => {
    if (valid) {
      loading.value = true
      // 勾选了需要记住密码设置在 cookie 中设置记住用户名和密码
      if (loginForm.value.rememberMe) {
        Cookies.set("username", loginForm.value.username, { expires: 30 })
        Cookies.set("password", encrypt(loginForm.value.password), { expires: 30 })
        Cookies.set("rememberMe", loginForm.value.rememberMe, { expires: 30 })
      } else {
        // 否则移除
        Cookies.remove("username")
        Cookies.remove("password")
        Cookies.remove("rememberMe")
      }
      // 调用action的登录方法
      userStore.login(loginForm.value).then(() => {
        const query = route.query
        const otherQueryParams = Object.keys(query).reduce((acc, cur) => {
          if (cur !== "redirect") {
            acc[cur] = query[cur]
          }
          return acc
        }, {})
        router.push({ path: redirect.value || "/", query: otherQueryParams })
      }).catch(() => {
        loading.value = false
        // 重新获取验证码
        if (captchaEnabled.value) {
          getCode()
        }
      })
    }
  })
}

// 暂未开放的功能占位提示（微信/GitHub/忘记密码/条款等，均无后端支持）
function notSupported() {
  proxy.$modal.msgWarning("该功能暂未开放，敬请期待")
}

function getCode() {
  getCodeImg().then(res => {
    captchaEnabled.value = res.captchaEnabled === undefined ? true : res.captchaEnabled
    if (captchaEnabled.value) {
      codeUrl.value = "data:image/gif;base64," + res.img
      loginForm.value.uuid = res.uuid
    }
  })
}

function getCookie() {
  const username = Cookies.get("username")
  const password = Cookies.get("password")
  const rememberMe = Cookies.get("rememberMe")
  loginForm.value = {
    username: username === undefined ? loginForm.value.username : username,
    password: password === undefined ? loginForm.value.password : decrypt(password),
    rememberMe: rememberMe === undefined ? false : Boolean(rememberMe)
  }
}

getCode()
getCookie()

// 登录页挂载时向后端查询是否开放注册，据此显隐注册入口（失败则默认隐藏，避免死链）
getRegisterEnabled()
  .then(res => { registerVisible.value = res.data === true })
  .catch(() => { registerVisible.value = false })
</script>

<style lang='scss' scoped>
.login {
  position: relative;
  display: flex;
  width: 100%;
  min-height: 100vh;
  overflow: hidden;
  background: #f5f3ff;
}

/* 极光背景层 */
.login-bg {
  position: absolute;
  inset: 0;
  overflow: hidden;
  z-index: 0;
}
.glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(90px);
  opacity: 0.85;
}
.g1 { width: 520px; height: 520px; background: #a78bfa; left: -120px; top: -40px; }
.g2 { width: 560px; height: 560px; background: #67e8f9; right: -120px; top: -80px; }
.g3 { width: 460px; height: 460px; background: #86efac; right: 6%; top: 32%; }
.g4 { width: 540px; height: 540px; background: #f472b6; right: -150px; bottom: -120px; }
.g5 { width: 460px; height: 460px; background: #fcd34d; right: 16%; bottom: -160px; }
.g6 { width: 500px; height: 440px; background: #fda4af; left: 20%; bottom: -90px; }

/* 顶部 logo */
.login-header {
  position: absolute;
  top: 28px;
  left: 40px;
  display: flex;
  align-items: center;
  gap: 12px;
  z-index: 2;
}
.header-logo {
  height: 80px;
  width: auto;
  display: block;
}
.header-title {
  display: flex;
  flex-direction: column;
  line-height: 1.18;
}
.header-name {
  font-size: 22px;
  font-weight: 600;
  color: #ffffff;
  text-shadow: 0 1px 6px rgba(79, 70, 229, 0.35);
}
.header-sub {
  margin-top: 2px;
  font-size: 14px;
  letter-spacing: 0.5px;
  color: rgba(255, 255, 255, 0.9);
}

/* 左侧文案 */
.login-brand {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 0 12%;
  position: relative;
  z-index: 1;
}
.brand-slogan {
  margin: 0 0 26px;
  font-size: 38px;
  font-weight: 700;
  line-height: 1.45;
  color: #ffffff;
  text-shadow: 0 2px 14px rgba(79, 70, 229, 0.35), 0 1px 4px rgba(0, 0, 0, 0.15);
}
.brand-feats {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-wrap: wrap;
  gap: 14px 30px;
}
.brand-feats li {
  position: relative;
  padding-left: 22px;
  font-size: 17px;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.95);
  text-shadow: 0 1px 6px rgba(79, 70, 229, 0.3), 0 1px 3px rgba(0, 0, 0, 0.12);
}
.brand-feats li::before {
  content: "✓";
  position: absolute;
  left: 0;
  top: 0;
  color: #ffffff;
  font-weight: 700;
}

/* 右侧白卡 */
.login-main {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px 24px 24px 0;
  position: relative;
  z-index: 1;
  box-sizing: border-box;
}
.login-card {
  width: 440px;
  max-width: 100%;
  min-height: 540px;
  background: #ffffff;
  border-radius: 18px;
  padding: 32px 36px 36px;
  box-shadow: 0 22px 55px rgba(15, 23, 42, 0.18);
  display: flex;
  flex-direction: column;
  justify-content: center;
}
.title {
  margin: 0 0 24px;
  font-size: 20px;
  font-weight: 700;
  color: #0f172a;
}
.subtitle {
  margin: 0 0 24px;
  font-size: 13px;
  color: #64748b;
}
.reg-link {
  color: #3b82f6;
  font-weight: 600;
  margin-left: 2px;
}
.login-form {
  width: 100%;

  .el-input {
    height: 42px;
    input {
      height: 42px;
    }
  }
  .input-icon {
    height: 39px;
    width: 14px;
    margin-left: 0px;
  }
}
.login-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 0 0 18px;
}
.forgot-link {
  color: #3b82f6;
  cursor: pointer;
  font-size: 13px;
}
.login-code {
  width: 33%;
  height: 42px;
  float: right;
  img {
    cursor: pointer;
    vertical-align: middle;
  }
}
.login-code-img {
  height: 42px;
  padding-left: 12px;
}

/* 第三方登录 */
.oauth-divider {
  display: flex;
  align-items: center;
  margin: 22px 0 16px;
  color: #94a3b8;
  font-size: 12px;
}
.oauth-divider::before,
.oauth-divider::after {
  content: "";
  flex: 1;
  height: 1px;
  background: #e5e7eb;
}
.oauth-divider span {
  padding: 0 12px;
}
.oauth-row {
  display: flex;
  gap: 12px;
}
.oauth-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 42px;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  background: #f8fafc;
  color: #374151;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}
.oauth-btn:hover {
  background: #f1f5f9;
  border-color: #cbd5e1;
}
.oauth-icon {
  width: 22px;
  height: 22px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ffffff;
  font-size: 12px;
  font-weight: 700;
}
.oauth-icon.wx { background: #22c55e; }
.oauth-icon.gh { background: #3b82f6; }

/* 底部 */
.el-login-footer {
  height: 40px;
  line-height: 40px;
  position: fixed;
  bottom: 0;
  width: 100%;
  text-align: center;
  color: rgba(255, 255, 255, 0.9);
  font-size: 12px;
  letter-spacing: 1px;
  z-index: 2;
}
.footer-link {
  margin-left: 16px;
  cursor: pointer;
  color: rgba(255, 255, 255, 0.9);
}

/* 响应式：窄屏堆叠为上下布局 */
@media (max-width: 768px) {
  .login {
    flex-direction: column;
  }
  .login-header {
    position: static;
    padding: 24px 0 0 24px;
  }
  .login-brand {
    flex: none;
    justify-content: flex-start;
    padding: 32px 24px;
  }
  .brand-slogan {
    font-size: 22px;
  }
  .login-main {
    padding: 24px;
  }
}

/* 深色模式：登录页强制保持亮色极光 + 白卡，避免暗色输入框在白卡上突兀 */
html.dark {
  .login {
    background: #f5f3ff;
  }
  .login-card {
    background: #ffffff;
    .title { color: #0f172a; }
    .subtitle { color: #64748b; }
    .el-input__wrapper {
      background-color: #f8fafc;
      box-shadow: 0 0 0 1px #e2e8f0 inset;
    }
    .el-input__inner {
      color: #1f2937;
      &::placeholder { color: #94a3b8; }
    }
  }
}
</style>
