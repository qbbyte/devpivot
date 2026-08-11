<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="模板编码" prop="templateCode">
        <el-input
          v-model="queryParams.templateCode"
          placeholder="请输入模板编码"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="场景类型(CLARIFY/PRD/TECH/DB/CHECK/POLISH)" prop="sceneType">
        <el-select v-model="queryParams.sceneType" placeholder="请选择场景类型(CLARIFY/PRD/TECH/DB/CHECK/POLISH)" clearable>
          <el-option
            v-for="dict in ai_scene_type"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="模板名称" prop="templateName">
        <el-input
          v-model="queryParams.templateName"
          placeholder="请输入模板名称"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="是否默认(Y/N)" prop="isDefault">
        <el-select v-model="queryParams.isDefault" placeholder="请选择是否默认(Y/N)" clearable>
          <el-option
            v-for="dict in sys_yes_no"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="是否启用(0启用 1停用)" prop="isEnabled">
        <el-select v-model="queryParams.isEnabled" placeholder="请选择是否启用(0启用 1停用)" clearable>
          <el-option
            v-for="dict in sys_normal_disable"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
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
          v-hasPermi="['system:template:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['system:template:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['system:template:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['system:template:export']"
        >导出</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="info"
          plain
          icon="Refresh"
          @click="handleClearCache"
          v-hasPermi="['system:template:edit']"
        >刷新缓存</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="templateList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="模板ID" align="center" prop="templateId" width="90" />
      <el-table-column label="模板编码" align="center" prop="templateCode" width="150" />
      <el-table-column label="场景类型" align="center" prop="sceneType" width="100">
        <template #default="scope">
          <dict-tag :options="ai_scene_type" :value="scope.row.sceneType"/>
        </template>
      </el-table-column>
      <el-table-column label="模板名称" align="center" prop="templateName" width="160" />
      <el-table-column label="系统提示词(template_content)" align="left" prop="templateContent" show-overflow-tooltip />
      <el-table-column label="用户提示词(user_template)" align="left" prop="userTemplate" show-overflow-tooltip />
      <el-table-column label="多模型差异化(JSON)" align="center" prop="modelSpecific" width="140" show-overflow-tooltip />
      <el-table-column label="默认" align="center" prop="isDefault" width="70">
        <template #default="scope">
          <dict-tag :options="sys_yes_no" :value="scope.row.isDefault"/>
        </template>
      </el-table-column>
      <el-table-column label="启用" align="center" prop="isEnabled" width="70">
        <template #default="scope">
          <dict-tag :options="sys_normal_disable" :value="scope.row.isEnabled"/>
        </template>
      </el-table-column>
      <el-table-column label="备注" align="center" prop="remark" width="120" show-overflow-tooltip />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="220">
        <template #default="scope">
          <el-button link type="primary" icon="VideoPlay" @click="handleTry(scope.row)" v-hasPermi="['system:template:query']">试跑</el-button>
          <el-button link type="primary" icon="CopyDocument" @click="handleClone(scope.row)" v-hasPermi="['system:template:add']">克隆</el-button>
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:template:edit']">修改</el-button>
          <el-button link type="primary" icon="Star" @click="handleSetDefault(scope.row)" v-hasPermi="['system:template:edit']" v-if="scope.row.isDefault !== 'Y'">设为默认</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['system:template:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total>0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 添加或修改Prompt模板对话框 -->
    <el-dialog :title="title" v-model="open" width="760px" append-to-body>
      <el-form ref="templateRef" :model="form" :rules="rules" label-width="120px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="模板编码" prop="templateCode">
              <el-input v-model="form.templateCode" placeholder="请输入模板编码" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="场景类型" prop="sceneType">
              <el-select v-model="form.sceneType" placeholder="请选择场景类型" style="width:100%">
                <el-option
                  v-for="dict in ai_scene_type"
                  :key="dict.value"
                  :label="dict.label"
                  :value="dict.value"
                ></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="模板名称" prop="templateName">
              <el-input v-model="form.templateName" placeholder="请输入模板名称" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="系统提示词" prop="templateContent">
              <el-input v-model="form.templateContent" type="textarea" :rows="6" placeholder="系统提示词（支持 {{变量}}）" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="用户提示词" prop="userTemplate">
              <el-input v-model="form.userTemplate" type="textarea" :rows="6" placeholder="用户提示词模板，使用 {{变量}} 占位，如 {{projectName}}" />
              <div class="var-tip" v-pre>变量示例：{{projectName}}、{{industryType}}、{{targetUser}}、{{message}} 等，需与各阶段 Controller 渲染时传入的变量名一致。</div>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="多模型差异化" prop="modelSpecific">
              <el-input v-model="form.modelSpecific" type="textarea" :rows="4"
                placeholder='JSON，形如 {"gpt-4o":{"system":"…","user":"…"}}，按 modelCode 覆盖默认提示词' />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="是否默认(Y/N)" prop="isDefault">
              <el-radio-group v-model="form.isDefault">
                <el-radio
                  v-for="dict in sys_yes_no"
                  :key="dict.value"
                  :label="dict.value"
                >{{dict.label}}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="是否启用" prop="isEnabled">
              <el-radio-group v-model="form.isEnabled">
                <el-radio
                  v-for="dict in sys_normal_disable"
                  :key="dict.value"
                  :label="dict.value"
                >{{dict.label}}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注" prop="remark">
              <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 试跑对话框 -->
    <el-dialog title="提示词试跑" v-model="tryOpen" width="820px" append-to-body @closed="onTryClosed">
      <el-form ref="tryRef" :model="tryForm" :rules="tryRules" label-width="90px">
        <el-descriptions :column="2" border size="small" style="margin-bottom:12px">
          <el-descriptions-item label="场景">{{ tryForm.sceneType || '-' }}</el-descriptions-item>
          <el-descriptions-item label="模板编码">{{ tryForm.templateCode || '-' }}</el-descriptions-item>
        </el-descriptions>

        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="试跑模型" prop="modelCode">
              <el-select v-model="tryForm.modelCode" placeholder="请选择模型" filterable style="width:100%">
                <el-option
                  v-for="m in tryModels"
                  :key="m.modelId"
                  :label="(m.modelName || m.modelCode) + ' (' + m.modelCode + ')'"
                  :value="m.modelCode"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label=" ">
              <el-button type="primary" :loading="tryLoading" icon="VideoPlay" @click="submitTry">运行试跑</el-button>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="模板变量" v-if="tryVarsList.length">
          <div v-for="v in tryVarsList" :key="v" style="margin-bottom:8px;width:100%">
            <span style="display:inline-block;width:160px;color:#409eff">{{ varLabel(v) }}</span>
            <el-input v-model="tryForm.vars[v]" :placeholder="'变量 ' + v + ' 的示例值'" style="width:calc(100% - 170px)" />
          </div>
        </el-form-item>

        <el-form-item label="试跑输入" prop="userInput">
          <el-input v-model="tryForm.userInput" type="textarea" :rows="4"
            placeholder="追加到用户提示词末尾的试跑内容（如一段需求描述、一个问题）" />
        </el-form-item>
      </el-form>

      <el-alert v-if="trySource" :title="'渲染来源：' + trySource" type="info" :closable="false" style="margin-bottom:8px" />
      <el-input v-model="tryResult" type="textarea" :rows="12" readonly placeholder="试跑结果将在此显示" />
    </el-dialog>
  </div>
</template>

<script setup name="Template">
import { listTemplate, getTemplate, delTemplate, addTemplate, updateTemplate, clearTemplateCache, tryRunTemplate, cloneTemplate, setDefaultTemplate } from "@/api/ai/template"
import { listAiconfig } from "@/api/ai/aiconfig"
import request from '@/utils/request'
import { getToken } from '@/utils/auth'

const { proxy } = getCurrentInstance()
const { sys_yes_no, ai_scene_type, sys_normal_disable } = useDict('sys_yes_no', 'ai_scene_type', 'sys_normal_disable')

const templateList = ref([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref("")

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    templateCode: undefined,
    sceneType: undefined,
    templateName: undefined,
    templateContent: undefined,
    modelSpecific: undefined,
    isDefault: undefined,
    isEnabled: undefined,
  },
  rules: {
    templateCode: [
      { required: true, message: "模板编码不能为空", trigger: "blur" }
    ],
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 试跑对话框状态 */
const tryOpen = ref(false)
const tryLoading = ref(false)
const tryResult = ref('')
const trySource = ref('')
const tryModels = ref([])
const tryVarsList = ref([])
const tryForm = reactive({
  templateId: null,
  sceneType: '',
  templateCode: '',
  modelCode: '',
  userInput: '',
  vars: {}
})
const tryRules = {
  modelCode: [{ required: true, message: "请选择试跑模型", trigger: "change" }]
}

/** 生成变量展示标签：{{var}}（避免在模板插值中直接写 {{ 字面量导致编译失败） */
function varLabel(v) {
  return '{{' + v + '}}'
}

/** 从模板文本中提取 {{var}} 变量名（去重） */
function extractVars(text) {
  const set = new Set()
  if (!text) return []
  const re = /\{\{\s*([\w.]+)\s*\}\}/g
  let m
  while ((m = re.exec(text)) !== null) {
    set.add(m[1])
  }
  return Array.from(set)
}

/** 查询Prompt模板列表 */
function getList() {
  loading.value = true
  listTemplate(queryParams.value).then(response => {
    templateList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

/** 取消按钮 */
function cancel() {
  open.value = false
  reset()
}

/** 表单重置 */
function reset() {
  form.value = {
    templateId: null,
    templateCode: null,
    sceneType: null,
    templateName: null,
    templateContent: null,
    userTemplate: null,
    modelSpecific: null,
    isDefault: null,
    isEnabled: null,
    createBy: null,
    createTime: null,
    updateBy: null,
    updateTime: null,
    remark: null
  }
  proxy.resetForm("templateRef")
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm("queryRef")
  handleQuery()
}

/** 多选框选中数据 */
function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.templateId)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "添加Prompt模板"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _templateId = row.templateId || ids.value
  getTemplate(_templateId).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改Prompt模板"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["templateRef"].validate(valid => {
    if (valid) {
      // modelSpecific 非空时校验 JSON 格式，避免错 JSON 持久化后渲染时仅 warn 跳过
      if (form.value.modelSpecific && form.value.modelSpecific.trim()) {
        try {
          JSON.parse(form.value.modelSpecific)
        } catch (e) {
          proxy.$modal.msgError("多模型差异化(JSON) 格式错误：" + e.message)
          return
        }
      }
      if (form.value.templateId != null) {
        updateTemplate(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addTemplate(form.value).then(() => {
          proxy.$modal.msgSuccess("新增成功")
          open.value = false
          getList()
        })
      }
    }
  })
}

/** 删除按钮操作 */
function handleDelete(row) {
  const _templateIds = row.templateId || ids.value
  proxy.$modal.confirm('是否确认删除Prompt模板编号为"' + _templateIds + '"的数据项？').then(function() {
    return delTemplate(_templateIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('system/template/export', {
    ...queryParams.value
  }, `template_${new Date().getTime()}.xlsx`)
}

/** 刷新渲染缓存 */
function handleClearCache() {
  clearTemplateCache().then(() => {
    proxy.$modal.msgSuccess("缓存已刷新，模板立即生效")
  })
}

/** 克隆为新版本 */
function handleClone(row) {
  proxy.$modal.confirm('是否克隆模板「' + (row.templateName || row.templateCode) + '」为新版本（副本默认停用，不影响线上生效模板）？').then(function() {
    return cloneTemplate({ templateId: row.templateId })
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("已克隆为新版本，可在列表中「设为默认」切换回滚")
  }).catch(() => {})
}

/** 设为默认（版本回滚） */
function handleSetDefault(row) {
  proxy.$modal.confirm('是否将模板「' + (row.templateName || row.templateCode) + '」设为当前场景默认并启用？同场景其他默认模板将被取消。').then(function() {
    return setDefaultTemplate({ templateId: row.templateId })
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("已设为默认并刷新缓存")
  }).catch(() => {})
}

/** 打开试跑对话框 */
function handleTry(row) {
  tryForm.templateId = row.templateId
  tryForm.sceneType = row.sceneType
  tryForm.templateCode = row.templateCode
  tryForm.modelCode = ''
  tryForm.userInput = ''
  tryForm.vars = {}
  tryResult.value = ''
  trySource.value = ''
  tryVarsList.value = extractVars((row.templateContent || '') + ' ' + (row.userTemplate || ''))
  tryVarsList.value.forEach(v => { tryForm.vars[v] = '' })
  loadModels()
  tryOpen.value = true
}

function loadModels() {
  listAiconfig({ isEnabled: '0', pageSize: 1000 }).then(res => {
    tryModels.value = res.rows || []
  }).catch(() => { tryModels.value = [] })
}

function submitTry() {
  proxy.$refs["tryRef"].validate(valid => {
    if (!valid) return
    tryLoading.value = true
    tryResult.value = ''
    trySource.value = ''
    const data = {
      sceneType: tryForm.sceneType,
      templateCode: tryForm.templateCode,
      modelCode: tryForm.modelCode,
      userInput: tryForm.userInput,
      vars: tryForm.vars
    }
    const base = (request.defaults && request.defaults.baseURL) || '/dev-api'
    const token = getToken() || ''
    fetch(base + '/system/template/tryRun', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': token ? 'Bearer ' + token : ''
      },
      body: JSON.stringify(data)
    }).then(resp => {
      if (!resp.ok) {
        tryLoading.value = false
        proxy.$modal.msgError('试跑请求失败：HTTP ' + resp.status)
        return
      }
      const reader = resp.body.getReader()
      const decoder = new TextDecoder('utf-8')
      let buffer = ''
      const pump = () => reader.read().then(({ done, value }) => {
        if (done) {
          if (buffer.length) handleSseChunk(buffer)
          tryLoading.value = false
          return
        }
        buffer += decoder.decode(value, { stream: true })
        let idx
        while ((idx = buffer.indexOf('\n\n')) >= 0) {
          const chunk = buffer.substring(0, idx)
          buffer = buffer.substring(idx + 2)
          handleSseChunk(chunk)
        }
        return pump()
      })
      pump()
    }).catch(() => {
      tryLoading.value = false
    })
  })
}

/** 解析单条 SSE 事件块：message 事件增量追加到结果，meta 事件读取渲染来源 */
function handleSseChunk(chunk) {
  const lines = chunk.split('\n')
  let eventName = 'message'
  const dataLines = []
  for (const line of lines) {
    if (line.startsWith('event:')) eventName = line.substring(6).trim()
    else if (line.startsWith('data:')) dataLines.push(line.substring(5))
  }
  const dataStr = dataLines.join('\n').trim()
  if (!dataStr) return
  if (eventName === 'meta') {
    try {
      const meta = JSON.parse(dataStr)
      trySource.value = meta.source || ''
    } catch (e) { /* 忽略无法解析的 meta */ }
  } else {
    tryResult.value += dataStr
  }
}

function onTryClosed() {
  tryResult.value = ''
  trySource.value = ''
}

getList()
</script>

<style scoped>
.var-tip {
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
  margin-top: 4px;
}
</style>
