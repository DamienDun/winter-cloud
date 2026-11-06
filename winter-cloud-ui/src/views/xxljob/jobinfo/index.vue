<template>
  <div class="app-container">
    <el-form :model="queryParams" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="执行器">
        <el-select v-model="queryParams.jobGroup" clearable placeholder="全部" style="width:180px">
          <el-option v-for="g in groupOptions" :key="g.id" :label="g.title" :value="g.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="queryParams.triggerStatus" clearable placeholder="全部" style="width:120px">
          <el-option label="停止" :value="0" />
          <el-option label="运行" :value="1" />
        </el-select>
      </el-form-item>
      <el-form-item label="任务描述"><el-input v-model="queryParams.jobDesc" clearable @keyup.enter.native="handleQuery" /></el-form-item>
      <el-form-item label="JobHandler"><el-input v-model="queryParams.executorHandler" clearable @keyup.enter.native="handleQuery" /></el-form-item>
      <el-form-item label="负责人"><el-input v-model="queryParams.author" clearable style="width:120px" @keyup.enter.native="handleQuery" /></el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['xxljob:jobinfo:add']">新增</el-button></el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="dataList">
      <el-table-column label="任务ID" prop="id" width="80" align="center" />
      <el-table-column label="任务描述" prop="jobDesc" min-width="150" :show-overflow-tooltip="true" />
      <el-table-column label="调度类型" min-width="170" align="center">
        <template slot-scope="scope">
          <el-button v-if="scope.row.scheduleType==='CRON' || scope.row.scheduleType==='FIX_RATE'" type="text" size="mini" @click="showNextTime(scope.row)">
            {{ scheduleLabel(scope.row.scheduleType) }}：{{ scope.row.scheduleConf }}
          </el-button>
          <span v-else>{{ scheduleLabel(scope.row.scheduleType) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="运行模式" min-width="160" align="center" :show-overflow-tooltip="true">
        <template slot-scope="scope">
          <span>{{ glueLabel(scope.row.glueType) }}<template v-if="scope.row.executorHandler">：{{ scope.row.executorHandler }}</template></span>
        </template>
      </el-table-column>
      <el-table-column label="负责人" prop="author" width="90" align="center" />
      <el-table-column label="状态" width="90" align="center">
        <template slot-scope="scope">
          <el-tag :type="scope.row.triggerStatus===1?'success':'info'" size="mini">{{ scope.row.triggerStatus===1 ? 'RUNNING' : 'STOP' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="200" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['xxljob:jobinfo:edit']">编辑</el-button>
          <el-dropdown size="mini" @command="cmd => onMore(cmd, scope.row)" v-hasPermi="['xxljob:jobinfo:edit','xxljob:jobinfo:trigger','xxljob:jobinfo:remove','xxljob:joblog:list']">
            <el-button size="mini" type="text">更多<i class="el-icon-arrow-down el-icon--right" /></el-button>
            <el-dropdown-menu slot="dropdown">
              <el-dropdown-item command="trigger" v-hasPermi="['xxljob:jobinfo:trigger']">执行一次</el-dropdown-item>
              <el-dropdown-item command="log" v-hasPermi="['xxljob:joblog:list']">查询日志</el-dropdown-item>
              <el-dropdown-item command="registry">注册节点</el-dropdown-item>
              <el-dropdown-item v-if="scope.row.scheduleType==='CRON' || scope.row.scheduleType==='FIX_RATE'" command="next">下次执行时间</el-dropdown-item>
              <el-dropdown-item v-if="scope.row.glueType && scope.row.glueType!=='BEAN'" command="glue" v-hasPermi="['xxljob:jobinfo:edit']">GLUE IDE</el-dropdown-item>
              <el-dropdown-item divided command="toggle" v-hasPermi="['xxljob:jobinfo:edit']">{{ scope.row.triggerStatus===1 ? '停止' : '启动' }}</el-dropdown-item>
              <el-dropdown-item command="copy" v-hasPermi="['xxljob:jobinfo:add']">复制</el-dropdown-item>
              <el-dropdown-item command="remove" v-hasPermi="['xxljob:jobinfo:remove']">删除</el-dropdown-item>
            </el-dropdown-menu>
          </el-dropdown>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" :visible.sync="open" width="920px" append-to-body custom-class="xxl-jobinfo-dialog">
      <el-form ref="form" :model="form" :rules="rules" label-width="120px" size="small">
        <fieldset class="xxl-fieldset"><legend>基础配置</legend>
          <el-row :gutter="16">
            <el-col :span="12"><el-form-item label="执行器" prop="jobGroup"><el-select v-model="form.jobGroup" style="width:100%"><el-option v-for="g in groupOptions" :key="g.id" :label="g.title + ' (' + g.appname + ')'" :value="g.id" /></el-select></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="任务描述" prop="jobDesc"><el-input v-model="form.jobDesc" maxlength="50" /></el-form-item></el-col>
          </el-row>
          <el-row :gutter="16">
            <el-col :span="12"><el-form-item label="负责人" prop="author"><el-input v-model="form.author" maxlength="50" /></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="报警邮件"><el-input v-model="form.alarmEmail" placeholder="多个邮件逗号分隔" maxlength="100" /></el-form-item></el-col>
          </el-row>
        </fieldset>
        <fieldset class="xxl-fieldset"><legend>调度配置</legend>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="调度类型" prop="scheduleType">
                <el-select v-model="form.scheduleType" style="width:100%" @change="onScheduleTypeChange">
                  <el-option v-for="o in scheduleOptions" :key="o.value" :label="o.label" :value="o.value" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item v-if="form.scheduleType==='CRON'" label="Cron" prop="scheduleConf"><xxl-cron-gen v-model="form.scheduleConf" /></el-form-item>
              <el-form-item v-else-if="form.scheduleType==='FIX_RATE'" label="固定速度" prop="scheduleConf"><el-input v-model="form.scheduleConf" placeholder="单位秒" /></el-form-item>
            </el-col>
          </el-row>
        </fieldset>
        <fieldset class="xxl-fieldset"><legend>任务配置</legend>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="运行模式" prop="glueType">
                <el-select v-model="form.glueType" style="width:100%" :disabled="!!form.id">
                  <el-option v-for="o in glueOptions" :key="o.value" :label="o.label" :value="o.value" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item v-if="form.glueType==='BEAN'" label="JobHandler" prop="executorHandler"><el-input v-model="form.executorHandler" /></el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="任务参数"><el-input v-model="form.executorParam" type="textarea" :rows="2" /></el-form-item>
        </fieldset>
        <fieldset class="xxl-fieldset"><legend>高级配置</legend>
          <el-row :gutter="16">
            <el-col :span="12"><el-form-item label="路由策略"><el-select v-model="form.executorRouteStrategy" style="width:100%"><el-option v-for="o in routeOptions" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="子任务ID"><el-input v-model="form.childJobId" placeholder="多个逗号分隔" /></el-form-item></el-col>
          </el-row>
          <el-row :gutter="16">
            <el-col :span="12"><el-form-item label="调度过期策略"><el-select v-model="form.misfireStrategy" style="width:100%"><el-option v-for="o in misfireOptions" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="阻塞处理策略"><el-select v-model="form.executorBlockStrategy" style="width:100%"><el-option v-for="o in blockOptions" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item></el-col>
          </el-row>
          <el-row :gutter="16">
            <el-col :span="12"><el-form-item label="任务超时时间"><el-input-number v-model="form.executorTimeout" :min="0" style="width:100%" placeholder="单位秒，大于零时生效" /></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="失败重试次数"><el-input-number v-model="form.executorFailRetryCount" :min="0" style="width:100%" placeholder="大于零时生效" /></el-form-item></el-col>
          </el-row>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="生效时间">
                <el-date-picker v-model="form.effectiveTimeBegin" type="datetime" value-format="yyyy-MM-dd HH:mm:ss" placeholder="任务生效开始时间" style="width:100%" clearable />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="失效时间">
                <el-date-picker v-model="form.effectiveTimeEnd" type="datetime" value-format="yyyy-MM-dd HH:mm:ss" placeholder="任务失效结束时间" style="width:100%" clearable />
              </el-form-item>
            </el-col>
          </el-row>
        </fieldset>
      </el-form>
      <div slot="footer" class="dialog-footer"><el-button type="primary" @click="submitForm">保 存</el-button><el-button @click="open=false">取 消</el-button></div>
    </el-dialog>

    <el-dialog title="下次执行时间" :visible.sync="nextTimeOpen" width="420px" append-to-body>
      <div v-loading="nextTimeLoading" class="next-time-box">
        <div v-if="nextTimeList.length"><div v-for="(t, i) in nextTimeList" :key="i" class="next-time-item">{{ t }}</div></div>
        <div v-else-if="!nextTimeLoading">{{ nextTimeMsg || '暂无数据' }}</div>
      </div>
      <div slot="footer" class="dialog-footer"><el-button type="primary" @click="nextTimeOpen=false">确 定</el-button></div>
    </el-dialog>

    <el-dialog title="执行一次" :visible.sync="triggerOpen" width="520px" append-to-body>
      <el-form label-width="100px" size="small">
        <el-form-item label="任务参数"><el-input v-model="triggerForm.executorParam" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="机器地址"><el-input v-model="triggerForm.addressList" placeholder="请输入本次执行的机器地址，为空则从执行器获取" /></el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer"><el-button type="primary" @click="submitTrigger">执行</el-button><el-button @click="triggerOpen=false">取 消</el-button></div>
    </el-dialog>

    <el-dialog title="注册节点" :visible.sync="registryOpen" width="480px" append-to-body>
      <div v-if="registryList.length" class="registry-list">
        <div v-for="(addr, idx) in registryList" :key="idx" class="registry-item"><span class="idx">{{ idx + 1 }}.</span><el-tag type="success" size="small">{{ addr }}</el-tag></div>
      </div>
      <div v-else>无</div>
      <div slot="footer" class="dialog-footer"><el-button type="primary" @click="registryOpen=false">确 定</el-button></div>
    </el-dialog>

    <el-dialog title="GLUE IDE" :visible.sync="glueOpen" width="900px" append-to-body destroy-on-close>
      <el-form size="small" label-width="90px">
        <el-form-item label="源码版本">
          <el-select v-model="glueHistoryId" clearable placeholder="当前版本" style="width:100%" @change="onGlueHistoryChange">
            <el-option v-for="h in glueHistory" :key="h.id" :label="(h.glueRemark || '') + ' @ ' + parseTime(h.updateTime)" :value="h.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="源码备注"><el-input v-model="glueForm.glueRemark" maxlength="100" placeholder="请输入备注(4~100字)" /></el-form-item>
        <el-form-item label="源码">
          <el-input v-model="glueForm.glueSource" type="textarea" :rows="18" class="glue-code" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer"><el-button type="primary" @click="submitGlue">保 存</el-button><el-button @click="glueOpen=false">取 消</el-button></div>
    </el-dialog>
  </div>
</template>

<script>
import XxlCronGen from '../components/XxlCronGen'
import { listAllJobGroup } from '@/api/xxljob/jobgroup'
import { listJobInfo, jobEnums, addJobInfo, updateJobInfo, delJobInfo, startJobInfo, stopJobInfo, triggerJobInfo, nextTriggerTime, getJobInfo, saveJobGlue } from '@/api/xxljob/jobinfo'
import { scheduleTypeLabel, misfireLabel, routeLabel, blockLabel, glueTypeLabel, labelOf, optionsOf } from '../dict'

export default {
  name: 'XxlJobInfo',
  components: { XxlCronGen },
  data() {
    const scheduleConfValidator = (rule, value, callback) => {
      if (this.form.scheduleType === 'CRON' || this.form.scheduleType === 'FIX_RATE') {
        if (!value) return callback(new Error('调度配置不能为空'))
      }
      callback()
    }
    return {
      loading: false, showSearch: true, total: 0, dataList: [], groupOptions: [],
      enums: { scheduleType: [], misfireStrategy: [], glueType: [], executorRouteStrategy: [], executorBlockStrategy: [] },
      open: false, title: '', triggerOpen: false, triggerForm: {},
      nextTimeOpen: false, nextTimeLoading: false, nextTimeList: [], nextTimeMsg: '',
      registryOpen: false, registryList: [],
      glueOpen: false, glueForm: { id: undefined, glueSource: '', glueRemark: '' }, glueHistory: [], glueHistoryId: undefined,
      queryParams: { pageNum: 1, pageSize: 10, jobGroup: undefined, triggerStatus: undefined, jobDesc: undefined, executorHandler: undefined, author: undefined },
      form: {},
      rules: {
        jobGroup: [{ required: true, message: '请选择执行器', trigger: 'change' }],
        jobDesc: [{ required: true, message: '任务描述不能为空', trigger: 'blur' }],
        author: [{ required: true, message: '负责人不能为空', trigger: 'blur' }],
        scheduleType: [{ required: true, message: '请选择调度类型', trigger: 'change' }],
        scheduleConf: [{ validator: scheduleConfValidator, trigger: 'blur' }],
        glueType: [{ required: true, message: '请选择运行模式', trigger: 'change' }],
        executorHandler: [{
          validator: (rule, value, callback) => {
            if (this.form.glueType === 'BEAN' && !value) return callback(new Error('JobHandler不能为空'))
            callback()
          },
          trigger: 'blur'
        }]
      }
    }
  },
  computed: {
    scheduleOptions() { return optionsOf(scheduleTypeLabel, this.enums.scheduleType.length ? this.enums.scheduleType : Object.keys(scheduleTypeLabel)) },
    misfireOptions() { return optionsOf(misfireLabel, this.enums.misfireStrategy.length ? this.enums.misfireStrategy : Object.keys(misfireLabel)) },
    routeOptions() { return optionsOf(routeLabel, this.enums.executorRouteStrategy.length ? this.enums.executorRouteStrategy : Object.keys(routeLabel)) },
    blockOptions() { return optionsOf(blockLabel, this.enums.executorBlockStrategy.length ? this.enums.executorBlockStrategy : Object.keys(blockLabel)) },
    glueOptions() { return optionsOf(glueTypeLabel, this.enums.glueType.length ? this.enums.glueType : Object.keys(glueTypeLabel)) }
  },
  created() { this.loadMeta(); this.getList() },
  methods: {
    scheduleLabel(v) { return labelOf(scheduleTypeLabel, v) },
    glueLabel(v) { return labelOf(glueTypeLabel, v) },
    loadMeta() {
      listAllJobGroup().then(res => { this.groupOptions = res.data || [] })
      jobEnums().then(res => { this.enums = Object.assign({}, this.enums, res.data || {}) })
    },
    getList() {
      this.loading = true
      listJobInfo(this.queryParams).then(res => { this.dataList = res.rows || []; this.total = res.total || 0 }).finally(() => { this.loading = false })
    },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.queryParams = { pageNum: 1, pageSize: 10, jobGroup: undefined, triggerStatus: undefined, jobDesc: undefined, executorHandler: undefined, author: undefined }; this.getList() },
    reset() {
      this.form = { id: undefined, jobGroup: this.groupOptions[0] ? this.groupOptions[0].id : undefined, jobDesc: '', author: '', alarmEmail: '', scheduleType: 'CRON', scheduleConf: '', misfireStrategy: 'DO_NOTHING', executorRouteStrategy: 'FIRST', executorHandler: '', executorParam: '', executorBlockStrategy: 'SERIAL_EXECUTION', executorTimeout: 0, executorFailRetryCount: 0, glueType: 'BEAN', glueRemark: 'GLUE代码初始化', glueSource: '', childJobId: '', effectiveTimeBegin: undefined, effectiveTimeEnd: undefined }
      this.resetForm('form')
    },
    onScheduleTypeChange() {
      this.form.scheduleConf = ''
      this.$nextTick(() => this.$refs.form && this.$refs.form.clearValidate('scheduleConf'))
    },
    loadNextTimes(scheduleType, scheduleConf) {
      this.nextTimeOpen = true
      this.nextTimeLoading = true
      this.nextTimeList = []
      this.nextTimeMsg = ''
      return nextTriggerTime({ scheduleType, scheduleConf }).then(res => {
        this.nextTimeList = res.data || []
        if (!this.nextTimeList.length) this.nextTimeMsg = res.msg || '暂无数据'
      }).catch(err => {
        this.nextTimeMsg = (err && (err.msg || err.message)) || '计算失败'
      }).finally(() => { this.nextTimeLoading = false })
    },
    showNextTime(row) { this.loadNextTimes(row.scheduleType, row.scheduleConf) },
    resolveRegistryByGroup(jobGroup) {
      const g = (this.groupOptions || []).find(x => x.id === jobGroup)
      if (!g) return []
      if (Array.isArray(g.registryList) && g.registryList.length) return g.registryList.filter(Boolean)
      if (g.addressList && String(g.addressList).trim()) return String(g.addressList).split(',').map(s => s.trim()).filter(Boolean)
      return []
    },
    openRegistry(row) {
      this.registryList = this.resolveRegistryByGroup(row.jobGroup)
      this.registryOpen = true
    },
    onMore(cmd, row) {
      if (cmd === 'trigger') this.openTrigger(row)
      else if (cmd === 'log') this.goLog(row)
      else if (cmd === 'registry') this.openRegistry(row)
      else if (cmd === 'next') this.showNextTime(row)
      else if (cmd === 'glue') this.openGlue(row)
      else if (cmd === 'toggle') this.handleToggle(row)
      else if (cmd === 'copy') this.handleCopy(row)
      else if (cmd === 'remove') this.handleDelete(row)
    },
    handleAdd() { this.reset(); this.open = true; this.title = '新增任务' },
    formatDateTime(v) {
      if (!v) return undefined
      if (typeof v === 'string' && v.length >= 19) return v.substring(0, 19).replace('T', ' ')
      const d = new Date(v)
      if (isNaN(d.getTime())) return undefined
      const p = n => (n < 10 ? '0' + n : '' + n)
      return d.getFullYear() + '-' + p(d.getMonth() + 1) + '-' + p(d.getDate()) + ' ' + p(d.getHours()) + ':' + p(d.getMinutes()) + ':' + p(d.getSeconds())
    },
    fillForm(row) {
      this.form = Object.assign({}, row, {
        effectiveTimeBegin: this.formatDateTime(row.effectiveTimeBegin),
        effectiveTimeEnd: this.formatDateTime(row.effectiveTimeEnd)
      })
    },
    handleUpdate(row) { this.reset(); this.fillForm(row); this.open = true; this.title = '更新任务' },
    handleCopy(row) {
      this.reset()
      this.fillForm(row)
      this.form.id = undefined
      this.form.triggerStatus = 0
      this.open = true
      this.title = '新增任务'
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (!valid) return
        const req = this.form.id ? updateJobInfo(this.form) : addJobInfo(this.form)
        req.then(() => { this.$modal.msgSuccess('操作成功'); this.open = false; this.getList() })
      })
    },
    handleDelete(row) {
      this.$modal.confirm('确认删除该任务？').then(() => delJobInfo(row.id)).then(() => { this.getList(); this.$modal.msgSuccess('删除成功') }).catch(() => {})
    },
    handleToggle(row) {
      const req = row.triggerStatus === 1 ? stopJobInfo(row.id) : startJobInfo(row.id)
      req.then(() => { this.$modal.msgSuccess(row.triggerStatus === 1 ? '已停止' : '已启动'); this.getList() })
    },
    openTrigger(row) { this.triggerForm = { id: row.id, executorParam: row.executorParam || '', addressList: '' }; this.triggerOpen = true },
    submitTrigger() { triggerJobInfo(this.triggerForm).then(() => { this.$modal.msgSuccess('触发成功'); this.triggerOpen = false }) },
    goLog(row) { this.$router.push({ path: '/xxljob/joblog', query: { jobGroup: row.jobGroup, jobId: row.id } }) },
    openGlue(row) {
      getJobInfo(row.id).then(res => {
        const data = res.data || {}
        const info = data.jobInfo || row
        this.glueHistory = data.jobLogGlues || []
        this.glueHistoryId = undefined
        this.glueForm = { id: info.id, glueSource: info.glueSource || '', glueRemark: '' }
        this.glueOpen = true
      })
    },
    onGlueHistoryChange(id) {
      if (!id) return
      const h = this.glueHistory.find(x => x.id === id)
      if (h) {
        this.glueForm.glueSource = h.glueSource || ''
        this.glueForm.glueRemark = h.glueRemark || ''
      }
    },
    submitGlue() {
      if (!this.glueForm.glueRemark || this.glueForm.glueRemark.length < 4) {
        this.$modal.msgWarning('源码备注长度限制为4~100')
        return
      }
      saveJobGlue(this.glueForm).then(() => {
        this.$modal.msgSuccess('保存成功')
        this.glueOpen = false
        this.getList()
      })
    }
  }
}
</script>

<style scoped>
.next-time-box { min-height: 80px; text-align: left; line-height: 1.8; padding-left: 12px; }
.next-time-item { padding: 2px 0; }
.xxl-fieldset { border: 1px solid #e6ebf5; border-radius: 4px; margin: 0 0 14px; padding: 8px 12px 0; }
.xxl-fieldset legend { padding: 0 8px; font-size: 13px; font-weight: 600; color: gray; border-bottom: none; }
.registry-list { line-height: 2; }
.registry-item .idx { margin-right: 6px; color: #909399; }
.glue-code >>> textarea { font-family: Consolas, Monaco, monospace; font-size: 13px; }
</style>
