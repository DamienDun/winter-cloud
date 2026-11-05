<template>
  <div class="app-container ds-table-page">
    <el-form size="small" :inline="true" label-width="80px" class="ds-toolbar">
      <el-form-item label="数据源">
        <el-select v-model="datasourceId" filterable clearable placeholder="请选择数据源" style="width: 280px" @change="onDatasourceChange">
          <el-option v-for="item in datasourceOptions" :key="item.id" :label="item.datasourceName + ' (' + item.datasourceType + ')'" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="表名过滤" v-if="workTab === 'table'">
        <el-input v-model="tableFilter" placeholder="关键字" clearable style="width: 180px" :disabled="!datasourceId" @keyup.enter.native="loadTables" />
      </el-form-item>
      <el-form-item v-if="workTab === 'table'">
        <el-button type="primary" icon="el-icon-search" size="mini" :disabled="!datasourceId" @click="loadTables">刷新表</el-button>
        <el-button type="success" plain icon="el-icon-plus" size="mini" :disabled="!datasourceId" @click="openCreateTable" v-hasPermi="['datasource:schema:add']">建表</el-button>
      </el-form-item>
    </el-form>

    <el-tabs v-model="workTab" v-if="datasourceId" class="ds-work-tabs">
      <el-tab-pane label="表数据" name="table">
        <el-row :gutter="12">
          <el-col :span="6">
            <el-card shadow="never" class="table-list-card" :body-style="{ padding: '8px' }">
              <div slot="header" class="clearfix">
                <span>数据表</span>
                <span class="table-count">{{ filteredTables.length }}</span>
              </div>
              <el-input v-model="tableFilterLocal" size="mini" placeholder="本地过滤" clearable prefix-icon="el-icon-search" class="mb8" />
              <el-scrollbar class="table-scroll">
                <div v-for="name in displayTables" :key="name" class="table-item" :class="{ active: name === currentTable }" @click="selectTable(name)">{{ name }}</div>
                <div v-if="!displayTables.length" class="empty-tip">暂无表</div>
              </el-scrollbar>
            </el-card>
          </el-col>

          <el-col :span="18">
            <el-card shadow="never" v-if="currentTable" :body-style="{ padding: '12px' }">
              <div slot="header" class="clearfix">
                <span>{{ currentTable }}</span>
                <el-tag v-if="!pkColumns.length" type="warning" size="mini" style="margin-left: 8px">无主键，仅可查询/新增</el-tag>
                <div style="float:right">
                  <el-button type="text" size="mini" icon="el-icon-s-grid" @click="openColumnMgr" v-hasPermi="['datasource:schema:edit']">字段</el-button>
                  <el-button type="text" size="mini" icon="el-icon-connection" @click="openIndexMgr" v-hasPermi="['datasource:schema:edit','datasource:schema:list']">索引</el-button>
                  <el-button type="text" size="mini" icon="el-icon-delete" style="color:#F56C6C" @click="openDropTable" v-hasPermi="['datasource:schema:remove']">删表</el-button>
                </div>
              </div>

              <el-row :gutter="10" class="mb8">
                <el-col :span="1.5">
                  <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['datasource:data:add']">新增</el-button>
                </el-col>
                <el-col :span="1.5">
                  <el-button icon="el-icon-refresh" size="mini" @click="getList">刷新</el-button>
                </el-col>
              </el-row>

              <el-table v-loading="loading" :data="dataList" border size="mini" max-height="480">
                <el-table-column v-for="col in columns" :key="col.name" :prop="col.name" :label="colLabel(col)" min-width="120" :show-overflow-tooltip="true" />
                <el-table-column label="操作" align="center" width="140" fixed="right">
                  <template slot-scope="scope">
                    <el-button size="mini" type="text" icon="el-icon-edit" :disabled="!pkColumns.length" @click="handleUpdate(scope.row)" v-hasPermi="['datasource:data:edit']">修改</el-button>
                    <el-button size="mini" type="text" icon="el-icon-delete" :disabled="!pkColumns.length" @click="handleDelete(scope.row)" v-hasPermi="['datasource:data:remove']">删除</el-button>
                  </template>
                </el-table-column>
              </el-table>
              <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />
            </el-card>
            <el-empty v-else description="请选择左侧数据表" />
          </el-col>
        </el-row>
      </el-tab-pane>

      <el-tab-pane label="SQL查询" name="sql" lazy>
        <sql-query-panel :datasource-id="datasourceId" :suggest-table="currentTable" />
      </el-tab-pane>
    </el-tabs>
    <el-empty v-else description="请先选择数据源" />

    <!-- 行编辑 -->
    <el-dialog :title="title" :visible.sync="open" width="960px" append-to-body custom-class="ds-table-row-dialog">
      <el-form ref="form" :model="form" :label-width="formLabelWidth" size="small" class="ds-table-row-form">
        <el-form-item v-for="col in columns" :key="col.name" :label="colLabel(col)">
          <el-input v-model="form[col.name]" :disabled="isEdit && isPk(col.name)" :placeholder="col.primarykey ? '主键' : (col.allowNull === 0 ? '必填' : '可空')" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="open = false">取 消</el-button>
      </div>
    </el-dialog>

    <!-- 建表 -->
    <el-dialog title="新建表" :visible.sync="createOpen" width="900px" append-to-body>
      <el-form :model="createForm" label-width="90px" size="small">
        <el-form-item label="表名" required>
          <el-input v-model="createForm.tableName" placeholder="仅字母数字下划线" />
        </el-form-item>
        <el-form-item label="表注释">
          <el-input v-model="createForm.comment" />
        </el-form-item>
      </el-form>
      <el-button type="primary" plain size="mini" icon="el-icon-plus" class="mb8" @click="addCreateCol">添加列</el-button>
      <el-table :data="createForm.columns" border size="mini">
        <el-table-column label="列名" min-width="120"><template slot-scope="scope"><el-input v-model="scope.row.name" size="mini" /></template></el-table-column>
        <el-table-column label="类型" min-width="130"><template slot-scope="scope"><el-input v-model="scope.row.type" size="mini" placeholder="varchar(64)" /></template></el-table-column>
        <el-table-column label="可空" width="70" align="center"><template slot-scope="scope"><el-checkbox v-model="scope.row.nullable" /></template></el-table-column>
        <el-table-column label="主键" width="70" align="center"><template slot-scope="scope"><el-checkbox v-model="scope.row.primaryKey" /></template></el-table-column>
        <el-table-column label="默认值" min-width="100"><template slot-scope="scope"><el-input v-model="scope.row.defaultValue" size="mini" /></template></el-table-column>
        <el-table-column label="注释" min-width="120"><template slot-scope="scope"><el-input v-model="scope.row.comment" size="mini" /></template></el-table-column>
        <el-table-column label="操作" width="70" align="center"><template slot-scope="scope"><el-button type="text" size="mini" @click="createForm.columns.splice(scope.$index,1)">删</el-button></template></el-table-column>
      </el-table>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitCreateTable">创建</el-button>
        <el-button @click="createOpen=false">取消</el-button>
      </div>
    </el-dialog>

    <!-- 删表二次确认 -->
    <el-dialog title="删除表（危险操作）" :visible.sync="dropOpen" width="480px" append-to-body>
      <el-alert type="error" :closable="false" show-icon title="删除后数据不可恢复，请谨慎操作。" class="mb8" />
      <p>将删除表：<b>{{ currentTable }}</b></p>
      <p>请再次输入表名以确认：</p>
      <el-input v-model="dropConfirmName" placeholder="输入完整表名" />
      <div slot="footer" class="dialog-footer">
        <el-button type="danger" :disabled="dropConfirmName !== currentTable" @click="submitDropTable">确认删除</el-button>
        <el-button @click="dropOpen=false">取消</el-button>
      </div>
    </el-dialog>

    <!-- 字段管理 -->
    <el-dialog :title="'字段管理 - ' + currentTable" :visible.sync="columnOpen" width="960px" append-to-body>
      <el-row :gutter="10" class="mb8">
        <el-col :span="1.5"><el-button type="primary" plain size="mini" icon="el-icon-plus" @click="openAddColumn">新增字段</el-button></el-col>
        <el-col :span="1.5"><el-button size="mini" icon="el-icon-refresh" @click="reloadColumns">刷新</el-button></el-col>
      </el-row>
      <el-table :data="columns" border size="mini" max-height="420">
        <el-table-column prop="name" label="列名" min-width="120" />
        <el-table-column prop="type" label="类型" min-width="100" />
        <el-table-column label="可空" width="70" align="center"><template slot-scope="scope">{{ scope.row.allowNull === 0 ? '否' : '是' }}</template></el-table-column>
        <el-table-column label="主键" width="70" align="center"><template slot-scope="scope">{{ scope.row.primarykey ? '是' : '' }}</template></el-table-column>
        <el-table-column prop="comment" label="注释" min-width="140" :show-overflow-tooltip="true" />
        <el-table-column label="操作" width="160" align="center">
          <template slot-scope="scope">
            <el-button type="text" size="mini" @click="openModifyColumn(scope.row)">修改</el-button>
            <el-button type="text" size="mini" style="color:#F56C6C" @click="dropColumn(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <el-dialog :title="colDlgTitle" :visible.sync="colDlgOpen" width="520px" append-to-body>
      <el-form :model="colForm" label-width="90px" size="small">
        <el-form-item label="列名" required><el-input v-model="colForm.name" :disabled="colDlgAction==='MODIFY'" /></el-form-item>
        <el-form-item label="类型" required><el-input v-model="colForm.type" placeholder="varchar(64) / bigint / datetime" /></el-form-item>
        <el-form-item label="可空"><el-switch v-model="colForm.nullable" /></el-form-item>
        <el-form-item label="默认值"><el-input v-model="colForm.defaultValue" /></el-form-item>
        <el-form-item label="注释"><el-input v-model="colForm.comment" /></el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitColumn">确定</el-button>
        <el-button @click="colDlgOpen=false">取消</el-button>
      </div>
    </el-dialog>

    <!-- 索引管理 -->
    <el-dialog :title="'索引管理 - ' + currentTable" :visible.sync="indexOpen" width="900px" append-to-body>
      <el-row :gutter="10" class="mb8">
        <el-col :span="1.5"><el-button type="primary" plain size="mini" icon="el-icon-plus" :disabled="!(ddlCapabilities.indexTypes || []).length" @click="openCreateIndex">添加索引</el-button></el-col>
        <el-col :span="8" v-if="!(ddlCapabilities.indexTypes || []).length"><span style="color:#909399;font-size:12px;line-height:28px">当前方言不支持二级索引（Hive/Presto）</span></el-col>
        <el-col :span="1.5"><el-button size="mini" icon="el-icon-refresh" @click="loadIndexes">刷新</el-button></el-col>
      </el-row>
      <el-table :data="indexList" border size="mini" v-loading="indexLoading">
        <el-table-column prop="indexName" label="名" min-width="120" />
        <el-table-column label="字段" min-width="160"><template slot-scope="scope">{{ (scope.row.columns || []).join(', ') }}</template></el-table-column>
        <el-table-column prop="indexType" label="索引类型" width="110" align="center" />
        <el-table-column prop="indexMethod" label="索引方法" width="100" align="center" />
        <el-table-column prop="comment" label="注释" min-width="140" :show-overflow-tooltip="true" />
        <el-table-column label="操作" width="140" align="center">
          <template slot-scope="scope">
            <el-button type="text" size="mini" :disabled="scope.row.indexName==='PRIMARY' || scope.row.indexType==='PRIMARY'" @click="openEditIndex(scope.row)">编辑</el-button>
            <el-button type="text" size="mini" style="color:#F56C6C" :disabled="scope.row.indexName==='PRIMARY' || scope.row.indexType==='PRIMARY'" @click="removeIndex(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <el-dialog :title="indexDlgTitle" :visible.sync="indexDlgOpen" width="560px" append-to-body>
      <el-form :model="indexForm" label-width="100px" size="small">
        <el-form-item label="名" required>
          <el-input v-model="indexForm.indexName" placeholder="索引名" />
        </el-form-item>
        <el-form-item label="字段" required>
          <el-select v-model="indexForm.columns" multiple filterable style="width:100%" placeholder="选择字段（可多选，顺序即索引列顺序）">
            <el-option v-for="c in columns" :key="c.name" :label="c.name" :value="c.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="索引类型" required>
          <el-select v-model="indexForm.indexType" style="width:100%" @change="onIndexTypeChange">
            <el-option v-for="tp in (ddlCapabilities.indexTypes || [])" :key="tp" :label="tp" :value="tp" />
          </el-select>
        </el-form-item>
        <el-form-item label="索引方法" v-if="(ddlCapabilities.indexMethods || []).length">
          <el-select v-model="indexForm.indexMethod" style="width:100%" :disabled="indexMethodDisabled" clearable placeholder="选择方法">
            <el-option v-for="m in (ddlCapabilities.indexMethods || [])" :key="m" :label="m" :value="m" />
          </el-select>
        </el-form-item>
        <el-form-item label="注释" v-if="ddlCapabilities.supportsIndexComment">
          <el-input v-model="indexForm.comment" type="textarea" :rows="2" placeholder="索引注释（当前方言支持）" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitIndex">确定</el-button>
        <el-button @click="indexDlgOpen=false">取消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listAllDatasource } from '@/api/datasource/info'
import { listTables, listColumns, listRows, addRow, updateRow, delRow, createTable, dropTable, alterColumn, listIndexes, schemaCapabilities, createIndex, updateIndex, dropIndex } from '@/api/datasource/data'
import SqlQueryPanel from './SqlQueryPanel.vue'

export default {
  name: 'DatasourceTable',
  components: { SqlQueryPanel },
  data() {
    return {
      workTab: 'table',
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
      queryParams: { pageNum: 1, pageSize: 10 },
      open: false,
      isEdit: false,
      title: '',
      form: {},
      editPk: {},
      createOpen: false,
      createForm: { tableName: '', comment: '', columns: [] },
      dropOpen: false,
      dropConfirmName: '',
      columnOpen: false,
      colDlgOpen: false,
      colDlgAction: 'ADD',
      colDlgTitle: '',
      colForm: { name: '', type: 'varchar(64)', nullable: true, defaultValue: '', comment: '' },
      indexOpen: false,
      indexLoading: false,
      indexList: [],
      indexDlgOpen: false,
      indexDlgTitle: '添加索引',
      indexEditing: false,
      indexForm: { indexName: '', oldIndexName: '', indexType: 'NORMAL', indexMethod: 'BTREE', comment: '', columns: [] },
      ddlCapabilities: { dialect: '', indexTypes: ['NORMAL','UNIQUE'], indexMethods: ['BTREE'], supportsIndexComment: false }
    }
  },
  computed: {
    formLabelWidth() {
      let max = 8
      ;(this.columns || []).forEach(col => {
        const label = this.colLabel(col) || ''
        let w = 0
        for (const ch of label) w += ch.charCodeAt(0) > 255 ? 2 : 1
        if (w > max) max = w
      })
      return Math.min(420, Math.max(160, max * 8 + 24)) + 'px'
    },
    filteredTables() { return this.tables || [] },
    displayTables() {
      const kw = (this.tableFilterLocal || '').trim().toLowerCase()
      if (!kw) return this.filteredTables
      return this.filteredTables.filter(t => String(t).toLowerCase().includes(kw))
    }
  },
  created() { this.loadDatasources() },
  methods: {
    loadDatasources() {
      listAllDatasource().then(res => { this.datasourceOptions = res.data || [] })
    },
    onDatasourceChange() {
      this.currentTable = ''
      this.columns = []
      this.pkColumns = []
      this.dataList = []
      this.total = 0
      this.tables = []
      this.ddlCapabilities = { dialect: '', indexTypes: ['NORMAL','UNIQUE'], indexMethods: ['BTREE'], supportsIndexComment: false }
      if (this.datasourceId) {
        this.loadTables()
        this.loadCapabilities()
      }
    },
    loadCapabilities() {
      if (!this.datasourceId) return
      schemaCapabilities({ datasourceId: this.datasourceId }).then(res => {
        this.ddlCapabilities = Object.assign({
          dialect: '',
          indexTypes: ['NORMAL', 'UNIQUE'],
          indexMethods: ['BTREE'],
          supportsIndexComment: false
        }, res.data || {})
      }).catch(() => {})
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
    reloadColumns() {
      this.loadColumns().then(() => this.$modal.msgSuccess('已刷新'))
    },
    getList() {
      if (!this.datasourceId || !this.currentTable) return
      this.loading = true
      listRows({ datasourceId: this.datasourceId, tableName: this.currentTable, pageNum: this.queryParams.pageNum, pageSize: this.queryParams.pageSize }).then(res => {
        this.dataList = res.rows || []
        this.total = res.total || 0
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    colLabel(col) {
      const tip = col.primarykey ? ' [PK]' : ''
      return (col.comment ? col.name + '(' + col.comment + ')' : col.name) + tip
    },
    isPk(name) { return this.pkColumns.indexOf(name) !== -1 },
    resetFormModel() {
      const m = {}
      this.columns.forEach(c => { m[c.name] = undefined })
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
      this.pkColumns.forEach(k => { pk[k] = row[k] })
      this.editPk = pk
      this.columns.forEach(c => { this.$set(this.form, c.name, row[c.name] == null ? undefined : String(row[c.name])) })
      this.open = true
      this.title = '修改 - ' + this.currentTable
    },
    submitForm() {
      const row = {}
      this.columns.forEach(c => {
        const v = this.form[c.name]
        if (v === undefined || v === '') {
          if (this.isEdit) row[c.name] = null
        } else {
          row[c.name] = v
        }
      })
      const payload = { datasourceId: this.datasourceId, tableName: this.currentTable, row }
      if (this.isEdit) {
        payload.pk = this.editPk
        updateRow(payload).then(() => { this.$modal.msgSuccess('修改成功'); this.open = false; this.getList() })
      } else {
        Object.keys(row).forEach(k => { if (row[k] === undefined || row[k] === null || row[k] === '') delete row[k] })
        payload.row = row
        addRow(payload).then(() => { this.$modal.msgSuccess('新增成功'); this.open = false; this.getList() })
      }
    },
    handleDelete(row) {
      const pk = {}
      this.pkColumns.forEach(k => { pk[k] = row[k] })
      this.$modal.confirm('确认删除该行数据？').then(() => delRow({ datasourceId: this.datasourceId, tableName: this.currentTable, pk })).then(() => {
        this.getList(); this.$modal.msgSuccess('删除成功')
      }).catch(() => {})
    },
    openCreateTable() {
      this.createForm = {
        tableName: '',
        comment: '',
        columns: [
          { name: 'id', type: 'bigint', nullable: false, primaryKey: true, defaultValue: '', comment: '主键' },
          { name: 'name', type: 'varchar(64)', nullable: true, primaryKey: false, defaultValue: '', comment: '' }
        ]
      }
      this.createOpen = true
    },
    addCreateCol() {
      this.createForm.columns.push({ name: '', type: 'varchar(64)', nullable: true, primaryKey: false, defaultValue: '', comment: '' })
    },
    submitCreateTable() {
      if (!this.createForm.tableName) return this.$modal.msgWarning('请填写表名')
      if (!this.createForm.columns.length) return this.$modal.msgWarning('请至少添加一列')
      createTable({
        datasourceId: this.datasourceId,
        tableName: this.createForm.tableName,
        comment: this.createForm.comment,
        columns: this.createForm.columns
      }).then(() => {
        this.$modal.msgSuccess('建表成功')
        this.createOpen = false
        this.loadTables()
        this.selectTable(this.createForm.tableName)
      })
    },
    openDropTable() {
      this.dropConfirmName = ''
      this.dropOpen = true
    },
    submitDropTable() {
      if (this.dropConfirmName !== this.currentTable) return
      this.$modal.confirm('最终确认：删除表 ' + this.currentTable + ' ？此操作不可恢复。').then(() => {
        return dropTable({
          datasourceId: this.datasourceId,
          tableName: this.currentTable,
          confirmTableName: this.dropConfirmName
        })
      }).then(() => {
        this.$modal.msgSuccess('已删除')
        this.dropOpen = false
        this.currentTable = ''
        this.columns = []
        this.dataList = []
        this.loadTables()
      }).catch(() => {})
    },
    openColumnMgr() { this.columnOpen = true },
    openAddColumn() {
      this.colDlgAction = 'ADD'
      this.colDlgTitle = '新增字段'
      this.colForm = { name: '', type: 'varchar(64)', nullable: true, defaultValue: '', comment: '' }
      this.colDlgOpen = true
    },
    openModifyColumn(row) {
      this.colDlgAction = 'MODIFY'
      this.colDlgTitle = '修改字段 - ' + row.name
      this.colForm = {
        name: row.name,
        type: (row.type || 'varchar(64)').toLowerCase(),
        nullable: row.allowNull !== 0,
        defaultValue: '',
        comment: row.comment || ''
      }
      this.colDlgOpen = true
    },
    submitColumn() {
      if (!this.colForm.name || !this.colForm.type) return this.$modal.msgWarning('列名和类型必填')
      alterColumn({
        datasourceId: this.datasourceId,
        tableName: this.currentTable,
        action: this.colDlgAction,
        column: {
          name: this.colForm.name,
          type: this.colForm.type,
          nullable: this.colForm.nullable,
          defaultValue: this.colForm.defaultValue,
          comment: this.colForm.comment
        }
      }).then(() => {
        this.$modal.msgSuccess('操作成功')
        this.colDlgOpen = false
        this.loadColumns().then(() => this.getList())
      })
    },
    dropColumn(row) {
      this.$modal.confirm('确认删除字段 ' + row.name + ' ？').then(() => this.$modal.confirm('二次确认：删除字段 ' + row.name + ' 后不可恢复，继续？')).then(() => {
        return alterColumn({
          datasourceId: this.datasourceId,
          tableName: this.currentTable,
          action: 'DROP',
          column: { name: row.name }
        })
      }).then(() => {
        this.$modal.msgSuccess('字段已删除')
        this.loadColumns().then(() => this.getList())
      }).catch(() => {})
    },
    openIndexMgr() {
      this.indexOpen = true
      this.loadCapabilities()
      this.loadIndexes()
    },
    loadIndexes() {
      this.indexLoading = true
      listIndexes({ datasourceId: this.datasourceId, tableName: this.currentTable }).then(res => {
        this.indexList = res.data || []
        this.indexLoading = false
      }).catch(() => { this.indexLoading = false })
    },
    openCreateIndex() {
      const types = this.ddlCapabilities.indexTypes || []
      if (!types.length) {
        return this.$modal.msgWarning('当前数据源方言不支持索引管理')
      }
      this.indexEditing = false
      this.indexDlgTitle = '添加索引' + (this.ddlCapabilities.dialect ? ' (' + this.ddlCapabilities.dialect + ')' : '')
      const methods = this.ddlCapabilities.indexMethods || []
      this.indexForm = {
        indexName: '',
        oldIndexName: '',
        indexType: types[0],
        indexMethod: methods[0] || '',
        comment: '',
        columns: []
      }
      this.indexDlgOpen = true
    },
    openEditIndex(row) {
      this.indexEditing = true
      this.indexDlgTitle = '编辑索引'
      this.indexForm = {
        indexName: row.indexName,
        oldIndexName: row.indexName,
        indexType: row.indexType === 'PRIMARY' ? 'UNIQUE' : (row.indexType || 'NORMAL'),
        indexMethod: row.indexMethod || 'BTREE',
        comment: row.comment || '',
        columns: [...(row.columns || [])]
      }
      this.onIndexTypeChange(this.indexForm.indexType)
      this.indexDlgOpen = true
    },
    onIndexTypeChange(tp) {
      if (tp === 'FULLTEXT' || tp === 'SPATIAL') {
        this.indexForm.indexMethod = ''
      } else if (!this.indexForm.indexMethod) {
        this.indexForm.indexMethod = 'BTREE'
      }
    },
    submitIndex() {
      if (!this.indexForm.indexName || !this.indexForm.columns.length) return this.$modal.msgWarning('请填写索引名并选择字段')
      const payload = {
        datasourceId: this.datasourceId,
        tableName: this.currentTable,
        indexName: this.indexForm.indexName,
        oldIndexName: this.indexForm.oldIndexName,
        indexType: this.indexForm.indexType,
        indexMethod: this.indexForm.indexMethod,
        comment: this.indexForm.comment,
        columns: this.indexForm.columns
      }
      const req = this.indexEditing ? updateIndex(payload) : createIndex(payload)
      req.then(() => {
        this.$modal.msgSuccess(this.indexEditing ? '索引已更新' : '索引已创建')
        this.indexDlgOpen = false
        this.loadIndexes()
      })
    },
    removeIndex(row) {
      this.$modal.confirm('确认删除索引 ' + row.indexName + ' ？').then(() => {
        return dropIndex({
          datasourceId: this.datasourceId,
          tableName: this.currentTable,
          indexName: row.indexName
        })
      }).then(() => {
        this.$modal.msgSuccess('索引已删除')
        this.loadIndexes()
      }).catch(() => {})
    }
  }
}
</script>

<style scoped>
.ds-toolbar { margin-bottom: 8px; }
.ds-work-tabs { margin-top: 4px; }
.ds-table-row-form >>> .el-form-item__label { white-space: nowrap; line-height: 32px; }
.table-list-card { min-height: 560px; }
.table-count { float: right; color: #909399; font-size: 12px; }
.table-scroll { height: 480px; }
.table-item { padding: 6px 10px; cursor: pointer; border-radius: 2px; font-size: 13px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.table-item:hover { background: #f5f7fa; }
.table-item.active { background: #ecf5ff; color: #409eff; }
.empty-tip { color: #909399; text-align: center; padding: 24px 0; font-size: 13px; }
.mb8 { margin-bottom: 8px; }
</style>
