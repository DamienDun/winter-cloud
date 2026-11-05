<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="88px">
      <el-form-item label="数据源名称" prop="datasourceName">
        <el-input v-model="queryParams.datasourceName" placeholder="请输入名称" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="类型" prop="datasourceType">
        <el-select v-model="queryParams.datasourceType" placeholder="类型" clearable>
          <el-option v-for="item in typeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="状态" clearable>
          <el-option label="启用" :value="1" />
          <el-option label="停用" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['datasource:info:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate" v-hasPermi="['datasource:info:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['datasource:info:remove']">删除</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="dataList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="名称" align="center" prop="datasourceName" min-width="120" :show-overflow-tooltip="true" />
      <el-table-column label="分组" align="center" prop="datasourceGroup" width="100" />
      <el-table-column label="类型" align="center" prop="datasourceType" width="110" />
      <el-table-column label="JDBC URL" align="center" prop="jdbcUrl" min-width="220" :show-overflow-tooltip="true" />
      <el-table-column label="用户名" align="center" prop="jdbcUsername" width="100" />
      <el-table-column label="状态" align="center" prop="status" width="80">
        <template slot-scope="scope">
          <el-tag :type="scope.row.status === 1 ? 'success' : 'info'" size="mini">{{ scope.row.status === 1 ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="修改时间" align="center" prop="gmtModified" width="160">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.gmtModified) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="280" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-view" @click="handleView(scope.row)" v-hasPermi="['datasource:info:query']">详情</el-button>
          <el-button size="mini" type="text" icon="el-icon-connection" @click="handleTestRow(scope.row)" v-hasPermi="['datasource:info:test']">测试</el-button>
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['datasource:info:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['datasource:info:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <!-- 查看详情（只读，密码不回显） -->
    <el-dialog title="数据源详情" :visible.sync="viewOpen" width="720px" append-to-body>
      <el-descriptions :column="2" border size="small" v-if="viewForm">
        <el-descriptions-item label="名称">{{ viewForm.datasourceName }}</el-descriptions-item>
        <el-descriptions-item label="分组">{{ viewForm.datasourceGroup }}</el-descriptions-item>
        <el-descriptions-item label="类型">{{ typeLabel(viewForm.datasourceType) }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="viewForm.status === 1 ? 'success' : 'info'" size="mini">{{ viewForm.status === 1 ? '启用' : '停用' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="JDBC URL" :span="2">
          <span style="word-break: break-all">{{ viewForm.jdbcUrl }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="驱动类" :span="2">{{ viewForm.jdbcDriverClass }}</el-descriptions-item>
        <el-descriptions-item label="用户名">{{ viewForm.jdbcUsername }}</el-descriptions-item>
        <el-descriptions-item label="密码">******</el-descriptions-item>
        <el-descriptions-item label="数据库名">{{ viewForm.databaseName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="ID">{{ viewForm.id }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ parseTime(viewForm.gmtCreate) }}</el-descriptions-item>
        <el-descriptions-item label="修改时间">{{ parseTime(viewForm.gmtModified) }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ viewForm.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
      <div slot="footer" class="dialog-footer">
        <el-button type="warning" plain icon="el-icon-connection" :loading="testing" @click="handleTestRow(viewForm)" v-hasPermi="['datasource:info:test']">测试连接</el-button>
        <el-button type="primary" plain icon="el-icon-edit" @click="openEditFromView" v-hasPermi="['datasource:info:edit']">修改</el-button>
        <el-button @click="viewOpen = false">关 闭</el-button>
      </div>
    </el-dialog>

    <el-dialog :title="title" :visible.sync="open" width="720px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="110px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="数据源名称" prop="datasourceName">
              <el-input v-model="form.datasourceName" placeholder="唯一业务名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="分组" prop="datasourceGroup">
              <el-input v-model="form.datasourceGroup" placeholder="Default" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="类型" prop="datasourceType">
              <el-select v-model="form.datasourceType" placeholder="请选择" style="width: 100%" @change="onTypeChange">
                <el-option v-for="item in typeOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-radio-group v-model="form.status">
                <el-radio :label="1">启用</el-radio>
                <el-radio :label="0">停用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="JDBC URL" prop="jdbcUrl">
          <el-input v-model="form.jdbcUrl" type="textarea" :rows="2" placeholder="jdbc:..." />
        </el-form-item>
        <el-form-item label="驱动类" prop="jdbcDriverClass">
          <el-input v-model="form.jdbcDriverClass" placeholder="驱动全类名" />
        </el-form-item>
        <el-row>
          <el-col :span="12">
            <el-form-item label="用户名" prop="jdbcUsername">
              <el-input v-model="form.jdbcUsername" autocomplete="off" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="密码" prop="jdbcPassword">
              <el-input v-model="form.jdbcPassword" type="password" show-password autocomplete="new-password" :placeholder="form.id ? '不修改请留空' : '请输入密码'" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="数据库名" prop="databaseName">
          <el-input v-model="form.databaseName" placeholder="可选" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="warning" plain icon="el-icon-connection" :loading="testing" @click="submitTest" v-hasPermi="['datasource:info:test']">测试连接</el-button>
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listDatasource, getDatasource, addDatasource, updateDatasource, delDatasource, testDatasource } from '@/api/datasource/info'

const TYPE_TEMPLATES = {
  mysql: {
    jdbcUrl: 'jdbc:mysql://127.0.0.1:3306/demo?useUnicode=true&characterEncoding=utf8&serverTimezone=GMT%2B8',
    jdbcDriverClass: 'com.mysql.cj.jdbc.Driver'
  },
  oracle: {
    jdbcUrl: 'jdbc:oracle:thin:@//127.0.0.1:1521/ORCL',
    jdbcDriverClass: 'oracle.jdbc.OracleDriver'
  },
  postgresql: {
    jdbcUrl: 'jdbc:postgresql://127.0.0.1:5432/demo',
    jdbcDriverClass: 'org.postgresql.Driver'
  },
  sqlserver: {
    jdbcUrl: 'jdbc:sqlserver://127.0.0.1:1433;DatabaseName=demo',
    jdbcDriverClass: 'com.microsoft.sqlserver.jdbc.SQLServerDriver'
  },
  hive: {
    jdbcUrl: 'jdbc:hive2://127.0.0.1:10000/default',
    jdbcDriverClass: 'org.apache.hive.jdbc.HiveDriver'
  },
  clickhouse: {
    jdbcUrl: 'jdbc:clickhouse://127.0.0.1:8123/default',
    jdbcDriverClass: 'ru.yandex.clickhouse.ClickHouseDriver'
  },
  presto: {
    jdbcUrl: 'jdbc:presto://127.0.0.1:8080/hive/default',
    jdbcDriverClass: 'com.facebook.presto.jdbc.PrestoDriver'
  }
}

export default {
  name: 'DatasourceInfo',
  data() {
    return {
      loading: true,
      testing: false,
      showSearch: true,
      total: 0,
      dataList: [],
      ids: [],
      single: true,
      multiple: true,
      open: false,
      viewOpen: false,
      viewForm: null,
      title: '',
      typeOptions: [
        { value: 'mysql', label: 'MySQL' },
        { value: 'oracle', label: 'Oracle' },
        { value: 'postgresql', label: 'PostgreSQL' },
        { value: 'sqlserver', label: 'SQLServer' },
        { value: 'hive', label: 'Hive' },
        { value: 'clickhouse', label: 'ClickHouse' },
        { value: 'presto', label: 'Presto' }
      ],
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        datasourceName: undefined,
        datasourceType: undefined,
        status: undefined
      },
      form: {},
      rules: {
        datasourceName: [{ required: true, message: '名称不能为空', trigger: 'blur' }],
        datasourceType: [{ required: true, message: '类型不能为空', trigger: 'change' }],
        jdbcUrl: [{ required: true, message: 'URL不能为空', trigger: 'blur' }],
        jdbcDriverClass: [{ required: true, message: '驱动不能为空', trigger: 'blur' }],
        jdbcUsername: [{ required: true, message: '用户名不能为空', trigger: 'blur' }]
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      listDatasource(this.queryParams).then(res => {
        this.dataList = res.rows || []
        this.total = res.total || 0
        this.loading = false
      }).catch(() => {
        this.loading = false
      })
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.resetForm('queryForm')
      this.handleQuery()
    },
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.id)
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },
    reset() {
      this.form = {
        id: undefined,
        datasourceName: undefined,
        datasourceGroup: 'Default',
        datasourceType: 'mysql',
        jdbcUrl: TYPE_TEMPLATES.mysql.jdbcUrl,
        jdbcDriverClass: TYPE_TEMPLATES.mysql.jdbcDriverClass,
        jdbcUsername: undefined,
        jdbcPassword: undefined,
        databaseName: undefined,
        status: 1,
        remark: undefined
      }
      this.resetForm('form')
    },
    onTypeChange(type) {
      const tpl = TYPE_TEMPLATES[type]
      if (!tpl) return
      // 仅在新增或 URL 仍是旧模板时覆盖，避免覆盖用户已改内容
      this.form.jdbcUrl = tpl.jdbcUrl
      this.form.jdbcDriverClass = tpl.jdbcDriverClass
    },
    typeLabel(type) {
      const hit = this.typeOptions.find(i => i.value === type)
      return hit ? hit.label : (type || '-')
    },
    handleAdd() {
      this.reset()
      this.open = true
      this.title = '新增数据源'
    },
    handleView(row) {
      if (!row || row.id == null) return
      getDatasource(row.id).then(res => {
        this.viewForm = Object.assign({}, res.data || {})
        this.viewOpen = true
      })
    },
    openEditFromView() {
      if (!this.viewForm || this.viewForm.id == null) return
      this.viewOpen = false
      this.handleUpdate(this.viewForm)
    },
    handleUpdate(row) {
      this.reset()
      const id = row && row.id != null ? row.id : this.ids[0]
      getDatasource(id).then(res => {
        this.form = Object.assign({}, res.data || {})
        this.form.jdbcPassword = undefined
        this.open = true
        this.title = '修改数据源'
      })
    },
    cancel() {
      this.open = false
      this.reset()
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (!valid) return
        // 新增必须填密码；修改留空表示不改密
        if (this.form.id == null && !this.form.jdbcPassword) {
          this.$modal.msgWarning('请填写密码')
          return
        }
        const req = this.form.id != null ? updateDatasource(this.form) : addDatasource(this.form)
        req.then(() => {
          this.$modal.msgSuccess(this.form.id != null ? '修改成功' : '新增成功')
          this.open = false
          this.getList()
        })
      })
    },
    handleDelete(row) {
      const ids = row && row.id != null ? row.id : this.ids.join(',')
      this.$modal.confirm('是否确认删除选中的数据源？').then(() => {
        return delDatasource(ids)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess('删除成功')
      }).catch(() => {})
    },
    handleTestRow(row) {
      this.testing = true
      testDatasource({ id: row.id }).then(res => {
        this.$modal.msgSuccess(res.msg || '连接成功')
      }).finally(() => {
        this.testing = false
      })
    },
    submitTest() {
      this.$refs.form.validate(valid => {
        if (!valid) return
        // 编辑且未填密码：带 id 让后端取库密文
        const payload = Object.assign({}, this.form)
        if (payload.id && !payload.jdbcPassword) {
          // keep empty password
        } else if (!payload.id && !payload.jdbcPassword) {
          this.$modal.msgWarning('请填写密码后再测试')
          return
        }
        this.testing = true
        testDatasource(payload).then(res => {
          this.$modal.msgSuccess(res.msg || '连接成功')
        }).finally(() => {
          this.testing = false
        })
      })
    }
  }
}
</script>
