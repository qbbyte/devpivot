<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="项目ID" prop="projectId">
        <el-input
          v-model="queryParams.projectId"
          placeholder="留空=全部项目"
          clearable
          style="width: 160px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="范围" prop="scope">
        <el-radio-group v-model="queryParams.scope">
          <el-radio-button label="全部" value="all" />
          <el-radio-button label="本项目" value="project" />
          <el-radio-button label="共享库" value="shared" />
        </el-radio-group>
      </el-form-item>
      <el-form-item label="阶段" prop="stage">
        <el-select v-model="queryParams.stage" placeholder="全部阶段" clearable style="width: 140px">
          <el-option v-for="s in stageOptions" :key="s.value" :label="s.label" :value="s.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="Plus"
          @click="handleAdd"
          v-hasRole="['admin']"
        >新增知识</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="docList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="文档ID" align="center" prop="docId" width="90" />
      <el-table-column label="项目ID" align="center" prop="projectId" width="90">
        <template #default="scope">{{ scope.row.projectId === -1 ? '共享' : scope.row.projectId }}</template>
      </el-table-column>
      <el-table-column label="范围" align="center" width="80">
        <template #default="scope">
          <el-tag v-if="scope.row.projectId === -1" type="success" size="small">共享库</el-tag>
          <el-tag v-else type="info" size="small">本项目</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="阶段" align="center" width="90">
        <template #default="scope">{{ scope.row.stage || '全局' }}</template>
      </el-table-column>
      <el-table-column label="标题" align="left" prop="title" show-overflow-tooltip />
      <el-table-column label="来源" align="center" width="90">
        <template #default="scope">
          <el-tag :type="scope.row.sourceType === 'pipeline' ? 'warning' : 'info'" size="small">
            {{ scope.row.sourceType === 'pipeline' ? '流水线' : '手动' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="切片数" align="center" prop="chunkCount" width="80" />
      <el-table-column label="创建时间" align="center" prop="createTime" width="160" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="160">
        <template #default="scope">
          <el-button link type="primary" icon="Search" @click="handleRetrieve(scope.row)">检索预览</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasRole="['admin']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total > 0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 新增知识对话框 -->
    <el-dialog :title="title" v-model="open" width="640px" append-to-body>
      <el-form ref="kbRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="归属范围" prop="scope">
          <el-radio-group v-model="form.scope">
            <el-radio label="本项目" value="project" />
            <el-radio label="组织共享库" value="shared" />
          </el-radio-group>
        </el-form-item>
        <el-form-item label="项目ID" prop="projectId" v-if="form.scope === 'project'">
          <el-input v-model="form.projectId" placeholder="请输入所属项目ID" />
        </el-form-item>
        <el-form-item label="阶段" prop="stage">
          <el-select v-model="form.stage" placeholder="请选择阶段（全局可留空）" clearable style="width: 100%">
            <el-option v-for="s in stageOptions" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入文档标题" />
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="10"
            placeholder="粘贴领域知识正文，将按换行自动切片索引（约 500 字/段）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 检索预览对话框 -->
    <el-dialog title="检索预览（将拼入该阶段 AI 提示词的知识上下文）" v-model="retrieveOpen" width="680px" append-to-body>
      <el-form :model="retrieveForm">
        <el-form-item label="检索语句" label-width="80px">
          <el-input
            v-model="retrieveForm.query"
            placeholder="例如：支付对账流程、状态机设计"
            @keyup.enter="doRetrieve"
          />
        </el-form-item>
        <el-form-item label-width="80px">
          <el-button type="primary" icon="Search" :loading="retrieving" @click="doRetrieve">检索</el-button>
        </el-form-item>
      </el-form>
      <el-input
        v-model="retrieveResult"
        type="textarea"
        :rows="12"
        readonly
        placeholder="检索后将在此展示命中切片拼接的上下文；无命中则为空（生成时不注入知识上下文）"
      />
    </el-dialog>
  </div>
</template>

<script setup name="KbAdmin">
import { ref, reactive, toRefs, getCurrentInstance } from 'vue'
import { listKbDocs, uploadKbDoc, deleteKbDoc, previewKbRetrieve } from '@/api/ai/kb'

const { proxy } = getCurrentInstance()

const stageOptions = [
  { label: '需求', value: 'REQ' },
  { label: '澄清', value: 'CLARIFY' },
  { label: 'PRD', value: 'PRD' },
  { label: '原型', value: 'PROTO' },
  { label: '技术方案', value: 'TECH' },
  { label: '数据库', value: 'DB' }
]

const docList = ref([])
const open = ref(false)
const retrieveOpen = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref('')
const retrieving = ref(false)
const retrieveResult = ref('')

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    projectId: undefined,
    scope: 'all',
    stage: undefined
  },
  rules: {
    content: [
      { required: true, message: '文档内容不能为空', trigger: 'blur' }
    ]
  },
  retrieveForm: {
    docId: undefined,
    projectId: undefined,
    stage: undefined,
    query: ''
  }
})

const { queryParams, form, rules, retrieveForm } = toRefs(data)

/** 构建列表请求参数 */
function buildParams() {
  const p = {}
  if (queryParams.value.scope === 'shared') {
    p.shared = true
  } else if (queryParams.value.scope === 'project' && queryParams.value.projectId) {
    p.projectId = queryParams.value.projectId
  }
  if (queryParams.value.stage) {
    p.stage = queryParams.value.stage
  }
  return p
}

/** 查询知识库文档列表 */
function getList() {
  loading.value = true
  listKbDocs(buildParams()).then(res => {
    const rows = res.data || []
    total.value = rows.length
    const start = (queryParams.value.pageNum - 1) * queryParams.value.pageSize
    docList.value = rows.slice(start, start + queryParams.value.pageSize)
    loading.value = false
  }).catch(() => {
    loading.value = false
  })
}

function cancel() {
  open.value = false
  reset()
}

function reset() {
  form.value = {
    scope: 'project',
    projectId: undefined,
    stage: undefined,
    title: '',
    content: ''
  }
  proxy.resetForm('kbRef')
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  proxy.resetForm('queryRef')
  queryParams.value.scope = 'all'
  queryParams.value.stage = undefined
  handleQuery()
}

function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.docId)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

function handleAdd() {
  reset()
  open.value = true
  title.value = '新增知识文档'
}

function submitForm() {
  proxy.$refs['kbRef'].validate(valid => {
    if (valid) {
      const payload = {
        shared: form.value.scope === 'shared',
        stage: form.value.stage || null,
        title: form.value.title || '未命名文档',
        content: form.value.content
      }
      if (form.value.scope === 'project') {
        if (!form.value.projectId) {
          proxy.$modal.msgWarning('请填写项目ID')
          return
        }
        payload.projectId = form.value.projectId
      }
      uploading.value = true
      uploadKbDoc(payload).then(() => {
        proxy.$modal.msgSuccess('索引成功')
        open.value = false
        getList()
      }).finally(() => {
        uploading.value = false
      })
    }
  })
}

const uploading = ref(false)

function handleRetrieve(row) {
  retrieveForm.value = {
    docId: row.docId,
    projectId: row.projectId,
    stage: row.stage,
    query: ''
  }
  retrieveResult.value = ''
  retrieveOpen.value = true
}

function doRetrieve() {
  if (!retrieveForm.value.query || !retrieveForm.value.query.trim()) {
    proxy.$modal.msgWarning('请填写检索语句')
    return
  }
  retrieving.value = true
  previewKbRetrieve({
    projectId: retrieveForm.value.projectId,
    stage: retrieveForm.value.stage || undefined,
    query: retrieveForm.value.query
  }).then(res => {
    const ctx = res.data && res.data.context
    retrieveResult.value = ctx && ctx.trim() ? ctx : '（无命中，该阶段生成时将不注入知识上下文）'
  }).finally(() => {
    retrieving.value = false
  })
}

function handleDelete(row) {
  const docId = row.docId
  proxy.$modal.confirm('是否确认删除文档编号为 "' + docId + '" 的知识文档（含其全部切片）？').then(function () {
    return deleteKbDoc(docId)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess('删除成功')
  }).catch(() => {})
}

getList()
</script>
