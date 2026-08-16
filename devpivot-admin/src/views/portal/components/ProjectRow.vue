<template>
  <div
    class="project-row"
    :class="{ 'is-top': project.isTop === 'Y' }"
    role="button"
    tabindex="0"
    :aria-label="`打开项目 ${project.projectName}`"
    @click="emit('open', project)"
    @keydown.enter.prevent="emit('open', project)"
  >
    <div class="pr-left">
      <div class="pr-top">
        <span v-if="project.isTop === 'Y'" class="pr-top-tag">置顶</span>
        <span class="pr-name">{{ project.projectName }}</span>
        <dict-tag :options="statusOptions" :value="project.status" />
      </div>
      <p class="pr-intro">{{ project.projectIntro || '暂无项目简介' }}</p>
      <div class="pr-meta">
        <span v-if="project.assigneeName" class="pr-meta-item">
          <el-icon><User /></el-icon>{{ project.assigneeName }}
        </span>
        <span v-if="project.updateTime" class="pr-meta-item">
          <el-icon><Clock /></el-icon>{{ formatTime(project.updateTime) }}
        </span>
      </div>
    </div>

    <div class="pr-right">
      <StageStepper :current-step="project.step" :show-label="true" />
      <button
        v-if="project.step !== 'DONE'"
        class="pr-cta"
        type="button"
        @click.stop="emit('continue', project)"
      >
        进入{{ stepLabel }}<el-icon><ArrowRight /></el-icon>
      </button>
      <span v-else class="pr-done">
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
.project-row {
  display: flex;
  align-items: stretch;
  gap: 24px;
  background: var(--c-bg-card, #fff);
  border: 1px solid var(--c-border, #eef0f3);
  border-radius: 12px;
  padding: 16px 20px;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.project-row:hover,
.project-row:focus-visible {
  border-color: var(--c-primary, #3370ff);
  box-shadow: 0 8px 24px rgba(51, 112, 255, 0.08);
  transform: translateY(-1px);
  outline: none;
}

.project-row:focus-visible {
  outline: 2px solid var(--c-primary, #3370ff);
  outline-offset: 2px;
}

/* 置顶：左侧主色强调条 */
.project-row.is-top {
  border-left: 3px solid var(--c-primary, #3370ff);
}

.pr-left {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 8px;
}

.pr-top {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.pr-top-tag {
  flex-shrink: 0;
  font-size: 11px;
  font-weight: 600;
  color: #fff;
  background: var(--c-danger, #f56c6c);
  border-radius: 4px;
  padding: 1px 6px;
}

.pr-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--c-text-1, #1d2129);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  min-width: 0;
}

.pr-intro {
  margin: 0;
  font-size: 13px;
  line-height: 1.65;
  color: var(--c-text-2, #4e5969);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.pr-meta {
  display: flex;
  align-items: center;
  gap: 18px;
  color: var(--c-text-3, #86909c);
  font-size: 12px;
}

.pr-meta-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.pr-meta-item .el-icon {
  font-size: 14px;
}

.pr-right {
  width: 260px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  justify-content: center;
  gap: 10px;
  padding-left: 20px;
  border-left: 1px solid var(--c-border, #eef0f3);
}

.pr-cta {
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

.pr-cta:hover {
  background: var(--c-primary, #3370ff);
  color: #fff;
}

.pr-cta .el-icon {
  font-size: 14px;
}

.pr-done {
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

.pr-done .el-icon {
  font-size: 15px;
}

/* ===== 移动端：右侧信息降为底部横排 ===== */
@media (max-width: 768px) {
  .project-row {
    flex-direction: column;
    gap: 14px;
    padding: 16px 18px;
  }

  .pr-right {
    width: 100%;
    padding-left: 0;
    border-left: none;
    border-top: 1px solid var(--c-border, #eef0f3);
    padding-top: 14px;
    align-items: stretch;
  }

  .pr-cta {
    justify-content: center;
  }
}
</style>
