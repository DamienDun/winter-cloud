<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="uploadId" prop="uploadId">
        <el-input v-model="queryParams.uploadId" placeholder="上传任务ID" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="MD5" prop="fileMd5">
        <el-input v-model="queryParams.fileMd5" placeholder="文件MD5" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="任务状态" clearable>
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="通道" prop="channelId">
        <el-input v-model="queryParams.channelId" placeholder="存储通道" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="路径" prop="filePath">
        <el-input v-model="queryParams.filePath" placeholder="对象路径" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-upload2" size="mini" @click="goUpload">大文件上传</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete">删除</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="dataList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="状态" align="center" prop="status" width="90">
        <template slot-scope="scope">
          <el-tag :type="statusTagType(scope.row.status)" size="mini">{{ statusLabel(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="进度" align="center" width="160">
        <template slot-scope="scope">
          <el-progress :percentage="progress(scope.row)" :stroke-width="14" :text-inside="true" />
        </template>
      </el-table-column>
      <el-table-column label="大小" align="center" width="110">
        <template slot-scope="scope">
          <span>{{ formatSize(scope.row.completeBytes) }} / {{ formatSize(scope.row.totalBytes) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="分片" align="center" width="90">
        <template slot-scope="scope">
          <span>{{ scope.row.partCount || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="通道" align="center" prop="channelId" width="90" />
      <el-table-column label="对象路径" align="center" prop="filePath" min-width="180" :show-overflow-tooltip="true" />
      <el-table-column label="访问地址" align="center" prop="accessPath" min-width="180" :show-overflow-tooltip="true">
        <template slot-scope="scope">
          <el-link v-if="scope.row.accessPath" :href="scope.row.accessPath" target="_blank" type="primary">打开</el-link>
        </template>
      </el-table-column>
      <el-table-column label="MD5" align="center" prop="fileMd5" width="140" :show-overflow-tooltip="true" />
      <el-table-column label="创建时间" align="center" prop="createTime" width="160">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="200" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-zoom-in"
            :disabled="!canPreview(scope.row)"
            @click="handlePreview(scope.row)"
          >预览</el-button>
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

    <el-dialog title="大文件任务详情" :visible.sync="open" width="760px" append-to-body>
      <el-descriptions :column="1" border size="small" v-if="form">
        <el-descriptions-item label="ID">{{ form.id }}</el-descriptions-item>
        <el-descriptions-item label="uploadId">{{ form.uploadId }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ statusLabel(form.status) }}</el-descriptions-item>
        <el-descriptions-item label="进度">{{ progress(form) }}%</el-descriptions-item>
        <el-descriptions-item label="总大小">{{ formatSize(form.totalBytes) }}</el-descriptions-item>
        <el-descriptions-item label="已完成">{{ formatSize(form.completeBytes) }}</el-descriptions-item>
        <el-descriptions-item label="分片大小">{{ formatSize(form.partBytes) }}</el-descriptions-item>
        <el-descriptions-item label="分片数">{{ form.partCount }}</el-descriptions-item>
        <el-descriptions-item label="已传分片">{{ form.uploadedPartNumbers }}</el-descriptions-item>
        <el-descriptions-item label="通道">{{ form.channelId }}</el-descriptions-item>
        <el-descriptions-item label="桶">{{ form.bucketName }}</el-descriptions-item>
        <el-descriptions-item label="文件类型">{{ form.fileType }}</el-descriptions-item>
        <el-descriptions-item label="对象路径">{{ form.filePath }}</el-descriptions-item>
        <el-descriptions-item label="访问地址">
          <el-link v-if="form.accessPath" :href="form.accessPath" target="_blank" type="primary">{{ form.accessPath }}</el-link>
        </el-descriptions-item>
        <el-descriptions-item label="MD5">{{ form.fileMd5 }}</el-descriptions-item>
        <el-descriptions-item label="备注">{{ form.remark }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ parseTime(form.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ parseTime(form.updateTime) }}</el-descriptions-item>
      </el-descriptions>
      <div slot="footer" class="dialog-footer">
        <el-button v-if="form && canPreview(form)" type="primary" plain icon="el-icon-zoom-in" @click="handlePreview(form)">预 览</el-button>
        <el-button @click="open = false">关 闭</el-button>
      </div>
    </el-dialog>

    <file-preview ref="filePreview" />
  </div>
</template>

<script>
import { listBigFileTask, getBigFileTask, delBigFileTask } from '@/api/file/bigFile'
import FilePreview from '@/components/FilePreview'

export default {
  name: 'BigFileTaskList',
  components: { FilePreview },
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
      statusOptions: [
        { value: 0, label: '取消' },
        { value: 1, label: '新建' },
        { value: 2, label: '上传中' },
        { value: 3, label: '上传完成' }
      ],
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        uploadId: undefined,
        fileMd5: undefined,
        status: undefined,
        channelId: undefined,
        filePath: undefined
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      listBigFileTask(this.queryParams).then(res => {
        this.dataList = res.rows || []
        this.total = res.total || 0
        this.loading = false
      }).catch(() => {
        this.loading = false
      })
    },
    statusLabel(status) {
      const hit = this.statusOptions.find(i => i.value === status)
      return hit ? hit.label : status
    },
    statusTagType(status) {
      if (status === 3) return 'success'
      if (status === 2) return 'warning'
      if (status === 0) return 'info'
      return ''
    },
    progress(row) {
      if (!row || !row.totalBytes) return 0
      const p = Math.round((Number(row.completeBytes || 0) / Number(row.totalBytes)) * 100)
      return Math.min(100, Math.max(0, p))
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
      this.$router.push('/file/bigUpload')
    },
    /** 仅上传完成且有访问地址才可预览 */
    canPreview(row) {
      return !!(row && row.accessPath && row.status === 3)
    },
    handleView(row) {
      getBigFileTask(row.id).then(res => {
        this.form = res.data
        this.open = true
      })
    },
    handlePreview(row) {
      if (!this.canPreview(row)) {
        this.$modal.msgWarning('仅已完成上传的任务可预览')
        return
      }
      this.$refs.filePreview.open(row.accessPath, {
        fileType: row.fileType,
        fileName: this.pickFileName(row)
      })
    },
    pickFileName(row) {
      if (!row) return ''
      const path = row.filePath || row.accessPath || ''
      const clean = path.split('?')[0]
      return clean.substring(clean.lastIndexOf('/') + 1)
    },
    handleDelete(row) {
      const ids = row && row.id != null ? String(row.id) : this.ids.join(',')
      this.$modal.confirm('是否确认删除选中的大文件任务？未完成将中止分片，已完成将删除对象。').then(() => {
        return delBigFileTask(ids)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess('删除成功')
      }).catch(() => {})
    }
  }
}
</script>