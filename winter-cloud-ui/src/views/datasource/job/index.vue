<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="88px">
      <el-form-item label="作业名称" prop="jobName">
        <el-input v-model="queryParams.jobName" placeholder="作业名称" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="作业状态" prop="jobStatus">
        <el-select v-model="queryParams.jobStatus" placeholder="状态" clearable>
          <el-option label="停用" :value="0" />
          <el-option label="开启" :value="1" />
          <el-option label="过期" :value="2" />
        </el-select>
      </el-form-item>
      <el-form-item label="触发方式" prop="triggerType">
        <el-select v-model="queryParams.triggerType" placeholder="触发方式" clearable>
          <el-option label="手动" :value="0" />
          <el-option label="定时" :value="1" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['datasource:job:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate" v-hasPermi="['datasource:job:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['datasource:job:remove']">删除</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="dataList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="作业名称" align="center" prop="jobName" min-width="140" :show-overflow-tooltip="true" />
      <el-table-column label="类型" align="center" prop="datasourceType" width="100" />
      <el-table-column label="数据源" align="center" prop="datasourceName" min-width="120" :show-overflow-tooltip="true" />
      <el-table-column label="同步" align="center" prop="syncType" width="80">
        <template slot-scope="scope">{{ scope.row.syncType === 1 ? '增量' : '全量' }}</template>
      </el-table-column>
      <el-table-column label="触发" align="center" prop="triggerType" width="80">
        <template slot-scope="scope">{{ scope.row.triggerType === 1 ? '定时' : '手动' }}</template>
      </el-table-column>
      <el-table-column label="Cron" align="center" prop="cron" min-width="120" :show-overflow-tooltip="true" />
      <el-table-column label="状态" align="center" prop="jobStatus" width="80">
        <template slot-scope="scope">
          <el-tag :type="jobStatusTag(scope.row.jobStatus)" size="mini">{{ jobStatusLabel(scope.row.jobStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="最近执行" align="center" prop="lastExecuteStatus" width="90">
        <template slot-scope="scope">{{ lastExecLabel(scope.row.lastExecuteStatus) }}</template>
      </el-table-column>
      <el-table-column label="xxlJobId" align="center" prop="xxlJobId" width="100" />
      <el-table-column label="操作" align="center" width="320" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-video-play" @click="handleStart(scope.row)" v-hasPermi="['datasource:job:changeStatus']" v-if="scope.row.jobStatus !== 1">开启</el-button>
          <el-button size="mini" type="text" icon="el-icon-video-pause" @click="handleStop(scope.row)" v-hasPermi="['datasource:job:changeStatus']" v-if="scope.row.jobStatus === 1">停用</el-button>
          <el-button size="mini" type="text" icon="el-icon-caret-right" @click="handleExecute(scope.row)" v-hasPermi="['datasource:job:execute']">执行</el-button>
          <el-button size="mini" type="text" icon="el-icon-document" @click="goLog(scope.row)">日志</el-button>
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['datasource:job:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['datasource:job:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" :visible.sync="open" width="680px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="110px" size="small">
        <el-form-item label="作业名称" prop="jobName">
          <el-input v-model="form.jobName" placeholder="请输入作业名称" />
        </el-form-item>
        <el-form-item label="数据源类型" prop="datasourceType">
          <el-select v-model="form.datasourceType" placeholder="请先选择数据源类型" style="width: 100%" @change="onDatasourceTypeChange">
            <el-option v-for="t in typeOptions" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="数据源" prop="datasourceId">
          <el-select
            v-model="form.datasourceId"
            placeholder="请选择数据源"
            filterable
            style="width: 100%"
            :disabled="!form.datasourceType"
            :loading="dsLoading"
          >
            <el-option
              v-for="ds in datasourceOptions"
              :key="String(ds.id)"
              :label="ds.datasourceName"
              :value="String(ds.id)"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="同步方式" prop="syncType">
          <el-radio-group v-model="form.syncType">
            <el-radio :label="0">全量</el-radio>
            <el-radio :label="1">增量</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="触发方式" prop="triggerType">
          <el-radio-group v-model="form.triggerType" @change="onTriggerChange">
            <el-radio :label="0">手动</el-radio>
            <el-radio :label="1">定时</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="Cron" prop="cron" v-if="form.triggerType === 1">
          <!-- 与 XXL-JOB 任务页同一套 cronGen -->
          <xxl-cron-gen v-model="form.cron" />
        </el-form-item>
        <el-form-item label="生效时间">
          <el-date-picker v-model="effectiveRange" type="datetimerange" value-format="yyyy-MM-dd HH:mm:ss" range-separator="至" start-placeholder="开始" end-placeholder="结束" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listJob, getJob, addJob, updateJob, delJob, startJob, stopJob, executeJob } from '@/api/datasource/job'
import { listAllDatasource } from '@/api/datasource/info'
import { listApiDatasource } from '@/api/datasource/api'
import XxlCronGen from '@/views/xxljob/components/XxlCronGen'

export default {
  name: 'DatasourceJob',
  components: { XxlCronGen },
  data() {
    return {
      loading: false,
      dsLoading: false,
      showSearch: true,
      total: 0,
      dataList: [],
      ids: [],
      single: true,
      multiple: true,
      open: false,
      title: '',
      jdbcAllOptions: [],
      datasourceOptions: [],
      typeOptions: [
        { value: 'api', label: 'API' },
        { value: 'mysql', label: 'MySQL' },
        { value: 'oracle', label: 'Oracle' },
        { value: 'postgresql', label: 'PostgreSQL' },
        { value: 'sqlserver', label: 'SQLServer' },
        { value: 'hive', label: 'Hive' },
        { value: 'clickhouse', label: 'ClickHouse' },
        { value: 'presto', label: 'Presto' }
      ],
      effectiveRange: [],
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        jobName: undefined,
        jobStatus: undefined,
        triggerType: undefined
      },
      form: {},
      rules: {
        jobName: [{ required: true, message: '作业名称不能为空', trigger: 'blur' }],
        datasourceType: [{ required: true, message: '请选择数据源类型', trigger: 'change' }],
        datasourceId: [{ required: true, message: '请选择数据源', trigger: 'change' }],
        syncType: [{ required: true, message: '请选择同步方式', trigger: 'change' }],
        triggerType: [{ required: true, message: '请选择触发方式', trigger: 'change' }],
        cron: [{ required: true, message: '请填写Cron', trigger: 'blur' }]
      }
    }
  },
  created() {
    this.getList()
    this.loadJdbcAll()
  },
  methods: {
    getList() {
      this.loading = true
      listJob(this.queryParams).then(res => {
        this.dataList = res.rows || []
        this.total = res.total || 0
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    loadJdbcAll() {
      return listAllDatasource().then(res => {
        this.jdbcAllOptions = (res.data || []).map(i => ({
          ...i,
          id: i.id != null ? String(i.id) : i.id
        }))
        return this.jdbcAllOptions
      }).catch(() => {
        this.jdbcAllOptions = []
        return []
      })
    },
    /** 按类型加载下拉：api → API数据源；其它 → JDBC 中对应类型 */
    loadDatasourcesByType(type) {
      if (!type) {
        this.datasourceOptions = []
        return Promise.resolve([])
      }
      this.dsLoading = true
      const done = list => {
        this.datasourceOptions = list
        this.dsLoading = false
        return list
      }
      if (type === 'api') {
        return listApiDatasource({ pageNum: 1, pageSize: 500, status: 1 }).then(res => {
          const list = (res.rows || []).map(i => ({
            id: i.id != null ? String(i.id) : i.id,
            datasourceName: i.datasourceName,
            datasourceType: 'api'
          }))
          return done(list)
        }).catch(() => done([]))
      }
      const applyJdbc = () => {
        const list = (this.jdbcAllOptions || []).filter(i => i.datasourceType === type)
        return done(list)
      }
      if (!this.jdbcAllOptions.length) {
        return this.loadJdbcAll().then(applyJdbc).catch(() => done([]))
      }
      return Promise.resolve(applyJdbc())
    },
    onDatasourceTypeChange(type) {
      this.form.datasourceId = undefined
      this.loadDatasourcesByType(type)
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
      this.ids = selection.map(i => i.id)
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },
    jobStatusLabel(v) {
      return ({ 0: '停用', 1: '开启', 2: '过期' })[v] || '-'
    },
    jobStatusTag(v) {
      return ({ 0: 'info', 1: 'success', 2: 'warning' })[v] || 'info'
    },
    lastExecLabel(v) {
      return ({ [-1]: '未执行', 0: '执行中', 1: '成功', 2: '失败', 3: '异常' })[v] || '-'
    },
    reset() {
      this.form = {
        id: undefined,
        jobName: undefined,
        datasourceType: undefined,
        datasourceId: undefined,
        syncType: 0,
        triggerType: 0,
        cron: '',
        remark: undefined
      }
      this.datasourceOptions = []
      this.effectiveRange = []
      this.resetForm('form')
    },
    onTriggerChange() {
      if (this.form.triggerType !== 1) {
        this.form.cron = ''
      }
    },
    handleAdd() {
      this.reset()
      this.open = true
      this.title = '新增作业'
    },
    handleUpdate(row) {
      this.reset()
      const id = row && row.id != null ? String(row.id) : String(this.ids[0])
      getJob(id).then(res => {
        const data = Object.assign({}, res.data || {})
        if (data.id != null) data.id = String(data.id)
        if (data.datasourceId != null) data.datasourceId = String(data.datasourceId)
        this.form = data
        if (this.form.effectiveTimeBegin && this.form.effectiveTimeEnd) {
          this.effectiveRange = [this.form.effectiveTimeBegin, this.form.effectiveTimeEnd]
        }
        this.open = true
        this.title = '修改作业'
        // 先按类型加载候选项，再回填数据源ID
        const dsId = this.form.datasourceId
        this.form.datasourceId = undefined
        this.loadDatasourcesByType(this.form.datasourceType).then(() => {
          this.$set(this.form, 'datasourceId', dsId)
        })
      })
    },
    cancel() {
      this.open = false
      this.reset()
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (!valid) return
        if (this.form.triggerType === 1 && !this.form.cron) {
          this.$modal.msgWarning('定时作业必须填写Cron')
          return
        }
        if (this.effectiveRange && this.effectiveRange.length === 2) {
          this.form.effectiveTimeBegin = this.effectiveRange[0]
          this.form.effectiveTimeEnd = this.effectiveRange[1]
        }
        const req = this.form.id != null ? updateJob(this.form) : addJob(this.form)
        req.then(() => {
          this.$modal.msgSuccess(this.form.id != null ? '修改成功' : '新增成功')
          this.open = false
          this.getList()
        })
      })
    },
    handleDelete(row) {
      const ids = row && row.id != null ? row.id : this.ids.join(',')
      this.$modal.confirm('是否确认删除选中的作业？').then(() => delJob(ids)).then(() => {
        this.getList()
        this.$modal.msgSuccess('删除成功')
      }).catch(() => {})
    },
    handleStart(row) {
      this.$modal.confirm('确认开启作业「' + row.jobName + '」？').then(() => startJob(row.id)).then(() => {
        this.$modal.msgSuccess('已开启')
        this.getList()
      }).catch(() => {})
    },
    handleStop(row) {
      this.$modal.confirm('确认停用作业「' + row.jobName + '」？').then(() => stopJob(row.id)).then(() => {
        this.$modal.msgSuccess('已停用')
        this.getList()
      }).catch(() => {})
    },
    handleExecute(row) {
      this.$modal.confirm('确认立即执行一次「' + row.jobName + '」？').then(() => executeJob(row.id)).then(() => {
        this.$modal.msgSuccess('已触发执行')
      }).catch(() => {})
    },
    goLog(row) {
      this.$router.push({ path: '/datasource/jobLog', query: { jobId: row.id, jobName: row.jobName } })
    }
  }
}
</script>
