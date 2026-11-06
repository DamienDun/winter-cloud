<template>
  <div class="app-container">
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-refresh" size="mini" @click="loadToday" v-hasPermi="['datasource:jobStats:list']">刷新今日统计</el-button>
      </el-col>
    </el-row>

    <el-card shadow="never" v-loading="loading">
      <div slot="header" class="clearfix">
        <span>今日作业统计</span>
        <span style="float: right; color: #909399; font-size: 13px">{{ parseTime(stats.dateTime, '{y}-{m}-{d}') || '-' }}</span>
      </div>
      <el-row :gutter="20">
        <el-col :xs="12" :sm="8" :md="6" v-for="item in cards" :key="item.key">
          <div class="stat-card">
            <div class="stat-label">{{ item.label }}</div>
            <div class="stat-value">{{ stats[item.key] == null ? 0 : stats[item.key] }}</div>
          </div>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script>
import { todayJobStats } from '@/api/datasource/jobStats'

export default {
  name: 'DatasourceJobStats',
  data() {
    return {
      loading: false,
      stats: {},
      cards: [
        { key: 'pendingJob', label: '待执行任务' },
        { key: 'executed', label: '已执行次数' },
        { key: 'failedJob', label: '失败作业个数' },
        { key: 'failedExecution', label: '失败执行次数' },
        { key: 'handleDataTotal', label: '处理数据总量' },
        { key: 'actualUpdateDataTotal', label: '实际更新总量' },
        { key: 'errorDataTotal', label: '异常数据总量' }
      ]
    }
  },
  created() {
    this.loadToday()
  },
  methods: {
    loadToday() {
      this.loading = true
      todayJobStats().then(res => {
        this.stats = res.data || {}
        this.loading = false
      }).catch(() => { this.loading = false })
    }
  }
}
</script>

<style scoped>
.stat-card {
  background: #f8fafc;
  border: 1px solid #eef2f7;
  border-radius: 4px;
  padding: 16px 12px;
  margin-bottom: 16px;
  text-align: center;
}
.stat-label {
  color: #909399;
  font-size: 13px;
  margin-bottom: 8px;
}
.stat-value {
  color: #303133;
  font-size: 26px;
  font-weight: 600;
  line-height: 1.2;
}
</style>
