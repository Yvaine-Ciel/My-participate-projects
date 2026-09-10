<template>
	<view class="app-page history-page">
		<view class="history-shell">
			<view class="history-header">
				<text class="page-title">历史记录</text>
				<view class="favorite-filter">
					<text>只看收藏</text>
					<switch
						class="filter-switch"
						:checked="onlyFavorites"
						color="#0878F7"
						@change="changeFavoriteFilter"
					/>
				</view>
			</view>

			<view class="empty-state" v-if="!loading && displayRecords.length === 0">
				{{ onlyFavorites ? '暂无收藏文案' : '暂无历史记录' }}
			</view>

			<view
				v-for="record in displayRecords"
				:key="record.uniqueKey"
				class="history-card"
				@tap="openDetail(record)"
			>
				<view class="card-top">
					<button
						class="card-icon"
						:class="{ muted: !record.favorite }"
						@tap.stop="toggleRecordFavorite(record)"
					>
						<text>{{ record.favorite ? '♥' : '♡' }}</text>
					</button>
					<view class="card-main">
						<view class="card-title-row">
							<text class="card-title">{{ cardTitle(record) }}</text>
							<text class="arrow">›</text>
						</view>
						<text class="card-preview">{{ record.displayGeneratedContent }}</text>
					</view>
				</view>

				<view class="card-bottom">
					<text class="record-time">{{ formatTime(record.createTime || record.favoriteTime) }}</text>
					<button class="delete-button" @tap.stop="deleteRecord(record)">删除</button>
				</view>
			</view>
		</view>

		<view v-if="detailRecord" class="detail-mask" @tap="closeDetail">
			<view class="detail-panel" @tap.stop>
				<view class="detail-header">
					<text class="detail-title">文案详情</text>
					<button class="detail-close" @tap.stop="closeDetail">×</button>
				</view>

				<scroll-view class="detail-body" scroll-y>
					<view class="detail-pager" v-if="showDetailSteps">
						<button
							class="pager-button"
							:disabled="detailStepIndex === 0"
							@tap.stop="prevDetailStep"
						>
							上一页
						</button>
						<text class="pager-text">第{{ detailStepIndex + 1 }} / {{ detailSteps.length }}页</text>
						<button
							class="pager-button"
							:disabled="detailStepIndex >= detailSteps.length - 1"
							@tap.stop="nextDetailStep"
						>
							下一页
						</button>
					</view>

					<view class="detail-section" v-if="showDetailSteps">
						<text class="detail-label">{{ detailStepTitle }}</text>
						<text class="detail-content">{{ detailStepUserMessage }}</text>
					</view>

					<view class="detail-section" v-else>
						<text class="detail-label">生成要求</text>
						<text class="detail-content">{{ detailRecord.displayScene || '无' }}</text>
					</view>

					<view class="detail-meta" v-if="detailRecord.displayMood || detailRecord.displayStyle || detailRecord.displayKeywords">
						<view class="detail-field" v-if="detailRecord.displayMood">
							<text class="detail-field-label">心情</text>
							<text class="detail-field-value">{{ detailRecord.displayMood }}</text>
						</view>
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
						<text class="detail-content">{{ detailGeneratedContent }}</text>
					</view>
				</scroll-view>

				<view class="detail-actions">
					<button class="detail-action" @tap.stop="copyDetail">复制</button>
					<button
						class="detail-action favorite"
						:class="{ active: detailRecord.favorite }"
						@tap.stop="toggleDetailFavorite"
					>
						{{ detailRecord.favorite ? '取消收藏' : '收藏' }}
					</button>
					<button class="detail-action danger" @tap.stop="deleteDetailRecord">删除</button>
				</view>
			</view>
		</view>

		<app-tabbar active="history"></app-tabbar>
	</view>
</template>

<script>
	import AppTabbar from '../../components/app-tabbar/app-tabbar.vue'
	import { get, post } from '../../common/request.js'
	import { ensureLogin, isLoggedIn, loadCurrentUser } from '../../common/auth.js'
	import { getFavorites, isFavorite, toggleFavorite } from '../../common/favorites.js'

	export default {
		components: {
			AppTabbar
		},
		data() {
			return {
				records: [],
				favorites: [],
				onlyFavorites: false,
				loading: false,
				detailRecord: null,
				detailSteps: [],
				detailStepIndex: 0
			}
		},
		computed: {
			displayRecords() {
				const source = this.onlyFavorites ? this.favorites : this.records
				return source.map((record, index) => {
					const favorite = this.onlyFavorites ? true : isFavorite(record)
					const uniqueKey = (record.id ? 'id:' + record.id : 'local:' + index)
					return Object.assign({}, record, {
						favorite,
						uniqueKey,
						displayScene: this.displayText(record.scene),
						displayMood: this.displayText(record.mood),
						displayStyle: this.displayText(record.style),
						displayKeywords: this.displayText(record.keywords),
						displayGeneratedContent: this.displayText(record.generatedContent)
					})
				})
			},
			showDetailSteps() {
				return !this.onlyFavorites && this.detailSteps.length > 0
			},
			detailStep() {
				if (!this.showDetailSteps) {
					return null
				}

				return this.detailSteps[this.detailStepIndex] || null
			},
			detailStepTitle() {
				const step = this.detailStep
				if (!step) {
					return '生成要求'
				}

				return step.stepNo <= 1 ? '第1次生成要求' : '第' + step.stepNo + '次优化要求'
			},
			detailStepUserMessage() {
				const step = this.detailStep
				if (!step) {
					return '无'
				}

				return this.displayText(step.userMessage) || this.detailRecord.displayScene || '无'
			},
			detailGeneratedContent() {
				const step = this.detailStep
				if (step) {
					return this.displayText(step.generatedContent) || '无'
				}

				return this.detailRecord && this.detailRecord.displayGeneratedContent
					? this.detailRecord.displayGeneratedContent
					: '无'
			}
		},
		onLoad(options) {
			this.onlyFavorites = options && options.favorite === '1'
		},
		onShow() {
			this.requireLogin().then(loggedIn => {
				if (loggedIn) {
					this.loadRecords(false)
				}
			})
		},
		onPullDownRefresh() {
			this.requireLogin().then(loggedIn => {
				if (loggedIn) {
					this.loadRecords(true)
					return
				}

				uni.stopPullDownRefresh()
			})
		},
		methods: {
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
			refreshFavorites() {
				return getFavorites().then(data => {
					this.favorites = Array.isArray(data) ? data : []
				})
			},
			changeFavoriteFilter(event) {
				this.onlyFavorites = event.detail.value
				this.loadRecords(false)
			},
			loadRecords(stopRefresh) {
				if (this.onlyFavorites) {
					this.loadFavorites(stopRefresh)
					return
				}

				this.loadHistory(stopRefresh)
			},
			loadFavorites(stopRefresh) {
				this.loading = true
				this.refreshFavorites().then(() => {
					this.loading = false
					if (stopRefresh) {
						uni.stopPullDownRefresh()
					}
				}).catch(message => {
					this.loading = false
					if (stopRefresh) {
						uni.stopPullDownRefresh()
					}
					uni.showToast({
						title: String(message),
						icon: 'none'
					})
				})
			},
			loadHistory(stopRefresh) {
				this.loading = true
				get('/api/copywriting/history').then(data => {
					this.records = Array.isArray(data) ? data : []
					this.loading = false
					if (stopRefresh) {
						uni.stopPullDownRefresh()
					}
				}).catch(message => {
					this.loading = false
					if (stopRefresh) {
						uni.stopPullDownRefresh()
					}
					uni.showToast({
						title: String(message),
						icon: 'none'
					})
				})
			},
			cardTitle(record) {
				if (!record) {
					return '朋友圈文案'
				}

				return record.displayScene || record.displayKeywords || record.displayStyle || '朋友圈文案'
			},
			displayText(value) {
				if (value === null || value === undefined) {
					return ''
				}

				const text = String(value)
				if (!this.looksGarbled(text)) {
					return text
				}

				try {
					const decoded = this.decodeMojibake(text)
					return decoded && !this.looksGarbled(decoded) ? decoded : text
				} catch (e) {
					return text
				}
			},
			decodeMojibake(text) {
				let encoded = ''

				for (let i = 0; i < text.length; i++) {
					const code = text.charCodeAt(i)
					if (code > 255) {
						encoded += encodeURIComponent(text.charAt(i))
						continue
					}

					const hex = code.toString(16)
					encoded += '%' + (hex.length === 1 ? '0' + hex : hex)
				}

				return decodeURIComponent(encoded)
			},
			looksGarbled(text) {
				return /[ÃÂäåæçèéïâ]/.test(text)
			},
			openDetail(record) {
				if (!record) {
					return
				}

				this.detailRecord = Object.assign({}, record)
				this.detailSteps = []
				this.detailStepIndex = 0

				if (!this.onlyFavorites && record.id) {
					this.loadDetailSteps(record.id)
				}
			},
			closeDetail() {
				this.detailRecord = null
				this.detailSteps = []
				this.detailStepIndex = 0
			},
			loadDetailSteps(recordId) {
				post('/api/copywriting/steps', {
					recordId
				}).then(data => {
					this.detailSteps = Array.isArray(data) ? data : []
					this.detailStepIndex = 0
				}).catch(message => {
					uni.showToast({
						title: String(message),
						icon: 'none'
					})
				})
			},
			prevDetailStep() {
				if (this.detailStepIndex > 0) {
					this.detailStepIndex--
				}
			},
			nextDetailStep() {
				if (this.detailStepIndex < this.detailSteps.length - 1) {
					this.detailStepIndex++
				}
			},
			copyRecord(record) {
				const content = record && (record.displayGeneratedContent || record.generatedContent)
				if (!content) {
					return
				}

				uni.setClipboardData({
					data: content
				})
			},
			copyDetail() {
				if (!this.detailGeneratedContent || this.detailGeneratedContent === '无') {
					return
				}

				uni.setClipboardData({
					data: this.detailGeneratedContent
				})
			},
			toggleRecordFavorite(record) {
				toggleFavorite(record).then(favorite => {
					this.applyFavoriteState(record, favorite)
					uni.showToast({
						title: favorite ? '已收藏' : '已取消',
						icon: 'none'
					})
				}).catch(message => {
					uni.showToast({
						title: String(message),
						icon: 'none'
					})
				})
			},
			toggleDetailFavorite() {
				if (!this.detailRecord) {
					return
				}

				toggleFavorite(this.detailRecord).then(favorite => {
					this.applyFavoriteState(this.detailRecord, favorite)
					this.detailRecord = Object.assign({}, this.detailRecord, {
						favorite
					})
					uni.showToast({
						title: favorite ? '已收藏' : '已取消',
						icon: 'none'
					})
				}).catch(message => {
					uni.showToast({
						title: String(message),
						icon: 'none'
					})
				})
			},
			applyFavoriteState(record, favorite) {
				const id = record && record.id
				if (!id) {
					return
				}

				this.records.forEach(item => {
					if (item.id === id) {
						item.favorite = favorite
					}
				})

				if (this.detailRecord && this.detailRecord.id === id) {
					this.detailRecord = Object.assign({}, this.detailRecord, {
						favorite
					})
				}

				if (favorite) {
					return
				}

				this.favorites = this.favorites.filter(item => item.id !== id)
			},
			deleteDetailRecord() {
				if (!this.detailRecord) {
					return
				}

				this.deleteRecord(this.detailRecord)
			},
			deleteRecord(record) {
				uni.showModal({
					title: '删除记录',
					content: '确定删除这条文案吗？',
					success: res => {
						if (!res.confirm) {
							return
						}

						if (!record.id) {
							uni.showToast({
								title: '缺少文案记录ID，无法删除',
								icon: 'none'
							})
							return
						}

						post('/api/copywriting/delete', {
							id: record.id
						}).then(() => {
							if (this.detailRecord && this.detailRecord.id === record.id) {
								this.closeDetail()
							}
							this.loadRecords(false)
						}).catch(message => {
							uni.showToast({
								title: String(message),
								icon: 'none'
							})
						})
					}
				})
			},
			formatTime(value) {
				if (!value) {
					return ''
				}

				return String(value).replace('T', ' ').slice(0, 16)
			}
		}
	}
</script>

<style>
	.history-page {
		padding-top: 92rpx;
	}

	.history-shell {
		min-height: calc(100vh - 248rpx);
		padding: 52rpx 40rpx;
		border-radius: 16rpx;
		background: rgba(255, 255, 255, 0.78);
	}

	.history-header {
		display: flex;
		align-items: center;
		justify-content: space-between;
		margin-bottom: 52rpx;
	}

	.favorite-filter {
		display: flex;
		align-items: center;
		color: #111111;
		font-size: 28rpx;
	}

	.filter-switch {
		margin-left: 12rpx;
		transform: scale(0.74);
		transform-origin: center right;
	}

	.history-card {
		margin-bottom: 28rpx;
		padding: 32rpx 32rpx 30rpx;
		border-radius: 16rpx;
		background: #FFFFFF;
		box-shadow: 0 16rpx 34rpx rgba(24, 58, 101, 0.08);
	}

	.card-top {
		display: flex;
		align-items: flex-start;
	}

	.card-icon {
		width: 48rpx;
		height: 48rpx;
		margin-right: 24rpx;
		border-radius: 8rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		background: #0878F7;
		color: #FFFFFF;
		font-size: 26rpx;
		line-height: 1;
	}

	.card-icon.muted {
		background: #C9C9C9;
	}

	.card-main {
		flex: 1;
		min-width: 0;
	}

	.card-title-row {
		display: flex;
		align-items: center;
		justify-content: space-between;
		margin-bottom: 18rpx;
	}

	.card-title {
		flex: 1;
		min-width: 0;
		overflow: hidden;
		color: #0A0A0A;
		font-size: 34rpx;
		font-weight: 800;
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	.arrow {
		color: #9A9A9A;
		font-size: 46rpx;
		line-height: 1;
	}

	.card-preview {
		display: -webkit-box;
		overflow: hidden;
		color: #242424;
		font-size: 28rpx;
		line-height: 1.45;
		text-overflow: ellipsis;
		-webkit-line-clamp: 2;
		-webkit-box-orient: vertical;
	}

	.card-bottom {
		display: flex;
		align-items: center;
		justify-content: space-between;
		margin-top: 30rpx;
	}

	.record-time {
		color: #555555;
		font-size: 26rpx;
	}

	.delete-button {
		height: 58rpx;
		padding: 0 12rpx;
		color: #222222;
		font-size: 26rpx;
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

	.detail-pager {
		display: flex;
		align-items: center;
		justify-content: space-between;
		margin-bottom: 26rpx;
	}

	.pager-button {
		width: 150rpx;
		height: 60rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		border-radius: 10rpx;
		background: #F2F4F7;
		color: #222222;
		font-size: 26rpx;
	}

	.pager-button[disabled] {
		color: #A0A0A0;
	}

	.pager-text {
		color: #555555;
		font-size: 26rpx;
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

	.detail-action.danger {
		background: #FFF3F2;
		color: #D94B3D;
	}
</style>
