<template>
  <div class="crontab-wrap" :class="{ 'is-compact': compact }">
    <el-tabs type="border-card" class="crontab-tabs">
      <el-tab-pane label="秒" v-if="shouldHide('second')">
        <CrontabSecond
          @update="updateCrontabValue"
          :check="checkNumber"
          :cron="crontabValueObj"
          ref="cronsecond"
        />
      </el-tab-pane>

      <el-tab-pane label="分钟" v-if="shouldHide('min')">
        <CrontabMin
          @update="updateCrontabValue"
          :check="checkNumber"
          :cron="crontabValueObj"
          ref="cronmin"
        />
      </el-tab-pane>

      <el-tab-pane label="小时" v-if="shouldHide('hour')">
        <CrontabHour
          @update="updateCrontabValue"
          :check="checkNumber"
          :cron="crontabValueObj"
          ref="cronhour"
        />
      </el-tab-pane>

      <el-tab-pane label="日" v-if="shouldHide('day')">
        <CrontabDay
          @update="updateCrontabValue"
          :check="checkNumber"
          :cron="crontabValueObj"
          ref="cronday"
        />
      </el-tab-pane>

      <el-tab-pane label="月" v-if="shouldHide('month')">
        <CrontabMonth
          @update="updateCrontabValue"
          :check="checkNumber"
          :cron="crontabValueObj"
          ref="cronmonth"
        />
      </el-tab-pane>

      <el-tab-pane label="周" v-if="shouldHide('week')">
        <CrontabWeek
          @update="updateCrontabValue"
          :check="checkNumber"
          :cron="crontabValueObj"
          ref="cronweek"
        />
      </el-tab-pane>

      <el-tab-pane label="年" v-if="shouldHide('year')">
        <CrontabYear
          @update="updateCrontabValue"
          :check="checkNumber"
          :cron="crontabValueObj"
          ref="cronyear"
        />
      </el-tab-pane>
    </el-tabs>

    <div class="popup-main">
      <div v-if="!compact" class="popup-result">
        <p class="title">时间表达式</p>
        <table>
          <thead>
            <th v-for="item of tabTitles" width="40" :key="item">{{item}}</th>
            <th>Cron 表达式</th>
          </thead>
          <tbody>
            <td>
              <span>{{crontabValueObj.second}}</span>
            </td>
            <td>
              <span>{{crontabValueObj.min}}</span>
            </td>
            <td>
              <span>{{crontabValueObj.hour}}</span>
            </td>
            <td>
              <span>{{crontabValueObj.day}}</span>
            </td>
            <td>
              <span>{{crontabValueObj.month}}</span>
            </td>
            <td>
              <span>{{crontabValueObj.week}}</span>
            </td>
            <td>
              <span>{{crontabValueObj.year}}</span>
            </td>
            <td>
              <span>{{crontabValueString}}</span>
            </td>
          </tbody>
        </table>
      </div>
      <CrontabResult v-if="!hideResult" :ex="crontabValueString"></CrontabResult>
      <!-- 外部可接管“最近运行时间”（如 xxl-job 走后端 nextTriggerTime） -->
      <slot name="runtime" :expression="crontabValueString"></slot>

      <div v-if="!compact" class="pop_btn">
        <el-button size="small" type="primary" @click="submitFill">确定</el-button>
        <el-button size="small" type="warning" @click="clearCron">重置</el-button>
        <el-button size="small" @click="hidePopup">取消</el-button>
      </div>
    </div>
  </div>
</template>

<script>
import CrontabSecond from "./second.vue"
import CrontabMin from "./min.vue"
import CrontabHour from "./hour.vue"
import CrontabDay from "./day.vue"
import CrontabMonth from "./month.vue"
import CrontabWeek from "./week.vue"
import CrontabYear from "./year.vue"
import CrontabResult from "./result.vue"

export default {
  data() {
    return {
      tabTitles: ["秒", "分钟", "小时", "日", "月", "周", "年"],
      tabActive: 0,
      myindex: 0,
      crontabValueObj: {
        second: "*",
        min: "*",
        hour: "*",
        day: "*",
        month: "*",
        week: "?",
        year: "",
      },
    }
  },
  name: "vcrontab",
  props: {
    expression: String,
    hideComponent: Array,
    // 隐藏内置“最近5次运行时间”，改由 slot=runtime 自定义
    hideResult: {
      type: Boolean,
      default: false
    },
    // xxl-job cronGen 紧凑模式：无表达式表/无底部按钮，勾选区 10 列铺满
    compact: {
      type: Boolean,
      default: false
    }
  },
  methods: {
    shouldHide(key) {
      if (this.hideComponent && this.hideComponent.includes(key)) return false
      return true
    },
    resolveExp() {
      // 反解析 表达式
      if (this.expression) {
        let arr = this.expression.split(" ")
        if (arr.length >= 6) {
          //6 位以上是合法表达式
          let obj = {
            second: arr[0],
            min: arr[1],
            hour: arr[2],
            day: arr[3],
            month: arr[4],
            week: arr[5],
            year: arr[6] ? arr[6] : "",
          }
          this.crontabValueObj = {
            ...obj,
          }
          for (let i in obj) {
            if (obj[i]) this.changeRadio(i, obj[i])
          }
        }
      } else {
        // 没有传入的表达式 则还原
        this.clearCron()
      }
    },
    // tab切换值
    tabCheck(index) {
      this.tabActive = index
    },
    // 由子组件触发，更改表达式组成的字段值
    updateCrontabValue(name, value, from) {
      "updateCrontabValue", name, value, from
      this.crontabValueObj[name] = value
      if (from && from !== name) {
        console.log(`来自组件 ${from} 改变了 ${name} ${value}`)
        this.changeRadio(name, value)
      }
    },
    // 赋值到组件
    changeRadio(name, value) {
      let arr = ["second", "min", "hour", "month"],
        refName = "cron" + name,
        insValue

      if (!this.$refs[refName]) return

      if (arr.includes(name)) {
        if (value === "*") {
          insValue = 1
        } else if (value.indexOf("-") > -1) {
          let indexArr = value.split("-")
          isNaN(indexArr[0])
            ? (this.$refs[refName].cycle01 = 0)
            : (this.$refs[refName].cycle01 = indexArr[0])
          this.$refs[refName].cycle02 = indexArr[1]
          insValue = 2
        } else if (value.indexOf("/") > -1) {
          let indexArr = value.split("/")
          isNaN(indexArr[0])
            ? (this.$refs[refName].average01 = 0)
            : (this.$refs[refName].average01 = indexArr[0])
          this.$refs[refName].average02 = indexArr[1]
          insValue = 3
        } else {
          insValue = 4
          // checkbox 的 label 为数字，需与回填类型一致
          this.$refs[refName].checkboxList = value.split(",").map(v => Number(v))
        }
      } else if (name == "day") {
        if (value === "*") {
          insValue = 1
        } else if (value == "?") {
          insValue = 2
        } else if (value.indexOf("-") > -1) {
          let indexArr = value.split("-")
          isNaN(indexArr[0])
            ? (this.$refs[refName].cycle01 = 0)
            : (this.$refs[refName].cycle01 = indexArr[0])
          this.$refs[refName].cycle02 = indexArr[1]
          insValue = 3
        } else if (value.indexOf("/") > -1) {
          let indexArr = value.split("/")
          isNaN(indexArr[0])
            ? (this.$refs[refName].average01 = 0)
            : (this.$refs[refName].average01 = indexArr[0])
          this.$refs[refName].average02 = indexArr[1]
          insValue = 4
        } else if (value.indexOf("W") > -1) {
          let indexArr = value.split("W")
          isNaN(indexArr[0])
            ? (this.$refs[refName].workday = 0)
            : (this.$refs[refName].workday = indexArr[0])
          insValue = 5
        } else if (value === "L") {
          insValue = 6
        } else {
          this.$refs[refName].checkboxList = value.split(",").map(v => Number(v))
          insValue = 7
        }
      } else if (name == "week") {
        if (value === "*") {
          insValue = 1
        } else if (value == "?") {
          insValue = 2
        } else if (value.indexOf("-") > -1) {
          let indexArr = value.split("-")
          isNaN(indexArr[0])
            ? (this.$refs[refName].cycle01 = 0)
            : (this.$refs[refName].cycle01 = indexArr[0])
          this.$refs[refName].cycle02 = indexArr[1]
          insValue = 3
        } else if (value.indexOf("#") > -1) {
          let indexArr = value.split("#")
          isNaN(indexArr[0])
            ? (this.$refs[refName].average01 = 1)
            : (this.$refs[refName].average01 = indexArr[0])
          this.$refs[refName].average02 = indexArr[1]
          insValue = 4
        } else if (value.indexOf("L") > -1) {
          let indexArr = value.split("L")
          isNaN(indexArr[0])
            ? (this.$refs[refName].weekday = 1)
            : (this.$refs[refName].weekday = indexArr[0])
          insValue = 5
        } else {
          this.$refs[refName].checkboxList = value.split(",")
          insValue = 6
        }
      } else if (name == "year") {
        if (value == "") {
          insValue = 1
        } else if (value == "*") {
          insValue = 2
        } else if (value.indexOf("-") > -1) {
          insValue = 3
        } else if (value.indexOf("/") > -1) {
          insValue = 4
        } else {
          this.$refs[refName].checkboxList = value.split(",")
          insValue = 5
        }
      }
      this.$refs[refName].radioValue = insValue
    },
    // 表单选项的子组件校验数字格式（通过-props传递）
    checkNumber(value, minLimit, maxLimit) {
      // 检查必须为整数
      value = Math.floor(value)
      if (value < minLimit) {
        value = minLimit
      } else if (value > maxLimit) {
        value = maxLimit
      }
      return value
    },
    // 隐藏弹窗
    hidePopup() {
      this.$emit("hide")
    },
    // 填充表达式
    submitFill() {
      this.$emit("fill", this.crontabValueString)
      this.hidePopup()
    },
    clearCron() {
      // 还原选择项
      ("准备还原")
      this.crontabValueObj = {
        second: "*",
        min: "*",
        hour: "*",
        day: "*",
        month: "*",
        week: "?",
        year: "",
      }
      for (let j in this.crontabValueObj) {
        this.changeRadio(j, this.crontabValueObj[j])
      }
    },
  },
  computed: {
    crontabValueString: function() {
      let obj = this.crontabValueObj
      let str =
        obj.second +
        " " +
        obj.min +
        " " +
        obj.hour +
        " " +
        obj.day +
        " " +
        obj.month +
        " " +
        obj.week +
        (obj.year == "" ? "" : " " + obj.year)
      return str
    },
  },
  components: {
    CrontabSecond,
    CrontabMin,
    CrontabHour,
    CrontabDay,
    CrontabMonth,
    CrontabWeek,
    CrontabYear,
    CrontabResult,
  },
  watch: {
    expression: "resolveExp",
    hideComponent(value) {
      // 隐藏部分组件
    },
    crontabValueString(val) {
      this.$emit("cron-change", val)
    },
  },
  mounted: function() {
    this.resolveExp()
  },
}
</script>
<style scoped>
.crontab-wrap {
  display: flex;
  flex-direction: column;
}
.crontab-tabs {
  flex: none;
}
.crontab-wrap >>> .el-form-item {
  margin-bottom: 8px;
}
/* 默认（监控中心等）：勾选区可滚动，避免过高 */
.crontab-wrap:not(.is-compact) {
  max-height: 72vh;
}
.crontab-wrap:not(.is-compact) >>> .crontab-tabs > .el-tabs__content {
  height: 220px;
  overflow-y: auto;
  padding: 8px 12px;
  box-sizing: border-box;
}
.crontab-wrap:not(.is-compact) >>> .cron-checkbox-group {
  display: flex;
  flex-wrap: wrap;
  padding-left: 18px;
  line-height: 20px;
}
.crontab-wrap:not(.is-compact) >>> .cron-checkbox-group .el-checkbox {
  margin: 1px 0;
  margin-right: 0;
  width: 46px;
}
/* compact：对齐 xxl-job cronGen，指定项 10 列网格一次铺开 */
.crontab-wrap.is-compact >>> .crontab-tabs > .el-tabs__content {
  padding: 0;
  overflow: visible;
}
.crontab-wrap.is-compact >>> .el-form-item {
  margin-bottom: 10px;
  overflow: visible;
}
.crontab-wrap.is-compact >>> .el-form-item__content {
  overflow: visible;
  line-height: 28px;
}
.crontab-wrap.is-compact >>> .el-radio {
  white-space: normal;
  height: auto;
  line-height: 28px;
}
.crontab-wrap.is-compact >>> .el-radio__label {
  font-size: 14px;
  white-space: normal;
}
.crontab-wrap.is-compact >>> .el-input-number--small {
  width: 110px;
}
.crontab-wrap.is-compact >>> .cron-checkbox-group {
  display: grid;
  grid-template-columns: repeat(10, minmax(0, 1fr));
  gap: 4px 2px;
  padding: 6px 0 0 24px;
  line-height: 22px;
}
.crontab-wrap.is-compact >>> .cron-checkbox-group .el-checkbox {
  margin: 0;
  margin-right: 0;
  width: auto;
  display: flex;
  align-items: center;
}
.crontab-wrap.is-compact >>> .cron-checkbox-group .el-checkbox__label {
  padding-left: 6px;
  font-size: 13px;
}
.crontab-wrap >>> .cron-checkbox-group .el-checkbox__label {
  padding-left: 4px;
  font-size: 13px;
}
.pop_btn {
  text-align: center;
  margin-top: 8px;
  flex-shrink: 0;
}
.popup-main {
  position: relative;
  margin: 6px auto 0;
  background: #fff;
  border-radius: 5px;
  font-size: 14px;
  overflow: hidden;
  flex-shrink: 0;
  text-align: left;
}
.crontab-wrap.is-compact .popup-main {
  overflow: visible;
  margin-top: 0;
}
.popup-title {
  overflow: hidden;
  line-height: 34px;
  padding-top: 6px;
  background: #f2f2f2;
}
.popup-result {
  box-sizing: border-box;
  line-height: 22px;
  margin: 16px auto 6px;
  padding: 10px 8px 6px;
  border: 1px solid #ccc;
  position: relative;
}
.popup-result .title {
  position: absolute;
  top: -22px;
  left: 50%;
  width: 140px;
  font-size: 13px;
  margin-left: -70px;
  text-align: center;
  line-height: 24px;
  background: #fff;
}
.popup-result table {
  text-align: center;
  width: 100%;
  margin: 0 auto;
}
.popup-result table span {
  display: block;
  width: 100%;
  font-family: arial;
  line-height: 28px;
  height: 28px;
  white-space: nowrap;
  overflow: hidden;
  border: 1px solid #e8e8e8;
}
.popup-result-scroll {
  font-size: 12px;
  line-height: 24px;
  height: 10em;
  overflow-y: auto;
}
</style>
