<template>
  <div class="app-container">
    <el-form :model="queryParams" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="AppName"><el-input v-model="queryParams.appname" clearable @keyup.enter.native="handleQuery" /></el-form-item>
      <el-form-item label="名称"><el-input v-model="queryParams.title" clearable @keyup.enter.native="handleQuery" /></el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['xxljob:jobgroup:add']">新增</el-button></el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList" />
    </el-row>
    <el-table v-loading="loading" :data="dataList">
      <el-table-column label="AppName" prop="appname" min-width="160" :show-overflow-tooltip="true" />
      <el-table-column label="名称" prop="title" min-width="140" :show-overflow-tooltip="true" />
      <el-table-column label="注册方式" width="110" align="center">
        <template slot-scope="scope">{{ scope.row.addressType === 0 ? '自动注册' : '手动录入' }}</template>
      </el-table-column>
      <el-table-column label="OnLine 机器地址" min-width="140" align="center">
        <template slot-scope="scope">
          <el-button
            v-if="registryCount(scope.row) > 0"
            type="text"
            size="mini"
            @click="openRegistry(scope.row)"
          >查看 ({{ registryCount(scope.row) }})</el-button>
          <span v-else>无</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="160">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['xxljob:jobgroup:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['xxljob:jobgroup:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" :visible.sync="open" width="640px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="110px" size="small">
        <fieldset class="xxl-fieldset">
          <legend>基础配置</legend>
          <el-form-item label="AppName" prop="appname"><el-input v-model="form.appname" placeholder="执行器AppName" maxlength="64" /></el-form-item>
          <el-form-item label="名称" prop="title"><el-input v-model="form.title" placeholder="执行器名称" maxlength="12" /></el-form-item>
          <el-form-item label="注册方式" prop="addressType">
            <el-radio-group v-model="form.addressType">
              <el-radio :label="0">自动注册</el-radio>
              <el-radio :label="1">手动录入</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="机器地址" v-if="form.addressType===1">
            <el-input v-model="form.addressList" type="textarea" :rows="3" placeholder="多个地址用逗号分隔，如 http://127.0.0.1:9999/" />
          </el-form-item>
        </fieldset>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">保 存</el-button>
        <el-button @click="open=false">取 消</el-button>
      </div>
    </el-dialog>

    <el-dialog title="注册节点" :visible.sync="registryOpen" width="480px" append-to-body>
      <div v-if="registryList.length" class="registry-list">
        <div v-for="(addr, idx) in registryList" :key="idx" class="registry-item">
          <span class="idx">{{ idx + 1 }}.</span>
          <el-tag type="success" size="small">{{ addr }}</el-tag>
        </div>
      </div>
      <div v-else>无</div>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="registryOpen=false">确 定</el-button>
      </div>
    </el-dialog>
  </div>
</template>
<script>
import { listJobGroup, addJobGroup, updateJobGroup, delJobGroup } from '@/api/xxljob/jobgroup'
export default {
  name: 'XxlJobGroup',
  data() {
    return {
      loading: false, showSearch: true, total: 0, dataList: [], open: false, title: '',
      registryOpen: false, registryList: [],
      queryParams: { pageNum: 1, pageSize: 10, appname: undefined, title: undefined },
      form: {},
      rules: {
        appname: [{ required: true, message: 'AppName不能为空', trigger: 'blur' }],
        title: [{ required: true, message: '名称不能为空', trigger: 'blur' }]
      }
    }
  },
  created() { this.getList() },
  methods: {
    // 在线机器：优先用 registryList，否则从 addressList 拆分
    resolveRegistry(row) {
      if (!row) return []
      if (Array.isArray(row.registryList) && row.registryList.length) {
        return row.registryList.filter(Boolean)
      }
      if (row.addressList && String(row.addressList).trim()) {
        return String(row.addressList).split(',').map(s => s.trim()).filter(Boolean)
      }
      return []
    },
    registryCount(row) {
      return this.resolveRegistry(row).length
    },
    openRegistry(row) {
      this.registryList = this.resolveRegistry(row)
      this.registryOpen = true
    },
    getList() {
      this.loading = true
      listJobGroup(this.queryParams).then(res => {
        this.dataList = res.rows || []
        this.total = res.total || 0
      }).finally(() => { this.loading = false })
    },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.queryParams = { pageNum: 1, pageSize: 10, appname: undefined, title: undefined }; this.getList() },
    reset() { this.form = { id: undefined, appname: undefined, title: undefined, addressType: 0, addressList: '' }; this.resetForm('form') },
    handleAdd() { this.reset(); this.open = true; this.title = '新增执行器' },
    handleUpdate(row) { this.reset(); this.form = Object.assign({}, row); this.open = true; this.title = '修改执行器' },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (!valid) return
        const req = this.form.id ? updateJobGroup(this.form) : addJobGroup(this.form)
        req.then(() => { this.$modal.msgSuccess(this.form.id ? '修改成功' : '新增成功'); this.open = false; this.getList() })
      })
    },
    handleDelete(row) {
      this.$modal.confirm('确认删除执行器？').then(() => delJobGroup(row.id)).then(() => { this.getList(); this.$modal.msgSuccess('删除成功') }).catch(() => {})
    }
  }
}
</script>
<style scoped>
.registry-list { line-height: 1.8; }
.registry-item { margin-bottom: 8px; }
.registry-item .idx { margin-right: 6px; color: #666; }
.xxl-fieldset { border: 1px solid #e6ebf5; border-radius: 4px; margin: 0; padding: 8px 12px 0; }
.xxl-fieldset legend { padding: 0 8px; font-size: 13px; font-weight: 600; color: gray; }
</style>
