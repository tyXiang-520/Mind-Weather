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

  const res = await fetch(BASE + url, opts)
  const json = await res.json()
  return json
}

export const api = {
  // Auth
  register: (email, password) => request('/auth/register', 'POST', { email, password }),
  login: (email, password) => request('/auth/login', 'POST', { email, password }),
  profile: () => request('/user/profile'),

  // Posts
  submitPost: (content, buildingName, zoneId) => request('/posts/text', 'POST', { content, buildingName, zoneId }),
  getMyPosts: (page = 1, pageSize = 10) => request(`/posts/my?page=${page}&pageSize=${pageSize}`),
  deletePost: (postId) => request(`/posts/${postId}`, 'DELETE'),

  // Map
  getMapOverview: () => request('/map/overview'),
  getMapAreas: () => request('/map/areas'),

  // My Weather
  getTodayWeather: () => request('/my-weather/today'),
  getMyMapData: () => request('/my-weather/map'),
  getEmotionTrend: (period = 'week') => request(`/my-weather/trend?period=${period}`),
  getWeatherCalendar: (month) => request(`/my-weather/calendar?month=${month}`),
  getAreaDistribution: () => request('/my-weather/distribution')
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
