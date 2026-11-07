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
      <el-table-column label="源类型" align="center" prop="datasourceType" width="90" />
      <el-table-column label="源数据源" align="center" prop="datasourceName" min-width="110" :show-overflow-tooltip="true" />
      <el-table-column label="目标数据源" align="center" prop="targetDatasourceName" min-width="110" :show-overflow-tooltip="true" />
      <el-table-column label="目标表" align="center" prop="targetTable" min-width="110" :show-overflow-tooltip="true" />
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

    <el-dialog :title="title" :visible.sync="open" width="1100px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="110px" size="small">
        <el-form-item label="作业名称" prop="jobName">
          <el-input v-model="form.jobName" placeholder="请输入作业名称" />
        </el-form-item>
        <el-divider content-position="left">源配置</el-divider>
        <el-form-item label="数据源类型" prop="datasourceType">
          <el-select v-model="form.datasourceType" placeholder="请先选择数据源类型" style="width: 100%" @change="onDatasourceTypeChange">
            <el-option v-for="t in typeOptions" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="源数据源" prop="datasourceId">
          <el-select
            v-model="form.datasourceId"
            placeholder="请选择源数据源"
            filterable
            style="width: 100%"
            :disabled="!form.datasourceType"
            :loading="dsLoading"
            @change="onSourceDatasourceChange"
          >
            <el-option
              v-for="ds in datasourceOptions"
              :key="String(ds.id)"
              :label="ds.datasourceName"
              :value="String(ds.id)"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="源表" prop="sourceTable" v-if="form.datasourceType && form.datasourceType !== 'api'">
          <el-select
            v-model="form.sourceTable"
            placeholder="请选择源表"
            filterable
            style="width: 100%"
            :disabled="!form.datasourceId"
            :loading="sourceTableLoading"
            @change="onSourceTableChange"
          >
            <el-option v-for="t in sourceTableOptions" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-divider content-position="left">目标配置</el-divider>
        <el-form-item label="目标数据源" prop="targetDatasourceId">
          <el-select
            v-model="form.targetDatasourceId"
            placeholder="请选择目标JDBC数据源"
            filterable
            style="width: 100%"
            @change="onTargetDatasourceChange"
          >
            <el-option
              v-for="ds in jdbcAllOptions"
              :key="'t-' + String(ds.id)"
              :label="ds.datasourceName + ' (' + ds.datasourceType + ')'"
              :value="String(ds.id)"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="目标数据表" prop="targetTable">
          <div style="display: flex; width: 100%; gap: 8px;">
            <el-select
              v-model="form.targetTable"
              placeholder="选择已有表，或输入新表名后回车"
              filterable
              allow-create
              default-first-option
              clearable
              style="flex: 1"
              :disabled="!form.targetDatasourceId"
              :loading="targetTableLoading"
              @change="onTargetTableChange"
            >
              <el-option v-for="t in targetTableSelectOptions" :key="t" :label="t" :value="t" />
            </el-select>
            <el-button
              size="mini"
              :disabled="!form.targetDatasourceId || !form.sourceTable"
              @click="useSourceTableName"
            >同源表名</el-button>
          </div>
        </el-form-item>
        <el-form-item label="字段映射" prop="fieldMappings">
          <div style="width: 100%">
            <el-button type="text" icon="el-icon-plus" @click="addMappingRow">添加映射</el-button>
            <el-button type="text" icon="el-icon-magic-stick" :disabled="!canAutoMap" @click="autoMapFields">同名自动映射</el-button>
            <el-button type="text" icon="el-icon-copy-document" :disabled="!sourceFieldOptions.length" @click="mapBySourceFieldNames">同源字段名</el-button>
            <el-button type="text" icon="el-icon-delete" :disabled="!(form.fieldMappings && form.fieldMappings.length)" @click="clearFieldMappings">一键清空</el-button>
            <div style="color: #909399; font-size: 12px; margin: 0 0 6px;">
              可拖拽「拖拽」列调整顺序（即建表字段顺序）；类型空时 API 默认 varchar(255)、JDBC 沿用源类型；注释空则沿用源注释。改成可能不兼容的类型会提示。
            </div>
            <el-table ref="mappingTable" :data="form.fieldMappings" size="mini" border empty-text="请添加字段映射" row-key="_rowKey">
              <el-table-column label="拖拽" width="56" align="center">
                <template slot-scope="scope">
                  <i class="el-icon-rank mapping-drag-handle" title="拖拽调整顺序" />
                </template>
              </el-table-column>
              <el-table-column label="源字段" min-width="150">
                <template slot-scope="scope">
                  <el-select
                    v-model="scope.row.sourceField"
                    filterable
                    clearable
                    placeholder="源字段"
                    style="width: 100%"
                    @change="onSourceFieldPicked(scope.row)"
                  >
                    <el-option v-for="f in sourceFieldOptions" :key="'s-' + f" :label="f" :value="f" />
                  </el-select>
                </template>
              </el-table-column>
              <el-table-column label="目标字段" min-width="130">
                <template slot-scope="scope">
                  <el-autocomplete
                    v-model="scope.row.targetField"
                    :fetch-suggestions="(q, cb) => queryTargetFieldSuggestions(q, cb, scope.row)"
                    clearable
                    placeholder="目标字段名"
                    style="width: 100%"
                  />
                </template>
              </el-table-column>
              <el-table-column label="目标类型" min-width="140">
                <template slot-scope="scope">
                  <el-autocomplete
                    v-model="scope.row.targetType"
                    :fetch-suggestions="queryTypeSuggestions"
                    clearable
                    :placeholder="isApiSource ? '默认varchar(255)' : '默认沿用源类型'"
                    style="width: 100%"
                    @change="onTargetTypeChange(scope.row)"
                    @select="() => onTargetTypeChange(scope.row)"
                  />
                </template>
              </el-table-column>
              <el-table-column label="字段注释" min-width="140">
                <template slot-scope="scope">
                  <el-input v-model="scope.row.targetComment" clearable :placeholder="isApiSource ? '可选' : '默认沿用源注释'" />
                </template>
              </el-table-column>
              <el-table-column width="50" align="center">
                <template slot-scope="scope">
                  <el-button type="text" icon="el-icon-delete" @click="form.fieldMappings.splice(scope.$index, 1)" />
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-form-item>
        <el-divider content-position="left">调度配置</el-divider>
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
import { listApiDatasource, listApiOutputFields } from '@/api/datasource/api'
import { listTables, listColumns } from '@/api/datasource/data'
import XxlCronGen from '@/views/xxljob/components/XxlCronGen'
import Sortable from 'sortablejs'

export default {
  name: 'DatasourceJob',
  components: { XxlCronGen },
  data() {
    return {
      mappingSortable: null,
      mappingRowKeySeq: 1,
      loading: false,
      dsLoading: false,
      sourceTableLoading: false,
      targetTableLoading: false,
      showSearch: true,
      total: 0,
      dataList: [],
      ids: [],
      single: true,
      multiple: true,
      open: false,
      title: '',
      // 修改回显加载中：忽略 select 的 change，避免清空已回显值并重复打列接口报错
      formHydrating: false,
      jdbcAllOptions: [],
      datasourceOptions: [],
      sourceTableOptions: [],
      targetTableOptions: [],
      sourceFieldOptions: [],
      /** JDBC 源字段元数据：{ name: { type, comment } } */
      sourceFieldMetaMap: {},
      targetFieldOptions: [],
      commonSqlTypes: [
        'varchar(64)', 'varchar(128)', 'varchar(255)', 'varchar(512)',
        'text', 'bigint', 'int', 'tinyint(1)', 'decimal(20,6)',
        'datetime', 'date', 'json'
      ],
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
        datasourceId: [{ required: true, message: '请选择源数据源', trigger: 'change' }],
        targetDatasourceId: [{ required: true, message: '请选择目标数据源', trigger: 'change' }],
        targetTable: [{ required: true, message: '请选择或填写目标数据表', trigger: 'change' }],
        syncType: [{ required: true, message: '请选择同步方式', trigger: 'change' }],
        triggerType: [{ required: true, message: '请选择触发方式', trigger: 'change' }],
        cron: [{ required: true, message: '请填写Cron', trigger: 'blur' }]
      }
    }
  },
  computed: {
    isApiSource() {
      return this.form && this.form.datasourceType === 'api'
    },
    canAutoMap() {
      // 有源字段即可：目标有列则做交集；无列则按源字段同名生成
      return this.sourceFieldOptions.length > 0
    },
    /** 已有表 + 当前自定义表名（编辑回显/新建表时保证下拉能显示值） */
    targetTableSelectOptions() {
      const set = new Set(this.targetTableOptions || [])
      if (this.form && this.form.targetTable) {
        set.add(this.form.targetTable)
      }
      if (this.form && this.form.sourceTable) {
        set.add(this.form.sourceTable)
      }
      return Array.from(set)
    },
    /**
     * 目标字段候选：库表已有列 + 当前映射已填值
     * + 全部源字段（含 data.xxx）及其末段，清空后仍可选回原来的 data.chiefNo
     */
    targetFieldSelectOptions() {
      const set = new Set(this.targetFieldOptions || [])
      ;(this.form.fieldMappings || []).forEach(m => {
        if (m && m.targetField) {
          set.add(m.targetField)
        }
        if (m && m.sourceField) {
          set.add(m.sourceField)
          const leaf = this.toLeafFieldName(m.sourceField)
          if (leaf) set.add(leaf)
        }
      })
      ;(this.sourceFieldOptions || []).forEach(f => {
        if (!f) return
        set.add(f)
        const leaf = this.toLeafFieldName(f)
        if (leaf) set.add(leaf)
      })
      return Array.from(set)
    }
  },
  created() {
    this.getList()
    this.loadJdbcAll()
  },
  watch: {
    open(val) {
      if (val) {
        this.$nextTick(() => this.initMappingSortable())
      } else {
        this.destroyMappingSortable()
      }
    }
  },
  beforeDestroy() {
    this.destroyMappingSortable()
  },
  methods: {
    initMappingSortable() {
      this.destroyMappingSortable()
      const table = this.$refs.mappingTable
      if (!table || !table.$el) return
      const el = table.$el.querySelector('.el-table__body-wrapper > table > tbody')
      if (!el) return
      this.mappingSortable = Sortable.create(el, {
        handle: '.mapping-drag-handle',
        animation: 150,
        ghostClass: 'mapping-row-ghost',
        onEnd: evt => {
          const oldIndex = evt.oldIndex
          const newIndex = evt.newIndex
          if (oldIndex == null || newIndex == null || oldIndex === newIndex) return
          const list = this.form.fieldMappings
          if (!list || !list.length) return
          const row = list.splice(oldIndex, 1)[0]
          list.splice(newIndex, 0, row)
        }
      })
    },
    destroyMappingSortable() {
      if (this.mappingSortable) {
        this.mappingSortable.destroy()
        this.mappingSortable = null
      }
    },
    ensureMappingRowKey(row) {
      if (row && row._rowKey == null) {
        this.$set(row, '_rowKey', 'm-' + (this.mappingRowKeySeq++))
      }
      return row
    },
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
      if (this.formHydrating) return
      this.form.datasourceId = undefined
      this.form.sourceTable = undefined
      this.sourceTableOptions = []
      this.sourceFieldOptions = []
      this.sourceFieldMetaMap = {}
      this.form.fieldMappings = []
      this.loadDatasourcesByType(type)
    },
    onSourceDatasourceChange(id) {
      if (this.formHydrating) return
      this.form.sourceTable = undefined
      this.sourceTableOptions = []
      this.sourceFieldOptions = []
      this.sourceFieldMetaMap = {}
      this.form.fieldMappings = []
      if (!id) return
      if (this.form.datasourceType === 'api') {
        this.loadApiSourceFields(id)
      } else {
        this.loadSourceTables(id)
      }
    },
    onSourceTableChange(table) {
      if (this.formHydrating) return
      this.sourceFieldOptions = []
      this.sourceFieldMetaMap = {}
      this.form.fieldMappings = []
      if (table && this.form.datasourceId) {
        this.loadJdbcSourceFields(this.form.datasourceId, table)
      }
    },
    onTargetDatasourceChange(id) {
      if (this.formHydrating) return
      this.form.targetTable = undefined
      this.targetTableOptions = []
      this.targetFieldOptions = []
      this.form.fieldMappings = []
      if (id) {
        this.loadTargetTables(id)
      }
    },
    onTargetTableChange(table) {
      if (this.formHydrating) return
      this.targetFieldOptions = []
      // 切换表时清空映射，避免旧目标字段残留误导；自定义新表会得到空列列表，再允许手输字段名
      this.form.fieldMappings = []
      if (table && this.form.targetDatasourceId) {
        this.loadTargetFields(this.form.targetDatasourceId, table)
      }
    },
    /** 目标表名直接使用源表名（新建表场景常用） */
    useSourceTableName() {
      if (!this.form.sourceTable) {
        this.$modal.msgWarning('请先选择源表（API 源无源表，请手动填写目标表名）')
        return
      }
      if (!this.form.targetDatasourceId) {
        this.$modal.msgWarning('请先选择目标数据源')
        return
      }
      this.$set(this.form, 'targetTable', this.form.sourceTable)
      this.onTargetTableChange(this.form.sourceTable)
    },
    /** 目标库中是否已存在该表（忽略大小写） */
    isExistingTargetTable(tableName) {
      if (!tableName) return false
      const name = String(tableName).toLowerCase()
      return (this.targetTableOptions || []).some(t => t && String(t).toLowerCase() === name)
    },
    loadSourceTables(datasourceId) {
      this.sourceTableLoading = true
      return listTables({ datasourceId }).then(res => {
        this.sourceTableOptions = res.data || []
        return this.sourceTableOptions
      }).catch(() => {
        this.sourceTableOptions = []
        return []
      }).finally(() => { this.sourceTableLoading = false })
    },
    loadTargetTables(datasourceId) {
      this.targetTableLoading = true
      return listTables({ datasourceId }).then(res => {
        this.targetTableOptions = res.data || []
        return this.targetTableOptions
      }).catch(() => {
        this.targetTableOptions = []
        return []
      }).finally(() => { this.targetTableLoading = false })
    },
    loadJdbcSourceFields(datasourceId, tableName) {
      return listColumns({ datasourceId, tableName }).then(res => {
        const metaMap = {}
        const names = []
        ;(res.data || []).forEach(c => {
          if (!c || !c.name) return
          names.push(c.name)
          metaMap[c.name] = {
            type: this.normalizeColumnType(c.type),
            comment: c.comment || ''
          }
          metaMap[String(c.name).toLowerCase()] = metaMap[c.name]
        })
        this.sourceFieldMetaMap = metaMap
        this.sourceFieldOptions = names
        return this.sourceFieldOptions
      }).catch(() => {
        this.sourceFieldOptions = []
        this.sourceFieldMetaMap = {}
        return []
      })
    },
    loadTargetFields(datasourceId, tableName) {
      // 自定义新表尚未落库：跳过 columns 接口，避免 assertTableExists 失败弹错
      if (!datasourceId || !tableName || !this.isExistingTargetTable(tableName)) {
        this.targetFieldOptions = []
        return Promise.resolve([])
      }
      return listColumns({ datasourceId, tableName }).then(res => {
        this.targetFieldOptions = (res.data || []).map(c => c.name).filter(Boolean)
        return this.targetFieldOptions
      }).catch(() => {
        this.targetFieldOptions = []
        return []
      })
    },
    loadApiSourceFields(datasourceId) {
      return listApiOutputFields(datasourceId).then(res => {
        this.sourceFieldMetaMap = {}
        this.sourceFieldOptions = (res.data || []).map(f => f.fieldName).filter(Boolean)
        return this.sourceFieldOptions
      }).catch(() => {
        this.sourceFieldOptions = []
        this.sourceFieldMetaMap = {}
        this.$modal.msgWarning('加载API出参失败，请确认接口可连通')
        return []
      })
    },
    normalizeColumnType(type) {
      if (!type) return ''
      return String(type).trim().toLowerCase().replace(/\s+/g, '')
    },
    getSourceMeta(sourceField) {
      if (!sourceField) return { type: '', comment: '' }
      const map = this.sourceFieldMetaMap || {}
      return map[sourceField] || map[String(sourceField).toLowerCase()] || map[this.toLeafFieldName(sourceField)] || { type: '', comment: '' }
    },
    /** 构造一行映射，带上类型/注释默认值 */
    buildMappingRow(sourceField, targetField) {
      const meta = this.getSourceMeta(sourceField)
      const row = {
        sourceField: sourceField || '',
        targetField: targetField != null ? targetField : this.toLeafFieldName(sourceField),
        targetType: '',
        targetComment: ''
      }
      if (this.isApiSource) {
        // API：不填类型时后端默认字符串；表单预填便于看见默认值
        row.targetType = 'varchar(255)'
        row.targetComment = ''
      } else {
        row.targetType = meta.type || ''
        row.targetComment = meta.comment || ''
      }
      return this.ensureMappingRowKey(row)
    },
    onSourceFieldPicked(row) {
      if (!row) return
      this.ensureMappingRowKey(row)
      const meta = this.getSourceMeta(row.sourceField)
      if (!row.targetField && row.sourceField) {
        row.targetField = this.toLeafFieldName(row.sourceField)
      }
      if (!row.targetType) {
        row.targetType = this.isApiSource ? 'varchar(255)' : (meta.type || '')
      }
      if (!row.targetComment) {
        row.targetComment = meta.comment || ''
      }
    },
    queryTypeSuggestions(queryString, cb) {
      const q = (queryString || '').toLowerCase()
      const list = (this.commonSqlTypes || [])
        .filter(t => !q || t.indexOf(q) !== -1)
        .map(t => ({ value: t }))
      cb(list)
    },
    /** 粗粒度类型族，用于不兼容提示 */
    typeFamily(type) {
      const t = this.normalizeColumnType(type)
      if (!t) return 'unknown'
      if (t.indexOf('char') >= 0 || t.indexOf('text') >= 0 || t === 'json' || t.indexOf('blob') >= 0) return 'string'
      if (t.indexOf('int') >= 0 || t === 'bit' || t.indexOf('bool') >= 0) return 'int'
      if (t.indexOf('decimal') >= 0 || t.indexOf('numeric') >= 0 || t.indexOf('float') >= 0 || t.indexOf('double') >= 0) return 'number'
      if (t.indexOf('date') >= 0 || t.indexOf('time') >= 0) return 'time'
      return 'other'
    },
    onTargetTypeChange(row) {
      if (!row || !row.targetType) return
      const meta = this.getSourceMeta(row.sourceField)
      const srcType = meta.type || (this.isApiSource ? 'varchar(255)' : '')
      if (!srcType) return
      const sf = this.typeFamily(srcType)
      const tf = this.typeFamily(row.targetType)
      // 宽松：字符串可接万物；数值族互转可接受；其余跨族提示
      if (tf === 'string' || sf === tf) return
      if ((sf === 'int' || sf === 'number') && (tf === 'int' || tf === 'number')) return
      this.$modal.msgWarning('目标类型「' + row.targetType + '」可能与源类型「' + srcType + '」不兼容，同步任务可能会失败')
    },
    addMappingRow() {
      if (!this.form.fieldMappings) {
        this.$set(this.form, 'fieldMappings', [])
      }
      this.form.fieldMappings.push(this.buildMappingRow('', ''))
      this.$nextTick(() => this.initMappingSortable())
    },
    autoMapFields() {
      if (!this.sourceFieldOptions.length) {
        this.$modal.msgWarning('请先加载源字段')
        return
      }
      // 目标表已有列：按同名（忽略大小写）做交集；否则按源字段名整表生成（新建表）
      if (this.targetFieldOptions.length) {
        const sourceSet = new Set(this.sourceFieldOptions.map(f => f.toLowerCase()))
        const mapped = []
        this.targetFieldOptions.forEach(tf => {
          if (sourceSet.has(tf.toLowerCase())) {
            const sf = this.sourceFieldOptions.find(s => s.toLowerCase() === tf.toLowerCase())
            mapped.push(this.buildMappingRow(sf, tf))
          }
        })
        this.form.fieldMappings = mapped
        if (!mapped.length) {
          this.$modal.msgWarning('未找到同名字段；可改用「同源字段名」按源字段生成目标列名')
        }
        this.$nextTick(() => this.initMappingSortable())
        return
      }
      this.mapBySourceFieldNames()
    },
    /** 目标字段按源字段生成：带路径时默认取末段（data.chiefNo → chiefNo） */
    mapBySourceFieldNames() {
      if (!this.sourceFieldOptions.length) {
        this.$modal.msgWarning('请先加载源字段')
        return
      }
      this.form.fieldMappings = this.sourceFieldOptions.map(sf => this.buildMappingRow(sf, this.toLeafFieldName(sf)))
      this.$nextTick(() => this.initMappingSortable())
    },
    /** API 嵌套字段取最后一段作为默认目标列名 */
    toLeafFieldName(field) {
      if (field == null || field === '') return ''
      const s = String(field)
      const i = s.lastIndexOf('.')
      return i >= 0 ? s.slice(i + 1) : s
    },
    queryTargetFieldSuggestions(queryString, cb, row) {
      const q = (queryString || '').toLowerCase()
      const preferred = []
      // 当前行源字段（如 data.chiefNo）及其末段优先，清空后仍好找回
      if (row && row.sourceField) {
        preferred.push(row.sourceField)
        const leaf = this.toLeafFieldName(row.sourceField)
        if (leaf && leaf !== row.sourceField) preferred.push(leaf)
      }
      const rest = (this.targetFieldSelectOptions || []).filter(f => preferred.indexOf(f) === -1)
      const merged = preferred.concat(rest)
      const list = merged
        .filter(f => !q || String(f).toLowerCase().indexOf(q) !== -1)
        .map(f => ({ value: f }))
      cb(list)
    },
    clearFieldMappings() {
      if (!(this.form.fieldMappings && this.form.fieldMappings.length)) {
        return
      }
      this.$modal.confirm('确认清空全部字段映射？').then(() => {
        this.form.fieldMappings = []
      }).catch(() => {})
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
      this.formHydrating = false
      this.form = {
        id: undefined,
        jobName: undefined,
        datasourceType: undefined,
        datasourceId: undefined,
        sourceTable: undefined,
        targetDatasourceId: undefined,
        targetTable: undefined,
        fieldMappings: [],
        syncType: 0,
        triggerType: 0,
        cron: '',
        remark: undefined
      }
      this.datasourceOptions = []
      this.sourceTableOptions = []
      this.targetTableOptions = []
      this.sourceFieldOptions = []
      this.sourceFieldMetaMap = {}
      this.targetFieldOptions = []
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
        if (data.targetDatasourceId != null) data.targetDatasourceId = String(data.targetDatasourceId)
        data.fieldMappings = data.fieldMappings || []
        const mappings = data.fieldMappings.slice()
        this.form = data
        if (this.form.effectiveTimeBegin && this.form.effectiveTimeEnd) {
          this.effectiveRange = [this.form.effectiveTimeBegin, this.form.effectiveTimeEnd]
        }
        // 先挂值再拉下拉选项，避免清空再 $set 触发 change 竞态
        this.formHydrating = true
        this.open = true
        this.title = '修改作业'
        const dsId = this.form.datasourceId
        const sourceTable = this.form.sourceTable
        const targetDsId = this.form.targetDatasourceId
        const targetTable = this.form.targetTable
        const sourceReady = this.loadDatasourcesByType(this.form.datasourceType).then(() => {
          if (this.form.datasourceType === 'api') {
            return dsId ? this.loadApiSourceFields(dsId) : []
          }
          if (!dsId) return []
          return this.loadSourceTables(dsId).then(() => {
            return sourceTable ? this.loadJdbcSourceFields(dsId, sourceTable) : []
          })
        })
        const targetReady = !targetDsId
          ? Promise.resolve([])
          : this.loadTargetTables(targetDsId).then(() => {
            return targetTable ? this.loadTargetFields(targetDsId, targetTable) : []
          })
        Promise.all([sourceReady, targetReady]).then(() => {
          // 兼容旧数据：补齐类型/注释字段
          const filled = (mappings || []).map(m => {
            const row = Object.assign({ targetType: '', targetComment: '' }, m)
            if (!row.targetType) {
              const meta = this.getSourceMeta(row.sourceField)
              row.targetType = this.isApiSource ? 'varchar(255)' : (meta.type || '')
            }
            if (!row.targetComment) {
              const meta = this.getSourceMeta(row.sourceField)
              row.targetComment = meta.comment || ''
            }
            return this.ensureMappingRowKey(row)
          })
          this.$set(this.form, 'fieldMappings', filled)
          this.$nextTick(() => this.initMappingSortable())
        }).finally(() => {
          this.$nextTick(() => { this.formHydrating = false })
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
        if (this.form.datasourceType !== 'api' && !this.form.sourceTable) {
          this.$modal.msgWarning('请选择源表')
          return
        }
        const mappings = (this.form.fieldMappings || [])
          .filter(i => i && i.sourceField && i.targetField)
          .map(i => ({
            sourceField: i.sourceField,
            targetField: i.targetField,
            targetType: i.targetType || undefined,
            targetComment: i.targetComment || undefined
            // _rowKey 仅前端拖拽用，不提交
          }))
        if (!mappings.length) {
          this.$modal.msgWarning('请至少配置一条字段映射')
          return
        }
        if (this.effectiveRange && this.effectiveRange.length === 2) {
          this.form.effectiveTimeBegin = this.effectiveRange[0]
          this.form.effectiveTimeEnd = this.effectiveRange[1]
        }
        const payload = Object.assign({}, this.form, { fieldMappings: mappings })
        const req = payload.id != null ? updateJob(payload) : addJob(payload)
        req.then(() => {
          this.$modal.msgSuccess(payload.id != null ? '修改成功' : '新增成功')
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
      this.$router.push({
        path: '/datasource/jobLog',
        query: { jobId: row.id != null ? String(row.id) : undefined, jobName: row.jobName }
      })
    }
  }
}
</script>

<style scoped>
.mapping-drag-handle {
  cursor: move;
  color: #909399;
  font-size: 16px;
}
.mapping-drag-handle:hover {
  color: #409EFF;
}
</style>

<style>
/* Sortable ghost 作用在表格行上，需非 scoped */
.mapping-row-ghost {
  opacity: 0.6;
  background: #f5f7fa !important;
}
</style>
