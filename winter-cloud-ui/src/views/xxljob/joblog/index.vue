<template>
  <div class="app-container">
    <el-form :model="queryParams" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="执行器">
        <el-select v-model="queryParams.jobGroup" clearable placeholder="全部" style="width:180px" @change="onGroupChange">
          <el-option v-for="g in groupOptions" :key="g.id" :label="g.title" :value="g.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="任务">
        <el-select v-model="queryParams.jobId" clearable placeholder="全部" style="width:200px">
          <el-option v-for="j in jobOptions" :key="j.id" :label="j.jobDesc" :value="j.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="queryParams.logStatus" clearable placeholder="全部" style="width:120px">
          <el-option label="成功" :value="1" />
          <el-option label="失败" :value="2" />
          <el-option label="进行中" :value="3" />
        </el-select>
      </el-form-item>
      <el-form-item label="调度时间">
        <el-date-picker v-model="dateRange" type="datetimerange" value-format="yyyy-MM-dd HH:mm:ss" range-separator="-" start-placeholder="开始" end-placeholder="结束" style="width:340px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
        <el-button type="danger" plain icon="el-icon-delete" size="mini" @click="openClear" v-hasPermi="['xxljob:joblog:remove']">清理</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="dataList" style="width:100%">
      <el-table-column label="任务ID" prop="jobId" width="90" align="center" />
      <el-table-column label="调度时间" min-width="180" align="center">
        <template slot-scope="scope">{{ formatLogTime(scope.row.triggerTime) }}</template>
      </el-table-column>
      <el-table-column label="调度结果" min-width="120" align="center">
        <template slot-scope="scope">
          <span v-if="Number(scope.row.triggerCode)===200" style="color:#67C23A">成功</span>
          <span v-else-if="Number(scope.row.triggerCode)===500" style="color:#F56C6C">失败</span>
          <span v-else-if="!Number(scope.row.triggerCode)"></span>
          <span v-else>{{ scope.row.triggerCode }}</span>
        </template>
      </el-table-column>
      <el-table-column label="执行时间" min-width="180" align="center">
        <template slot-scope="scope">{{ formatLogTime(scope.row.handleTime) }}</template>
      </el-table-column>
      <el-table-column label="执行结果" min-width="120" align="center">
        <template slot-scope="scope">
          <span v-if="Number(scope.row.handleCode)===200" style="color:#67C23A">成功</span>
          <span v-else-if="Number(scope.row.handleCode)===500" style="color:#F56C6C">失败</span>
          <span v-else-if="Number(scope.row.handleCode)===502" style="color:#F56C6C">失败(超时)</span>
          <span v-else-if="Number(scope.row.handleCode)===0 && Number(scope.row.triggerCode)===200" style="color:#E6A23C">进行中</span>
          <span v-else-if="Number(scope.row.handleCode)===0" style="color:#909399">未执行</span>
          <span v-else>{{ scope.row.handleCode }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" min-width="180" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-view"
            @click="openDetail(scope.row)"
          >查看详情</el-button>
          <el-button
            v-if="Number(scope.row.triggerCode)===200 && Number(scope.row.handleCode)===0"
            size="mini"
            type="text"
            icon="el-icon-close"
            style="color:#F56C6C"
            @click="handleKill(scope.row)"
            v-hasPermi="['xxljob:joblog:remove']"
          >终止任务</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <el-dialog title="查看详情" :visible.sync="detailOpen" width="960px" append-to-body @close="stopPullLog">
      <el-tabs v-model="detailTab">
        <el-tab-pane label="调度备注" name="trigger"><pre class="log-pre">{{ detailTrigger }}</pre></el-tab-pane>
        <el-tab-pane label="执行备注" name="handle"><pre class="log-pre">{{ detailHandle }}</pre></el-tab-pane>
        <el-tab-pane label="执行日志" name="exec">
          <div class="exec-toolbar">
            <el-tag v-if="logPulling" size="mini" type="warning">滚动刷新中...</el-tag>
            <el-tag v-else size="mini" type="success">已结束</el-tag>
          </div>
          <pre ref="logConsole" class="log-pre">{{ detailExec }}</pre>
        </el-tab-pane>
      </el-tabs>
      <div slot="footer" class="dialog-footer"><el-button @click="detailOpen=false">关 闭</el-button></div>
    </el-dialog>

    <el-dialog title="清理日志" :visible.sync="clearOpen" width="480px" append-to-body>
      <el-form label-width="100px" size="small">
        <el-form-item label="清理方式">
          <el-select v-model="clearType" style="width:100%">
            <el-option v-for="o in cleanTypes" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="danger" @click="submitClear">清 理</el-button>
        <el-button @click="clearOpen=false">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listAllJobGroup } from '@/api/xxljob/jobgroup'
import { listJobLog, listJobsByGroup, logDetail, clearJobLog, killJobLog } from '@/api/xxljob/joblog'
import { logCleanTypes } from '../dict'

export default {
  name: 'XxlJobLog',
  data() {
    return {
      loading: false, showSearch: true, total: 0, dataList: [], groupOptions: [], jobOptions: [], dateRange: [],
      detailOpen: false, detailTab: 'trigger', detailTrigger: '', detailHandle: '', detailExec: '',
      clearOpen: false, clearType: 1, cleanTypes: logCleanTypes,
      queryParams: { pageNum: 1, pageSize: 10, jobGroup: undefined, jobId: undefined, logStatus: undefined, filterTime: undefined },
      logPulling: false,
      fromLineNum: 1,
      pullFailCount: 0,
      pullTimer: null,
      pullInFlight: false,
      currentLogRow: null
    }
  },
  created() {
    listAllJobGroup().then(res => { this.groupOptions = res.data || [] })
    this.applyRouteQuery()
  },
  // keep-alive 下从任务管理再次「查询日志」时重新应用路由参数
  watch: {
    '$route'(to) {
      if (to.path && to.path.indexOf('joblog') >= 0) {
        this.applyRouteQuery()
      }
    }
  },
  beforeDestroy() {
    this.stopPullLog()
  },
  methods: {
    formatLogTime(v) {
      if (v == null || v === '') return ''
      const d = new Date(v)
      if (isNaN(d.getTime()) || d.getTime() <= 0) return ''
      return this.parseTime(d)
    },
    applyRouteQuery() {
      const q = this.$route.query || {}
      const jobGroup = (q.jobGroup != null && q.jobGroup !== '') ? Number(q.jobGroup) : undefined
      const jobId = (q.jobId != null && q.jobId !== '') ? Number(q.jobId) : undefined
      this.queryParams.pageNum = 1
      this.queryParams.jobGroup = Number.isFinite(jobGroup) ? jobGroup : undefined
      // 保留路由中的 jobId，不要被 onGroupChange 清掉
      this.queryParams.jobId = Number.isFinite(jobId) ? jobId : undefined
      if (this.queryParams.jobGroup) {
        this.loadJobsByGroup(this.queryParams.jobGroup)
      } else {
        this.jobOptions = []
      }
      this.getList()
    },
    loadJobsByGroup(val) {
      if (!val) {
        this.jobOptions = []
        return
      }
      listJobsByGroup(val).then(res => { this.jobOptions = res.data || [] })
    },
    onGroupChange(val) {
      // 手动切换执行器时清空任务筛选
      this.jobOptions = []
      this.queryParams.jobId = undefined
      this.loadJobsByGroup(val)
    },
    buildFilterTime() {
      if (this.dateRange && this.dateRange.length === 2) this.queryParams.filterTime = this.dateRange[0] + ' - ' + this.dateRange[1]
      else this.queryParams.filterTime = undefined
    },
    getList() {
      this.loading = true
      this.buildFilterTime()
      listJobLog(this.queryParams).then(res => {
        this.dataList = res.rows || []
        this.total = res.total || 0
      }).finally(() => { this.loading = false })
    },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() {
      this.dateRange = []
      this.queryParams = { pageNum: 1, pageSize: 10, jobGroup: undefined, jobId: undefined, logStatus: undefined, filterTime: undefined }
      this.jobOptions = []
      this.getList()
    },
    stripHtml(s) {
      if (!s) return ''
      return String(s).replace(/<br\s*\/?>/gi, '\n').replace(/<[^>]+>/g, '')
    },
    openDetail(row) {
      this.stopPullLog()
      this.detailOpen = true
      // 调度失败时优先看调度备注；否则看执行日志
      const canPullExec = Number(row.triggerCode) === 200 || Number(row.handleCode) > 0
      this.detailTab = canPullExec ? 'exec' : 'trigger'
      this.detailTrigger = this.stripHtml(row.triggerMsg) || '(无)'
      this.detailHandle = this.stripHtml(row.handleMsg) || '(无)'
      this.detailExec = ''
      this.currentLogRow = row
      this.fromLineNum = 1
      this.pullFailCount = 0

      if (!canPullExec) {
        this.detailExec = '任务发起调度失败，无法查看执行日志'
        this.logPulling = false
        return
      }
      if (!row.executorAddress) {
        this.detailExec = '(无执行器地址，无法拉取执行日志)'
        this.logPulling = false
        return
      }

      this.logPulling = true
      this.pullLogOnce()
      // handleCode=0 表示进行中，滚动刷新
      if (Number(row.handleCode) === 0) {
        this.pullTimer = setInterval(() => this.pullLogOnce(), 3000)
      }
    },
    pullLogOnce() {
      const row = this.currentLogRow
      if (!row || !this.detailOpen || this.pullInFlight) return
      if (this.pullFailCount > 20) {
        this.appendLog('\n[滚动日志失败次数过多，已停止]')
        this.stopPullLog()
        return
      }
      this.pullFailCount++
      this.pullInFlight = true
      const triggerTime = row.triggerTime ? new Date(row.triggerTime).getTime() : 0
      logDetail({
        executorAddress: row.executorAddress,
        triggerTime,
        logId: row.id,
        fromLineNum: this.fromLineNum
      }).then(res => {
        const c = res.data || {}
        const from = Number(c.fromLineNum || this.fromLineNum)
        const to = Number(c.toLineNum || 0)
        const content = c.logContent || ''
        const ended = !!c.end

        if (from !== this.fromLineNum) return
        if (this.fromLineNum > to) {
          if (ended) {
            this.appendLog('\n[Rolling Log Finish]')
            this.stopPullLog()
          }
          return
        }

        this.fromLineNum = to + 1
        this.pullFailCount = 0
        this.appendLog(content)

        if (Number(row.handleCode) > 0) {
          this.appendLog('\n[Load Log Finish]')
          this.stopPullLog()
          return
        }
        if (ended) {
          this.appendLog('\n[Rolling Log Finish]')
          this.stopPullLog()
        }
      }).catch(() => {}).finally(() => { this.pullInFlight = false })
    },
    appendLog(text) {
      if (!text) return
      this.detailExec = (this.detailExec || '') + text
      this.$nextTick(() => {
        const el = this.$refs.logConsole
        if (el) el.scrollTop = el.scrollHeight
      })
    },
    stopPullLog() {
      this.logPulling = false
      this.pullInFlight = false
      if (this.pullTimer) {
        clearInterval(this.pullTimer)
        this.pullTimer = null
      }
    },
    handleKill(row) {
      this.$modal.confirm('确认终止该任务执行？').then(() => killJobLog(row.id)).then(() => {
        this.$modal.msgSuccess('已终止')
        this.getList()
      }).catch(() => {})
    },
    openClear() {
      if (this.queryParams.jobGroup == null) {
        this.$modal.msgWarning('请先选择执行器')
        return
      }
      this.clearOpen = true
    },
    submitClear() {
      clearJobLog({
        jobGroup: this.queryParams.jobGroup || 0,
        jobId: this.queryParams.jobId || 0,
        type: this.clearType
      }).then(() => {
        this.$modal.msgSuccess('清理成功')
        this.clearOpen = false
        this.getList()
      })
    }
  }
}
</script>

<style scoped>
.exec-toolbar { margin-bottom: 8px; }
.log-pre {
  max-height: 480px;
  overflow: auto;
  background: #1e1e1e;
  color: #d4d4d4;
  padding: 12px;
  font-size: 12px;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-all;
  margin: 0;
}
</style>
