<script>
	import { loadCurrentUser } from './common/auth.js'

	export default {
		onLaunch: function() {
			console.log('AI Copywriter Launch')
			this.loadLoginUser()
		},
		onShow: function() {
			console.log('AI Copywriter Show')
			this.loadLoginUser()
		},
		onHide: function() {
			console.log('AI Copywriter Hide')
		},
		methods: {
			loadLoginUser() {
				loadCurrentUser().catch(() => {})
			}
		}
	}
</script>

<style>
	page {
		min-height: 100%;
		background: #F7FAFF;
		color: #111111;
		font-family: -apple-system, BlinkMacSystemFont, "Helvetica Neue", Helvetica, Arial, sans-serif;
		font-size: 28rpx;
	}

	view,
	text,
	input,
	textarea,
	button,
	scroll-view {
		box-sizing: border-box;
	}

	button {
		margin: 0;
		padding: 0;
		border-radius: 8rpx;
		line-height: 1.4;
		background: transparent;
		font-size: 28rpx;
	}

	button::after {
		border: none;
	}

	.app-page {
		min-height: 100vh;
		padding: 44rpx 40rpx 156rpx;
		background:
			linear-gradient(180deg, #F4F9FF 0%, #F8FBFF 40%, #FFFFFF 100%);
	}

	.page-title {
		display: block;
		color: #050505;
		font-size: 56rpx;
		font-weight: 800;
		line-height: 1.2;
		letter-spacing: 0;
	}

	.page-subtitle {
		display: block;
		margin-top: 12rpx;
		color: #666666;
		font-size: 28rpx;
		line-height: 1.45;
	}

	.surface-card {
		border-radius: 16rpx;
		background: #FFFFFF;
		box-shadow: 0 18rpx 42rpx rgba(34, 67, 115, 0.08);
	}

	.panel {
		border-radius: 16rpx;
		background: #FFFFFF;
		padding: 40rpx;
		box-shadow: 0 18rpx 42rpx rgba(34, 67, 115, 0.08);
	}

	.field {
		margin-bottom: 32rpx;
	}

	.field-label {
		display: block;
		margin-bottom: 16rpx;
		color: #111111;
		font-size: 30rpx;
		font-weight: 700;
	}

	.input,
	.textarea {
		width: 100%;
		color: #101010;
		font-size: 32rpx;
		background: #F7FAFF;
		border-radius: 16rpx;
		padding: 0 24rpx;
	}

	.input {
		height: 88rpx;
	}

	.textarea {
		min-height: 260rpx;
		padding: 24rpx;
		line-height: 1.55;
	}

	.primary-button {
		width: 100%;
		height: 88rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		border-radius: 44rpx;
		background: #0878F7;
		color: #FFFFFF;
		font-size: 34rpx;
		font-weight: 800;
		box-shadow: 0 12rpx 28rpx rgba(8, 120, 247, 0.24);
	}

	.ghost-button {
		height: 72rpx;
		display: inline-flex;
		align-items: center;
		justify-content: center;
		color: #666666;
		font-size: 30rpx;
	}

	.outline-button {
		height: 72rpx;
		padding: 0 32rpx;
		display: inline-flex;
		align-items: center;
		justify-content: center;
		border: 1rpx solid #E1E4EA;
		border-radius: 36rpx;
		background: #FFFFFF;
		color: #005EDB;
		font-size: 30rpx;
		font-weight: 600;
	}

	.empty-state {
		padding: 120rpx 32rpx;
		text-align: center;
		color: #888888;
		font-size: 30rpx;
	}
</style>
