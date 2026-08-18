<template>
  <div class="register">
    <!-- 极光背景层（纯 CSS 光晕叠加，无图片依赖） -->
    <div class="login-bg" aria-hidden="true">
      <span class="glow g1"></span>
      <span class="glow g2"></span>
      <span class="glow g3"></span>
      <span class="glow g4"></span>
      <span class="glow g5"></span>
      <span class="glow g6"></span>
      <!-- 粒子点阵层（Canvas 交互：鼠标排斥场） -->
      <canvas ref="particleCanvas" class="particle-grid"></canvas>
    </div>

    <!-- 鼠标光晕层（最顶层，跟随鼠标，不阻挡点击） -->
    <div class="cursor-glow" ref="cursorGlow" aria-hidden="true"></div>

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
    <div class="register-main">
      <div class="register-card">
        <h3 class="title">注册你的 devPivot 账户</h3>
        <p class="subtitle">已有账户？<router-link class="link-type reg-link" to="/login">立即登录</router-link></p>
        <el-form ref="registerRef" :model="registerForm" :rules="registerRules" class="register-form">
          <el-form-item prop="username">
            <el-input
              v-model="registerForm.username"
              type="text"
              size="large"
              auto-complete="off"
              placeholder="账号"
            >
              <template #prefix><svg-icon icon-class="user" class="el-input__icon input-icon" /></template>
            </el-input>
          </el-form-item>
          <el-form-item prop="password" :rules="registerPwdValidator">
            <el-input
              v-model="registerForm.password"
              type="password"
              size="large"
              auto-complete="off"
              placeholder="密码"
              @keyup.enter="handleRegister"
            >
              <template #prefix><svg-icon icon-class="password" class="el-input__icon input-icon" /></template>
            </el-input>
          </el-form-item>
          <el-form-item prop="confirmPassword">
            <el-input
              v-model="registerForm.confirmPassword"
              type="password"
              size="large"
              auto-complete="off"
              placeholder="确认密码"
              @keyup.enter="handleRegister"
            >
              <template #prefix><svg-icon icon-class="password" class="el-input__icon input-icon" /></template>
            </el-input>
          </el-form-item>
          <el-form-item prop="code" v-if="captchaEnabled">
            <el-input
              size="large"
              v-model="registerForm.code"
              auto-complete="off"
              placeholder="验证码"
              style="width: 63%"
              @keyup.enter="handleRegister"
            >
              <template #prefix><svg-icon icon-class="validCode" class="el-input__icon input-icon" /></template>
            </el-input>
            <div class="register-code">
              <img :src="codeUrl" @click="getCode" class="register-code-img"/>
            </div>
          </el-form-item>
          <el-form-item style="width:100%;">
            <el-button
              :loading="loading"
              size="large"
              type="primary"
              style="width:100%;"
              @click.prevent="handleRegister"
            >
              <span v-if="!loading">注 册</span>
              <span v-else>注 册 中...</span>
            </el-button>
            <div class="register-options">
              <router-link class="link-type" to="/login">使用已有账户登录</router-link>
            </div>
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
import { ElMessageBox, ElMessage } from "element-plus"
import { getCodeImg, register } from "@/api/login"
import defaultSettings from '@/settings'
import logo from '@/assets/logo/logo.png'
import { usePasswordRule } from "@/utils/passwordRule"

const title = import.meta.env.VITE_APP_TITLE
const footerContent = defaultSettings.footerContent
const router = useRouter()
const { proxy } = getCurrentInstance()
const { registerPwdValidator } = usePasswordRule()

const registerForm = ref({
  username: "",
  password: "",
  confirmPassword: "",
  code: "",
  uuid: ""
})

const equalToPassword = (rule, value, callback) => {
  if (registerForm.value.password !== value) {
    callback(new Error("两次输入的密码不一致"))
  } else {
    callback()
  }
}

const registerRules = {
  username: [
    { required: true, trigger: "blur", message: "请输入您的账号" },
    { min: 2, max: 20, message: "用户账号长度必须介于 2 和 20 之间", trigger: "blur" }
  ],
  confirmPassword: [
    { required: true, trigger: "blur", message: "请再次输入您的密码" },
    { required: true, validator: equalToPassword, trigger: "blur" }
  ],
  code: [{ required: true, trigger: "change", message: "请输入验证码" }]
}

const codeUrl = ref("")
const loading = ref(false)
const captchaEnabled = ref(true)

// 暂未开放的功能占位提示（微信/GitHub/条款等，均无后端支持）
function notSupported() {
  ElMessage({ message: "该功能暂未开放，敬请期待", type: "warning" })
}

function handleRegister() {
  proxy.$refs.registerRef.validate(valid => {
    if (valid) {
      loading.value = true
      register(registerForm.value).then(res => {
        const username = registerForm.value.username
        ElMessageBox.alert("<font color='red'>恭喜你，您的账号 " + username + " 注册成功！</font>", "系统提示", {
          dangerouslyUseHTMLString: true,
          type: "success",
        }).then(() => {
          router.push("/login")
        }).catch(() => {})
      }).catch(() => {
        loading.value = false
        if (captchaEnabled) {
          getCode()
        }
      })
    }
  })
}

function getCode() {
  getCodeImg().then(res => {
    captchaEnabled.value = res.captchaEnabled === undefined ? true : res.captchaEnabled
    if (captchaEnabled.value) {
      codeUrl.value = "data:image/gif;base64," + res.img
      registerForm.value.uuid = res.uuid
    }
  })
}

getCode()

/* ────────────────── Canvas 粒子交互系统（鼠标排斥场）───────────────── */
const particleCanvas = ref(null)
const cursorGlow = ref(null)   // 顶层鼠标光晕元素

onMounted(() => initParticleCanvas())
onUnmounted(() => cleanupParticleCanvas())

let particles = []
let mouse = { x: -9999, y: -9999 }
let rafId = null
let canvasEl = null
let ctx = null
let dotSprite = null   // 预渲染的柔和灰点精灵（磨砂玻璃质感）

const CFG = {
  gap: 26, dotR: 0.8, subR: 0.55,
  repulseR: 130, repulseF: 55,
  friction: 0.82, springK: 0.08,
  spriteScale: 5,
}

function initParticleCanvas() {
  canvasEl = particleCanvas.value
  if (!canvasEl) return
  ctx = canvasEl.getContext('2d')
  if (!dotSprite) dotSprite = makeDotSprite()   // 预渲染一次，绘制时直接 drawImage，性能友好
  resizeCanvas()
  buildParticles()
  bindEvents()
  animate()
}

// 预渲染一个柔和径向渐变的小圆点（浅灰、边缘羽化），模拟 iOS 磨砂玻璃的柔光质感
function makeDotSprite() {
  const s = 14
  const c = document.createElement('canvas')
  c.width = s
  c.height = s
  const g = c.getContext('2d')
  const grad = g.createRadialGradient(s / 2, s / 2, 0, s / 2, s / 2, s / 2)
  grad.addColorStop(0, 'rgba(108,116,138,0.85)')   // 浅灰核心（再加深一档）
  grad.addColorStop(0.5, 'rgba(108,116,138,0.40)')
  grad.addColorStop(1, 'rgba(170,175,190,0)')       // 边缘完全透明，羽化
  g.fillStyle = grad
  g.fillRect(0, 0, s, s)
  return c
}

function cleanupParticleCanvas() {
  if (rafId) cancelAnimationFrame(rafId)
  window.removeEventListener('resize', onResize)
  window.removeEventListener('mousemove', onMouseMove)
  window.removeEventListener('mouseout', onWindowMouseOut)
  window.removeEventListener('touchmove', onTouchMove)
  window.removeEventListener('touchend', onMouseLeave)
}

function resizeCanvas() {
  const rect = canvasEl.parentElement.getBoundingClientRect()
  const dpr = Math.min(window.devicePixelRatio || 1, 2)
  canvasEl.width = rect.width * dpr
  canvasEl.height = rect.height * dpr
  ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
  canvasEl.style.width = rect.width + 'px'
  canvasEl.style.height = rect.height + 'px'
}

function onResize() { resizeCanvas(); buildParticles() }

function buildParticles() {
  particles = []
  const w = canvasEl.clientWidth, h = canvasEl.clientHeight, g = CFG.gap
  function maskAlpha(x, y) {
    const nx = x / w, ny = y / h, d = (nx + ny) / 2
    if (d < 0.35) return 0.45; if (d < 0.6) return 0.28; if (d < 0.8) return 0.12; return 0.03
  }
  for (let y = g / 2; y < h; y += g) {
    for (let x = g / 2; x < w; x += g) {
      const a = maskAlpha(x, y); if (a < 0.04) continue
      particles.push({ ox: x, oy: y, x, y, vx: 0, vy: 0, r: CFG.dotR, alpha: a })
    }
  }
  const sg = g * 1.5
  for (let y = sg / 2 + g * 0.25; y < h; y += sg) {
    for (let x = sg / 2 + g * 0.5; x < w; x += sg) {
      const a = maskAlpha(x, y) * 0.65; if (a < 0.03) continue
      particles.push({ ox: x, oy: y, x, y, vx: 0, vy: 0, r: CFG.subR, alpha: a })
    }
  }
}

function bindEvents() {
  // 鼠标/触摸事件挂在 window 上，而非 canvas 本身：
  // canvas 位于 .login-bg(z-index:0) 内，被上层 .login-brand/.register-main 覆盖，
  // 事件永远到不了 canvas；挂在 window 则页面任意位置移动都能驱动粒子。
  window.addEventListener('resize', onResize)
  window.addEventListener('mousemove', onMouseMove)
  window.addEventListener('mouseout', onWindowMouseOut)
  window.addEventListener('touchmove', onTouchMove, { passive: true })
  window.addEventListener('touchend', onMouseLeave)
}
function onMouseMove(e) { const r = canvasEl.getBoundingClientRect(); mouse.x = e.clientX - r.left; mouse.y = e.clientY - r.top; if (cursorGlow.value) { cursorGlow.value.style.transform = `translate(${e.clientX}px, ${e.clientY}px) translate(-50%, -50%)`; const overCard = e.target && e.target.closest && e.target.closest('.register-main'); cursorGlow.value.style.opacity = overCard ? '0' : '1' } }
function onTouchMove(e) { if (!e.touches[0]) return; const r = canvasEl.getBoundingClientRect(); mouse.x = e.touches[0].clientX - r.left; mouse.y = e.touches[0].clientY - r.top }
function onMouseLeave() { mouse.x = -9999; mouse.y = -9999; if (cursorGlow.value) cursorGlow.value.style.opacity = '0' }

// 鼠标移出整个浏览器窗口时重置，避免停在最后一点的排斥力残留
function onWindowMouseOut(e) {
  if (!e.relatedTarget && !e.toElement) {
    mouse.x = -9999
    mouse.y = -9999
    if (cursorGlow.value) cursorGlow.value.style.opacity = '0'
  }
}

function animate() {
  ctx.clearRect(0, 0, canvasEl.clientWidth, canvasEl.clientHeight)
  const mr2 = CFG.repulseR ** 2, mr = CFG.repulseR, mf = CFG.repulseF, fric = CFG.friction, sk = CFG.springK
  for (let i = 0; i < particles.length; i++) {
    const p = particles[i]
    const dx = p.x - mouse.x, dy = p.y - mouse.y, dist2 = dx * dx + dy * dy
    let near = 0   // 0(排斥圈边缘)~1(鼠标正下方)：用于"点亮"附近粒子
    if (dist2 < mr2 && dist2 > 0.01) {
      const dist = Math.sqrt(dist2), force = (mr - dist) / mr * mf
      p.vx += (dx / dist) * force; p.vy += (dy / dist) * force
      near = 1 - dist / mr
    }
    p.vx += (p.ox - p.x) * sk; p.vy += (p.oy - p.y) * sk
    p.vx *= fric; p.vy *= fric
    p.x += p.vx; p.y += p.vy
    // 绘制（用预渲染的柔光灰点精灵；靠近鼠标的粒子被"点亮"变大变亮）
    const size = p.r * CFG.spriteScale * (1 + near * 0.9)
    ctx.globalAlpha = Math.min(1, p.alpha * 0.95 * (1 + near * 0.7))
    ctx.drawImage(dotSprite, p.x - size / 2, p.y - size / 2, size, size)
    ctx.globalAlpha = 1
  }
  rafId = requestAnimationFrame(animate)
}
</script>

<style lang='scss' scoped>
.register {
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

/* 粒子点阵层（Canvas 交互：鼠标排斥场 + 浅灰磨砂柔光质感） */
.particle-grid {
  position: absolute;
  inset: 0;
  z-index: 1;
  pointer-events: none;   /* 不拦截卡片输入，鼠标事件由 window 统一监听 */
  filter: blur(0.4px);    /* 轻微模糊，强化 iOS 磨砂玻璃的柔化观感 */
}

/* 鼠标光晕层：最顶层跟随鼠标的柔光圈，不阻挡点击 */
.cursor-glow {
  position: fixed;
  top: 0;
  left: 0;
  width: 320px;
  height: 320px;
  border-radius: 50%;
  pointer-events: none;        /* 完全不拦截卡片/输入框点击 */
  z-index: 50;                 /* 高于卡片与文案层(z-index:1)，鼠标到哪亮到哪 */
  opacity: 0;                  /* 默认隐藏，鼠标进入后由 JS 设为 1 */
  transition: opacity 0.25s ease;
  background: radial-gradient(
    circle,
    rgba(255, 255, 255, 0.55) 0%,
    rgba(226, 232, 255, 0.30) 35%,
    rgba(226, 232, 255, 0) 70%
  );
  mix-blend-mode: screen;      /* 在浅色背景上以"提亮"方式发光，而非压暗 */
}

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
.register-main {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px 24px 24px 0;
  position: relative;
  z-index: 1;
  box-sizing: border-box;
}
.register-card {
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
.register-form {
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
.register-options {
  margin-top: 14px;
  text-align: right;
}
.register-code {
  width: 33%;
  height: 42px;
  float: right;
  img {
    cursor: pointer;
    vertical-align: middle;
  }
}
.register-code-img {
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
  .register {
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
  .register-main {
    padding: 24px;
  }
}

/* 深色模式：注册页强制保持亮色极光 + 白卡，避免暗色输入框在白卡上突兀 */
html.dark {
  .register {
    background: #f5f3ff;
  }
  .register-card {
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
