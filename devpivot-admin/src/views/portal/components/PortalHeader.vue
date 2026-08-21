<template>
  <header class="portal-header">
    <div class="portal-header-inner">
      <div class="portal-brand">
        <div class="brand-logo">
          <img :src="logoPng" alt="devPivot" />
        </div>
        <span class="brand-name">AI 智能需求设计</span>
      </div>
      <div class="portal-nav">
        <nav class="portal-links">
          <router-link to="/portal" class="pn-item" :class="{ active: route.path === '/portal' }">工作台</router-link>
          <router-link to="/portal/team" class="pn-item" :class="{ active: route.path.startsWith('/portal/team') }">我的团队</router-link>
          <router-link v-hasRole="['admin']" to="/index" class="pn-item pn-admin">
            <el-icon><Setting /></el-icon>
            <span>进入管理后台</span>
          </router-link>
        </nav>
        <el-dropdown trigger="click" @command="onUserCommand">
          <div class="user-trigger" role="button" tabindex="0" :aria-label="`用户菜单：${userStore.nickName || '用户'}`">
            <span class="user-avatar">{{ avatarChar }}</span>
            <span class="user-name">{{ userStore.nickName || '用户' }}</span>
            <el-icon class="user-caret"><ArrowDown /></el-icon>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item disabled>
                <div class="user-info">
                  <span class="user-info-name">{{ userStore.nickName || '用户' }}</span>
                  <span class="user-info-sub">{{ userStore.userName || '' }}</span>
                </div>
              </el-dropdown-item>
              <el-dropdown-item command="logout" :divided="true">
                <el-icon><SwitchButton /></el-icon>退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>
  </header>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Setting, ArrowDown, SwitchButton } from '@element-plus/icons-vue'
import useUserStore from '@/store/modules/user'
import logoPng from '@/assets/logo/logo.png'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const avatarChar = computed(() => (userStore.nickName || '用').slice(0, 1))

function onUserCommand(cmd) {
  if (cmd === 'logout') {
    userStore.logOut().then(() => {
      location.href = '/index'
    }).catch(() => {
      location.href = '/index'
    })
  }
}
</script>

<style scoped>
.portal-header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--c-border);
}

.portal-header-inner {
  max-width: none;
  margin: 0;
  height: 68px;
  padding: 0 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.portal-brand {
  display: flex;
  align-items: center;
  gap: 12px;
}

.brand-logo {
  width: 52px;
  height: 52px;
  border-radius: 12px;
  background: var(--c-surface);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  flex-shrink: 0;
  box-shadow: 0 2px 8px rgba(37, 99, 235, 0.12);
}

.brand-logo img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  display: block;
}

.brand-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--c-text);
  letter-spacing: 0.3px;
}

.portal-nav {
  display: flex;
  align-items: center;
  gap: 10px;
}

.portal-links {
  display: flex;
  align-items: center;
  gap: 6px;
}

/* ----- 用户菜单 ----- */
.user-trigger {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 5px 10px 5px 6px;
  border-radius: 999px;
  border: 1px solid var(--c-border);
  background: var(--c-surface);
  cursor: pointer;
  transition: border-color 0.18s ease, box-shadow 0.18s ease;
  outline: none;
}
.user-trigger:hover,
.user-trigger:focus-visible {
  border-color: var(--c-primary-light);
  box-shadow: var(--shadow-sm);
}
.user-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--c-primary), var(--c-primary-light));
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  flex-shrink: 0;
}
.user-name {
  font-size: 13px;
  color: var(--c-text);
  max-width: 120px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.user-caret {
  font-size: 12px;
  color: var(--c-text-subtle);
}

.user-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 2px 0;
}
.user-info-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--c-text);
}
.user-info-sub {
  font-size: 11px;
  color: var(--c-text-subtle);
}

.pn-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  border-radius: var(--radius-sm);
  color: var(--c-text-muted);
  font-size: 14px;
  text-decoration: none;
  transition: background 0.18s, color 0.18s;
}

.pn-item:hover {
  background: var(--c-primary-bg);
  color: var(--c-text);
}

.pn-item.active {
  color: var(--c-primary);
  font-weight: 600;
}

.pn-item.active:hover {
  background: transparent;
}

.pn-admin {
  color: var(--c-text-subtle);
  margin-left: 4px;
}

@media (max-width: 768px) {
  .portal-header-inner {
    padding: 0 16px;
  }
  .pn-item {
    padding: 8px 10px;
  }
  .portal-links .pn-admin span {
    display: none;
  }
  .user-name {
    display: none;
  }
}
</style>
