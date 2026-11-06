<template>
  <!-- 对齐 xxl-job-admin 2.3.0 plugins/cronGen/cronGen.js -->
  <div class="xxl-cron-gen">
    <el-input :value="value" placeholder="请输入Cron表达式" @input="$emit('input', $event)">
      <template slot="append">
        <el-popover
          v-model="visible"
          placement="bottom"
          width="425"
          trigger="click"
          popper-class="xxl-cron-popover"
          @show="onShow"
        >
          <div class="cron-gen-main" @click.stop>
            <el-tabs v-model="activeTab" class="cron-gen-tabs">
              <el-tab-pane label="秒" name="second">
                <div class="line"><label><input type="radio" v-model="mode.second" value="1" @change="generate"> 每秒 允许的通配符[, - * /]</label></div>
                <div class="line">
                  <label><input type="radio" v-model="mode.second" value="2" @change="generate"> 周期 从</label>
                  <input class="num" v-model="cfg.second.s0" @change="generate"> -
                  <input class="num" v-model="cfg.second.e0" @change="generate"> 秒
                </div>
                <div class="line">
                  <label><input type="radio" v-model="mode.second" value="3" @change="generate"> 从</label>
                  <input class="num" v-model="cfg.second.s1" @change="generate"> 秒开始,每
                  <input class="num" v-model="cfg.second.e1" @change="generate"> 秒执行一次
                </div>
                <div class="line"><label><input type="radio" v-model="mode.second" value="4" @change="onAppoint('second')"> 指定</label></div>
                <div v-for="(row, ri) in secondRows" :key="'s'+ri" class="imp">
                  <label v-for="n in row" :key="'s'+n" class="ck">
                    <input type="checkbox" :value="n" v-model="checks.second" :disabled="mode.second!=='4'" @change="onCheck('second')">{{ pad(n) }}
                  </label>
                </div>
              </el-tab-pane>

              <el-tab-pane label="分钟" name="min">
                <div class="line"><label><input type="radio" v-model="mode.min" value="1" @change="generate"> 每分钟 允许的通配符[, - * /]</label></div>
                <div class="line">
                  <label><input type="radio" v-model="mode.min" value="2" @change="generate"> 周期 从</label>
                  <input class="num" v-model="cfg.min.s0" @change="generate"> -
                  <input class="num" v-model="cfg.min.e0" @change="generate"> 分钟
                </div>
                <div class="line">
                  <label><input type="radio" v-model="mode.min" value="3" @change="generate"> 从</label>
                  <input class="num" v-model="cfg.min.s1" @change="generate"> 分钟开始,每
                  <input class="num" v-model="cfg.min.e1" @change="generate"> 分钟执行一次
                </div>
                <div class="line"><label><input type="radio" v-model="mode.min" value="4" @change="onAppoint('min')"> 指定</label></div>
                <div v-for="(row, ri) in secondRows" :key="'m'+ri" class="imp">
                  <label v-for="n in row" :key="'m'+n" class="ck">
                    <input type="checkbox" :value="n" v-model="checks.min" :disabled="mode.min!=='4'" @change="onCheck('min')">{{ pad(n) }}
                  </label>
                </div>
              </el-tab-pane>

              <el-tab-pane label="小时" name="hour">
                <div class="line"><label><input type="radio" v-model="mode.hour" value="1" @change="generate"> 每小时 允许的通配符[, - * /]</label></div>
                <div class="line">
                  <label><input type="radio" v-model="mode.hour" value="2" @change="generate"> 周期 从</label>
                  <input class="num" v-model="cfg.hour.s0" @change="generate"> -
                  <input class="num" v-model="cfg.hour.e0" @change="generate"> 小时
                </div>
                <div class="line">
                  <label><input type="radio" v-model="mode.hour" value="3" @change="generate"> 从</label>
                  <input class="num" v-model="cfg.hour.s1" @change="generate"> 小时开始,每
                  <input class="num" v-model="cfg.hour.e1" @change="generate"> 小时执行一次
                </div>
                <div class="line"><label><input type="radio" v-model="mode.hour" value="4" @change="onAppoint('hour')"> 指定</label></div>
                <div v-for="(row, ri) in hourRows" :key="'h'+ri" class="imp">
                  <label v-for="n in row" :key="'h'+n" class="ck">
                    <input type="checkbox" :value="n" v-model="checks.hour" :disabled="mode.hour!=='4'" @change="onCheck('hour')">{{ pad(n) }}
                  </label>
                </div>
              </el-tab-pane>

              <el-tab-pane label="日" name="day">
                <div class="line"><label><input type="radio" v-model="mode.day" value="1" @change="generate"> 每天 允许的通配符[, - * / L W]</label></div>
                <div class="line"><label><input type="radio" v-model="mode.day" value="2" @change="generate"> 不指定</label></div>
                <div class="line">
                  <label><input type="radio" v-model="mode.day" value="3" @change="generate"> 周期 从</label>
                  <input class="num" v-model="cfg.day.s0" @change="generate"> -
                  <input class="num" v-model="cfg.day.e0" @change="generate"> 日
                </div>
                <div class="line">
                  <label><input type="radio" v-model="mode.day" value="4" @change="generate"> 从</label>
                  <input class="num" v-model="cfg.day.s1" @change="generate"> 日开始,每
                  <input class="num" v-model="cfg.day.e1" @change="generate"> 天执行一次
                </div>
                <div class="line">
                  <label><input type="radio" v-model="mode.day" value="5" @change="generate"> 每月</label>
                  <input class="num" v-model="cfg.day.work" @change="generate"> 号最近的那个工作日
                </div>
                <div class="line"><label><input type="radio" v-model="mode.day" value="6" @change="generate"> 本月最后一天</label></div>
                <div class="line"><label><input type="radio" v-model="mode.day" value="7" @change="onAppoint('day')"> 指定</label></div>
                <div v-for="(row, ri) in dayRows" :key="'d'+ri" class="imp">
                  <label v-for="n in row" :key="'d'+n" class="ck">
                    <input type="checkbox" :value="n" v-model="checks.day" :disabled="mode.day!=='7'" @change="onCheck('day')">{{ pad(n) }}
                  </label>
                </div>
              </el-tab-pane>

              <el-tab-pane label="月" name="month">
                <div class="line"><label><input type="radio" v-model="mode.month" value="1" @change="generate"> 每月 允许的通配符[, - * /]</label></div>
                <div class="line"><label><input type="radio" v-model="mode.month" value="2" @change="generate"> 不指定</label></div>
                <div class="line">
                  <label><input type="radio" v-model="mode.month" value="3" @change="generate"> 周期 从</label>
                  <input class="num" v-model="cfg.month.s0" @change="generate"> -
                  <input class="num" v-model="cfg.month.e0" @change="generate"> 月
                </div>
                <div class="line">
                  <label><input type="radio" v-model="mode.month" value="4" @change="generate"> 从</label>
                  <input class="num" v-model="cfg.month.s1" @change="generate"> 月开始,每
                  <input class="num" v-model="cfg.month.e1" @change="generate"> 月执行一次
                </div>
                <div class="line"><label><input type="radio" v-model="mode.month" value="5" @change="onAppoint('month')"> 指定</label></div>
                <div v-for="(row, ri) in monthRows" :key="'mo'+ri" class="imp">
                  <label v-for="n in row" :key="'mo'+n" class="ck">
                    <input type="checkbox" :value="n" v-model="checks.month" :disabled="mode.month!=='5'" @change="onCheck('month')">{{ pad(n) }}
                  </label>
                </div>
              </el-tab-pane>

              <el-tab-pane label="周" name="week">
                <div class="line"><label><input type="radio" v-model="mode.week" value="1" @change="generate"> 每周 允许的通配符[, - * / L #]</label></div>
                <div class="line"><label><input type="radio" v-model="mode.week" value="2" @change="generate"> 不指定</label></div>
                <div class="line">
                  <label><input type="radio" v-model="mode.week" value="3" @change="generate"> 周期 从星期</label>
                  <input class="num" v-model="cfg.week.s0" @change="generate"> -
                  <input class="num" v-model="cfg.week.e0" @change="generate">
                </div>
                <div class="line">
                  <label><input type="radio" v-model="mode.week" value="4" @change="generate"> 第</label>
                  <input class="num" v-model="cfg.week.s1" @change="generate"> 周的星期
                  <input class="num" v-model="cfg.week.e1" @change="generate">
                </div>
                <div class="line">
                  <label><input type="radio" v-model="mode.week" value="5" @change="generate"> 本月最后一个星期</label>
                  <input class="num" v-model="cfg.week.last" @change="generate">
                </div>
                <div class="line"><label><input type="radio" v-model="mode.week" value="6" @change="onAppoint('week')"> 指定</label></div>
                <div class="imp">
                  <label v-for="n in weekList" :key="'w'+n" class="ck">
                    <input type="checkbox" :value="n" v-model="checks.week" :disabled="mode.week!=='6'" @change="onCheck('week')">{{ n }}
                  </label>
                </div>
              </el-tab-pane>

              <el-tab-pane label="年" name="year">
                <div class="line"><label><input type="radio" v-model="mode.year" value="1" @change="generate"> 不指定 允许的通配符[, - * /] 非必填</label></div>
                <div class="line"><label><input type="radio" v-model="mode.year" value="2" @change="generate"> 每年</label></div>
                <div class="line">
                  <label><input type="radio" v-model="mode.year" value="3" @change="generate"> 周期从</label>
                  <input class="num year" v-model="cfg.year.s0" @change="generate"> -
                  <input class="num year" v-model="cfg.year.e0" @change="generate"> 年
                </div>
              </el-tab-pane>
            </el-tabs>

            <div class="cron-runtime">
              <label>最近运行时间:</label>
              <textarea rows="6" readonly :value="runTimeText"></textarea>
            </div>
          </div>
          <el-button slot="reference" icon="el-icon-edit" />
        </el-popover>
      </template>
    </el-input>
  </div>
</template>

<script>
import { nextTriggerTime } from '@/api/xxljob/jobinfo'

function chunk(arr, size) {
  const rows = []
  for (let i = 0; i < arr.length; i += size) rows.push(arr.slice(i, i + size))
  return rows
}

function range(start, end) {
  const list = []
  for (let i = start; i <= end; i++) list.push(i)
  return list
}

export default {
  name: 'XxlCronGen',
  props: {
    value: { type: String, default: '' }
  },
  data() {
    const y = new Date().getFullYear()
    return {
      visible: false,
      activeTab: 'second',
      timer: null,
      runTimeText: '',
      hidden: { second: '*', min: '*', hour: '*', day: '*', month: '*', week: '?', year: '' },
      mode: { second: '1', min: '1', hour: '1', day: '1', month: '1', week: '2', year: '1' },
      cfg: {
        second: { s0: '1', e0: '2', s1: '0', e1: '1' },
        min: { s0: '1', e0: '2', s1: '0', e1: '1' },
        hour: { s0: '1', e0: '2', s1: '0', e1: '1' },
        day: { s0: '1', e0: '2', s1: '1', e1: '1', work: '1' },
        month: { s0: '1', e0: '2', s1: '1', e1: '1' },
        week: { s0: '1', e0: '2', s1: '1', e1: '1', last: '1' },
        year: { s0: String(y), e0: String(y + 1) }
      },
      checks: { second: [], min: [], hour: [], day: [], month: [], week: [] },
      secondRows: chunk(range(0, 59), 10),
      hourRows: chunk(range(0, 23), 6),
      dayRows: chunk(range(1, 31), 10),
      monthRows: [range(1, 6), range(7, 12)],
      weekList: range(1, 7)
    }
  },
  beforeDestroy() {
    if (this.timer) clearTimeout(this.timer)
  },
  methods: {
    pad(n) {
      return n < 10 ? '0' + n : String(n)
    },
    onShow() {
      this.parse(this.value || '')
      this.refreshRunTime()
    },
    onAppoint(field) {
      if (!this.checks[field].length) this.checks[field] = [field === 'day' || field === 'month' || field === 'week' ? 1 : 0]
      this.onCheck(field)
    },
    onCheck(field) {
      const vals = this.checks[field].slice().sort((a, b) => a - b)
      const maxMap = { second: 60, min: 60, hour: 24, day: 31, month: 12, week: 7 }
      if (!vals.length) {
        this.hidden[field] = (field === 'day' || field === 'week') ? '?' : '*'
      } else if (vals.length === maxMap[field]) {
        this.hidden[field] = '*'
      } else {
        this.hidden[field] = vals.join(',')
      }
      this.emitCron()
    },
    clearChecks(field) {
      this.checks[field] = []
    },
    applyMode(field) {
      const m = this.mode[field]
      const c = this.cfg[field]
      if (field === 'second' || field === 'min' || field === 'hour') {
        if (m === '1') { this.hidden[field] = '*'; this.clearChecks(field) }
        else if (m === '2') { this.hidden[field] = c.s0 + '-' + c.e0; this.clearChecks(field) }
        else if (m === '3') { this.hidden[field] = c.s1 + '/' + c.e1; this.clearChecks(field) }
        else if (m === '4') { this.onCheck(field); return }
      } else if (field === 'day') {
        if (m === '1') { this.hidden.day = '*'; this.clearChecks('day') }
        else if (m === '2') { this.hidden.day = '?'; this.clearChecks('day') }
        else if (m === '3') { this.hidden.day = c.s0 + '-' + c.e0; this.clearChecks('day') }
        else if (m === '4') { this.hidden.day = c.s1 + '/' + c.e1; this.clearChecks('day') }
        else if (m === '5') { this.hidden.day = c.work + 'W'; this.clearChecks('day') }
        else if (m === '6') { this.hidden.day = 'L'; this.clearChecks('day') }
        else if (m === '7') { this.onCheck('day'); return }
      } else if (field === 'month') {
        if (m === '1') { this.hidden.month = '*'; this.clearChecks('month') }
        else if (m === '2') { this.hidden.month = '?'; this.clearChecks('month') }
        else if (m === '3') { this.hidden.month = c.s0 + '-' + c.e0; this.clearChecks('month') }
        else if (m === '4') { this.hidden.month = c.s1 + '/' + c.e1; this.clearChecks('month') }
        else if (m === '5') { this.onCheck('month'); return }
      } else if (field === 'week') {
        if (m === '1') { this.hidden.week = '*'; this.clearChecks('week') }
        else if (m === '2') { this.hidden.week = '?'; this.clearChecks('week') }
        else if (m === '3') { this.hidden.week = c.s0 + '-' + c.e0; this.clearChecks('week') }
        else if (m === '4') { this.hidden.week = c.s1 + '/' + c.e1; this.clearChecks('week') }
        else if (m === '5') { this.hidden.week = c.last + 'L'; this.clearChecks('week') }
        else if (m === '6') { this.onCheck('week'); return }
      } else if (field === 'year') {
        if (m === '1') this.hidden.year = ''
        else if (m === '2') this.hidden.year = '*'
        else if (m === '3') this.hidden.year = c.s0 + '-' + c.e0
      }
      this.emitCron()
    },
    generate() {
      this.applyMode(this.activeTab)
    },
    emitCron() {
      const h = this.hidden
      const second = h.second || '*'
      const min = h.min || '*'
      const hour = h.hour || '*'
      const day = h.day || '*'
      const month = h.month || '*'
      const week = h.week || '?'
      const year = h.year
      const result = year !== '' ? [second, min, hour, day, month, week, year].join(' ') : [second, min, hour, day, month, week].join(' ')
      this.$emit('input', result)
      if (this.timer) clearTimeout(this.timer)
      this.timer = setTimeout(() => this.refreshRunTime(), 300)
    },
    refreshRunTime() {
      const scheduleConf = this.value
      if (!scheduleConf || !String(scheduleConf).trim()) {
        this.runTimeText = ''
        return
      }
      nextTriggerTime({ scheduleType: 'CRON', scheduleConf }).then(res => {
        const list = res.data || []
        this.runTimeText = list.length ? list.join('\n') : (res.msg || '')
      }).catch(err => {
        this.runTimeText = (err && (err.msg || err.message)) || '计算失败'
      })
    },
    parse(expression) {
      if (!expression) return
      const regs = expression.trim().split(/\s+/)
      if (regs.length < 6) return
      this.hidden.second = regs[0]
      this.hidden.min = regs[1]
      this.hidden.hour = regs[2]
      this.hidden.day = regs[3]
      this.hidden.month = regs[4]
      this.hidden.week = regs[5]
      this.hidden.year = regs[6] || ''
      this.initObj(regs[0], 'second')
      this.initObj(regs[1], 'min')
      this.initObj(regs[2], 'hour')
      this.initDay(regs[3])
      this.initMonth(regs[4])
      this.initWeek(regs[5])
      if (regs[6]) this.initYear(regs[6])
    },
    initObj(val, field) {
      if (val === '*') this.mode[field] = '1'
      else if (val.indexOf('-') > -1) {
        const a = val.split('-')
        this.mode[field] = '2'
        this.cfg[field].s0 = a[0]
        this.cfg[field].e0 = a[1]
      } else if (val.indexOf('/') > -1) {
        const a = val.split('/')
        this.mode[field] = '3'
        this.cfg[field].s1 = a[0]
        this.cfg[field].e1 = a[1]
      } else {
        this.mode[field] = '4'
        this.checks[field] = val === '?' ? [] : val.split(',').map(v => Number(v))
      }
    },
    initDay(val) {
      if (val === '*') this.mode.day = '1'
      else if (val === '?') this.mode.day = '2'
      else if (val.indexOf('-') > -1) {
        const a = val.split('-'); this.mode.day = '3'; this.cfg.day.s0 = a[0]; this.cfg.day.e0 = a[1]
      } else if (val.indexOf('/') > -1) {
        const a = val.split('/'); this.mode.day = '4'; this.cfg.day.s1 = a[0]; this.cfg.day.e1 = a[1]
      } else if (val.indexOf('W') > -1) {
        this.mode.day = '5'; this.cfg.day.work = val.split('W')[0]
      } else if (val === 'L') this.mode.day = '6'
      else {
        this.mode.day = '7'
        this.checks.day = val.split(',').map(v => Number(v))
      }
    },
    initMonth(val) {
      if (val === '*') this.mode.month = '1'
      else if (val === '?') this.mode.month = '2'
      else if (val.indexOf('-') > -1) {
        const a = val.split('-'); this.mode.month = '3'; this.cfg.month.s0 = a[0]; this.cfg.month.e0 = a[1]
      } else if (val.indexOf('/') > -1) {
        const a = val.split('/'); this.mode.month = '4'; this.cfg.month.s1 = a[0]; this.cfg.month.e1 = a[1]
      } else {
        this.mode.month = '5'
        this.checks.month = val.split(',').map(v => Number(v))
      }
    },
    initWeek(val) {
      if (val === '*') this.mode.week = '1'
      else if (val === '?') this.mode.week = '2'
      else if (val.indexOf('/') > -1) {
        const a = val.split('/'); this.mode.week = '4'; this.cfg.week.s1 = a[0]; this.cfg.week.e1 = a[1]
      } else if (val.indexOf('-') > -1) {
        const a = val.split('-'); this.mode.week = '3'; this.cfg.week.s0 = a[0]; this.cfg.week.e0 = a[1]
      } else if (val.indexOf('L') > -1) {
        this.mode.week = '5'; this.cfg.week.last = val.split('L')[0]
      } else {
        this.mode.week = '6'
        this.checks.week = val.split(',').map(v => Number(v))
      }
    },
    initYear(val) {
      if (val === '*') this.mode.year = '2'
      else if (val.indexOf('-') > -1) {
        const a = val.split('-'); this.mode.year = '3'; this.cfg.year.s0 = a[0]; this.cfg.year.e0 = a[1]
      } else this.mode.year = '1'
    }
  },
  watch: {
    activeTab() {
      // 切换 tab 时按当前模式刷新表达式（对齐 cronGen generate）
      this.generate()
    }
  }
}
</script>

<style scoped>
.cron-gen-main {
  width: 410px;
  min-height: 420px;
  box-sizing: border-box;
  padding: 4px 8px 8px;
  font-size: 12px;
  color: #333;
  text-align: left;
}
.line {
  line-height: 24px;
  margin-bottom: 2px;
  white-space: nowrap;
}
.line label {
  font-weight: normal;
  margin: 0;
  cursor: pointer;
}
.num {
  width: 35px;
  height: 20px;
  text-align: center;
  margin: 0 3px;
  border: 1px solid #ccc;
  border-radius: 2px;
  vertical-align: middle;
}
.num.year {
  width: 45px;
}
.imp {
  line-height: 22px;
  white-space: nowrap;
}
.ck {
  display: inline-block;
  margin: 0;
  margin-left: 5px;
  font-weight: normal;
  cursor: pointer;
}
.ck input {
  margin: 0 2px 0 0;
  vertical-align: middle;
}
.cron-runtime {
  margin-top: 10px;
  text-align: left;
}
.cron-runtime label {
  display: block;
  font-weight: 600;
  margin-bottom: 4px;
}
.cron-runtime textarea {
  width: 90%;
  resize: none;
  background: none;
  border: none;
  outline: none;
  font-size: 12px;
  line-height: 1.6;
  color: #333;
  padding: 0;
  text-align: left;
}
</style>

<style>
.xxl-cron-popover {
  max-width: 500px !important;
  width: 425px;
  padding: 8px 4px;
  z-index: 4000 !important;
}
.xxl-cron-popover .cron-gen-tabs > .el-tabs__header {
  margin: 0 0 8px;
}
.xxl-cron-popover .cron-gen-tabs > .el-tabs__header .el-tabs__item {
  height: 32px;
  line-height: 32px;
  font-size: 12px;
  padding: 0 12px;
}
.xxl-cron-popover .cron-gen-tabs > .el-tabs__content {
  padding: 0;
  overflow: visible;
}
.xxl-cron-gen .el-input-group__append {
  padding: 0;
  background: #fff;
}
.xxl-cron-gen .el-input-group__append .el-button {
  margin: 0;
  border: none;
  border-radius: 0 4px 4px 0;
  height: 32px;
  padding: 0 12px;
}
</style>
