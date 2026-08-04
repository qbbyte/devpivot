<template>
  <aside class="step-sidebar">
    <div class="step-item" v-for="(s, idx) in steps" :key="idx">
      <div class="step-line">
        <span class="step-dot" :class="['step-dot-' + stateClass(idx)]">
          <el-icon v-if="idx < current" :size="13"><Check /></el-icon>
          <span v-else>{{ idx + 1 }}</span>
        </span>
        <span v-if="idx < steps.length - 1" class="step-connector" :class="{ done: idx < current }"></span>
      </div>
      <div class="step-body">
        <div class="step-title" :class="['title-' + stateClass(idx)]">{{ s.title }}</div>
        <div class="step-desc" v-if="s.desc">{{ s.desc }}</div>
      </div>
    </div>
  </aside>
</template>

<script setup name="StepSidebar">
const props = defineProps({
  steps: {
    type: Array,
    required: true
  },
  current: {
    type: Number,
    default: 0
  }
})

function stateClass(idx) {
  if (idx < props.current) return 'done'
  if (idx === props.current) return 'active'
  return 'todo'
}
</script>

<style scoped>
.step-sidebar {
  display: flex;
  flex-direction: column;
  gap: 0;
  padding: 24px 20px;
}

.step-item {
  display: flex;
  gap: 14px;
}

.step-line {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex-shrink: 0;
}

.step-dot {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 600;
  transition: all 0.25s;
  z-index: 1;
  background: #eef0f3;
  color: #9aa1ab;
}

.step-connector {
  width: 2px;
  height: 32px;
  background: #e8eaee;
  transition: background 0.25s;
}

.step-connector.done {
  background: #a0cfff;
}

/* dot states */
.step-dot-todo {
  background: #eef0f3;
  color: #9aa1ab;
}

.step-dot-active {
  background: #409eff;
  color: #fff;
  box-shadow: 0 0 0 4px rgba(64, 158, 255, 0.15);
}

.step-dot-done {
  background: #67c23a;
  color: #fff;
}

/* body */
.step-body {
  padding-top: 2px;
  min-width: 0;
}

.step-title {
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 2px;
}

.title-todo {
  color: #9aa1ab;
}

.title-active {
  color: #409eff;
  font-weight: 600;
}

.title-done {
  color: #606266;
}

.step-desc {
  font-size: 12px;
  color: #b6bdc6;
}
</style>
