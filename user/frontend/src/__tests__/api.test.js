import { describe, it, expect, vi, afterEach } from 'vitest'
import { api, saveToken, clearToken, isLoggedIn } from '../api/index.js'

describe('API module', () => {
  afterEach(() => {
    clearToken()
  })

  it('isLoggedIn returns false without token', () => {
    expect(isLoggedIn()).toBe(false)
  })

  it('isLoggedIn returns true with token', () => {
    saveToken('test-token')
    expect(isLoggedIn()).toBe(true)
  })

  it('saveToken and clearToken work correctly', () => {
    saveToken('test-token')
    expect(localStorage.getItem('token')).toBe('test-token')
    clearToken()
    const val = localStorage.getItem('token')
    expect(val === null || val === '').toBe(true)
  })

  it('builds correct API URLs', () => {
    const tests = [
      { fn: api.register, args: ['a@b.c', 'pwd'], url: '/auth/register', method: 'POST' },
      { fn: api.login, args: ['a@b.c', 'pwd'], url: '/auth/login', method: 'POST' },
      { fn: api.getMyPosts, args: [1, 10], url: '/posts/my?page=1&pageSize=10', method: 'GET' },
      { fn: api.getBuildingPosts, args: ['北大楼'], url: '/posts/building?name=%E5%8C%97%E5%A4%A7%E6%A5%BC&page=1&pageSize=20', method: 'GET' },
      { fn: api.getBuildingPosts, args: ['图书馆', 1, 10], url: '/posts/building?name=%E5%9B%BE%E4%B9%A6%E9%A6%86&page=1&pageSize=10', method: 'GET' },
      { fn: api.getComments, args: [1], url: '/posts/1/comments?page=1&pageSize=20', method: 'GET' },
      { fn: api.toggleLike, args: [1], url: '/posts/1/like', method: 'POST' },
      { fn: api.getTodayStats, args: [], url: '/stats/today', method: 'GET' },
      { fn: api.getMyStats, args: [], url: '/stats/my', method: 'GET' },
      { fn: api.getWeatherDistribution, args: ['A'], url: '/stats/weather?zoneId=A', method: 'GET' },
      { fn: api.getWeatherDistribution, args: [], url: '/stats/weather', method: 'GET' },
    ]

    for (const t of tests) {
      const spy = vi.spyOn(globalThis, 'fetch')
      spy.mockResolvedValue({ json: () => Promise.resolve({ code: 0 }) })
      const result = t.fn(...t.args)
      expect(spy).toHaveBeenCalled()
      const callUrl = spy.mock.calls[0][0]
      expect(callUrl).toContain(t.url)
      expect(spy.mock.calls[0][1].method).toBe(t.method)
      spy.mockRestore()
    }
  })

  it('submitPost sends correct body', async () => {
    const spy = vi.spyOn(globalThis, 'fetch')
    spy.mockResolvedValue({ json: () => Promise.resolve({ code: 0 }) })

    await api.submitPost('test content', '图书馆', 'A')
    const options = spy.mock.calls[0][1]
    expect(options.method).toBe('POST')
    expect(options.body).toBe(JSON.stringify({ content: 'test content', buildingName: '图书馆', zoneId: 'A' }))

    spy.mockRestore()
  })

  it('includes Authorization header when token exists', async () => {
    saveToken('my-token')
    const spy = vi.spyOn(globalThis, 'fetch')
    spy.mockResolvedValue({ json: () => Promise.resolve({ code: 0 }) })

    await api.getMyPosts()
    const options = spy.mock.calls[0][1]
    expect(options.headers['Authorization']).toBe('Bearer my-token')

    spy.mockRestore()
    clearToken()
  })
})
