import { createLogger } from '../utils/debug.js'

const log = createLogger('API')
const BASE = '/api/v1'

function getToken() {
  return localStorage.getItem('token') || ''
}

async function request(url, method = 'GET', body = null) {
  const headers = { 'Content-Type': 'application/json' }
  const token = getToken()
  if (token) headers['Authorization'] = 'Bearer ' + token

  const opts = { method, headers }
  if (body) opts.body = JSON.stringify(body)

  log.apiReq(method, url, body)
  const res = await fetch(BASE + url, opts)
  const json = await res.json()
  log.apiRes(method, url, json)
  return json
}

export const api = {
  // ═══════════════ Auth ═══════════════
  register: (email, password) => request('/auth/register', 'POST', { email, password }),
  login: (email, password) => request('/auth/login', 'POST', { email, password }),
  profile: () => request('/auth/profile'),
  updateProfile: (data) => request('/auth/profile', 'PUT', data),
  changePassword: (oldPassword, newPassword) => request('/auth/password', 'PUT', { oldPassword, newPassword }),

  // ═══════════════ Posts ═══════════════
  submitPost: (content, buildingName, zoneId, anonymous = false) => request('/posts/text', 'POST', { content, buildingName, zoneId, anonymous }),
  getMyPosts: (page = 1, pageSize = 10) => request(`/posts/my?page=${page}&pageSize=${pageSize}`),
  deletePost: (postId) => request(`/posts/${postId}`, 'DELETE'),
  getBuildingPosts: (name, page = 1, pageSize = 20) => request(`/posts/building?name=${encodeURIComponent(name)}&page=${page}&pageSize=${pageSize}`),
  getZonePosts: (zoneId, page = 1, pageSize = 20) => request(`/posts/zone?zoneId=${zoneId}&page=${page}&pageSize=${pageSize}`),

  // ═══════════════ Map ═══════════════
  getMapOverview: () => request('/map/overview'),
  getMapAreas: () => request('/map/areas'),

  // ═══════════════ My Weather ═══════════════
  getTodayWeather: () => request('/my-weather/today'),
  getMyMapData: () => request('/my-weather/map'),
  getEmotionTrend: (period = 'week') => request(`/my-weather/trend?period=${period}`),
  getWeatherCalendar: (month) => request(`/my-weather/calendar?month=${month}`),
  getAreaDistribution: () => request('/my-weather/distribution'),

  // ═══════════════ Comments ═══════════════
  getComments: (postId, page = 1, pageSize = 20) => request(`/posts/${postId}/comments?page=${page}&pageSize=${pageSize}`),
  addComment: (postId, content) => request(`/posts/${postId}/comments`, 'POST', { content }),
  deleteComment: (commentId) => request(`/comments/${commentId}`, 'DELETE'),

  // ═══════════════ Likes ═══════════════
  toggleLike: (postId) => request(`/posts/${postId}/like`, 'POST'),
  getLikeStatus: (postId) => request(`/posts/${postId}/like/status`),

  // ═══════════════ Stats ═══════════════
  getWeatherDistribution: (zoneId) => request(`/stats/weather${zoneId ? `?zoneId=${zoneId}` : ''}`),
  getTodayStats: () => request('/stats/today'),
  getMyStats: () => request('/stats/my'),
}

export function saveToken(token) {
  localStorage.setItem('token', token)
}

export function clearToken() {
  localStorage.removeItem('token')
}

export function isLoggedIn() {
  return !!getToken()
}

const PROFILE_KEY = 'userProfile'

export function saveProfile(data) {
  localStorage.setItem(PROFILE_KEY, JSON.stringify(data))
}

export function getProfile() {
  try {
    return JSON.parse(localStorage.getItem(PROFILE_KEY)) || {}
  } catch {
    return {}
  }
}

export function clearProfile() {
  localStorage.removeItem(PROFILE_KEY)
}
