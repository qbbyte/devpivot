<template>
  <div class="stage-stepper" :class="[size, { done: isDone }]">
    <div class="ss-track" role="img" :aria-label="ariaLabel">
      <div class="ss-fill" :style="{ width: percent + '%', background: trackColor }"></div>
      <span class="ss-dot" :style="{ left: percent + '%', background: trackColor }"></span>
    </div>
    <div v-if="showLabel" class="ss-label">
      <span class="ss-state" :class="isDone ? 'is-done' : 'is-doing'">{{ isDone ? '已完成' : '当前' }}</span>
      <span class="ss-text">{{ currentLabel }} · {{ percent }}%</span>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  // 当前阶段值：REQ / CLARIFY / PRD / PROTO / TECH / DB / DONE
  currentStep: { type: String, required: true },
  // 阶段定义（默认 7 阶段流水线）
  stages: {
    type: Array,
    default: () => [
      { value: 'REQ', label: '需求采集' },
      { value: 'CLARIFY', label: 'AI 澄清' },
      { value: 'PRD', label: 'PRD 文档' },
      { value: 'PROTO', label: '原型设计' },
      { value: 'ARCH', label: '系统架构' },
      { value: 'TECH', label: '技术方案' },
      { value: 'DB', label: '数据库设计' },
      { value: 'DONE', label: '完成' }
    ]
  },
  showLabel: { type: Boolean, default: true },
  // sm（精选卡）/ md（列表行）
  size: { type: String, default: 'md' }
})

const isDone = computed(() => props.currentStep === 'DONE')
const currentIndex = computed(() => props.stages.findIndex(s => s.value === props.currentStep))

const percent = computed(() => {
  if (isDone.value) return 100
  if (currentIndex.value === -1) return 0
  return Math.round(((currentIndex.value + 1) / props.stages.length) * 100)
})

const currentLabel = computed(() => {
  if (isDone.value) return '已完成'
  const hit = props.stages[currentIndex.value]
  return hit ? hit.label : '未开始'
})

// 三态收敛：蓝=进行中 / 绿=已完成，避免七彩噪点
const trackColor = computed(() =>
  isDone.value ? 'var(--c-success, #10b981)' : 'var(--c-primary, #2563eb)'
)

const ariaLabel = computed(() => `当前阶段：${currentLabel.value}，进度 ${percent.value}%`)
</script>

<style scoped>
.stage-stepper {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.ss-track {
  position: relative;
  flex: 1;
  height: 4px;
  border-radius: 2px;
  background: var(--c-border, #eef0f3);
}

.ss-fill {
  position: absolute;
  left: 0;
  top: 0;
  height: 100%;
  border-radius: 2px;
  transition: width 0.3s ease;
}

.ss-dot {
  position: absolute;
  top: 50%;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  transform: translate(-50%, -50%);
  box-shadow: 0 0 0 3px var(--c-bg-card, #fff);
  transition: left 0.3s ease;
}

.ss-label {
  display: flex;
  align-items: center;
  gap: 6px;
  white-space: nowrap;
  flex-shrink: 0;
}

.ss-state {
  font-size: 11px;
  font-weight: 600;
  padding: 1px 7px;
  border-radius: 999px;
}

.ss-state.is-doing {
  color: var(--c-primary, #2563eb);
  background: var(--c-primary-soft, #eff4ff);
}

.ss-state.is-done {
  color: var(--c-success, #10b981);
  background: rgba(16, 185, 129, 0.08);
}

.ss-text {
  font-size: 12px;
  color: var(--c-text-3, #86909c);
}

/* 精选卡尺寸 */
.stage-stepper.sm .ss-track { height: 3px; }
.stage-stepper.sm .ss-dot { width: 8px; height: 8px; }

@media (prefers-reduced-motion: reduce) {
  .ss-fill,
  .ss-dot { transition: none; }
}
</style>
