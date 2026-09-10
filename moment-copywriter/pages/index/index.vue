<template>
	<view class="app-page home-page">
		<view class="top-space"></view>

		<view class="title-wrap">
			<text class="main-title">AI文案生成器</text>
		</view>

		<scroll-view class="category-scroll" scroll-x>
			<view class="category-row">
				<button
					v-for="item in categories"
					:key="item.name"
					class="category-chip"
					:class="{ active: category === item.name }"
					@tap="chooseCategory(item)"
				>
					{{ item.name }}
				</button>
			</view>
		</scroll-view>

		<view class="surface-card input-card">
			<textarea
				class="textarea prompt-input"
				v-model="scene"
				:placeholder="placeholder"
				maxlength="220"
			></textarea>

			<button
				class="primary-button generate-button"
				:loading="loading"
				:disabled="loading"
				@tap="generateCopywriting"
			>
				{{ loading ? '生成中' : '生成文案' }}
			</button>
		</view>

		<view class="surface-card hint-card">
			<text class="hint-title">试试这些内容</text>
			<view class="example-list">
				<button
					v-for="item in examples"
					:key="item"
					class="example-item"
					@tap="useExample(item)"
				>
					{{ item }}
				</button>
			</view>
		</view>

		<view v-if="detailRecord" class="detail-mask" @tap="closeDetail">
			<view class="detail-panel" @tap.stop>
				<view class="detail-header">
					<text class="detail-title">文案详情</text>
					<button class="detail-close" @tap.stop="closeDetail">×</button>
				</view>

				<scroll-view class="detail-body" scroll-y>
					<view class="detail-section">
						<text class="detail-label">生成要求</text>
						<text class="detail-content">{{ detailRecord.displayScene || '无' }}</text>
					</view>

					<view class="detail-meta" v-if="detailRecord.displayStyle || detailRecord.displayKeywords">
						<view class="detail-field" v-if="detailRecord.displayStyle">
							<text class="detail-field-label">风格</text>
							<text class="detail-field-value">{{ detailRecord.displayStyle }}</text>
						</view>
						<view class="detail-field" v-if="detailRecord.displayKeywords">
							<text class="detail-field-label">关键词</text>
							<text class="detail-field-value">{{ detailRecord.displayKeywords }}</text>
						</view>
					</view>

					<view class="detail-section">
						<text class="detail-label">生成内容</text>
						<text class="detail-content">{{ detailRecord.displayGeneratedContent || '无' }}</text>
					</view>

					<view class="detail-section">
						<text class="detail-label">继续优化</text>
						<textarea
							class="optimize-input"
							v-model="optimizeMessage"
							placeholder="例如：再温柔一点，少一点正式感"
							maxlength="180"
						></textarea>
						<button
							class="optimize-button"
							:loading="optimizing"
							:disabled="optimizing"
							@tap.stop="optimizeCopywriting"
						>
							{{ optimizing ? '优化中' : '发送优化要求' }}
						</button>
					</view>
				</scroll-view>

				<view class="detail-actions">
					<button class="detail-action" @tap.stop="copyResult">复制</button>
					<button
						class="detail-action favorite"
						:class="{ active: detailRecord.favorite }"
						@tap.stop="toggleCurrentFavorite"
					>
						{{ detailRecord.favorite ? '取消收藏' : '收藏' }}
					</button>
				</view>
			</view>
		</view>

		<app-tabbar active="home"></app-tabbar>
	</view>
</template>

<script>
	import AppTabbar from '../../components/app-tabbar/app-tabbar.vue'
	import { get, post } from '../../common/request.js'
	import { ensureLogin, isLoggedIn, loadCurrentUser } from '../../common/auth.js'
	import { isFavorite, toggleFavorite } from '../../common/favorites.js'

	export default {
		components: {
			AppTabbar
		},
		data() {
			return {
				category: '节日祝福',
				scene: '',
				result: '',
				recordId: 0,
				recordKeywords: '',
				loading: false,
				optimizing: false,
				optimizeMessage: '',
				favorite: false,
				detailRecord: null,
				detailSteps: [],
				userTags: [],
				categories: [
					{
						name: '朋友圈文案',
						placeholder: '输入想发布的场景，例如：傍晚散步，晚风轻轻吹过，心情很好'
					},
					{
						name: '节日祝福',
						placeholder: '输入祝福对象、想要的风格，例如：给朋友的节日祝福，温柔简短'
					},
					{
						name: '自我介绍',
						placeholder: '输入你的身份、特点和用途，例如：工作或学习场景，真诚自然'
					},
					{
						name: '演讲稿',
						placeholder: '输入主题、场合和时长，例如：一次分享，主题是坚持'
					},
					{
						name: '短视频配文',
						placeholder: '输入视频内容和情绪，例如：日常 vlog，轻松治愈'
					},
					{
						name: '治愈短句',
						placeholder: '输入情绪或关键词，例如：最近很累，想要一点鼓励'
					}
				]
			}
		},
		computed: {
			placeholder() {
				const current = this.categories.find(item => item.name === this.category)
				return current ? current.placeholder : '请输入你的需求'
			},
			examples() {
				return this.buildExamples()
			},
			currentRecord() {
				const keywords = this.recordKeywords || this.buildDisplayKeywords()
				return {
					id: this.recordId,
					recordId: this.recordId,
					style: this.category,
					scene: this.scene,
					keywords,
					generatedContent: this.result,
					content: this.result,
					favorite: this.favorite,
					displayScene: this.scene,
					displayStyle: this.category,
					displayKeywords: keywords,
					displayGeneratedContent: this.result
				}
			}
		},
		onShow() {
			this.refreshUserTags()
		},
		methods: {
			chooseCategory(item) {
				this.category = item.name
			},
			generateCopywriting() {
				this.requireLogin().then(loggedIn => {
					if (!loggedIn) {
						return
					}

					this.submitGenerateCopywriting()
				})
			},
			requireLogin() {
				if (isLoggedIn()) {
					return Promise.resolve(true)
				}

				return loadCurrentUser().then(user => {
					if (user) {
						return true
					}

					ensureLogin()
					return false
				}).catch(() => {
					ensureLogin()
					return false
				})
			},
			submitGenerateCopywriting() {
				if (!this.scene.trim()) {
					uni.showToast({
						title: '请输入文案需求',
						icon: 'none'
					})
					return
				}

				this.loading = true
				this.recordId = 0
				this.recordKeywords = ''
				this.favorite = false
				this.detailRecord = null
				this.detailSteps = []
				this.optimizeMessage = ''

				const scene = this.scene.trim()
				const keywords = this.buildGenerateKeywords(scene)
				post('/api/copywriting/generate', {
					scene,
					mood: this.category,
					style: this.category,
					keywords
				}).then(data => {
					this.result = data && data.content ? data.content : ''
					this.recordId = data && data.recordId ? data.recordId : 0
					this.recordKeywords = data && data.keywords ? data.keywords : this.buildDisplayKeywords(scene)
					this.favorite = isFavorite(data)
					this.detailSteps = this.recordId > 0 ? [{
						recordId: this.recordId,
						stepNo: 1,
						userMessage: scene,
						generatedContent: this.result
					}] : []
					this.loading = false
					this.openDetail()
				}).catch(message => {
					this.loading = false
					uni.showToast({
						title: String(message),
						icon: 'none'
					})
				})
			},
			copyResult() {
				if (!this.result) {
					return
				}

				uni.setClipboardData({
					data: this.result
				})
			},
			openDetail() {
				if (!this.result) {
					return
				}

				this.detailRecord = Object.assign({}, this.currentRecord)
			},
			closeDetail() {
				this.detailRecord = null
			},
			optimizeCopywriting() {
				if (!this.recordId) {
					uni.showToast({
						title: '缺少文案记录ID，无法优化',
						icon: 'none'
					})
					return
				}

				const message = this.optimizeMessage.trim()
				if (!message) {
					uni.showToast({
						title: '请输入优化要求',
						icon: 'none'
					})
					return
				}

				this.optimizing = true
				post('/api/copywriting/optimize', {
					recordId: this.recordId,
					message
				}).then(data => {
					this.result = data && data.content ? data.content : this.result
					this.detailSteps = data && Array.isArray(data.steps) ? data.steps : this.detailSteps
					this.optimizeMessage = ''
					this.optimizing = false
					this.openDetail()
				}).catch(error => {
					this.optimizing = false
					uni.showToast({
						title: String(error),
						icon: 'none'
					})
				})
			},
			toggleCurrentFavorite() {
				if (!this.result) {
					return
				}

				toggleFavorite(Object.assign({}, this.currentRecord, {
					favorite: this.favorite
				})).then(favorite => {
					this.favorite = favorite
					if (this.detailRecord) {
						this.detailRecord = Object.assign({}, this.detailRecord, {
							favorite
						})
					}
					uni.showToast({
						title: this.favorite ? '已收藏' : '已取消',
						icon: 'none'
					})
				}).catch(message => {
					uni.showToast({
						title: String(message),
						icon: 'none'
					})
				})
			},
			useExample(item) {
				this.scene = item
			},
			refreshUserTags() {
				this.userTags = []
				if (isLoggedIn()) {
					this.loadUserTags()
					return
				}

				loadCurrentUser().then(user => {
					if (user) {
						this.loadUserTags()
					}
				}).catch(() => {})
			},
			loadUserTags() {
				get('/api/user-tags').then(data => {
					this.userTags = Array.isArray(data) ? data : []
				}).catch(() => {
					this.userTags = []
				})
			},
			buildGenerateKeywords(scene) {
				const text = String(scene === undefined ? this.scene : scene).trim()
				const time = this.currentTimeContext()
				const parts = [
					text,
					'当前时间：' + time.label
				]

				return parts.filter(Boolean).join('；')
			},
			buildDisplayKeywords(scene) {
				const keywords = this.buildGenerateKeywords(scene)
				if (!this.userTags.length) {
					return keywords
				}

				return keywords + '；用户标签：' + this.userTags.join('、')
			},
			currentTimeContext() {
				const now = new Date()
				const month = now.getMonth() + 1
				const day = now.getDate()
				const hour = now.getHours()
				const part = hour < 6 ? '凌晨' : hour < 12 ? '上午' : hour < 14 ? '中午' : hour < 18 ? '下午' : '晚上'
				const label = month + '月' + day + '日' + part

				return {
					label,
					season: this.seasonName(month),
					festival: this.festivalName(month, day)
				}
			},
			seasonName(month) {
				if (month >= 3 && month <= 5) {
					return '春天'
				}

				if (month >= 6 && month <= 8) {
					return '夏天'
				}

				if (month >= 9 && month <= 11) {
					return '秋天'
				}

				return '冬天'
			},
			festivalName(month, day) {
				const key = (month < 10 ? '0' + month : month) + '-' + (day < 10 ? '0' + day : day)
				const festivals = {
					'01-01': '元旦',
					'03-08': '妇女节',
					'04-05': '清明节',
					'05-01': '劳动节',
					'06-01': '儿童节',
					'09-10': '教师节',
					'10-01': '国庆节',
					'12-25': '圣诞节'
				}

				return festivals[key] || ''
			},
			userProfile() {
				const tagText = this.userTags.join(' ')

				if (/老师|教师|班主任|辅导员/.test(tagText)) {
					return {
						identity: '老师',
						daily: '课后备课或校园日常',
						blessingTarget: '同事或学生家长',
						intro: '教师公开课或家长会',
						speech: '班会或教学分享',
						video: '课堂日常或校园记录',
						healing: '忙碌教学后给自己的鼓励'
					}
				}

				if (/学生|大学生|研究生|高中生|初中生/.test(tagText)) {
					return {
						identity: '学生',
						daily: '学习生活或校园日常',
						blessingTarget: '老师或同学',
						intro: '新班级或社团面试',
						speech: '班会分享或竞选发言',
						video: '校园生活 vlog',
						healing: '学习压力下给自己的鼓励'
					}
				}

				if (/程序员|开发|工程师|产品|设计/.test(tagText)) {
					return {
						identity: '职场人',
						daily: '工作间隙或项目完成后的日常',
						blessingTarget: '同事或朋友',
						intro: '面试或团队入职',
						speech: '项目复盘或工作分享',
						video: '工作日常 vlog',
						healing: '工作结束后给自己的放松短句'
					}
				}

				if (/妈妈|爸爸|宝妈|家长/.test(tagText)) {
					return {
						identity: '家长',
						daily: '陪伴家人或亲子日常',
						blessingTarget: '家人或朋友',
						intro: '家长会或社群介绍',
						speech: '亲子活动分享',
						video: '亲子生活 vlog',
						healing: '照顾家庭后的温柔鼓励'
					}
				}

				const identity = this.userTags.length ? this.userTags[0] : '你'
				return {
					identity,
					daily: '今天的日常生活',
					blessingTarget: '身边的人',
					intro: '日常自我介绍',
					speech: '一次简短分享',
					video: '今天的生活片段',
					healing: '给自己的鼓励'
				}
			},
			buildExamples() {
				const time = this.currentTimeContext()
				const profile = this.userProfile()

				if (this.category === '朋友圈文案') {
					return [
						time.label + '，记录' + profile.daily + '，适合' + profile.identity + '发朋友圈',
						time.season + '里的一个小瞬间，写得自然一点',
						'结合我的标签' + this.userTagText() + '，写一条今天适合发的朋友圈'
					]
				}

				if (this.category === '节日祝福') {
					const festival = time.festival
					const blessingTheme = festival ? festival + '祝福' : time.label + '适合发送的日常祝福'
					return [
						'给' + profile.blessingTarget + '的' + blessingTheme + '，符合' + profile.identity + '身份',
						'结合我的标签' + this.userTagText() + '，写一段不过时的祝福',
						time.season + '里给重要的人一段温柔祝福'
					]
				}

				if (this.category === '自我介绍') {
					return [
						profile.intro + '自我介绍，突出我的标签' + this.userTagText(),
						'结合' + profile.identity + '身份，写一段真诚自然的自我介绍',
						'适合今天使用的简短自我介绍，不夸张'
					]
				}

				if (this.category === '演讲稿') {
					return [
						profile.speech + '，结合' + time.label + '，三分钟',
						'围绕我的标签' + this.userTagText() + '，写一段积极诚恳的演讲',
						time.season + '主题分享，语气自然有感染力'
					]
				}

				if (this.category === '短视频配文') {
					return [
						profile.video + '，结合' + time.label + '，轻松自然',
						'根据我的标签' + this.userTagText() + '，写一条短视频配文',
						time.season + '氛围的日常 vlog 配文'
					]
				}

				return [
					profile.healing + '，结合' + time.label,
					'根据我的标签' + this.userTagText() + '，写一句温柔短句',
					time.season + '适合收藏的治愈短句'
				]
			},
			userTagText() {
				return this.userTags.length ? '：' + this.userTags.join('、') : '和当前身份'
			}
		}
	}
</script>

<style>
	.top-space {
		height: 54rpx;
	}

	.title-wrap {
		margin-bottom: 48rpx;
		text-align: center;
	}

	.main-title {
		color: #030303;
		font-size: 60rpx;
		font-weight: 900;
		line-height: 1.2;
		letter-spacing: 0;
	}

	.category-scroll {
		width: 100%;
		white-space: nowrap;
		margin-bottom: 36rpx;
	}

	.category-row {
		display: flex;
		width: max-content;
		padding-right: 40rpx;
	}

	.category-chip {
		height: 58rpx;
		margin-right: 16rpx;
		padding: 0 24rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		border: 1rpx solid #DADDE4;
		border-radius: 29rpx;
		background: #FFFFFF;
		color: #272727;
		font-size: 28rpx;
		white-space: nowrap;
	}

	.category-chip.active {
		border-color: #006DF0;
		background: #0878F7;
		color: #FFFFFF;
		font-weight: 800;
	}

	.input-card {
		padding: 44rpx 40rpx 42rpx;
		margin-bottom: 40rpx;
	}

	.prompt-input {
		margin-bottom: 44rpx;
		padding: 0;
		background: #FFFFFF;
	}

	.generate-button {
		height: 88rpx;
	}

	.hint-card {
		padding: 34rpx 34rpx 24rpx;
	}

	.hint-title {
		display: block;
		margin-bottom: 22rpx;
		color: #111111;
		font-size: 32rpx;
		font-weight: 800;
	}

	.example-item {
		width: 100%;
		min-height: 76rpx;
		padding: 18rpx 24rpx;
		display: block;
		box-sizing: border-box;
		border-bottom: 1rpx solid #F0F1F4;
		color: #555555;
		font-size: 28rpx;
		line-height: 1.45;
		text-align: left;
	}

	.example-item:last-child {
		border-bottom: none;
	}

	.detail-mask {
		position: fixed;
		left: 0;
		right: 0;
		top: 0;
		bottom: 0;
		z-index: 40;
		display: flex;
		align-items: flex-end;
		background: rgba(0, 0, 0, 0.38);
	}

	.detail-panel {
		position: relative;
		width: 100%;
		max-height: 84vh;
		padding: 34rpx 36rpx calc(34rpx + env(safe-area-inset-bottom));
		border-radius: 24rpx 24rpx 0 0;
		background: #FFFFFF;
		box-sizing: border-box;
	}

	.detail-header {
		display: flex;
		align-items: center;
		padding-right: 82rpx;
		margin-bottom: 24rpx;
	}

	.detail-title {
		color: #0A0A0A;
		font-size: 38rpx;
		font-weight: 800;
	}

	.detail-close {
		position: absolute;
		top: 28rpx;
		right: 32rpx;
		width: 58rpx;
		height: 58rpx;
		margin: 0;
		padding: 0;
		display: flex;
		align-items: center;
		justify-content: center;
		border-radius: 50%;
		background: #F2F4F7;
		color: #333333;
		font-size: 38rpx;
		line-height: 1;
	}

	.detail-body {
		height: 56vh;
	}

	.detail-section {
		margin-bottom: 30rpx;
	}

	.detail-label {
		display: block;
		margin-bottom: 14rpx;
		color: #555555;
		font-size: 26rpx;
	}

	.detail-content {
		display: block;
		color: #111111;
		font-size: 30rpx;
		line-height: 1.55;
		white-space: pre-wrap;
		word-break: break-word;
	}

	.optimize-input {
		width: 100%;
		height: 136rpx;
		padding: 18rpx 20rpx;
		box-sizing: border-box;
		border-radius: 10rpx;
		background: #F7FAFF;
		color: #111111;
		font-size: 28rpx;
		line-height: 1.45;
	}

	.optimize-button {
		width: 100%;
		height: 74rpx;
		margin-top: 18rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		border-radius: 10rpx;
		background: #0878F7;
		color: #FFFFFF;
		font-size: 28rpx;
	}

	.detail-meta {
		margin-bottom: 30rpx;
		padding: 22rpx 24rpx;
		border-radius: 12rpx;
		background: #F7FAFF;
	}

	.detail-field {
		margin-bottom: 14rpx;
	}

	.detail-field:last-child {
		margin-bottom: 0;
	}

	.detail-field-label {
		margin-right: 18rpx;
		color: #666666;
		font-size: 26rpx;
	}

	.detail-field-value {
		color: #111111;
		font-size: 28rpx;
	}

	.detail-actions {
		display: flex;
		align-items: center;
		justify-content: space-between;
		padding-top: 24rpx;
	}

	.detail-action {
		width: 30%;
		height: 74rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		border-radius: 10rpx;
		background: #F2F4F7;
		color: #222222;
		font-size: 28rpx;
	}

	.detail-action.favorite {
		background: #EAF3FF;
		color: #0069E8;
	}

	.detail-action.favorite.active {
		background: #0878F7;
		color: #FFFFFF;
	}
</style>
