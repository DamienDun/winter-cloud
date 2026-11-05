<template>
  <div class="ds-sql-panel">
    <el-alert
      title="仅支持只读语句：SELECT / WITH / SHOW / DESCRIBE / EXPLAIN；禁止写操作与 DDL；默认最多返回 200 行，硬上限 1000 行，超时 30 秒。"
      type="info"
      :closable="false"
      show-icon
      class="mb8"
    />
    <el-form size="small" :inline="true" class="mb8">
      <el-form-item label="最大行数">
        <el-input-number v-model="maxRows" :min="1" :max="1000" :step="50" controls-position="right" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-caret-right" :loading="loading" :disabled="!canRun" @click="handleRun" v-hasPermi="['datasource:sql:query']">执行</el-button>
        <el-button icon="el-icon-document" :disabled="!suggestTable" @click="fillSuggest" v-hasPermi="['datasource:sql:query']">当前表模板</el-button>
        <el-button icon="el-icon-refresh-left" :disabled="loading" @click="handleClear">清空结果</el-button>
      </el-form-item>
      <el-form-item>
        <span class="tip">Ctrl + Enter 执行</span>
      </el-form-item>
    </el-form>

    <el-input
      type="textarea"
      v-model="sql"
      :rows="10"
      placeholder="在此编写 SQL，例如：SELECT * FROM your_table WHERE id > 0 LIMIT 50"
      class="sql-editor mb8"
      @keydown.ctrl.enter.native="handleRun"
    />

    <div class="result-meta mb8" v-if="hasResult">
      <el-tag size="mini" type="success">行数 {{ rowCount }}</el-tag>
      <el-tag size="mini" type="info" style="margin-left: 8px">耗时 {{ costMs }} ms</el-tag>
      <el-tag v-if="truncated" size="mini" type="warning" style="margin-left: 8px">已截断至 {{ maxRows }}</el-tag>
    </div>

    <el-table v-loading="loading" :data="rows" border size="mini" max-height="480" v-if="columns.length">
      <el-table-column
        v-for="col in columns"
        :key="col"
        :prop="col"
        :label="col"
        min-width="120"
        :show-overflow-tooltip="true"
      >
        <template slot-scope="scope">
          {{ formatCell(scope.row[col]) }}
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-else-if="hasResult && !columns.length" description="查询成功，无结果列" />
    <el-empty v-else-if="!loading && !hasResult" description="执行 SQL 后在此展示结果" />
  </div>
</template>

<script>
import { runSqlQuery } from '@/api/datasource/sql'

export default {
  name: 'SqlQueryPanel',
  props: {
    datasourceId: { type: [Number, String], default: undefined },
    suggestTable: { type: String, default: '' }
  },
  data() {
    return {
      sql: '',
      maxRows: 200,
      loading: false,
      columns: [],
      rows: [],
      rowCount: 0,
      costMs: 0,
      truncated: false,
      hasResult: false
    }
  },
  computed: {
    canRun() {
      return !!this.datasourceId && !!(this.sql && this.sql.trim()) && !this.loading
    }
  },
  methods: {
    fillSuggest() {
      if (!this.suggestTable) return
      this.sql = 'SELECT * FROM ' + this.suggestTable + ' LIMIT 50'
    },
    handleRun() {
      if (!this.canRun) return
      this.loading = true
      runSqlQuery({
        datasourceId: this.datasourceId,
        sql: this.sql,
        maxRows: this.maxRows
      }).then(res => {
        const data = res.data || {}
        this.columns = data.columns || []
        this.rows = data.rows || []
        this.rowCount = data.rowCount != null ? data.rowCount : this.rows.length
        this.costMs = data.costMs != null ? data.costMs : 0
        this.truncated = !!data.truncated
        this.hasResult = true
        if (this.truncated) {
          this.$modal.msgWarning('结果已按最大行数截断')
        }
      }).finally(() => {
        this.loading = false
      })
    },
    handleClear() {
      this.columns = []
      this.rows = []
      this.rowCount = 0
      this.costMs = 0
      this.truncated = false
      this.hasResult = false
    },
    formatCell(val) {
      if (val === null || val === undefined) return ''
      if (typeof val === 'object') {
        try {
          return JSON.stringify(val)
        } catch (e) {
          return String(val)
        }
      }
      return val
    }
  }
}
</script>

<style scoped>
.ds-sql-panel .sql-editor >>> textarea {
  font-family: Consolas, Monaco, 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.5;
}
.result-meta {
  display: flex;
  align-items: center;
}
.tip {
  color: #909399;
  font-size: 12px;
}
</style>
