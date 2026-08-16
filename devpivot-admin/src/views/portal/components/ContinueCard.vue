<template>
  <div
    class="continue-card"
    :class="{ 'is-top': project.isTop === 'Y' }"
    role="button"
    tabindex="0"
    :aria-label="`打开项目 ${project.projectName}`"
    @click="emit('open', project)"
    @keydown.enter.prevent="emit('open', project)"
  >
    <div class="cc-top">
      <span v-if="project.isTop === 'Y'" class="cc-top-tag">置顶</span>
      <span class="cc-name">{{ project.projectName }}</span>
      <dict-tag :options="statusOptions" :value="project.status" />
    </div>

    <p class="cc-intro">{{ project.projectIntro || '暂无项目简介' }}</p>

    <div class="cc-meta">
      <span v-if="project.assigneeName" class="cc-meta-item">
        <el-icon><User /></el-icon>{{ project.assigneeName }}
      </span>
      <span v-if="project.updateTime" class="cc-meta-item">
        <el-icon><Clock /></el-icon>{{ formatTime(project.updateTime) }}
      </span>
    </div>

    <div class="cc-foot">
      <StageStepper :current-step="project.step" size="sm" :show-label="true" />
      <button
        v-if="project.step !== 'DONE'"
        class="cc-cta"
        type="button"
        @click.stop="emit('continue', project)"
      >
        进入{{ stepLabel }}<el-icon><ArrowRight /></el-icon>
      </button>
      <span v-else class="cc-done">
        <el-icon><CircleCheck /></el-icon>项目已完成
      </span>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { User, Clock, ArrowRight, CircleCheck } from '@element-plus/icons-vue'
import { useDict } from '@/utils/dict'
import StageStepper from './StageStepper.vue'

const props = defineProps({
  project: { type: Object, required: true }
})

const emit = defineEmits(['open', 'continue'])

const { ai_project_status, ai_project_step } = useDict('ai_project_status', 'ai_project_step')
const statusOptions = ai_project_status

const stepLabel = computed(() => {
  const hit = ai_project_step.value.find(o => o.value === props.project.step)
  return hit ? hit.label : '项目'
})

function formatTime(value) {
  if (!value) return ''
  return String(value).replace('T', ' ').slice(0, 16)
}
</script>

<style scoped>
.continue-card {
  display: flex;
  flex-direction: column;
  gap: 10px;
  background: var(--c-bg-card, #fff);
  border: 1px solid var(--c-border, #eef0f3);
  border-radius: 14px;
  padding: 18px 20px;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.continue-card:hover,
.continue-card:focus-visible {
  border-color: var(--c-primary, #3370ff);
  box-shadow: 0 10px 28px rgba(51, 112, 255, 0.1);
  transform: translateY(-2px);
  outline: none;
}

.continue-card:focus-visible {
  outline: 2px solid var(--c-primary, #3370ff);
  outline-offset: 2px;
}

.continue-card.is-top {
  border-left: 3px solid var(--c-primary, #3370ff);
}

.cc-top {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.cc-top-tag {
  flex-shrink: 0;
  font-size: 11px;
  font-weight: 600;
  color: #fff;
  background: var(--c-danger, #f56c6c);
  border-radius: 4px;
  padding: 1px 6px;
}

.cc-name {
  flex: 1;
  min-width: 0;
  font-size: 15px;
  font-weight: 600;
  color: var(--c-text-1, #1d2129);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.cc-intro {
  margin: 0;
  font-size: 13px;
  line-height: 1.6;
  color: var(--c-text-2, #4e5969);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.cc-meta {
  display: flex;
  align-items: center;
  gap: 16px;
  color: var(--c-text-3, #86909c);
  font-size: 12px;
}

.cc-meta-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.cc-meta-item .el-icon {
  font-size: 14px;
}

.cc-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 2px;
  padding-top: 12px;
  border-top: 1px solid var(--c-border, #eef0f3);
}

.cc-cta {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  font-weight: 500;
  color: var(--c-primary, #3370ff);
  background: transparent;
  border: 1px solid var(--c-primary, #3370ff);
  border-radius: 8px;
  padding: 6px 14px;
  cursor: pointer;
  transition: background 0.18s ease, color 0.18s ease;
}

.cc-cta:hover {
  background: var(--c-primary, #3370ff);
  color: #fff;
}

.cc-cta .el-icon {
  font-size: 14px;
}

.cc-done {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  font-weight: 600;
  color: var(--c-success, #00b42a);
  background: rgba(0, 180, 42, 0.08);
  border-radius: 999px;
  padding: 4px 12px;
}

.cc-done .el-icon {
  font-size: 15px;
}
</style>
