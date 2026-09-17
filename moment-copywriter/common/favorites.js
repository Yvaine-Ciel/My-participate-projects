import { get, post } from './request.js'

// 从文案对象中取出收藏接口需要的记录 ID
function recordIdOf(record) {
	if (!record) {
		return 0
	}

	return Number(record.id || record.recordId || 0)
}

// 获取当前用户收藏列表
export function getFavorites() {
	return get('/api/copywriting/favorites')
}

// 判断文案对象当前是否已收藏
export function isFavorite(record) {
	return !!(record && record.favorite)
}

// 收藏一条文案主记录
export function addFavorite(record) {
	const id = recordIdOf(record)
	if (!id) {
		return Promise.reject('缺少文案记录ID，无法收藏')
	}

	return post('/api/copywriting/favorite/add', {
		recordId: id
	})
}

// 取消收藏一条文案主记录
export function removeFavorite(record) {
	const id = recordIdOf(record)
	if (!id) {
		return Promise.reject('缺少文案记录ID，无法取消收藏')
	}

	return post('/api/copywriting/favorite/delete', {
		recordId: id
	})
}

// 根据当前收藏状态自动收藏或取消收藏
export function toggleFavorite(record) {
	if (isFavorite(record)) {
		return removeFavorite(record).then(() => false)
	}

	return addFavorite(record).then(() => true)
}
