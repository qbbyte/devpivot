<template>
  <header class="portal-header">
    <div class="portal-header-inner">
      <div class="portal-brand">
        <div class="brand-logo">
          <img :src="logoPng" alt="devPivot" />
        </div>
        <span class="brand-name">AI 智能需求设计</span>
      </div>
      <nav class="portal-nav">
        <router-link to="/portal" class="pn-item" :class="{ active: route.path === '/portal' }">工作台</router-link>
        <router-link to="/portal/team" class="pn-item" :class="{ active: route.path.startsWith('/portal/team') }">我的团队</router-link>
        <router-link v-hasRole="['admin']" to="/index" class="pn-item pn-admin">
          <el-icon><Setting /></el-icon>
          <span>进入管理后台</span>
        </router-link>
      </nav>
    </div>
  </header>
</template>

<script setup>
import { useRoute } from 'vue-router'
import { Setting } from '@element-plus/icons-vue'
import logoPng from '@/assets/logo/logo.png'

const route = useRoute()
</script>

<style scoped>
.portal-header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid #eef0f3;
}

.portal-header-inner {
  max-width: 1440px;
  margin: 0 auto;
  height: 68px;
  padding: 0 24px;
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
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  flex-shrink: 0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
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
  color: #1d2129;
  letter-spacing: 0.3px;
}

.portal-nav {
  display: flex;
  align-items: center;
  gap: 6px;
}

.pn-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  border-radius: 8px;
  color: #4e5969;
  font-size: 14px;
  text-decoration: none;
  transition: background 0.18s, color 0.18s;
}

.pn-item:hover {
  background: #f2f5f9;
  color: #1d2129;
}

.pn-item.active {
  color: #3370ff;
  font-weight: 600;
}

.pn-item.active:hover {
  background: transparent;
}

.pn-admin {
  color: #86909c;
  margin-left: 4px;
}

@media (max-width: 768px) {
  .portal-header-inner {
    padding: 0 16px;
  }
  .pn-item {
    padding: 8px 10px;
  }
}
</style>
