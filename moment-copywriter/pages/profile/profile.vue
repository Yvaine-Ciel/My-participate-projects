<template>
	<view class="app-page profile-page">
		<text class="page-title profile-title">我的</text>

		<view class="user-card">
			<view class="avatar-wrap">
				<view class="avatar-head"></view>
				<view class="avatar-body"></view>
			</view>
			<view class="user-info" v-if="user">
				<text class="username">{{ displayUsername }}</text>
				<text class="login-state">已登录</text>
			</view>
			<view class="user-info" v-else>
				<text class="username">未登录</text>
				<text class="login-state">登录后保存文案历史</text>
			</view>
		</view>

		<view class="menu-card">
			<button class="menu-row" @tap="goFavorites">
				<view class="menu-icon bubble-icon">
					<text>...</text>
				</view>
				<text class="menu-text">收藏的文案</text>
				<text class="menu-arrow">›</text>
			</button>

			<view class="divider"></view>

			<button class="menu-row" @tap="goHistory">
				<view class="menu-icon note-icon">
					<text>▤</text>
				</view>
				<text class="menu-text">历史记录</text>
				<text class="menu-arrow">›</text>
			</button>

			<view class="divider"></view>

			<button class="menu-row" @tap="openTagPanel">
				<view class="menu-icon tag-icon">
					<text>#</text>
				</view>
				<text class="menu-text">标签</text>
				<text class="menu-arrow">›</text>
			</button>

			<view class="divider"></view>

			<button class="menu-row" @tap="clearHistory">
				<view class="menu-icon note-icon">
					<text>⌫</text>
				</view>
				<text class="menu-text">清空全部历史记录</text>
				<text class="menu-arrow">›</text>
			</button>

			<view class="divider"></view>

			<button class="menu-row" @tap="showAbout">
				<view class="menu-icon check-icon">
					<text>✓</text>
				</view>
				<text class="menu-text">关于小程序</text>
				<text class="menu-arrow">›</text>
			</button>

			<view class="divider" v-if="user"></view>

			<button class="menu-row" v-if="user" @tap="logout">
				<view class="menu-icon logout-icon">
					<text>×</text>
				</view>
				<text class="menu-text">退出登录</text>
				<text class="menu-arrow">›</text>
			</button>
		</view>

		<view v-if="tagPanelVisible" class="tag-mask" @tap="closeTagPanel">
			<view class="tag-panel" @tap.stop>
				<view class="tag-header">
					<text class="tag-title">标签</text>
					<button class="tag-close" @tap.stop="closeTagPanel">×</button>
				</view>

				<view class="tag-input-row">
					<input
						class="tag-input"
						v-model="tagInput"
						placeholder="例如：老师、学生、宝妈"
						maxlength="12"
						@confirm="addTag"
					/>
					<button class="tag-add" @tap="addTag">添加</button>
				</view>

				<view class="tag-list" v-if="userTags.length">
					<view class="tag-item" v-for="tag in userTags" :key="tag">
						<text>{{ tag }}</text>
						<button class="tag-delete" @tap.stop="removeTag(tag)">×</button>
					</view>
				</view>

				<text class="tag-empty" v-else>暂无标签</text>
			</view>
		</view>

		<view class="login-actions" v-if="!user">
			<button class="primary-button" @tap="goLogin">登录</button>
			<button class="outline-button register-button" @tap="goRegister">注册</button>
		</view>

		<app-tabbar active="profile"></app-tabbar>
	</view>
</template>

<script>
	import AppTabbar from '../../components/app-tabbar/app-tabbar.vue'
	import { get, post } from '../../common/request.js'
	import { clearUser, getUser, ensureLogin, loadCurrentUser } from '../../common/auth.js'

	export default {
		components: {
			AppTabbar
		},
		data() {
			return {
				user: null,
				tagPanelVisible: false,
				tagInput: '',
				userTags: []
			}
		},
		computed: {
			displayUsername() {
				return this.user ? this.displayText(this.user.username) : ''
			}
		},
		onShow() {
			this.user = getUser()
			if (this.user) {
				this.loadTags()
			} else {
				this.userTags = []
			}
			loadCurrentUser().then(user => {
				this.user = user
				this.loadTags()
			}).catch(() => {
				this.user = null
				this.userTags = []
			})
		},
		methods: {
			// 点击收藏文案：登录后跳转到历史页的收藏筛选模式
			goFavorites() {
				if (!ensureLogin()) {
					return
				}

				uni.reLaunch({
					url: '/pages/history/history?favorite=1'
				})
			},
			// 点击历史记录：登录后跳转到历史页
			goHistory() {
				if (!ensureLogin()) {
					return
				}

				uni.reLaunch({
					url: '/pages/history/history'
				})
			},
			// 点击标签入口：登录后打开标签管理弹层
			openTagPanel() {
				if (!ensureLogin()) {
					return
				}

				this.tagPanelVisible = true
				this.loadTags()
			},
			// 关闭标签弹层，并清空输入框
			closeTagPanel() {
				this.tagPanelVisible = false
				this.tagInput = ''
			},
			// 加载当前用户标签
			loadTags() {
				return get('/api/user-tags').then(data => {
					this.userTags = Array.isArray(data) ? data : []
				}).catch(message => {
					this.userTags = []
					if (this.tagPanelVisible) {
						uni.showToast({
							title: String(message),
							icon: 'none'
						})
					}
				})
			},
			// 点击添加标签：校验后提交新增标签接口
			addTag() {
				const text = this.tagInput.trim()
				if (!text) {
					uni.showToast({
						title: '请输入标签',
						icon: 'none'
					})
					return
				}

				if (this.userTags.indexOf(text) !== -1) {
					uni.showToast({
						title: '标签已存在',
						icon: 'none'
					})
					return
				}

				post('/api/user-tags/add', {
					name: text
				}).then(data => {
					this.userTags = data && Array.isArray(data.tags) ? data.tags : []
					this.tagInput = ''
				}).catch(message => {
					uni.showToast({
						title: String(message),
						icon: 'none'
					})
				})
			},
			// 点击标签删除按钮：删除指定标签并刷新标签列表
			removeTag(tag) {
				post('/api/user-tags/delete', {
					name: tag
				}).then(data => {
					this.userTags = data && Array.isArray(data.tags) ? data.tags : []
				}).catch(message => {
					uni.showToast({
						title: String(message),
						icon: 'none'
					})
				})
			},
			// 点击清空历史：确认后清空历史记录，收藏文案会保留
			clearHistory() {
				if (!ensureLogin()) {
					return
				}

				uni.showModal({
					title: '清空历史',
					content: '确定清空全部历史记录吗？',
					success: res => {
						if (!res.confirm) {
							return
						}

						post('/api/copywriting/clear-history').then(data => {
							const deletedCount = data && data.deletedCount ? data.deletedCount : 0
							uni.showToast({
								title: '已清空' + deletedCount + '条记录',
								icon: 'none'
							})
						}).catch(message => {
							uni.showToast({
								title: String(message),
								icon: 'none'
							})
						})
					}
				})
			},
			// 点击登录：跳转到登录页
			goLogin() {
				uni.navigateTo({
					url: '/pages/login/login'
				})
			},
			// 点击注册：跳转到注册页
			goRegister() {
				uni.navigateTo({
					url: '/pages/register/register'
				})
			},
			// 点击关于小程序：展示应用说明弹窗
			showAbout() {
				uni.showModal({
					title: '关于小程序',
					content: 'AI文案生成器，用于生成、复制和收藏多场景文案。',
					showCancel: false
				})
			},
			// 点击退出登录：请求退出接口，失败时也清理本地登录状态
			logout() {
				post('/api/logout', {}, {
					auth: false
				}).then(() => {
					this.clearAndGoHome()
				}).catch(() => {
					this.clearAndGoHome()
				})
			},
			// 清理本地用户状态并回到首页
			clearAndGoHome() {
				clearUser()
				this.user = null
				this.userTags = []
				uni.reLaunch({
					url: '/pages/index/index'
				})
			},
			// 展示文本前做一次兜底解码，减少乱码显示
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
			// 尝试把常见的 UTF-8 误解码文本还原
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
			// 判断文本是否像乱码
			looksGarbled(text) {
				return /[ÃÂäåæçèéïâ]/.test(text)
			}
		}
	}
</script>

<style>
	.profile-page {
		padding-top: 92rpx;
	}

	.profile-title {
		margin-bottom: 50rpx;
	}

	.user-card {
		min-height: 250rpx;
		padding: 48rpx 48rpx;
		display: flex;
		align-items: center;
		border-radius: 16rpx 16rpx 0 0;
		background: linear-gradient(135deg, #CFE4FF 0%, #E8F3FF 100%);
	}

	.avatar-wrap {
		width: 132rpx;
		height: 132rpx;
		margin-right: 44rpx;
		position: relative;
		border-radius: 50%;
		background: #FFFFFF;
		overflow: hidden;
	}

	.avatar-head {
		position: absolute;
		left: 50%;
		top: 34rpx;
		width: 42rpx;
		height: 42rpx;
		transform: translateX(-50%);
		border-radius: 50%;
		background: #EEF4FF;
	}

	.avatar-body {
		position: absolute;
		left: 50%;
		bottom: 22rpx;
		width: 88rpx;
		height: 50rpx;
		transform: translateX(-50%);
		border-radius: 48rpx 48rpx 0 0;
		background: #EEF4FF;
	}

	.user-info {
		flex: 1;
		min-width: 0;
	}

	.username {
		display: block;
		overflow: hidden;
		color: #050505;
		font-size: 42rpx;
		font-weight: 800;
		line-height: 1.2;
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	.login-state {
		display: block;
		margin-top: 18rpx;
		color: #666666;
		font-size: 30rpx;
	}

	.menu-card {
		padding: 22rpx 32rpx;
		border-radius: 16rpx;
		background: #FFFFFF;
		box-shadow: 0 18rpx 42rpx rgba(24, 58, 101, 0.08);
		transform: translateY(-28rpx);
	}

	.menu-row {
		width: 100%;
		height: 120rpx;
		display: flex;
		align-items: center;
		text-align: left;
	}

	.menu-icon {
		width: 58rpx;
		height: 58rpx;
		margin-right: 28rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		border-radius: 8rpx;
		background: #F2F7FF;
		color: #0878F7;
		font-size: 28rpx;
		font-weight: 700;
	}

	.note-icon,
	.check-icon,
	.tag-icon,
	.logout-icon {
		font-size: 34rpx;
	}

	.logout-icon {
		color: #D94B3D;
		background: #FFF3F2;
	}

	.menu-text {
		flex: 1;
		color: #111111;
		font-size: 34rpx;
		font-weight: 500;
	}

	.menu-arrow {
		color: #8B8B8B;
		font-size: 54rpx;
		line-height: 1;
	}

	.divider {
		height: 1rpx;
		margin-left: 86rpx;
		background: #EFEFEF;
	}

	.login-actions {
		margin-top: 12rpx;
	}

	.register-button {
		width: 100%;
		margin-top: 22rpx;
	}

	.tag-mask {
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

	.tag-panel {
		position: relative;
		width: 100%;
		max-height: 72vh;
		padding: 34rpx 36rpx calc(34rpx + env(safe-area-inset-bottom));
		border-radius: 24rpx 24rpx 0 0;
		background: #FFFFFF;
		box-sizing: border-box;
	}

	.tag-header {
		display: flex;
		align-items: center;
		padding-right: 82rpx;
		margin-bottom: 28rpx;
	}

	.tag-title {
		color: #0A0A0A;
		font-size: 38rpx;
		font-weight: 800;
	}

	.tag-close {
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

	.tag-input-row {
		display: flex;
		align-items: center;
		margin-bottom: 26rpx;
	}

	.tag-input {
		flex: 1;
		height: 78rpx;
		padding: 0 22rpx;
		border-radius: 10rpx;
		background: #F7FAFF;
		color: #111111;
		font-size: 28rpx;
	}

	.tag-add {
		width: 138rpx;
		height: 78rpx;
		margin-left: 18rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		border-radius: 10rpx;
		background: #0878F7;
		color: #FFFFFF;
		font-size: 28rpx;
	}

	.tag-list {
		display: flex;
		flex-wrap: wrap;
	}

	.tag-item {
		min-height: 58rpx;
		margin: 0 16rpx 16rpx 0;
		padding: 0 12rpx 0 22rpx;
		display: flex;
		align-items: center;
		border-radius: 29rpx;
		background: #EAF3FF;
		color: #0069E8;
		font-size: 26rpx;
	}

	.tag-delete {
		width: 42rpx;
		height: 42rpx;
		margin-left: 8rpx;
		padding: 0;
		display: flex;
		align-items: center;
		justify-content: center;
		color: #0069E8;
		font-size: 28rpx;
		line-height: 1;
	}

	.tag-empty {
		display: block;
		color: #666666;
		font-size: 28rpx;
	}
</style>
