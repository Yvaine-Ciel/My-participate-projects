<template>
	<view class="tabbar">
		<button
			v-for="item in tabs"
			:key="item.key"
			class="tabbar-item"
			:class="{ active: active === item.key }"
			@tap="go(item)"
		>
			<view class="tabbar-icon">
				<text>{{ item.icon }}</text>
			</view>
			<text class="tabbar-label">{{ item.label }}</text>
		</button>
	</view>
</template>

<script>
	export default {
		name: 'AppTabbar',
		props: {
			active: {
				type: String,
				default: 'home'
			}
		},
		data() {
			return {
				tabs: [
					{ key: 'home', label: '首页', icon: '⌂', url: '/pages/index/index' },
					{ key: 'history', label: '历史记录', icon: '▣', url: '/pages/history/history' },
					{ key: 'profile', label: '我的', icon: '●', url: '/pages/profile/profile' }
				]
			}
		},
		methods: {
			// 点击底部导航：切换到对应主页面，当前页不重复跳转
			go(item) {
				if (item.key === this.active) {
					return
				}

				uni.reLaunch({
					url: item.url
				})
			}
		}
	}
</script>

<style>
	.tabbar {
		position: fixed;
		left: 0;
		right: 0;
		bottom: 0;
		z-index: 20;
		height: 132rpx;
		padding: 12rpx 42rpx calc(12rpx + env(safe-area-inset-bottom));
		display: flex;
		align-items: center;
		justify-content: space-between;
		background: rgba(255, 255, 255, 0.96);
		border-top: 1rpx solid #EEF0F4;
	}

	.tabbar-item {
		width: 160rpx;
		height: 104rpx;
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
		color: #666666;
	}

	.tabbar-icon {
		width: 42rpx;
		height: 42rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		color: currentColor;
		font-size: 42rpx;
		line-height: 1;
	}

	.tabbar-label {
		display: block;
		margin-top: 8rpx;
		color: currentColor;
		font-size: 26rpx;
		line-height: 1.2;
	}

	.tabbar-item.active {
		color: #0069E8;
		font-weight: 700;
	}
</style>
