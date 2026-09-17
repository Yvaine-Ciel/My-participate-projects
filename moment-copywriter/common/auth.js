import { API_BASE_URL, API_TIMEOUT } from './config.js'

let currentUser = null
let sessionCookie = ''

// 保存当前登录用户到前端内存
export function saveUser(user) {
	currentUser = user || null
}

// 获取当前前端缓存的登录用户
export function getUser() {
	return currentUser
}

// 获取当前用户 ID，未登录时返回 0
export function getUserId() {
	const user = getUser()
	return user && user.id ? Number(user.id) : 0
}

// 判断前端当前是否已有登录用户
export function isLoggedIn() {
	return getUserId() > 0
}

// 清空前端用户和会话 Cookie
export function clearUser() {
	currentUser = null
	sessionCookie = ''
}

// 确保用户已登录，未登录时跳转登录页
export function ensureLogin() {
	if (isLoggedIn()) {
		return true
	}

	uni.navigateTo({
		url: '/pages/login/login'
	})
	return false
}

// 获取当前保存的后端 Session Cookie
export function getSessionCookie() {
	return sessionCookie
}

// 从响应头中提取并保存 JSESSIONID
export function saveSessionCookie(header) {
	const rawCookie = getHeader(header, 'Set-Cookie')
	if (!rawCookie) {
		return
	}

	const cookieText = Array.isArray(rawCookie) ? rawCookie.join(',') : String(rawCookie)
	const matched = cookieText.match(/JSESSIONID=[^;,]+/i)
	if (matched) {
		sessionCookie = matched[0]
	}
}

// 请求后端当前用户接口，尝试恢复登录状态
export function loadCurrentUser() {
	return new Promise((resolve, reject) => {
		uni.request({
			url: normalizeUrl('/api/current-user'),
			method: 'GET',
			timeout: API_TIMEOUT,
			withCredentials: true,
			header: buildAuthHeader(),
			success(res) {
				saveSessionCookie(res.header)

				const body = res.data || {}
				if (res.statusCode >= 200 && res.statusCode < 300 && body.success !== false) {
					const user = body.data && body.data.user ? body.data.user : null
					saveUser(user)
					resolve(user)
					return
				}

				clearUser()
				reject(body.message || '请先登录')
			},
			fail(err) {
				reject(err.errMsg || '网络连接失败')
			}
		})
	})
}

// 拼接完整 API 地址
function normalizeUrl(path) {
	const baseUrl = API_BASE_URL.replace(/\/$/, '')
	const apiPath = path.indexOf('/') === 0 ? path : '/' + path
	return baseUrl + apiPath
}

// 构建当前用户请求头，包含 Session Cookie
function buildAuthHeader() {
	if (!sessionCookie) {
		return {}
	}

	return {
		Cookie: sessionCookie
	}
}

// 兼容不同大小写的响应头读取
function getHeader(header, name) {
	if (!header) {
		return ''
	}

	if (header[name]) {
		return header[name]
	}

	const lowerName = name.toLowerCase()
	const key = Object.keys(header).find(item => item.toLowerCase() === lowerName)
	return key ? header[key] : ''
}
