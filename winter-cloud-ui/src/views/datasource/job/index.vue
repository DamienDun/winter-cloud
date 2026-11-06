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
        <el-form-item label="数据源" prop="datasourceId">
          <el-select v-model="form.datasourceId" placeholder="请选择数据源" filterable style="width: 100%">
            <el-option v-for="ds in datasourceOptions" :key="ds.id" :label="ds.datasourceName + ' (' + ds.datasourceType + ')'" :value="ds.id" />
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
          <el-input v-model="form.cron" placeholder="如 0 0/5 * * * ?" style="width: 70%" />
          <el-button type="text" @click="previewCron" style="margin-left: 8px">预览下次</el-button>
          <div v-if="cronPreview.length" style="color: #909399; font-size: 12px; line-height: 1.6; margin-top: 4px">
            <div v-for="(t, i) in cronPreview" :key="i">{{ t }}</div>
          </div>
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
import { listJob, getJob, addJob, updateJob, delJob, startJob, stopJob, executeJob, nextTriggerTime } from '@/api/datasource/job'
import { listAllDatasource } from '@/api/datasource/info'

export default {
  name: 'DatasourceJob',
  data() {
    return {
      loading: false,
      showSearch: true,
      total: 0,
      dataList: [],
      ids: [],
      single: true,
      multiple: true,
      open: false,
      title: '',
      datasourceOptions: [],
      cronPreview: [],
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
        datasourceId: [{ required: true, message: '请选择数据源', trigger: 'change' }],
        syncType: [{ required: true, message: '请选择同步方式', trigger: 'change' }],
        triggerType: [{ required: true, message: '请选择触发方式', trigger: 'change' }],
        cron: [{ required: true, message: '请填写Cron', trigger: 'blur' }]
      }
    }
  },
  created() {
    this.getList()
    this.loadDatasources()
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
    loadDatasources() {
      listAllDatasource().then(res => {
        this.datasourceOptions = res.data || []
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
        datasourceId: undefined,
        syncType: 0,
        triggerType: 0,
        cron: '',
        remark: undefined
      }
      this.effectiveRange = []
      this.cronPreview = []
      this.resetForm('form')
    },
    onTriggerChange() {
      if (this.form.triggerType !== 1) {
        this.form.cron = ''
        this.cronPreview = []
      }
    },
    previewCron() {
      if (!this.form.cron) {
        this.$modal.msgWarning('请先填写Cron')
        return
      }
      nextTriggerTime(this.form.cron).then(res => {
        this.cronPreview = res.data || []
      })
    },
    handleAdd() {
      this.reset()
      this.open = true
      this.title = '新增作业'
    },
    handleUpdate(row) {
      this.reset()
      const id = row && row.id != null ? row.id : this.ids[0]
      getJob(id).then(res => {
        this.form = Object.assign({}, res.data || {})
        if (this.form.effectiveTimeBegin && this.form.effectiveTimeEnd) {
          this.effectiveRange = [this.form.effectiveTimeBegin, this.form.effectiveTimeEnd]
        }
        this.open = true
        this.title = '修改作业'
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
