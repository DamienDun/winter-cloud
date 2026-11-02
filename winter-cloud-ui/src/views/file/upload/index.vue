<template>
  <div class="app-container">
    <el-card shadow="never" class="box-card">
      <div slot="header" class="clearfix">
        <span>普通文件上传</span>
        <span class="header-tip">适合小文件，直传 `/file/upload`</span>
      </div>

      <el-form label-width="100px" size="small">
        <el-form-item label="选择文件">
          <file-upload
            v-model="fileUrls"
            :limit="10"
            :file-size="50"
            :file-type="fileType"
            :is-show-tip="true"
          />
        </el-form-item>
        <el-form-item label="访问地址" v-if="fileUrls">
          <el-input type="textarea" :rows="4" :value="fileUrls" readonly />
          <div class="url-list">
            <div v-for="(url, index) in urlList" :key="index" class="url-item">
              <el-link :href="url" target="_blank" type="primary" :underline="false">{{ url }}</el-link>
            </div>
          </div>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="box-card" style="margin-top: 16px;">
      <div slot="header" class="clearfix">
        <span>图片上传</span>
      </div>
      <image-upload v-model="imageUrls" :limit="5" :file-size="10" :file-type="imageType" />
      <div class="url-list" v-if="imageUrls" style="margin-top: 12px;">
        <div v-for="(url, index) in imageList" :key="index" class="url-item">
          <el-link :href="url" target="_blank" type="primary" :underline="false">{{ url }}</el-link>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script>
export default {
  name: 'FileUploadPage',
  data() {
    return {
      fileUrls: '',
      imageUrls: '',
      fileType: ['bmp', 'gif', 'jpg', 'jpeg', 'png', 'doc', 'docx', 'xls', 'xlsx', 'ppt', 'pptx', 'html', 'htm', 'txt', 'rar', 'zip', 'gz', 'bz2', 'mp4', 'avi', 'rmvb', 'pdf'],
      // 对齐后端图片扩展名
      imageType: ['bmp', 'gif', 'jpg', 'jpeg', 'png', 'tif', 'tiff', 'dib', 'webp', 'svg']
    }
  },
  computed: {
    urlList() {
      return this.splitUrls(this.fileUrls)
    },
    imageList() {
      return this.splitUrls(this.imageUrls)
    }
  },
  methods: {
    splitUrls(val) {
      if (!val) {
        return []
      }
      return String(val).split(',').map(s => s.trim()).filter(Boolean)
    }
  }
}
</script>

<style scoped>
.header-tip {
  margin-left: 12px;
  color: #909399;
  font-size: 12px;
}
.url-list {
  margin-top: 8px;
}
.url-item {
  line-height: 22px;
  word-break: break-all;
}
</style>
