<template>
  <div class="empty-state" :class="`empty-state--${variant}`">
    <!-- 插画：仅 onboarding 展示流程化场景，noResult 展示轻量搜索空态 -->
    <div class="es-illustration" aria-hidden="true">
      <svg v-if="variant === 'onboarding'" viewBox="0 0 240 176" width="240" height="176" role="presentation">
        <defs>
          <linearGradient id="esGrad" x1="0" y1="0" x2="1" y2="1">
            <stop offset="0%" stop-color="#3370ff" />
            <stop offset="100%" stop-color="#6e52ff" />
          </linearGradient>
          <linearGradient id="esGradSoft" x1="0" y1="0" x2="1" y2="1">
            <stop offset="0%" stop-color="#e8f0ff" />
            <stop offset="100%" stop-color="#f1ecff" />
          </linearGradient>
        </defs>
        <!-- 柔和底色圆 -->
        <circle cx="120" cy="92" r="74" fill="url(#esGradSoft)" />
        <circle cx="58" cy="46" r="7" fill="#cfe0ff" />
        <circle cx="196" cy="58" r="9" fill="#e2d8ff" />
        <!-- 主卡片 -->
        <rect x="64" y="58" width="112" height="78" rx="14" fill="#ffffff" stroke="#e6ebf5" stroke-width="1.5" />
        <rect x="64" y="58" width="112" height="22" rx="14" fill="url(#esGrad)" />
        <rect x="64" y="70" width="112" height="10" fill="url(#esGrad)" />
        <rect x="80" y="92" width="80" height="7" rx="3.5" fill="#e9edf5" />
        <rect x="80" y="106" width="60" height="7" rx="3.5" fill="#eef1f7" />
        <rect x="80" y="120" width="70" height="7" rx="3.5" fill="#eef1f7" />
        <!-- 浮动 + 徽标 -->
        <circle cx="176" cy="58" r="16" fill="url(#esGrad)" />
        <path d="M176 50 v16 M168 58 h16" stroke="#fff" stroke-width="2.6" stroke-linecap="round" />
        <!-- 阶段小点（暗示流水线） -->
        <circle cx="86" cy="154" r="5" fill="#3370ff" />
        <circle cx="108" cy="154" r="5" fill="#7f93ff" />
        <circle cx="130" cy="154" r="5" fill="#c9d4ff" />
        <circle cx="152" cy="154" r="5" fill="#d9def0" />
        <!-- 星点 -->
        <path d="M186 110 l2 5 5 2 -5 2 -2 5 -2 -5 -5 -2 5 -2 z" fill="#ffb340" />
      </svg>

      <svg v-else viewBox="0 0 200 160" width="200" height="160" role="presentation">
        <circle cx="100" cy="80" r="62" fill="#f3f5f9" />
        <circle cx="100" cy="80" r="34" fill="none" stroke="#cdd4e0" stroke-width="4" />
        <line x1="124" y1="104" x2="146" y2="126" stroke="#cdd4e0" stroke-width="6" stroke-linecap="round" />
        <line x1="86" y1="80" x2="114" y2="80" stroke="#aeb8c8" stroke-width="4" stroke-linecap="round" />
      </svg>
    </div>

    <h2 class="es-title">{{ title }}</h2>
    <p class="es-desc">{{ desc }}</p>

    <!-- onboarding：三步说明 + 主 CTA -->
    <template v-if="variant === 'onboarding'">
      <ul class="es-steps">
        <li v-for="(s, i) in steps" :key="i" class="es-step">
          <span class="es-step-no">{{ i + 1 }}</span>
          <div class="es-step-text">
            <strong>{{ s.title }}</strong>
            <span>{{ s.text }}</span>
          </div>
        </li>
      </ul>
      <el-button type="primary" class="es-cta" @click="$emit('create')">
        <el-icon><Plus /></el-icon>
        <span>创建第一个项目</span>
      </el-button>
    </template>

    <!-- noResult：清除筛选 -->
    <template v-else>
      <el-button class="es-cta es-cta--ghost" @click="$emit('reset')">
        <el-icon><RefreshRight /></el-icon>
        <span>清除筛选条件</span>
      </el-button>
    </template>
  </div>
</template>

<script setup name="EmptyState">
import { computed } from 'vue'
import { Plus, RefreshRight } from '@element-plus/icons-vue'

const props = defineProps({
  // onboarding：全新用户无项目；noResult：有项目但筛选无命中
  variant: {
    type: String,
    default: 'onboarding',
    validator: v => ['onboarding', 'noResult'].includes(v)
  },
  keyword: {
    type: String,
    default: ''
  }
})

defineEmits(['create', 'reset'])

const steps = [
  { title: '填写需求', text: '一句话描述你的产品想法' },
  { title: 'AI 生成', text: '串行产出澄清 / PRD / 原型 / 技术方案 / 库表' },
  { title: '一键落地', text: '导出文档与 SQL，直接交付研发' }
]

const title = computed(() =>
  props.variant === 'onboarding'
    ? '还没有项目，开启你的第一次 AI 需求设计'
    : '未找到符合条件的项目'
)

const desc = computed(() => {
  if (props.variant === 'onboarding') {
    return '三步即可把模糊想法，变成可落地的需求文档与数据库设计'
  }
  const kw = props.keyword ? `「${props.keyword}」` : ''
  return `没有项目匹配${kw}当前的关键词或筛选条件，试试调整后再搜索`
})
</script>

<style scoped>
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 56px 24px 64px;
  background: #fff;
  border-radius: 16px;
  border: 1px solid #eeeef0;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.02);
}

.es-illustration {
  margin-bottom: 8px;
  line-height: 0;
}

.es-title {
  margin: 12px 0 8px;
  font-size: 18px;
  font-weight: 600;
  color: #1d2129;
}

.es-desc {
  margin: 0;
  max-width: 440px;
  font-size: 14px;
  line-height: 1.7;
  color: #86909c;
}

/* 三步说明 */
.es-steps {
  list-style: none;
  margin: 26px 0 28px;
  padding: 0;
  display: flex;
  gap: 14px;
  flex-wrap: wrap;
  justify-content: center;
}

.es-step {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  width: 220px;
  text-align: left;
  padding: 14px 16px;
  background: #f7f9fc;
  border-radius: 12px;
  border: 1px solid #eef1f6;
}

.es-step-no {
  flex-shrink: 0;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: linear-gradient(135deg, #3370ff, #6e52ff);
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
}

.es-step-text {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.es-step-text strong {
  font-size: 14px;
  font-weight: 600;
  color: #1d2129;
}

.es-step-text span {
  font-size: 12.5px;
  line-height: 1.5;
  color: #86909c;
}

.es-cta {
  border-radius: 9px;
  padding: 11px 26px;
  font-size: 14px;
  font-weight: 500;
  background: linear-gradient(135deg, #3370ff, #5b8bff);
  border: none;
  box-shadow: 0 6px 16px rgba(51, 112, 255, 0.28);
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}

.es-cta:hover {
  transform: translateY(-1px);
  box-shadow: 0 10px 22px rgba(51, 112, 255, 0.34);
}

.es-cta:focus-visible {
  outline: 2px solid #3370ff;
  outline-offset: 2px;
}

.es-cta .el-icon {
  margin-right: 6px;
}

.es-cta--ghost {
  background: #fff;
  color: #4e5969;
  border: 1px solid #d9dee6;
  box-shadow: none;
}

.es-cta--ghost:hover {
  color: #3370ff;
  border-color: #3370ff;
  background: #f5f8ff;
  transform: none;
}

@media (prefers-reduced-motion: reduce) {
  .es-cta { transition: none; }
  .es-cta:hover { transform: none; }
}

@media (max-width: 768px) {
  .empty-state { padding: 40px 18px 48px; }
  .es-steps { flex-direction: column; align-items: center; }
  .es-step { width: 100%; max-width: 300px; }
}
</style>
