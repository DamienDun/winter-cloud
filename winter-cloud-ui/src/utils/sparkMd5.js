import SparkMD5 from 'spark-md5'

/** 计算文件 MD5 时的分块大小（仅用于本地哈希，与上传分片无关） */
const DEFAULT_HASH_CHUNK = 20 * 1024 * 1024

/**
 * 分块计算文件 MD5（对齐 paperless spark-md5 用法）
 * @param {File} file
 * @param {number} [chunkSize]
 * @returns {Promise<string>}
 */
export function calcFileMd5(file, chunkSize = DEFAULT_HASH_CHUNK) {
  return new Promise((resolve, reject) => {
    const blobSlice = File.prototype.slice || File.prototype.mozSlice || File.prototype.webkitSlice
    const chunks = Math.ceil(file.size / chunkSize)
    let currentChunk = 0
    const spark = new SparkMD5.ArrayBuffer()
    const fileReader = new FileReader()

    fileReader.onload = function(e) {
      spark.append(e.target.result)
      currentChunk++
      if (currentChunk < chunks) {
        loadNext()
      } else {
        resolve(spark.end())
      }
    }
    fileReader.onerror = function(e) {
      reject(e)
    }

    function loadNext() {
      const start = currentChunk * chunkSize
      let end = start + chunkSize
      if (end > file.size) {
        end = file.size
      }
      fileReader.readAsArrayBuffer(blobSlice.call(file, start, end))
    }

    loadNext()
  })
}

export default calcFileMd5
