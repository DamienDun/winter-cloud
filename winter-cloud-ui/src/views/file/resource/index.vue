<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="文件名" prop="originName">
        <el-input v-model="queryParams.originName" placeholder="原始文件名" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="MD5" prop="fileMd5">
        <el-input v-model="queryParams.fileMd5" placeholder="文件MD5" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="通道" prop="channelId">
        <el-input v-model="queryParams.channelId" placeholder="存储通道" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="类型" prop="fileType">
        <el-select v-model="queryParams.fileType" placeholder="文件类型" clearable>
          <el-option v-for="item in fileTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-upload2" size="mini" @click="goUpload">上传文件</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete">删除</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="dataList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="文件名" align="center" prop="originName" min-width="140" :show-overflow-tooltip="true" />
      <el-table-column label="类型" align="center" prop="fileType" width="90">
        <template slot-scope="scope">
          <span>{{ fileTypeLabel(scope.row.fileType) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="大小" align="center" prop="length" width="100">
        <template slot-scope="scope">
          <span>{{ formatSize(scope.row.length) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="通道" align="center" prop="channelId" width="90" />
      <el-table-column label="桶" align="center" prop="bucketName" width="120" :show-overflow-tooltip="true" />
      <el-table-column label="访问地址" align="center" prop="path" min-width="180" :show-overflow-tooltip="true">
        <template slot-scope="scope">
          <el-link v-if="scope.row.path" :href="scope.row.path" target="_blank" type="primary">{{ scope.row.path }}</el-link>
        </template>
      </el-table-column>
      <el-table-column label="MD5" align="center" prop="fileMd5" width="140" :show-overflow-tooltip="true" />
      <el-table-column label="创建时间" align="center" prop="gmtCreate" width="160">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.gmtCreate) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="140" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-view" @click="handleView(scope.row)">详情</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)">删除</el-button>
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

    <el-dialog title="资源文件详情" :visible.sync="open" width="720px" append-to-body>
      <el-descriptions :column="1" border size="small" v-if="form">
        <el-descriptions-item label="ID">{{ form.id }}</el-descriptions-item>
        <el-descriptions-item label="文件名">{{ form.originName }}</el-descriptions-item>
        <el-descriptions-item label="类型">{{ fileTypeLabel(form.fileType) }}</el-descriptions-item>
        <el-descriptions-item label="大小">{{ formatSize(form.length) }}</el-descriptions-item>
        <el-descriptions-item label="通道">{{ form.channelId }}</el-descriptions-item>
        <el-descriptions-item label="桶">{{ form.bucketName }}</el-descriptions-item>
        <el-descriptions-item label="访问地址">
          <el-link v-if="form.path" :href="form.path" target="_blank" type="primary">{{ form.path }}</el-link>
        </el-descriptions-item>
        <el-descriptions-item label="相对路径">{{ form.relativePath }}</el-descriptions-item>
        <el-descriptions-item label="MD5">{{ form.fileMd5 }}</el-descriptions-item>
        <el-descriptions-item label="上传次数">{{ form.applyUploadCount }}</el-descriptions-item>
        <el-descriptions-item label="模块">{{ form.module }}</el-descriptions-item>
        <el-descriptions-item label="备注">{{ form.remark }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ parseTime(form.gmtCreate) }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ parseTime(form.gmtModified) }}</el-descriptions-item>
      </el-descriptions>
      <div slot="footer" class="dialog-footer">
        <el-button @click="open = false">关 闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listResourceFile, getResourceFile, delResourceFile } from '@/api/file/resource'

export default {
  name: 'ResourceFileList',
  data() {
    return {
      loading: true,
      showSearch: true,
      total: 0,
      dataList: [],
      ids: [],
      multiple: true,
      open: false,
      form: null,
      fileTypeOptions: [
        { value: 0, label: '其他' },
        { value: 1, label: '图片' },
        { value: 2, label: '音频' },
        { value: 3, label: '视频' },
        { value: 4, label: 'pdf' },
        { value: 5, label: 'doc' },
        { value: 6, label: 'excel' },
        { value: 7, label: 'ppt' },
        { value: 8, label: 'txt' },
        { value: 9, label: 'html' }
      ],
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        originName: undefined,
        fileMd5: undefined,
        channelId: undefined,
        fileType: undefined
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      listResourceFile(this.queryParams).then(res => {
        this.dataList = res.rows || []
        this.total = res.total || 0
        this.loading = false
      }).catch(() => {
        this.loading = false
      })
    },
    fileTypeLabel(type) {
      const hit = this.fileTypeOptions.find(i => i.value === type)
      return hit ? hit.label : type
    },
    formatSize(bytes) {
      if (bytes === null || bytes === undefined) return '-'
      const n = Number(bytes)
      if (n < 1024) return n + ' B'
      if (n < 1024 * 1024) return (n / 1024).toFixed(2) + ' KB'
      if (n < 1024 * 1024 * 1024) return (n / 1024 / 1024).toFixed(2) + ' MB'
      return (n / 1024 / 1024 / 1024).toFixed(2) + ' GB'
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
      this.ids = selection.map(item => String(item.id))
      this.multiple = !selection.length
    },
    goUpload() {
      this.$router.push('/file/upload')
    },
    handleView(row) {
      getResourceFile(row.id).then(res => {
        this.form = res.data
        this.open = true
      })
    },
    handleDelete(row) {
      const ids = row && row.id != null ? String(row.id) : this.ids.join(',')
      this.$modal.confirm('是否确认删除选中的资源文件？删除后对象存储文件也会移除。').then(() => {
        return delResourceFile(ids)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess('删除成功')
      }).catch(() => {})
    }
  }
}
</script>