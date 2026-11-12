<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="88px">
      <el-form-item label="数据源名称" prop="datasourceName">
        <el-input v-model="queryParams.datasourceName" placeholder="请输入名称" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="分组" prop="datasourceGroup">
        <el-input v-model="queryParams.datasourceGroup" placeholder="分组" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="数据源类型" prop="datasourceType">
        <el-select v-model="queryParams.datasourceType" placeholder="类型" clearable>
          <el-option label="HTTP" value="HTTP" />
          <el-option label="WebService" value="WEBSERVICE" />
        </el-select>
      </el-form-item>
      <el-form-item label="请求方式" prop="reqType">
        <el-select v-model="queryParams.reqType" placeholder="请求方式" clearable>
          <el-option label="POST" :value="0" />
          <el-option label="GET" :value="1" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="状态" clearable>
          <el-option label="启用" :value="1" />
          <el-option label="停用" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['datasource:api:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate" v-hasPermi="['datasource:api:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['datasource:api:remove']">删除</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="dataList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="名称" align="center" prop="datasourceName" min-width="120" :show-overflow-tooltip="true" />
      <el-table-column label="分组" align="center" prop="datasourceGroup" width="100" :show-overflow-tooltip="true" />
      <el-table-column label="类型" align="center" prop="datasourceType" width="100">
        <template slot-scope="scope">
          <el-tag :type="scope.row.datasourceType === 'WEBSERVICE' ? 'warning' : ''" size="mini">
            {{ scope.row.datasourceType === 'WEBSERVICE' ? 'WebService' : 'HTTP' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="URL" align="center" prop="apiUrl" min-width="220" :show-overflow-tooltip="true" />
      <el-table-column label="请求" align="center" prop="reqType" width="80">
        <template slot-scope="scope">{{ scope.row.reqType === 0 ? 'POST' : 'GET' }}</template>
      </el-table-column>
      <el-table-column label="分页" align="center" prop="pageApi" width="70">
        <template slot-scope="scope">{{ Number(scope.row.pageApi) === 1 ? '是' : '否' }}</template>
      </el-table-column>
      <el-table-column label="依赖源" align="center" prop="belongToRelyDatasource" width="100">
        <template slot-scope="scope">{{ scope.row.belongToRelyDatasource === 0 ? '是' : '否' }}</template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="80">
        <template slot-scope="scope">
          <el-tag :type="scope.row.status === 1 ? 'success' : 'info'" size="mini">{{ scope.row.status === 1 ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="修改时间" align="center" prop="gmtModified" width="160">
        <template slot-scope="scope"><span>{{ parseTime(scope.row.gmtModified) }}</span></template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="280" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-view" @click="handleView(scope.row)" v-hasPermi="['datasource:api:query']">详情</el-button>
          <el-button size="mini" type="text" icon="el-icon-connection" @click="handleTestRow(scope.row)" v-hasPermi="['datasource:api:test']">测试</el-button>
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['datasource:api:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['datasource:api:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" :visible.sync="open" width="960px" append-to-body v-if="open" custom-class="api-ds-form-dialog">
      <el-form ref="form" :model="form" :rules="rules" label-width="110px" size="small">
        <el-divider content-position="left">基本信息</el-divider>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="数据源名称" prop="datasourceName">
              <el-input v-model="form.datasourceName" maxlength="100" placeholder="请输入名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="分组" prop="datasourceGroup">
              <el-input v-model="form.datasourceGroup" maxlength="200" placeholder="Default" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-radio-group v-model="form.status">
                <el-radio :label="1">启用</el-radio>
                <el-radio :label="0">停用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="描述" prop="datasourceDesc">
              <el-input v-model="form.datasourceDesc" maxlength="200" placeholder="可选" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">接口配置</el-divider>
        <el-form-item label="数据源类型" prop="datasourceType">
          <el-radio-group v-model="form.datasourceType" @change="onDatasourceTypeChange">
            <el-radio label="HTTP">HTTP</el-radio>
            <el-radio label="WEBSERVICE">WebService</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="接口地址" prop="apiUrl">
          <el-input v-model="form.apiUrl" :placeholder="isWebService ? 'http://host/service?wsdl 或 SOAP 端点' : 'http://host/api/path'" />
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="请求方式" prop="reqType">
              <el-radio-group v-model="form.reqType" :disabled="isWebService">
                <el-radio :label="1">GET</el-radio>
                <el-radio :label="0">POST</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="数据项KEY" prop="dataItemKey">
              <el-input
                v-model="form.dataItemKey"
                :placeholder="isWebService ? 'Envelope.Body.xxx.return' : 'data / data.list / rows'"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item v-if="isWebService" label="SOAPAction" prop="soapAction">
          <el-input v-model="form.reqParams.soapAction" placeholder="可选，如 http://tempuri.org/GetList" />
        </el-form-item>
        <el-form-item label="分页接口" prop="pageApi">
          <el-radio-group v-model="form.pageApi">
            <el-radio :label="0">否</el-radio>
            <el-radio :label="1">是</el-radio>
          </el-radio-group>
        </el-form-item>
        <div class="api-ds-hint">
          <template v-if="isWebService">
            WebService 固定 POST，Body 填 SOAP XML；数据项 KEY 用 XML 点路径（忽略命名空间），如 Envelope.Body.getListResponse.return。分页时会改 XML 中 pageNum/pageSize 标签文本。
          </template>
          <template v-else>
            数据项 KEY 指向响应中的业务列表节点。勾选「分页接口」后：测试连接会把 pageSize 临时改为 2；作业同步按页拉取并逐页写入，避免内存堆积。
          </template>
        </div>

        <el-divider content-position="left">依赖鉴权</el-divider>
        <el-form-item label="依赖数据源" prop="belongToRelyDatasource">
          <el-radio-group v-model="form.belongToRelyDatasource" @change="onBelongRelyChange">
            <el-radio :label="1">否</el-radio>
            <el-radio :label="0">是</el-radio>
          </el-radio-group>
        </el-form-item>
        <template v-if="form.belongToRelyDatasource === 0">
          <el-form-item label="依赖API" prop="relyDatasourceId">
            <el-select
              v-model="form.relyDatasourceId"
              clearable
              filterable
              :filter-method="filterRelyOptions"
              :loading="relyLoading"
              placeholder="输入名称模糊搜索并选择"
              style="width: 100%"
              @change="onRelyChange"
              @visible-change="onRelyVisible"
            >
              <el-option
                v-for="item in relyOptionsView"
                :key="String(item.id)"
                :label="item.datasourceName"
                :value="String(item.id)"
              >
                <span>{{ item.datasourceName }}</span>
                <span style="float: right; color: #909399; font-size: 12px">{{ item.reqType === 0 ? 'POST' : 'GET' }}</span>
              </el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="出参填充" v-if="form.relyDatasourceId" label-width="110px" class="api-ds-fill-item">
            <div class="api-ds-panel api-ds-fill-panel">
              <div class="api-ds-panel-toolbar">
                <span class="api-ds-panel-title">将依赖接口出参写入请求头/入参</span>
                <el-button type="text" size="mini" icon="el-icon-refresh" :loading="fieldLoading" @click="loadRelyFields">刷新出参</el-button>
              </div>
              <div class="api-ds-fill-row">
                <el-select
                  v-model="fillField"
                  filterable
                  clearable
                  :loading="fieldLoading"
                  placeholder="依赖出参字段"
                  class="api-ds-fill-field"
                >
                  <el-option
                    v-for="f in relyFields"
                    :key="f.fieldName"
                    :label="f.fieldName"
                    :value="f.fieldName"
                  >
                    <span>{{ f.fieldName }}</span>
                    <span style="float: right; color: #909399; font-size: 12px; max-width: 140px; overflow: hidden; text-overflow: ellipsis">{{ f.sampleValue }}</span>
                  </el-option>
                </el-select>
                <el-select v-model="fillTarget" class="api-ds-fill-target">
                  <el-option label="请求头" value="headers" />
                  <el-option label="入参" value="params" />
                </el-select>
                <el-input v-model="fillName" placeholder="参数名，如 Authorization" class="api-ds-fill-name" />
                <el-button type="primary" plain size="mini" :disabled="!fillField" @click="applyRelyField">填入</el-button>
              </div>
              <!-- v-pre：禁止把 {{rely.xxx}} 当 Vue 表达式（HTML 实体仍会被解码成 mustache 导致整页渲染报错） -->
              <div class="api-ds-hint api-ds-hint--inpanel" v-pre>填入后值为 <code>{{rely.字段路径}}</code>，测试/同步时自动替换为依赖接口实际出参。</div>
            </div>
          </el-form-item>
        </template>

        <el-divider content-position="left">请求参数</el-divider>
        <el-form-item label-width="0" class="api-ds-block-item">
          <div class="api-ds-panel">
            <div class="api-ds-panel-toolbar">
              <span class="api-ds-panel-title">Headers</span>
              <el-button type="text" icon="el-icon-plus" @click="addParamRow('headers')">添加</el-button>
            </div>
            <div class="api-ds-panel-body">
              <el-table :data="(form.reqParams && form.reqParams.headers) || []" size="mini" border empty-text="暂无请求头" max-height="220">
                <el-table-column label="Name" prop="name" min-width="140">
                  <template slot-scope="scope"><el-input v-model="scope.row.name" size="mini" placeholder="如 svolt-portal-auth" /></template>
                </el-table-column>
                <el-table-column label="Value" prop="value" min-width="200">
                  <template slot-scope="scope"><el-input v-model="scope.row.value" size="mini" placeholder="支持 rely.token 占位" /></template>
                </el-table-column>
                <el-table-column label="启用" width="70" align="center">
                  <template slot-scope="scope"><el-switch v-model="scope.row.isAvailable" :active-value="0" :inactive-value="1" /></template>
                </el-table-column>
                <el-table-column width="50" align="center">
                  <template slot-scope="scope"><el-button type="text" icon="el-icon-delete" @click="form.reqParams.headers.splice(scope.$index, 1)" /></template>
                </el-table-column>
              </el-table>
            </div>
          </div>
        </el-form-item>
        <el-form-item label-width="0" class="api-ds-block-item">
          <div class="api-ds-panel">
            <div class="api-ds-panel-toolbar">
              <span class="api-ds-panel-title">Params（查询参数）</span>
              <el-button type="text" icon="el-icon-plus" @click="addParamRow('params')">添加</el-button>
            </div>
            <div class="api-ds-panel-body">
              <el-table :data="(form.reqParams && form.reqParams.params) || []" size="mini" border empty-text="暂无入参" max-height="220">
                <el-table-column label="Name" prop="name" min-width="140">
                  <template slot-scope="scope"><el-input v-model="scope.row.name" size="mini" placeholder="如 pageNum" /></template>
                </el-table-column>
                <el-table-column label="Value" prop="value" min-width="200">
                  <template slot-scope="scope"><el-input v-model="scope.row.value" size="mini" /></template>
                </el-table-column>
                <el-table-column label="启用" width="70" align="center">
                  <template slot-scope="scope"><el-switch v-model="scope.row.isAvailable" :active-value="0" :inactive-value="1" /></template>
                </el-table-column>
                <el-table-column width="50" align="center">
                  <template slot-scope="scope"><el-button type="text" icon="el-icon-delete" @click="form.reqParams.params.splice(scope.$index, 1)" /></template>
                </el-table-column>
              </el-table>
            </div>
          </div>
        </el-form-item>
        <el-form-item label-width="0" class="api-ds-block-item" v-if="form.reqType === 0 || isWebService">
          <div class="api-ds-panel">
            <div class="api-ds-panel-toolbar">
              <span class="api-ds-panel-title">{{ isWebService ? 'Body（SOAP XML）' : 'Body（JSON）' }}</span>
            </div>
            <div class="api-ds-panel-body">
              <el-input
                v-model="form.reqParams.body"
                type="textarea"
                :rows="isWebService ? 10 : 5"
                :placeholder="isWebService
                  ? '<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">...</soapenv:Envelope>'
                  : '{\"pageNum\":1,\"pageSize\":100}'"
                class="api-ds-body-input"
              />
            </div>
          </div>
        </el-form-item>
        <div class="api-ds-hint">
          作业同步自动翻页：Body/入参含 pageNum（或 pageNo/page/current）时，从第 1 页翻到业务数组为空；测试连接只请求当前配置的一页。
        </div>

        <el-divider content-position="left">其它</el-divider>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="可选" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="warning" plain icon="el-icon-connection" :loading="testing" @click="submitTest" v-hasPermi="['datasource:api:test']">测试连接</el-button>
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>

    <el-dialog title="API数据源详情" :visible.sync="viewOpen" width="820px" append-to-body>
      <el-descriptions :column="2" border size="small" v-if="viewForm">
        <el-descriptions-item label="ID">{{ viewForm.id || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ viewForm.status === 1 ? '启用' : '停用' }}</el-descriptions-item>
        <el-descriptions-item label="名称">{{ viewForm.datasourceName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="分组">{{ viewForm.datasourceGroup || '-' }}</el-descriptions-item>
        <el-descriptions-item label="数据源类型">{{ viewForm.datasourceType === 'WEBSERVICE' ? 'WebService' : 'HTTP' }}</el-descriptions-item>
        <el-descriptions-item label="请求方式">{{ viewForm.reqType === 0 ? 'POST' : 'GET' }}</el-descriptions-item>
        <el-descriptions-item label="URL" :span="2">{{ viewForm.apiUrl || '-' }}</el-descriptions-item>
        <el-descriptions-item label="数据项KEY">{{ viewForm.dataItemKey || '-' }}</el-descriptions-item>
        <el-descriptions-item label="分页接口">{{ Number(viewForm.pageApi) === 1 ? '是' : '否' }}</el-descriptions-item>
        <el-descriptions-item v-if="viewForm.datasourceType === 'WEBSERVICE'" label="SOAPAction" :span="2">
          {{ (viewForm.reqParams && viewForm.reqParams.soapAction) || (viewReqParams && viewReqParams.soapAction) || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="依赖数据源">{{ viewForm.belongToRelyDatasource === 0 ? '是' : '否' }}</el-descriptions-item>
        <el-descriptions-item label="依赖API">
          <template v-if="viewForm.belongToRelyDatasource === 0">
            {{ viewRelyName || '-' }}
            <span v-if="viewForm.relyDatasourceId" style="color: #909399; margin-left: 6px">({{ viewForm.relyDatasourceId }})</span>
          </template>
          <template v-else>-</template>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ parseTime(viewForm.gmtCreate) || '-' }}</el-descriptions-item>
        <el-descriptions-item label="修改时间">{{ parseTime(viewForm.gmtModified) || '-' }}</el-descriptions-item>
        <el-descriptions-item label="描述" :span="2">{{ viewForm.datasourceDesc || '-' }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ viewForm.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
      <div v-if="viewForm" class="view-req-params">
        <div class="view-req-title">请求头</div>
        <el-table :data="viewReqParams.headers" size="mini" border empty-text="无">
          <el-table-column label="名称" prop="name" min-width="120" show-overflow-tooltip />
          <el-table-column label="值" prop="value" min-width="200" show-overflow-tooltip />
          <el-table-column label="类型" prop="type" width="80" align="center" />
          <el-table-column label="启用" width="70" align="center">
            <template slot-scope="scope">{{ scope.row.isAvailable === 0 ? '是' : '否' }}</template>
          </el-table-column>
        </el-table>
        <div class="view-req-title">入参</div>
        <el-table :data="viewReqParams.params" size="mini" border empty-text="无">
          <el-table-column label="名称" prop="name" min-width="120" show-overflow-tooltip />
          <el-table-column label="值" prop="value" min-width="200" show-overflow-tooltip />
          <el-table-column label="类型" prop="type" width="80" align="center" />
          <el-table-column label="启用" width="70" align="center">
            <template slot-scope="scope">{{ scope.row.isAvailable === 0 ? '是' : '否' }}</template>
          </el-table-column>
        </el-table>
        <div class="view-req-title">分页参数</div>
        <el-table :data="viewReqParams.page" size="mini" border empty-text="无">
          <el-table-column label="名称" prop="name" min-width="120" show-overflow-tooltip />
          <el-table-column label="值" prop="value" min-width="200" show-overflow-tooltip />
          <el-table-column label="类型" prop="type" width="80" align="center" />
          <el-table-column label="启用" width="70" align="center">
            <template slot-scope="scope">{{ scope.row.isAvailable === 0 ? '是' : '否' }}</template>
          </el-table-column>
        </el-table>
        <div class="view-req-title">Body</div>
        <pre class="view-json-block">{{ formatJsonText(viewReqParams.body) }}</pre>
        <div class="view-req-title">前置脚本</div>
        <pre class="view-json-block">{{ viewReqParams.preRequestScript || '-' }}</pre>
        <div class="view-req-title">完整 JSON</div>
        <pre class="view-json-block">{{ formatReqParamsJson(viewForm.reqParams || viewForm.reqParamsJson) }}</pre>
      </div>
      <div slot="footer" class="dialog-footer">
        <el-button @click="viewOpen = false">关 闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listApiDatasource, getApiDatasource, addApiDatasource, updateApiDatasource, delApiDatasource, testApiDatasource, listRelyCandidates, listApiOutputFields } from '@/api/datasource/api'

export default {
  name: 'DatasourceApi',
  data() {
    return {
      loading: false,
      testing: false,
      showSearch: true,
      total: 0,
      dataList: [],
      ids: [],
      single: true,
      multiple: true,
      open: false,
      viewOpen: false,
      title: '',
      viewForm: null,
      viewRelyName: '',
      testResult: null,
      relyOptions: [],
      relyOptionsView: [],
      relyLoading: false,
      relyFields: [],
      fieldLoading: false,
      fillField: undefined,
      fillTarget: 'headers',
      fillName: '',
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        datasourceName: undefined,
        datasourceGroup: undefined,
        datasourceType: undefined,
        reqType: undefined,
        status: undefined
      },
      // 对话框未打开时模板也会渲染 form.reqParams.*，必须给默认结构，否则整页白屏
      form: {
        datasourceGroup: 'Default',
        datasourceType: 'HTTP',
        reqType: 1,
        status: 1,
        pageApi: 0,
        belongToRelyDatasource: 1,
        reqParams: { params: [], headers: [], body: '', page: [], preRequestScript: '', soapAction: '' }
      },
      rules: {
        datasourceName: [{ required: true, message: '名称不能为空', trigger: 'blur' }],
        datasourceType: [{ required: true, message: '请选择数据源类型', trigger: 'change' }],
        apiUrl: [{ required: true, message: 'URL不能为空', trigger: 'blur' }],
        reqType: [{ required: true, message: '请选择请求方式', trigger: 'change' }]
      }
    }
  },
  created() {
    this.getList()
  },
  computed: {
    isWebService() {
      return this.form && this.form.datasourceType === 'WEBSERVICE'
    },
    viewReqParams() {
      const empty = { params: [], headers: [], body: '', page: [], preRequestScript: '', soapAction: '' }
      if (!this.viewForm) {
        return empty
      }
      if (this.viewForm.reqParams && typeof this.viewForm.reqParams === 'object') {
        return {
          params: this.viewForm.reqParams.params || [],
          headers: this.viewForm.reqParams.headers || [],
          body: this.viewForm.reqParams.body || '',
          page: this.viewForm.reqParams.page || [],
          preRequestScript: this.viewForm.reqParams.preRequestScript || '',
          soapAction: this.viewForm.reqParams.soapAction || ''
        }
      }
      // 兼容仅返回 reqParamsJson 字符串的情况
      try {
        const parsed = typeof this.viewForm.reqParamsJson === 'string'
          ? JSON.parse(this.viewForm.reqParamsJson || '{}')
          : (this.viewForm.reqParamsJson || {})
        return {
          params: parsed.params || [],
          headers: parsed.headers || [],
          body: parsed.body || '',
          page: parsed.page || [],
          preRequestScript: parsed.preRequestScript || '',
          soapAction: parsed.soapAction || ''
        }
      } catch (e) {
        return empty
      }
    }
  },
  methods: {
    emptyReqParams() {
      return { params: [], headers: [], body: '', page: [], preRequestScript: '', soapAction: '' }
    },
    onDatasourceTypeChange(val) {
      if (val === 'WEBSERVICE') {
        this.form.reqType = 0
        if (!this.form.reqParams) {
          this.$set(this.form, 'reqParams', this.emptyReqParams())
        }
        if (this.form.reqParams.soapAction == null) {
          this.$set(this.form.reqParams, 'soapAction', '')
        }
      }
    },
    /** 详情/测试结果：尽量格式化 JSON，失败则原样返回 */
    formatJsonText(val) {
      if (val == null || val === '') {
        return '-'
      }
      if (typeof val === 'object') {
        try {
          return JSON.stringify(val, null, 2)
        } catch (e) {
          return String(val)
        }
      }
      const text = String(val)
      try {
        return JSON.stringify(JSON.parse(text), null, 2)
      } catch (e) {
        // body 里偶发字面量 \n（非真实换行），先还原再试解析
        try {
          const normalized = text.replace(/\\n/g, '\n').replace(/\\"/g, '"')
          return JSON.stringify(JSON.parse(normalized), null, 2)
        } catch (e2) {
          return text
        }
      }
    },
    /**
     * 完整请求参数展示：body 字段本身是 JSON 字符串时，先解析成对象再整体美化，
     * 避免出现 "body": "{\\n \"a\": 1}" 这种转义难看的展示。
     */
    formatReqParamsJson(val) {
      if (val == null || val === '') {
        return '-'
      }
      let obj = val
      if (typeof val === 'string') {
        try {
          obj = JSON.parse(val)
        } catch (e) {
          return this.formatJsonText(val)
        }
      }
      if (typeof obj !== 'object' || obj === null) {
        return this.formatJsonText(val)
      }
      const cloned = Object.assign({}, obj)
      if (typeof cloned.body === 'string' && cloned.body.trim()) {
        try {
          cloned.body = JSON.parse(cloned.body)
        } catch (e) {
          try {
            cloned.body = JSON.parse(cloned.body.replace(/\\n/g, '\n').replace(/\\"/g, '"'))
          } catch (e2) {
            // 解析失败则保留原字符串
          }
        }
      }
      try {
        return JSON.stringify(cloned, null, 2)
      } catch (e) {
        return String(val)
      }
    },
    getList() {
      this.loading = true
      listApiDatasource(this.queryParams).then(res => {
        // 列表行ID统一字符串，避免点「修改」时带着丢精度的 Number 去查详情
        this.dataList = (res.rows || []).map(row => ({
          ...row,
          id: row.id != null ? String(row.id) : row.id,
          relyDatasourceId: row.relyDatasourceId != null ? String(row.relyDatasourceId) : row.relyDatasourceId
        }))
        // 后端 total 可能是字符串，Pagination 的 prop 要求 Number
        this.total = Number(res.total) || 0
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    loadRelyOptions() {
      this.relyLoading = true
      // excludeId 用字符串，避免雪花ID在JS里精度丢失
      return listRelyCandidates({
        excludeId: this.form.id != null ? String(this.form.id) : undefined
      }).then(res => {
        const list = (res.data || []).map(i => ({
          ...i,
          id: i.id != null ? String(i.id) : i.id
        }))
        this.relyOptions = list
        this.relyOptionsView = list
        if (!list.length) {
          this.$modal.msgWarning('暂无其它可用的API数据源可选')
        }
        return list
      }).catch(err => {
        this.relyOptions = []
        this.relyOptionsView = []
        this.$modal.msgError((err && err.message) || '加载依赖API列表失败')
        return []
      }).finally(() => {
        this.relyLoading = false
      })
    },
    filterRelyOptions(query) {
      const q = (query || '').trim().toLowerCase()
      if (!q) {
        this.relyOptionsView = this.relyOptions
        return
      }
      this.relyOptionsView = this.relyOptions.filter(i => (i.datasourceName || '').toLowerCase().indexOf(q) >= 0)
    },
    onRelyVisible(visible) {
      if (visible) {
        this.loadRelyOptions()
      }
    },
    onBelongRelyChange(val) {
      if (val !== 0) {
        this.form.relyDatasourceId = undefined
        this.relyFields = []
        this.fillField = undefined
        this.relyOptions = []
        this.relyOptionsView = []
      } else {
        this.loadRelyOptions()
      }
    },
    onRelyChange(id) {
      this.fillField = undefined
      this.relyFields = []
      if (id) {
        this.loadRelyFields()
      }
    },
    loadRelyFields() {
      if (!this.form.relyDatasourceId) {
        return
      }
      this.fieldLoading = true
      listApiOutputFields(this.form.relyDatasourceId).then(res => {
        this.relyFields = res.data || []
        if (!this.relyFields.length) {
          this.$modal.msgWarning('依赖接口未解析到出参字段，请确认该接口可连通且返回JSON')
        }
      }).catch(() => {
        this.relyFields = []
      }).finally(() => {
        this.fieldLoading = false
      })
    },
    applyRelyField() {
      if (!this.fillField) {
        this.$modal.msgWarning('请先选择依赖出参')
        return
      }
      const target = this.fillTarget === 'params' ? 'params' : 'headers'
      const name = (this.fillName || this.fillField.split('.').pop() || this.fillField).trim()
      if (!this.form.reqParams[target]) {
        this.$set(this.form.reqParams, target, [])
      }
      this.form.reqParams[target].push({
        name: name,
        value: '{{rely.' + this.fillField + '}}',
        type: 'string',
        isAvailable: 0
      })
      this.$modal.msgSuccess('已填入' + (target === 'headers' ? '请求头' : '入参'))
      this.fillName = ''
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
      this.ids = selection.map(i => (i.id != null ? String(i.id) : i.id))
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },
    reset() {
      this.form = {
        id: undefined,
        datasourceName: undefined,
        datasourceGroup: 'Default',
        datasourceType: 'HTTP',
        apiUrl: undefined,
        reqType: 1,
        dataItemKey: undefined,
        pageApi: 0,
        relyDatasourceId: undefined,
        belongToRelyDatasource: 1,
        datasourceDesc: undefined,
        status: 1,
        remark: undefined,
        reqParams: this.emptyReqParams()
      }
      this.clearTestResultOverlay()
      this.testResult = null
      this.resetForm('form')
    },
    addParamRow(key) {
      if (!this.form.reqParams[key]) this.$set(this.form.reqParams, key, [])
      this.form.reqParams[key].push({ name: '', value: '', type: 'string', isAvailable: 0 })
    },
    handleAdd() {
      this.clearTestResultOverlay()
      this.reset()
      this.open = true
      this.title = '新增API数据源'
    },
    handleView(row) {
      const id = row && row.id != null ? String(row.id) : row.id
      this.viewRelyName = ''
      getApiDatasource(id).then(res => {
        const data = res.data || {}
        if (data.id != null) data.id = String(data.id)
        if (data.relyDatasourceId != null) data.relyDatasourceId = String(data.relyDatasourceId)
        this.viewForm = data
        this.viewOpen = true
        // 回填依赖API名称
        if (data.belongToRelyDatasource === 0 && data.relyDatasourceId) {
          getApiDatasource(data.relyDatasourceId).then(relyRes => {
            this.viewRelyName = (relyRes.data && relyRes.data.datasourceName) || ''
          }).catch(() => {
            this.viewRelyName = ''
          })
        }
      })
    },
    handleUpdate(row) {
      this.clearTestResultOverlay()
      this.reset()
      // 必须用字符串ID请求，禁止 Number(row.id) 丢精度
      const id = row && row.id != null ? String(row.id) : String(this.ids[0])
      getApiDatasource(id).then(res => {
        const data = Object.assign({}, res.data || {})
        data.reqParams = this.normalizeReqParams(data.reqParams || data.reqParamsJson)
        // 接口可能返回字符串 "0"/"1"，统一成数字，否则 v-if === 0 不成立、依赖区不渲染
        data.belongToRelyDatasource = data.belongToRelyDatasource == null ? 1 : Number(data.belongToRelyDatasource)
        data.pageApi = data.pageApi == null ? 0 : Number(data.pageApi)
        data.datasourceType = data.datasourceType ? String(data.datasourceType).toUpperCase() : 'HTTP'
        if (data.datasourceType === 'SOAP' || data.datasourceType === 'WS' || data.datasourceType === 'WEB_SERVICE') {
          data.datasourceType = 'WEBSERVICE'
        }
        data.reqType = data.datasourceType === 'WEBSERVICE' ? 0 : (data.reqType == null ? 1 : Number(data.reqType))
        data.status = data.status == null ? 1 : Number(data.status)
        // 雪花ID统一转字符串，与依赖下拉 option value 对齐
        if (data.id != null) data.id = String(data.id)
        if (!data.datasourceGroup) data.datasourceGroup = 'Default'
        const relyId = data.relyDatasourceId != null ? String(data.relyDatasourceId) : undefined
        // 先不绑依赖ID，等候选加载完再赋值，避免 el-select 对不上 option 显示成裸数字
        data.relyDatasourceId = undefined
        this.form = data
        this.open = true
        this.title = '修改API数据源'
        if (this.form.belongToRelyDatasource === 0) {
          this.loadRelyOptions().then(list => {
            if (relyId) {
              this.ensureRelyOption(relyId, list)
              this.$set(this.form, 'relyDatasourceId', relyId)
              this.loadRelyFields()
            }
          })
        }
      })
    },
    /** 保证依赖下拉能回显名称：候选里没有时补一条占位 option */
    ensureRelyOption(relyId, list) {
      const id = String(relyId)
      const exists = (list || []).some(i => String(i.id) === id)
      if (exists) return
      const placeholder = { id: id, datasourceName: '依赖API(' + id + ')', reqType: 1, status: 0 }
      this.relyOptions = [placeholder].concat(this.relyOptions || [])
      this.relyOptionsView = this.relyOptions
      this.$modal.msgWarning('依赖API(' + id + ')不在候选列表中，可能已被删除或停用')
    },
    normalizeReqParams(raw) {
      const empty = this.emptyReqParams()
      if (raw == null || raw === '') return empty
      let obj = raw
      if (typeof raw === 'string') {
        try {
          obj = JSON.parse(raw)
        } catch (e) {
          return empty
        }
      }
      return {
        params: obj.params || [],
        headers: obj.headers || [],
        body: obj.body || '',
        page: obj.page || [],
        preRequestScript: obj.preRequestScript || '',
        soapAction: obj.soapAction || ''
      }
    },
    cancel() {
      this.open = false
      this.reset()
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (!valid) return
        const req = this.form.id != null ? updateApiDatasource(this.form) : addApiDatasource(this.form)
        req.then(() => {
          this.$modal.msgSuccess(this.form.id != null ? '修改成功' : '新增成功')
          this.open = false
          this.getList()
        })
      })
    },
    handleDelete(row) {
      const ids = row && row.id != null ? row.id : this.ids.join(',')
      this.$modal.confirm('是否确认删除选中的API数据源？').then(() => delApiDatasource(ids)).then(() => {
        this.getList()
        this.$modal.msgSuccess('删除成功')
      }).catch(() => {})
    },
    handleTestRow(row) {
      this.testing = true
      testApiDatasource({ id: row.id }).then(res => {
        this.showTestResult(res.data || res)
      }).catch(err => {
        const data = (err && err.data) || (err.response && err.response.data && err.response.data.data)
        if (data) {
          this.showTestResult(data)
        } else if (err && err.message) {
          this.$modal.msgError(err.message)
        }
      }).finally(() => { this.testing = false })
    },
    submitTest() {
      if (!this.$refs.form) {
        this.$modal.msgError('表单未就绪，请关闭后重试')
        return
      }
      this.$refs.form.validate(valid => {
        if (!valid) {
          this.$modal.msgWarning('请先完善必填项后再测试')
          return
        }
        this.testing = true
        testApiDatasource(this.form).then(res => {
          this.showTestResult(res.data || res)
        }).catch(err => {
          const data = (err && err.data) || (err.response && err.response.data && err.response.data.data)
          if (data) {
            this.showTestResult(data)
          } else if (err && err.message) {
            this.$modal.msgError(err.message)
          }
        }).finally(() => { this.testing = false })
      })
    },
    showTestResult(data) {
      this.clearTestResultOverlay()
      this.testResult = data || {}
      const ok = this.testResult.testStatus === 0
      const cost = this.testResult.testTime != null ? (this.testResult.testTime + ' ms') : '-'
      const body = this.escapeHtml(this.formatJsonText(this.testResult.testResult))
      // MessageBox 由 PopupManager 管理层级，能稳定压在编辑 Dialog 之上，且不会残留透明遮罩
      this.$msgbox({
        title: '测试结果',
        message:
          '<div class="api-test-result-msg">' +
          '<p><b>状态：</b><span class="' + (ok ? 'is-ok' : 'is-fail') + '">' + (ok ? '成功' : '失败') + '</span>' +
          '&nbsp;&nbsp;<b>耗时：</b>' + cost + '</p>' +
          '<pre class="api-test-result-msg__pre">' + body + '</pre>' +
          '</div>',
        dangerouslyUseHTMLString: true,
        customClass: 'api-test-result-msgbox',
        confirmButtonText: '关 闭',
        closeOnClickModal: false,
        closeOnPressEscape: true
      }).catch(() => {})
      if (ok) {
        this.$modal.msgSuccess('连接成功')
      } else if (this.testResult.testStatus != null) {
        this.$modal.msgError('连接失败')
      }
    },
    escapeHtml(text) {
      return String(text == null ? '' : text)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
    },
    /** 清掉历史测试遮罩残留，避免挡住房编辑框点击 */
    clearTestResultOverlay() {
      if (this._testResultPortal) {
        const el = this._testResultPortal.$el
        try {
          this._testResultPortal.$destroy()
        } catch (e) { /* ignore */ }
        if (el && el.parentNode) el.parentNode.removeChild(el)
        this._testResultPortal = null
      }
      document.querySelectorAll('.api-test-result-portal').forEach(node => {
        if (node && node.parentNode) node.parentNode.removeChild(node)
      })
    }
  },
  beforeDestroy() {
    this.clearTestResultOverlay()
  }
}
</script>

<style scoped>
.view-req-params {
  margin-top: 14px;
}
.view-req-title {
  margin: 12px 0 6px;
  font-size: 13px;
  font-weight: 600;
  color: #303133;
}
.view-json-block {
  margin: 0;
  padding: 10px 12px;
  max-height: 240px;
  overflow: auto;
  background: #f5f7fa;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  font-family: Consolas, Monaco, 'Courier New', monospace;
  font-size: 12px;
  line-height: 1.55;
  white-space: pre-wrap;
  word-break: break-all;
  color: #303133;
}
.view-json-block--tall {
  max-height: 420px;
}
.api-ds-hint {
  margin: 0 0 12px 110px;
  color: #909399;
  font-size: 12px;
  line-height: 1.5;
}
.api-ds-hint--inpanel {
  margin: 8px 0 0;
}
.api-ds-block-item {
  margin-bottom: 12px;
}
.api-ds-block-item >>> .el-form-item__content {
  margin-left: 0 !important;
}
.api-ds-fill-item >>> .el-form-item__content {
  line-height: normal;
}
.api-ds-panel {
  width: 100%;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  background: #fff;
  overflow: hidden;
}
.api-ds-panel-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 6px 12px;
  border-bottom: 1px solid #ebeef5;
  background: #fafbfc;
}
.api-ds-panel-title {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
}
.api-ds-panel-body {
  padding: 10px 12px 12px;
}
.api-ds-fill-panel {
  margin-top: 0;
}
.api-ds-fill-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  padding: 10px 12px 0;
}
.api-ds-fill-field {
  width: 240px;
}
.api-ds-fill-target {
  width: 110px;
}
.api-ds-fill-name {
  width: 200px;
}
.api-ds-fill-panel .api-ds-hint--inpanel {
  padding: 0 12px 10px;
}
.api-ds-body-input >>> textarea {
  font-family: Consolas, Monaco, 'Courier New', monospace;
  font-size: 12px;
  line-height: 1.5;
}
</style>

<style>
.api-ds-form-dialog .el-dialog__body {
  max-height: calc(100vh - 160px);
  overflow-y: auto;
  padding-top: 10px;
}
.api-test-result-msgbox {
  width: 720px;
}
.api-test-result-msg {
  text-align: left;
  color: #606266;
  font-size: 13px;
}
.api-test-result-msg .is-ok {
  color: #67c23a;
  font-weight: 600;
}
.api-test-result-msg .is-fail {
  color: #f56c6c;
  font-weight: 600;
}
.api-test-result-msg__pre {
  margin: 10px 0 0;
  padding: 10px 12px;
  max-height: 420px;
  overflow: auto;
  background: #f5f7fa;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  font-family: Consolas, Monaco, 'Courier New', monospace;
  font-size: 12px;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
