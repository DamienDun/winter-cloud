<template>
	<el-form size="small">
		<el-form-item>
			<el-radio v-model='radioValue' :label="1">
				每秒 允许的通配符[, - * /]
			</el-radio>
		</el-form-item>

		<el-form-item>
			<el-radio v-model='radioValue' :label="2">
				周期 从
				<el-input-number v-model='cycle01' :min="0" :max="58" /> -
				<el-input-number v-model='cycle02' :min="cycle01 ? cycle01 + 1 : 1" :max="59" /> 秒
			</el-radio>
		</el-form-item>

		<el-form-item>
			<el-radio v-model='radioValue' :label="3">
				从
				<el-input-number v-model='average01' :min="0" :max="58" /> 秒开始，每
				<el-input-number v-model='average02' :min="1" :max="59 - average01 || 0" /> 秒执行一次
			</el-radio>
		</el-form-item>

		<el-form-item>
			<el-radio v-model='radioValue' :label="4">指定</el-radio>
			<!-- 平铺 0-59，对齐 xxl-job cronGen，避免下拉多选 -->
			<el-checkbox-group v-model="checkboxList" class="cron-checkbox-group">
				<el-checkbox v-for="item in 60" :key="item" :label="item - 1">{{ item - 1 < 10 ? '0' + (item - 1) : item - 1 }}</el-checkbox>
			</el-checkbox-group>
		</el-form-item>
	</el-form>
</template>

<script>
export default {
	data() {
		return {
			radioValue: 1,
			cycle01: 1,
			cycle02: 2,
			average01: 0,
			average02: 1,
			checkboxList: [],
			checkNum: this.$options.propsData.check
		}
	},
	name: 'crontab-second',
	props: ['check', 'radioParent'],
	methods: {
		// 单选按钮值变化时
		radioChange() {
			switch (this.radioValue) {
				case 1:
					this.$emit('update', 'second', '*', 'second')
					break
				case 2:
					this.$emit('update', 'second', this.cycleTotal)
					break
				case 3:
					this.$emit('update', 'second', this.averageTotal)
					break
				case 4:
					this.$emit('update', 'second', this.checkboxString)
					break
			}
		},
		// 周期两个值变化时
		cycleChange() {
			if (this.radioValue == '2') {
				this.$emit('update', 'second', this.cycleTotal)
			}
		},
		// 平均两个值变化时
		averageChange() {
			if (this.radioValue == '3') {
				this.$emit('update', 'second', this.averageTotal)
			}
		},
		// checkbox值变化时：勾选即切到“指定”
		checkboxChange() {
			this.radioValue = 4
			this.$emit('update', 'second', this.checkboxString)
		}
	},
	watch: {
		'radioValue': 'radioChange',
		'cycleTotal': 'cycleChange',
		'averageTotal': 'averageChange',
		'checkboxString': 'checkboxChange',
		radioParent() {
			this.radioValue = this.radioParent
		}
	},
	computed: {
		// 计算两个周期值
		cycleTotal: function () {
			const cycle01 = this.checkNum(this.cycle01, 0, 58)
			const cycle02 = this.checkNum(this.cycle02, cycle01 ? cycle01 + 1 : 1, 59)
			return cycle01 + '-' + cycle02
		},
		// 计算平均用到的值
		averageTotal: function () {
			const average01 = this.checkNum(this.average01, 0, 58)
			const average02 = this.checkNum(this.average02, 1, 59 - average01 || 0)
			return average01 + '/' + average02
		},
		// 计算勾选的checkbox值合集
		checkboxString: function () {
			let str = this.checkboxList.join()
			return str == '' ? '*' : str
		}
	}
}
</script>

<style scoped>
.cron-checkbox-group {
	display: flex;
	flex-wrap: wrap;
	padding-left: 22px;
	line-height: 22px;
}
.cron-checkbox-group >>> .el-checkbox {
	margin: 2px 0;
	margin-right: 0;
	min-width: 48px;
}
.cron-checkbox-group >>> .el-checkbox__label {
	padding-left: 6px;
	font-size: 13px;
}
</style>
