<template>
  <div class="app-container home">
    <!-- 欢迎区 -->
    <el-card class="welcome-card" shadow="never">
      <div class="welcome">
        <img class="welcome-avatar" :src="userStore.avatar || defAva" alt="avatar" />
        <div class="welcome-text">
          <h2>欢迎回来，{{ displayName }} 👋</h2>
          <p>devPivot 协同研发引擎 · 让 AI 陪你从构思走到上线</p>
        </div>
        <div class="welcome-actions">
          <el-button type="primary" @click="go('/portal')">
            <el-icon><DataBoard /></el-icon>
            <span>进入研发门户</span>
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 快捷入口 -->
    <el-row :gutter="20" class="quick-row">
      <el-col v-for="item in shortcuts" :key="item.title" :xs="24" :sm="12">
        <el-card shadow="hover" class="quick-card" @click="go(item.path)">
          <el-icon class="quick-icon" :size="26">
            <component :is="item.icon" />
          </el-icon>
          <div class="quick-title">{{ item.title }}</div>
          <div class="quick-desc">{{ item.desc }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 平台能力 -->
    <el-card class="section-card" shadow="never">
      <template #header>
        <span class="section-title">平台能力</span>
      </template>
      <el-row :gutter="20">
        <el-col v-for="item in features" :key="item.title" :xs="24" :sm="12" :md="8">
          <div class="feature">
            <el-icon class="feature-icon" :size="22">
              <component :is="item.icon" />
            </el-icon>
            <div class="feature-body">
              <div class="feature-title">{{ item.title }}</div>
              <div class="feature-desc">{{ item.desc }}</div>
            </div>
          </div>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup name="Index">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import useUserStore from '@/store/modules/user'
import defAva from '@/assets/images/profile.jpg'
import {
  DataBoard,
  User,
  ChatLineSquare,
  Document,
  PieChart,
  Cpu,
  Coin,
  Files
} from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()

const displayName = computed(() => userStore.nickName || userStore.name || '开发者')

function go(path) {
  router.push(path)
}

const shortcuts = [
  { title: '研发门户', desc: '推进你的需求 / PRD / 原型 / 技术方案', icon: DataBoard, path: '/portal' },
  { title: '团队协作', desc: '实时讨论、共享知识库，多人协同研发', icon: User, path: '/portal/team' }
]

const features = [
  { title: 'AI 需求澄清', desc: '智能追问与收敛，把模糊想法变成清晰需求', icon: ChatLineSquare },
  { title: 'PRD 文档生成', desc: '基于上下文自动产出结构化产品需求文档', icon: Document },
  { title: '产品原型设计', desc: '一键生成可交互原型，快速验证产品形态', icon: PieChart },
  { title: '技术方案撰写', desc: '对接需求自动生成技术选型与实现方案', icon: Cpu },
  { title: '数据库设计', desc: '由方案推导表结构与字段，落地 DDL', icon: Coin },
  { title: '知识库检索', desc: '沉淀业务规则与术语，生成时精准引用', icon: Files }
]
</script>

<style scoped lang="scss">
.home {
  overflow-x: hidden;
  min-height: calc(100vh - 50px);
  background: #ffffff;
}

/* 欢迎区 —— 白色卡片 */
.welcome-card {
  border: 1px solid #eef0f3;
  border-radius: 18px;
  background: #ffffff;
  color: #0f172a;
  box-shadow: 0 8px 32px rgba(15, 23, 42, 0.08);
  margin-bottom: 20px;

  :deep(.el-card__body) {
    padding: 28px 32px;
  }

  .welcome {
    display: flex;
    align-items: center;
    gap: 20px;
    flex-wrap: wrap;
  }

  .welcome-avatar {
    flex: 0 0 auto;
    width: 56px;
    height: 56px;
    border-radius: 50%;
    object-fit: cover;
    box-shadow: 0 4px 14px rgba(14, 165, 233, 0.25);
  }

  .welcome-text {
    flex: 1 1 240px;
    min-width: 0;

    h2 {
      margin: 0;
      font-size: 22px;
      font-weight: 700;
      color: #0f172a;
    }

    p {
      margin: 6px 0 0;
      font-size: 13px;
      color: #475569;
    }
  }

  .welcome-actions {
    flex: 0 0 auto;
    display: flex;
    gap: 12px;

    .el-button {
      display: inline-flex;
      align-items: center;
      gap: 4px;
    }

    .el-button:not(.el-button--primary) {
      background: #f5f7fa;
      border: 1px solid #e4e7ed;
      color: #0f172a;

      &:hover {
        background: #eef1f6;
      }
    }
  }
}

/* 快捷入口 */
.quick-row {
  margin-bottom: 20px;
}

.quick-card {
  cursor: pointer;
  border-radius: 12px;
  border: 1px solid #ebeef5;
  transition: transform 0.15s ease, box-shadow 0.15s ease;

  &:hover {
    transform: translateY(-3px);
  }

  :deep(.el-card__body) {
    display: flex;
    flex-direction: column;
    gap: 6px;
    padding: 22px 24px;
  }

  .quick-icon {
    color: #0ea5e9;
    margin-bottom: 4px;
  }

  .quick-title {
    font-size: 16px;
    font-weight: 600;
    color: #1f2937;
  }

  .quick-desc {
    font-size: 13px;
    color: #909399;
    line-height: 1.5;
  }
}

/* 平台能力 */
.section-card {
  border-radius: 12px;
  border: 1px solid #ebeef5;

  .section-title {
    font-size: 15px;
    font-weight: 600;
    color: #1f2937;
  }

  .feature {
    display: flex;
    align-items: flex-start;
    gap: 12px;
    padding: 16px 8px;
  }

  .feature-icon {
    flex: 0 0 auto;
    color: #06b6d4;
    margin-top: 2px;
  }

  .feature-title {
    font-size: 14px;
    font-weight: 600;
    color: #303133;
  }

  .feature-desc {
    margin-top: 4px;
    font-size: 13px;
    color: #909399;
    line-height: 1.5;
  }
}
</style>
