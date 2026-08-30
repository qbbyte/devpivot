<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="用户ID" prop="userId">
        <el-input
          v-model="queryParams.userId"
          placeholder="请输入用户ID"
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
      <el-form-item label="API密钥" prop="apiKey">
        <el-input
          v-model="queryParams.apiKey"
          placeholder="请输入API密钥"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="是否启用" prop="isActive">
        <el-select v-model="queryParams.isActive" placeholder="请选择是否启用" clearable>
          <el-option
            v-for="dict in sys_yes_no"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="每日调用次数上限" prop="dailyQuota">
        <el-input
          v-model="queryParams.dailyQuota"
          placeholder="请输入每日调用次数上限"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="累计消耗token" prop="usedTokens">
        <el-input
          v-model="queryParams.usedTokens"
          placeholder="请输入累计消耗token"
          clearable
          @keyup.enter="handleQuery"
        />
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
          v-hasPermi="['system:key:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['system:key:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['system:key:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['system:key:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="keyList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="密钥ID" align="center" prop="keyId" />
      <el-table-column label="用户ID" align="center" prop="userId" />
      <el-table-column label="供应商" align="center" prop="provider" />
      <el-table-column label="API密钥(脱敏)" align="center" prop="maskedApiKey" />
      <el-table-column label="是否启用" align="center" prop="isActive">
        <template #default="scope">
          <dict-tag :options="sys_yes_no" :value="scope.row.isActive"/>
        </template>
      </el-table-column>
      <el-table-column label="每日调用次数上限" align="center" prop="dailyQuota" />
      <el-table-column label="累计消耗token" align="center" prop="usedTokens" />
      <el-table-column label="备注" align="center" prop="remark" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:key:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['system:key:remove']">删除</el-button>
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

    <!-- 添加或修改用户API Key配置对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="keyRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="用户ID" prop="userId">
              <el-input v-model="form.userId" placeholder="请输入用户ID" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="供应商" prop="provider">
              <el-input v-model="form.provider" placeholder="请输入供应商" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="API密钥" prop="apiKey">
              <el-input v-model="form.apiKey" :placeholder="form.maskedApiKey ? ('当前：' + form.maskedApiKey + '，留空则保持原密钥') : '加密存储，请填写完整密钥'" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="是否启用" prop="isActive">
              <el-radio-group v-model="form.isActive">
                <el-radio
                  v-for="dict in sys_yes_no"
                  :key="dict.value"
                  :value="dict.value"
                >{{dict.label}}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="每日调用次数上限" prop="dailyQuota">
              <el-input v-model="form.dailyQuota" placeholder="请输入每日调用次数上限" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="累计消耗token" prop="usedTokens">
              <el-input v-model="form.usedTokens" placeholder="请输入累计消耗token" />
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
  </div>
</template>

<script setup name="Key">
import { listKey, getKey, delKey, addKey, updateKey } from "@/api/ai/key"

const { proxy } = getCurrentInstance()
const { sys_yes_no } = useDict('sys_yes_no')

const keyList = ref([])
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
    userId: undefined,
    provider: undefined,
    apiKey: undefined,
    isActive: undefined,
    dailyQuota: undefined,
    usedTokens: undefined,
  },
  rules: {
    userId: [
      { required: true, message: "用户ID不能为空", trigger: "blur" }
    ],
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询用户API Key配置列表 */
function getList() {
  loading.value = true
  listKey(queryParams.value).then(response => {
    keyList.value = response.rows
    total.value = response.total
    loading.value = false
  }).catch(() => {
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
    keyId: null,
    userId: null,
    provider: null,
    apiKey: null,
    isActive: null,
    dailyQuota: null,
    usedTokens: null,
    createBy: null,
    createTime: null,
    updateBy: null,
    updateTime: null,
    remark: null
  }
  proxy.resetForm("keyRef")
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
  ids.value = selection.map(item => item.keyId)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "添加用户API Key配置"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _keyId = row.keyId || ids.value
  getKey(_keyId).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改用户API Key配置"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["keyRef"].validate(valid => {
    if (valid) {
      if (form.value.keyId != null) {
        updateKey(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addKey(form.value).then(() => {
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
  const _keyIds = row.keyId || ids.value
  proxy.$modal.confirm('是否确认删除用户API Key配置编号为"' + _keyIds + '"的数据项？').then(function() {
    return delKey(_keyIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('system/key/export', {
    ...queryParams.value
  }, `key_${new Date().getTime()}.xlsx`)
}

getList()
</script>
