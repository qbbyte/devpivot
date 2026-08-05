<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="标识" prop="modelCode">
        <el-input
          v-model="queryParams.modelCode"
          placeholder="请输入模型标识"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="名称" prop="modelName">
        <el-input
          v-model="queryParams.modelName"
          placeholder="请输入模型名称"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="供应商" prop="provider">
        <el-input
          v-model="queryParams.provider"
          placeholder="请输入供应商"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="路由类型" prop="modelType">
        <el-select v-model="queryParams.modelType" placeholder="请选择路由类型" clearable>
          <el-option
            v-for="dict in ai_model_type"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="是否启用" prop="isEnabled">
        <el-select v-model="queryParams.isEnabled" placeholder="请选择是否启用" clearable>
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
          v-hasPermi="['system:aiconfig:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['system:aiconfig:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['system:aiconfig:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['system:aiconfig:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="aiconfigList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="ID" align="center" prop="modelId" width="60" />
      <el-table-column label="标识" align="center" prop="modelCode" />
      <el-table-column label="名称" align="center" prop="modelName" />
      <el-table-column label="供应商" align="center" prop="provider" />
      <el-table-column label="接口地址" align="center" prop="baseUrl" :show-overflow-tooltip="true" />
      <el-table-column label="密钥" align="center" prop="apiKey" :show-overflow-tooltip="true" />
      <el-table-column label="路由类型" align="center" prop="modelType">
        <template #default="scope">
          <dict-tag :options="ai_model_type" :value="scope.row.modelType"/>
        </template>
      </el-table-column>
      <el-table-column label="上下文" align="center" prop="contextLength" width="90" />
      <el-table-column label="状态" align="center" prop="isEnabled">
        <template #default="scope">
          <dict-tag :options="sys_normal_disable" :value="scope.row.isEnabled"/>
        </template>
      </el-table-column>
      <el-table-column label="默认参数" align="center" prop="defaultParams" :show-overflow-tooltip="true" />
      <el-table-column label="排序" align="center" prop="sort" width="70" />
      <el-table-column label="备注" align="center" prop="remark" :show-overflow-tooltip="true" />
      <el-table-column label="操作" align="center" fixed="right" width="200" class-name="small-padding fixed-width">
        <template #default="scope">
          <div style="white-space: nowrap;">
            <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:aiconfig:edit']">修改</el-button>
            <el-button link type="success" icon="VideoPlay" :loading="testingId === scope.row.modelId" @click="handleTest(scope.row)" v-hasPermi="['system:aiconfig:query']">测试</el-button>
            <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['system:aiconfig:remove']">删除</el-button>
          </div>
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

    <!-- 添加或修改AI模型配置对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="aiconfigRef" :model="form" :rules="rules" label-width="90px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="标识" prop="modelCode">
              <el-input v-model="form.modelCode" placeholder="如 deepseek" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="名称" prop="modelName">
              <el-input v-model="form.modelName" placeholder="如 DeepSeek-V3" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="供应商" prop="provider">
              <el-select
                v-model="form.provider"
                filterable
                allow-create
                default-first-option
                placeholder="选择或输入供应商"
                style="width: 100%"
                @change="onProviderChange"
              >
                <el-option
                  v-for="item in supplierPresets"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                ></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="接口地址" prop="baseUrl">
              <el-input v-model="form.baseUrl" placeholder="如 https://api.deepseek.com/v1" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="API密钥" prop="apiKey">
              <el-input v-model="form.apiKey" placeholder="加密存储，请填写完整密钥" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="路由类型" prop="modelType">
              <el-select v-model="form.modelType" placeholder="请选择路由类型">
                <el-option
                  v-for="dict in ai_model_type"
                  :key="dict.value"
                  :label="dict.label"
                  :value="dict.value"
                ></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="上下文长度" prop="contextLength">
              <el-input v-model="form.contextLength" placeholder="如 128000" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="启用状态" prop="isEnabled">
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
            <el-form-item label="排序" prop="sort">
              <el-input v-model="form.sort" placeholder="数字越小越靠前" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注" prop="remark">
              <el-input v-model="form.remark" type="textarea" placeholder="请输入备注" />
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
  </div>
</template>

<script setup name="Aiconfig">
import { listAiconfig, getAiconfig, delAiconfig, addAiconfig, updateAiconfig, testAiconfig } from "@/api/ai/aiconfig"
import { ElMessageBox } from 'element-plus'

const { proxy } = getCurrentInstance()
const { ai_model_type, sys_normal_disable } = useDict('ai_model_type', 'sys_normal_disable')

// 常见供应商预设：选中后自动带出默认接口地址与上下文长度（仅在对应字段为空时填充，避免覆盖已输入内容）
const supplierPresets = [
  { value: 'OpenAI', label: 'OpenAI', baseUrl: 'https://api.openai.com/v1', contextLength: 128000 },
  { value: 'DeepSeek', label: 'DeepSeek', baseUrl: 'https://api.deepseek.com/v1', contextLength: 64000 },
  { value: 'Qwen', label: '阿里百炼(Qwen)', baseUrl: 'https://dashscope.aliyuncs.com/compatible-mode/v1', contextLength: 32768 },
  { value: 'Anthropic', label: 'Anthropic(Claude)', baseUrl: 'https://api.anthropic.com/v1', contextLength: 200000 },
  { value: 'Zhipu', label: '智谱(Zhipu)', baseUrl: 'https://open.bigmodel.cn/api/paas/v4', contextLength: 128000 },
  { value: 'Local', label: '本地(Ollama)', baseUrl: 'http://localhost:11434/v1', contextLength: 8192 },
]

function onProviderChange(provider) {
  if (!provider) return
  const preset = supplierPresets.find(p => p.value === provider)
  if (!preset) return
  if (!form.value.baseUrl) form.value.baseUrl = preset.baseUrl
  if (!form.value.contextLength) form.value.contextLength = preset.contextLength
}

const aiconfigList = ref([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const testingId = ref(null)
const total = ref(0)
const title = ref("")

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    modelCode: undefined,
    modelName: undefined,
    provider: undefined,
    modelType: undefined,
    isEnabled: undefined,
  },
  rules: {
    modelCode: [
      { required: true, message: "模型标识不能为空", trigger: "blur" }
    ],
    modelName: [
      { required: true, message: "模型名称不能为空", trigger: "blur" }
    ],
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 测试模型按钮操作：对单条配置做一次连通性自检并弹窗展示结果 */
function handleTest(row) {
  testingId.value = row.modelId
  testAiconfig(row.modelId).then(res => {
    const data = res.data || {}
    const ok = data.success === true
    const lines = []
    lines.push(ok ? '✓ 测试通过' : '✗ 测试失败')
    if (data.statusCode != null) lines.push('HTTP 状态码：' + data.statusCode)
    if (data.latencyMs != null) lines.push('耗时：' + data.latencyMs + ' ms')
    lines.push('详情：' + (data.message || '无'))
    if (data.sample) lines.push('模型回复：' + data.sample)
    ElMessageBox.alert(lines.join('\n'), '模型测试结果', {
      type: ok ? 'success' : 'error',
      confirmButtonText: '知道了'
    })
  }).catch(() => {
    proxy.$modal.msgError('测试请求失败，请检查网络或后端日志')
  }).finally(() => {
    testingId.value = null
  })
}

/** 查询AI模型配置列表 */
function getList() {
  loading.value = true
  listAiconfig(queryParams.value).then(response => {
    aiconfigList.value = response.rows
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
    modelId: null,
    modelCode: null,
    modelName: null,
    provider: null,
    baseUrl: null,
    apiKey: null,
    modelType: null,
    contextLength: null,
    isEnabled: null,
    defaultParams: null,
    sort: null,
    createBy: null,
    createTime: null,
    updateBy: null,
    updateTime: null,
    remark: null
  }
  proxy.resetForm("aiconfigRef")
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
  ids.value = selection.map(item => item.modelId)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "添加AI模型配置"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _modelId = row.modelId || ids.value
  getAiconfig(_modelId).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改AI模型配置"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["aiconfigRef"].validate(valid => {
    if (valid) {
      if (form.value.modelId != null) {
        updateAiconfig(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addAiconfig(form.value).then(() => {
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
  const _modelIds = row.modelId || ids.value
  proxy.$modal.confirm('是否确认删除AI模型配置编号为"' + _modelIds + '"的数据项？').then(function() {
    return delAiconfig(_modelIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('system/aiconfig/export', {
    ...queryParams.value
  }, `aiconfig_${new Date().getTime()}.xlsx`)
}

getList()
</script>
