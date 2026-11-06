<template>
  <div class="app-container xxl-dashboard" v-loading="loading">
    <el-row :gutter="16" class="stat-row">
      <el-col :xs="24" :sm="8">
        <div class="stat-card stat-blue">
          <div class="stat-title">任务数量</div>
          <div class="stat-num">{{ info.jobInfoCount }}</div>
          <div class="stat-desc">调度中心运行的任务数量</div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="8">
        <div class="stat-card stat-orange">
          <div class="stat-title">调度次数</div>
          <div class="stat-num">{{ info.jobLogCount }}</div>
          <div class="stat-desc">调度中心触发的调度次数</div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="8">
        <div class="stat-card stat-green">
          <div class="stat-title">执行器数量</div>
          <div class="stat-num">{{ info.executorCount }}</div>
          <div class="stat-desc">调度中心在线的执行器机器数量</div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="mt16">
      <el-col :span="24">
        <el-card shadow="never">
          <div slot="header" class="clearfix">
            <span>调度报表</span>
            <el-date-picker
              v-model="dateRange"
              class="fr"
              size="small"
              type="datetimerange"
              value-format="yyyy-MM-dd HH:mm:ss"
              range-separator="-"
              start-placeholder="开始"
              end-placeholder="结束"
              :picker-options="pickerOptions"
              @change="loadChart"
            />
          </div>
          <el-row :gutter="16">
            <el-col :xs="24" :md="16">
              <div ref="lineChart" class="chart-box" />
            </el-col>
            <el-col :xs="24" :md="8">
              <div ref="pieChart" class="chart-box" />
            </el-col>
          </el-row>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import * as echarts from 'echarts'
import { dashboardInfo, chartInfo } from '@/api/xxljob/dashboard'

function pad(n) {
  return n < 10 ? '0' + n : '' + n
}
function fmt(d) {
  return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate())
    + ' ' + pad(d.getHours()) + ':' + pad(d.getMinutes()) + ':' + pad(d.getSeconds())
}

export default {
  name: 'XxlJobDashboard',
  data() {
    const end = new Date()
    end.setHours(23, 59, 59, 0)
    const start = new Date()
    start.setDate(start.getDate() - 7)
    start.setHours(0, 0, 0, 0)
    return {
      loading: false,
      info: { jobInfoCount: 0, jobLogCount: 0, executorCount: 0 },
      dateRange: [fmt(start), fmt(end)],
      lineChart: null,
      pieChart: null,
      pickerOptions: {
        shortcuts: [
          {
            text: '最近一周',
            onClick(picker) {
              const e = new Date(); e.setHours(23, 59, 59, 0)
              const s = new Date(); s.setDate(s.getDate() - 7); s.setHours(0, 0, 0, 0)
              picker.$emit('pick', [s, e])
            }
          },
          {
            text: '最近一月',
            onClick(picker) {
              const e = new Date(); e.setHours(23, 59, 59, 0)
              const s = new Date(); s.setMonth(s.getMonth() - 1); s.setHours(0, 0, 0, 0)
              picker.$emit('pick', [s, e])
            }
          }
        ]
      }
    }
  },
  mounted() {
    this.lineChart = echarts.init(this.$refs.lineChart)
    this.pieChart = echarts.init(this.$refs.pieChart)
    window.addEventListener('resize', this.resize)
    this.loadAll()
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.resize)
    if (this.lineChart) { this.lineChart.dispose(); this.lineChart = null }
    if (this.pieChart) { this.pieChart.dispose(); this.pieChart = null }
  },
  methods: {
    resize() {
      this.lineChart && this.lineChart.resize()
      this.pieChart && this.pieChart.resize()
    },
    loadAll() {
      this.loading = true
      Promise.all([
        dashboardInfo().then(res => { this.info = res.data || this.info }),
        this.loadChart(false)
      ]).finally(() => { this.loading = false })
    },
    loadChart(showLoading = true) {
      if (!this.dateRange || this.dateRange.length !== 2) return Promise.resolve()
      if (showLoading) this.loading = true
      return chartInfo({ startDate: this.dateRange[0], endDate: this.dateRange[1] })
        .then(res => {
          const d = res.data || {}
          this.renderLine(d)
          this.renderPie(d)
        })
        .finally(() => { if (showLoading) this.loading = false })
    },
    renderLine(d) {
      this.lineChart.setOption({
        title: { text: '日期分布图', left: 'center', textStyle: { fontSize: 14 } },
        tooltip: { trigger: 'axis' },
        legend: { data: ['成功', '失败', '进行中'], bottom: 0 },
        grid: { left: 40, right: 20, top: 40, bottom: 40 },
        xAxis: { type: 'category', boundaryGap: false, data: d.triggerDayList || [] },
        yAxis: { type: 'value', minInterval: 1 },
        series: [
          { name: '成功', type: 'line', stack: '总量', areaStyle: {}, data: d.triggerDayCountSucList || [], itemStyle: { color: '#00a65a' } },
          { name: '失败', type: 'line', stack: '总量', areaStyle: {}, data: d.triggerDayCountFailList || [], itemStyle: { color: '#c23632' } },
          { name: '进行中', type: 'line', stack: '总量', areaStyle: {}, data: d.triggerDayCountRunningList || [], itemStyle: { color: '#f39c12' } }
        ]
      })
    },
    renderPie(d) {
      this.pieChart.setOption({
        title: { text: '成功比例图', left: 'center', textStyle: { fontSize: 14 } },
        tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
        legend: { bottom: 0, data: ['成功', '失败', '进行中'] },
        series: [{
          type: 'pie',
          radius: ['35%', '60%'],
          center: ['50%', '48%'],
          data: [
            { name: '成功', value: d.triggerCountSucTotal || 0, itemStyle: { color: '#00a65a' } },
            { name: '失败', value: d.triggerCountFailTotal || 0, itemStyle: { color: '#c23632' } },
            { name: '进行中', value: d.triggerCountRunningTotal || 0, itemStyle: { color: '#f39c12' } }
          ]
        }]
      })
    }
  }
}
</script>

<style scoped>
.stat-row { margin-bottom: 8px; }
.mt16 { margin-top: 16px; }
.stat-card {
  color: #fff;
  border-radius: 4px;
  padding: 16px 18px;
  margin-bottom: 12px;
  min-height: 110px;
}
.stat-blue { background: #00c0ef; }
.stat-orange { background: #f39c12; }
.stat-green { background: #00a65a; }
.stat-title { font-size: 14px; opacity: .95; }
.stat-num { font-size: 28px; font-weight: 600; margin: 6px 0; }
.stat-desc { font-size: 12px; opacity: .9; }
.chart-box { height: 360px; width: 100%; }
.fr { float: right; }
</style>
