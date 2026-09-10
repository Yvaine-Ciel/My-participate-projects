<template>
	<view class="app-page auth-page">
		<view class="auth-header">
			<text class="page-title">注册</text>
			<text class="page-subtitle">创建账号后即可保存生成历史。</text>
		</view>

		<view class="panel auth-panel">
			<view class="field">
				<text class="field-label">用户名</text>
				<input
					class="input"
					v-model="username"
					placeholder="请输入用户名"
					maxlength="50"
				/>
			</view>

			<view class="field">
				<text class="field-label">手机号</text>
				<input
					class="input"
					v-model="phone"
					placeholder="请输入手机号"
					type="number"
					maxlength="11"
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

			<view class="field">
				<text class="field-label">确认密码</text>
				<input
					class="input"
					v-model="confirmPassword"
					placeholder="请再次输入密码"
					password
					maxlength="32"
				/>
			</view>

			<button
				class="primary-button"
				:loading="loading"
				:disabled="loading"
				@tap="register"
			>
				{{ loading ? '注册中' : '注册' }}
			</button>

			<view class="auth-footer">
				<button class="link-button" @tap="goLogin">已有账号，去登录</button>
			</view>
		</view>
	</view>
</template>

<script>
	import { post } from '../../common/request.js'

	export default {
		data() {
			return {
				username: '',
				phone: '',
				password: '',
				confirmPassword: '',
				loading: false
			}
		},
		methods: {
			register() {
				if (!this.username.trim() || !this.phone.trim() || !this.password.trim()) {
					uni.showToast({
						title: '请输入用户名、手机号和密码',
						icon: 'none'
					})
					return
				}

				if (!/^1[3-9]\d{9}$/.test(this.phone.trim())) {
					uni.showToast({
						title: '请输入正确的11位手机号',
						icon: 'none'
					})
					return
				}

				if (this.password !== this.confirmPassword) {
					uni.showToast({
						title: '两次密码不一致',
						icon: 'none'
					})
					return
				}

				this.loading = true
				post('/api/register', {
					username: this.username.trim(),
					password: this.password,
					phone: this.phone.trim()
				}, {
					auth: false
				}).then(() => {
					this.loading = false
					uni.showToast({
						title: '注册成功',
						icon: 'success'
					})
					setTimeout(() => {
						uni.redirectTo({
							url: '/pages/login/login'
						})
					}, 500)
				}).catch(message => {
					this.loading = false
					uni.showToast({
						title: String(message),
						icon: 'none'
					})
				})
			},
			goLogin() {
				uni.redirectTo({
					url: '/pages/login/login'
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
