<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="88px">
      <el-form-item label="作业名称" prop="jobName">
        <el-input v-model="queryParams.jobName" placeholder="作业名称" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="作业ID" prop="jobId">
        <el-input v-model="queryParams.jobId" placeholder="作业ID" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="执行状态" prop="executeStatus">
        <el-select v-model="queryParams.executeStatus" placeholder="状态" clearable>
          <el-option label="执行中" :value="0" />
          <el-option label="成功" :value="1" />
          <el-option label="失败" :value="2" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['datasource:jobLog:remove']">删除</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="dataList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="作业名称" align="center" prop="jobName" min-width="140" :show-overflow-tooltip="true" />
      <el-table-column label="触发方式" align="center" prop="triggerType" width="90">
        <template slot-scope="scope">{{ scope.row.triggerType === 1 ? '定时' : '手动' }}</template>
      </el-table-column>
      <el-table-column label="触发人" align="center" prop="triggerUsername" width="100" />
      <el-table-column label="执行状态" align="center" prop="executeStatus" width="90">
        <template slot-scope="scope">
          <el-tag :type="statusTag(scope.row.executeStatus)" size="mini">{{ statusLabel(scope.row.executeStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="开始时间" align="center" prop="executeBeginTime" width="160">
        <template slot-scope="scope"><span>{{ parseTime(scope.row.executeBeginTime) }}</span></template>
      </el-table-column>
      <el-table-column label="结束时间" align="center" prop="executeEndTime" width="160">
        <template slot-scope="scope"><span>{{ parseTime(scope.row.executeEndTime) }}</span></template>
      </el-table-column>
      <el-table-column label="耗时" align="center" prop="timeConsuming" width="90" />
      <el-table-column label="处理量" align="center" prop="handleDataTotal" width="80" />
      <el-table-column label="操作" align="center" width="140" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-view" @click="handleView(scope.row)" v-hasPermi="['datasource:jobLog:query']">详情</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['datasource:jobLog:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <el-dialog title="日志详情" :visible.sync="viewOpen" width="720px" append-to-body>
      <el-descriptions :column="2" border size="small" v-if="viewForm">
        <el-descriptions-item label="作业名称">{{ viewForm.jobName }}</el-descriptions-item>
        <el-descriptions-item label="触发人">{{ viewForm.triggerUsername }}</el-descriptions-item>
        <el-descriptions-item label="触发方式">{{ viewForm.triggerType === 1 ? '定时' : '手动' }}</el-descriptions-item>
        <el-descriptions-item label="执行状态">{{ statusLabel(viewForm.executeStatus) }}</el-descriptions-item>
        <el-descriptions-item label="开始时间">{{ parseTime(viewForm.executeBeginTime) }}</el-descriptions-item>
        <el-descriptions-item label="结束时间">{{ parseTime(viewForm.executeEndTime) }}</el-descriptions-item>
        <el-descriptions-item label="耗时">{{ viewForm.timeConsuming }}</el-descriptions-item>
        <el-descriptions-item label="处理/更新/异常">{{ viewForm.handleDataTotal }} / {{ viewForm.actualUpdateDataTotal }} / {{ viewForm.errorDataTotal }}</el-descriptions-item>
        <el-descriptions-item label="日志信息" :span="2">
          <pre style="white-space: pre-wrap; margin: 0; font-family: inherit">{{ viewForm.jobMessage }}</pre>
        </el-descriptions-item>
      </el-descriptions>
      <div slot="footer" class="dialog-footer">
        <el-button @click="viewOpen = false">关 闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listJobLog, getJobLog, delJobLog } from '@/api/datasource/jobLog'

export default {
  name: 'DatasourceJobLog',
  data() {
    return {
      loading: false,
      showSearch: true,
      total: 0,
      dataList: [],
      ids: [],
      multiple: true,
      viewOpen: false,
      viewForm: null,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        jobName: undefined,
        jobId: undefined,
        executeStatus: undefined
      }
    }
  },
  created() {
    this.applyRouteQuery()
    this.getList()
  },
  watch: {
    '$route'(to) {
      if (to.path && to.path.indexOf('jobLog') >= 0) {
        this.applyRouteQuery()
        this.handleQuery()
      }
    }
  },
  methods: {
    applyRouteQuery() {
      const q = this.$route.query || {}
      // 统一转字符串，避免雪花 ID 被 Number 化丢精度后查不到日志
      if (q.jobId != null && q.jobId !== '') {
        this.queryParams.jobId = String(q.jobId)
      }
      if (q.jobName) {
        this.queryParams.jobName = q.jobName
      }
    },
    getList() {
      this.loading = true
      const params = Object.assign({}, this.queryParams)
      if (params.jobId === '' || params.jobId == null) {
        delete params.jobId
      } else {
        params.jobId = String(params.jobId)
      }
      listJobLog(params).then(res => {
        this.dataList = res.rows || []
        this.total = res.total || 0
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.resetForm('queryForm')
      this.queryParams.jobId = undefined
      this.queryParams.jobName = undefined
      this.handleQuery()
    },
    handleSelectionChange(selection) {
      this.ids = selection.map(i => i.id)
      this.multiple = !selection.length
    },
    statusLabel(v) {
      return ({ 0: '执行中', 1: '成功', 2: '失败' })[v] || '-'
    },
    statusTag(v) {
      return ({ 0: 'warning', 1: 'success', 2: 'danger' })[v] || 'info'
    },
    handleView(row) {
      getJobLog(row.id).then(res => {
        this.viewForm = res.data || {}
        this.viewOpen = true
      })
    },
    handleDelete(row) {
      const ids = row && row.id != null ? row.id : this.ids.join(',')
      this.$modal.confirm('是否确认删除选中的日志？').then(() => delJobLog(ids)).then(() => {
        this.getList()
        this.$modal.msgSuccess('删除成功')
      }).catch(() => {})
    }
  }
}
</script>
