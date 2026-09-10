<template>
	<view class="app-page auth-page">
		<view class="auth-header">
			<text class="page-title">登录</text>
			<text class="page-subtitle">进入后可生成并保存朋友圈文案。</text>
		</view>

		<view class="panel auth-panel">
			<view class="field">
				<text class="field-label">手机号</text>
				<input
					class="input"
					v-model="phone"
					placeholder="请输入手机号"
					type="number"
					maxlength="20"
				/>
			</view>

			<view class="field">
				<text class="field-label">密码</text>
				<input
					class="input"
					v-model="password"
					placeholder="请输入密码"
					password
					maxlength="32"
				/>
			</view>

			<button
				class="primary-button"
				:loading="loading"
				:disabled="loading"
				@tap="login"
			>
				{{ loading ? '登录中' : '登录' }}
			</button>

			<view class="auth-footer">
				<button class="link-button" @tap="goRegister">创建账号</button>
			</view>
		</view>
	</view>
</template>

<script>
	import { post } from '../../common/request.js'
	import { saveUser } from '../../common/auth.js'

	export default {
		data() {
			return {
				phone: '',
				password: '',
				loading: false
			}
		},
		methods: {
			login() {
				if (!this.phone.trim() || !this.password.trim()) {
					uni.showToast({
						title: '请输入手机号和密码',
						icon: 'none'
					})
					return
				}

				this.loading = true
				post('/api/login', {
					phone: this.phone,
					password: this.password
				}, {
					auth: false
				}).then(data => {
					this.loading = false
					if (data && data.user) {
						saveUser(data.user)
					}
					uni.reLaunch({
						url: '/pages/index/index'
					})
				}).catch(message => {
					this.loading = false
					uni.showToast({
						title: String(message),
						icon: 'none'
					})
				})
			},
			goRegister() {
				uni.navigateTo({
					url: '/pages/register/register'
				})
			}
		}
	}
</script>

<style>
	.auth-header {
		margin-bottom: 36rpx;
		padding-top: 48rpx;
	}

	.auth-panel {
		margin-bottom: 24rpx;
	}

	.auth-footer {
		display: flex;
		justify-content: center;
		margin-top: 28rpx;
	}
</style>
