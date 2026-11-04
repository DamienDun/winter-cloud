<template>
  <div class="app-container ds-table-page">
    <el-form size="small" :inline="true" label-width="80px" class="ds-toolbar">
      <el-form-item label="数据源">
        <el-select
          v-model="datasourceId"
          filterable
          clearable
          placeholder="请选择数据源"
          style="width: 280px"
          @change="onDatasourceChange"
        >
          <el-option
            v-for="item in datasourceOptions"
            :key="item.id"
            :label="item.datasourceName + ' (' + item.datasourceType + ')'"
            :value="item.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="表名过滤">
        <el-input
          v-model="tableFilter"
          placeholder="关键字"
          clearable
          style="width: 180px"
          :disabled="!datasourceId"
          @keyup.enter.native="loadTables"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" :disabled="!datasourceId" @click="loadTables">刷新表</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="12" v-if="datasourceId">
      <el-col :span="6">
        <el-card shadow="never" class="table-list-card" :body-style="{ padding: '8px' }">
          <div slot="header" class="clearfix">
            <span>数据表</span>
            <span class="table-count">{{ filteredTables.length }}</span>
          </div>
          <el-input
            v-model="tableFilterLocal"
            size="mini"
            placeholder="本地过滤"
            clearable
            prefix-icon="el-icon-search"
            class="mb8"
          />
          <el-scrollbar class="table-scroll">
            <div
              v-for="name in displayTables"
              :key="name"
              class="table-item"
              :class="{ active: name === currentTable }"
              @click="selectTable(name)"
            >
              {{ name }}
            </div>
            <div v-if="!displayTables.length" class="empty-tip">暂无表</div>
          </el-scrollbar>
        </el-card>
      </el-col>

      <el-col :span="18">
        <el-card shadow="never" v-if="currentTable" :body-style="{ padding: '12px' }">
          <div slot="header" class="clearfix">
            <span>{{ currentTable }}</span>
            <el-tag v-if="!pkColumns.length" type="warning" size="mini" style="margin-left: 8px">无主键，仅可查询/新增</el-tag>
          </div>

          <el-row :gutter="10" class="mb8">
            <el-col :span="1.5">
              <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['datasource:data:add']">新增</el-button>
            </el-col>
            <el-col :span="1.5">
              <el-button icon="el-icon-refresh" size="mini" @click="getList">刷新</el-button>
            </el-col>
          </el-row>

          <el-table v-loading="loading" :data="dataList" border size="mini" max-height="520">
            <el-table-column
              v-for="col in columns"
              :key="col.name"
              :prop="col.name"
              :label="colLabel(col)"
              min-width="120"
              :show-overflow-tooltip="true"
            />
            <el-table-column label="操作" align="center" width="140" fixed="right">
              <template slot-scope="scope">
                <el-button
                  size="mini"
                  type="text"
                  icon="el-icon-edit"
                  :disabled="!pkColumns.length"
                  @click="handleUpdate(scope.row)"
                  v-hasPermi="['datasource:data:edit']"
                >修改</el-button>
                <el-button
                  size="mini"
                  type="text"
                  icon="el-icon-delete"
                  :disabled="!pkColumns.length"
                  @click="handleDelete(scope.row)"
                  v-hasPermi="['datasource:data:remove']"
                >删除</el-button>
              </template>
            </el-table-column>
          </el-table>

          <pagination
            v-show="total > 0"
            :total="total"
            :page.sync="queryParams.pageNum"
            :limit.sync="queryParams.pageSize"
            @pagination="getList"
          />
        </el-card>
        <el-empty v-else description="请选择左侧数据表" />
      </el-col>
    </el-row>
    <el-empty v-else description="请先选择数据源" />

    <el-dialog :title="title" :visible.sync="open" width="960px" append-to-body custom-class="ds-table-row-dialog">
      <el-form ref="form" :model="form" :label-width="formLabelWidth" size="small" class="ds-table-row-form">
        <el-form-item v-for="col in columns" :key="col.name" :label="colLabel(col)">
          <el-input
            v-model="form[col.name]"
            :disabled="isEdit && isPk(col.name)"
            :placeholder="col.primarykey ? '主键' : (col.allowNull === 0 ? '必填' : '可空')"
          />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="open = false">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listAllDatasource } from '@/api/datasource/info'
import { listTables, listColumns, listRows, addRow, updateRow, delRow } from '@/api/datasource/data'

export default {
  name: 'DatasourceTable',
  data() {
    return {
      datasourceId: undefined,
      datasourceOptions: [],
      tableFilter: '',
      tableFilterLocal: '',
      tables: [],
      currentTable: '',
      columns: [],
      pkColumns: [],
      loading: false,
      dataList: [],
      total: 0,
      queryParams: {
        pageNum: 1,
        pageSize: 10
      },
      open: false,
      isEdit: false,
      title: '',
      form: {},
      editPk: {}
    }
  },
  computed: {
    // dynamic label width to avoid wrap
    formLabelWidth() {
      let max = 8
      ;(this.columns || []).forEach(col => {
        const label = this.colLabel(col) || ""
        let w = 0
        for (const ch of label) {
          w += ch.charCodeAt(0) > 255 ? 2 : 1
        }
        if (w > max) max = w
      })
      const px = Math.min(420, Math.max(160, max * 8 + 24))
      return px + "px"
    },
    filteredTables() {
      return this.tables || []
    },
    displayTables() {
      const kw = (this.tableFilterLocal || '').trim().toLowerCase()
      if (!kw) return this.filteredTables
      return this.filteredTables.filter(t => String(t).toLowerCase().includes(kw))
    }
  },
  created() {
    this.loadDatasources()
  },
  methods: {
    loadDatasources() {
      listAllDatasource().then(res => {
        this.datasourceOptions = res.data || []
      })
    },
    onDatasourceChange() {
      this.currentTable = ''
      this.columns = []
      this.pkColumns = []
      this.dataList = []
      this.total = 0
      this.tables = []
      if (this.datasourceId) {
        this.loadTables()
      }
    },
    loadTables() {
      if (!this.datasourceId) return
      listTables({ datasourceId: this.datasourceId, tableName: this.tableFilter || undefined }).then(res => {
        this.tables = res.data || []
        if (this.currentTable && !this.tables.includes(this.currentTable)) {
          this.currentTable = ''
          this.dataList = []
        }
      })
    },
    selectTable(name) {
      this.currentTable = name
      this.queryParams.pageNum = 1
      this.loadColumns().then(() => this.getList())
    },
    loadColumns() {
      return listColumns({ datasourceId: this.datasourceId, tableName: this.currentTable }).then(res => {
        this.columns = res.data || []
        this.pkColumns = this.columns.filter(c => c.primarykey).map(c => c.name)
      })
    },
    getList() {
      if (!this.datasourceId || !this.currentTable) return
      this.loading = true
      listRows({
        datasourceId: this.datasourceId,
        tableName: this.currentTable,
        pageNum: this.queryParams.pageNum,
        pageSize: this.queryParams.pageSize
      }).then(res => {
        this.dataList = res.rows || []
        this.total = res.total || 0
        this.loading = false
      }).catch(() => {
        this.loading = false
      })
    },
    colLabel(col) {
      const tip = col.primarykey ? ' [PK]' : ''
      return (col.comment ? col.name + '(' + col.comment + ')' : col.name) + tip
    },
    isPk(name) {
      return this.pkColumns.indexOf(name) !== -1
    },
    resetFormModel() {
      const m = {}
      this.columns.forEach(c => {
        m[c.name] = undefined
      })
      this.form = m
    },
    handleAdd() {
      this.isEdit = false
      this.editPk = {}
      this.resetFormModel()
      this.open = true
      this.title = '新增 - ' + this.currentTable
    },
    handleUpdate(row) {
      this.isEdit = true
      this.resetFormModel()
      const pk = {}
      this.pkColumns.forEach(k => {
        pk[k] = row[k]
      })
      this.editPk = pk
      this.columns.forEach(c => {
        this.$set(this.form, c.name, row[c.name] == null ? undefined : String(row[c.name]))
      })
      this.open = true
      this.title = '修改 - ' + this.currentTable
    },
    submitForm() {
      const row = {}
      this.columns.forEach(c => {
        const v = this.form[c.name]
        if (v === undefined || v === '') {
          // 空串：新增时跳过让库默认值生效；修改时写 null
          if (this.isEdit) {
            row[c.name] = null
          }
        } else {
          row[c.name] = v
        }
      })
      const payload = {
        datasourceId: this.datasourceId,
        tableName: this.currentTable,
        row
      }
      if (this.isEdit) {
        payload.pk = this.editPk
        updateRow(payload).then(() => {
          this.$modal.msgSuccess('修改成功')
          this.open = false
          this.getList()
        })
      } else {
        // 新增去掉空字段
        Object.keys(row).forEach(k => {
          if (row[k] === undefined || row[k] === null || row[k] === '') delete row[k]
        })
        payload.row = row
        addRow(payload).then(() => {
          this.$modal.msgSuccess('新增成功')
          this.open = false
          this.getList()
        })
      }
    },
    handleDelete(row) {
      const pk = {}
      this.pkColumns.forEach(k => {
        pk[k] = row[k]
      })
      this.$modal.confirm('确认删除该行数据？').then(() => {
        return delRow({
          datasourceId: this.datasourceId,
          tableName: this.currentTable,
          pk
        })
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess('删除成功')
      }).catch(() => {})
    }
  }
}
</script>

<style scoped>
.ds-toolbar {
  margin-bottom: 8px;
}
.table-list-card {
  min-height: 560px;
}
.table-count {
  float: right;
  color: #909399;
  font-size: 12px;
}
.table-scroll {
  height: 480px;
}
.table-item {
  padding: 6px 10px;
  cursor: pointer;
  border-radius: 2px;
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.table-item:hover {
  background: #f5f7fa;
}
.table-item.active {
  background: #ecf5ff;
  color: #409eff;
}
.empty-tip {
  color: #909399;
  text-align: center;
  padding: 24px 0;
  font-size: 13px;
}
.mb8 {
  margin-bottom: 8px;
}
</style>
