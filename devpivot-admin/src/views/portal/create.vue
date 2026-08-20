<template>
  <div class="create-page">
    <header class="create-header">
      <button class="back-link" @click="goBack">
        <el-icon><ArrowLeft /></el-icon>
        <span>返回</span>
      </button>
      <span class="header-title">新建项目</span>
      <span class="header-spacer"></span>
    </header>

    <main class="create-main">
      <div class="form-wrap">
        <div class="form-top">
          <h2>新建项目</h2>
          <p>填写项目基本信息，创建后即可开始 AI 需求设计流程</p>
        </div>

        <el-form ref="projectRef" :model="form" :rules="rules" label-position="top" class="create-form">
          <el-form-item label="项目名称" prop="projectName">
            <el-input v-model="form.projectName" placeholder="请输入项目名称" maxlength="50" show-word-limit />
          </el-form-item>

          <el-form-item label="行业分类" prop="industryType">
            <el-input v-model="form.industryType" placeholder="如：电商、教育" maxlength="30" />
          </el-form-item>

          <el-form-item label="目标用户群体" prop="targetUser">
            <el-input v-model="form.targetUser" placeholder="如：企业内部运营人员" maxlength="50" />
          </el-form-item>

          <el-form-item label="项目简介" prop="projectIntro">
            <el-input
              v-model="form.projectIntro"
              type="textarea"
              :rows="4"
              placeholder="简要描述项目要解决的问题和期望的系统"
              maxlength="200"
              show-word-limit
            />
          </el-form-item>

          <div class="form-actions">
            <el-button type="primary" :loading="submitting" class="submit-btn" @click="submitForm">
              创建项目
            </el-button>
          </div>
        </el-form>
      </div>
    </main>
  </div>
</template>

<script setup name="CreateProject">
import { ref, reactive, getCurrentInstance } from 'vue'
import { useRouter } from 'vue-router'
import { addProject } from '@/api/ai/project'
import useUserStore from '@/store/modules/user'

const { proxy } = getCurrentInstance()
const router = useRouter()
const userStore = useUserStore()

const submitting = ref(false)

const form = reactive({
  projectName: undefined,
  industryType: undefined,
  projectIntro: undefined,
  targetUser: undefined
})

const rules = {
  projectName: [
    { required: true, message: '请输入项目名称', trigger: 'blur' }
  ]
}

function goBack() {
  router.push('/portal')
}

function submitForm() {
  proxy.$refs.projectRef.validate(valid => {
    if (valid) {
      submitting.value = true
      addProject({
        ...form,
        assigneeId: userStore.userId,
        step: 'REQ',
        isTop: 'N',
        status: '0',
        delFlag: '0'
      }).then(() => {
        proxy.$modal.msgSuccess('创建成功')
        router.push('/portal')
      }).catch(() => {
        submitting.value = false
      })
    }
  })
}
</script>

<style scoped>
.create-page {
  min-height: 100vh;
  background: var(--c-bg);
}

/* ---- header ---- */
.create-header {
  position: sticky;
  top: 0;
  z-index: 10;
  display: flex;
  align-items: center;
  height: 52px;
  padding: 0 24px;
  background: rgba(255, 255, 255, 0.85);
  border-bottom: 1px solid var(--c-border);
  backdrop-filter: blur(8px);
}

.back-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  border: none;
  background: none;
  color: var(--c-text-muted);
  font-size: 13px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 6px;
  transition: color 0.2s, background 0.2s;
}

.back-link:hover {
  color: var(--c-primary);
  background: var(--c-primary-bg);
}

.header-title {
  margin-left: 12px;
  font-size: 14px;
  font-weight: 600;
  color: var(--c-text);
}

.header-spacer {
  flex: 1;
}

/* ---- main ---- */
.create-main {
  padding: 48px 24px 80px;
}

.form-wrap {
  width: 100%;
  max-width: 680px;
  margin: 0 auto;
  padding: 36px 44px 40px;
  background: var(--c-surface);
  border: 1px solid var(--c-border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
}

.form-top {
  margin-bottom: 36px;
  text-align: center;
}

.form-top h2 {
  margin: 0 0 8px;
  font-size: 24px;
  font-weight: 700;
  color: var(--c-text);
  line-height: 1.4;
}

.form-top p {
  margin: 0;
  font-size: 14px;
  color: var(--c-text-muted);
  line-height: 1.6;
}

/* ---- form ---- */
.create-form :deep(.el-form-item) {
  margin-bottom: 24px;
}

.create-form :deep(.el-form-item__label) {
  font-size: 14px;
  font-weight: 500;
  color: var(--c-text);
  padding-bottom: 6px;
}

.create-form :deep(.el-input__wrapper),
.create-form :deep(.el-textarea__inner) {
  border-radius: var(--radius-sm);
  box-shadow: 0 0 0 1px var(--c-border) inset;
  transition: box-shadow 0.25s ease, border-color 0.25s ease;
}

.create-form :deep(.el-input__wrapper:hover),
.create-form :deep(.el-textarea__inner:hover) {
  box-shadow: 0 0 0 1px #c9cdd4 inset;
}

.create-form :deep(.el-input__wrapper.is-focus),
.create-form :deep(.el-textarea__inner:focus) {
  box-shadow: 0 0 0 1px var(--c-primary) inset, 0 0 0 3px rgba(37, 99, 235, 0.15);
}

@media (max-width: 640px) {
  .create-main {
    padding: 32px 16px 64px;
  }

  .form-wrap {
    max-width: 100%;
    padding: 28px 22px 32px;
  }

  .form-top {
    margin-bottom: 32px;
  }

  .form-top h2 {
    font-size: 20px;
  }
}

/* ---- actions ---- */
.form-actions {
  display: flex;
  justify-content: center;
  margin-top: 32px;
  padding-top: 32px;
  border-top: 1px solid var(--c-border-light);
}

.submit-btn {
  min-width: 140px;
  border-radius: var(--radius-sm);
  padding: 10px 40px;
  font-size: 14px;
  font-weight: 500;
  letter-spacing: 0.5px;
  transition: transform 0.15s ease, box-shadow 0.2s ease;
}

.submit-btn:active {
  transform: scale(0.97);
}
</style>