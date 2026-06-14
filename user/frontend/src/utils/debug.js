/**
 * 统一前端 debug 日志工具
 *
 * 生产构建时 (Vite build → import.meta.env.PROD) 所有方法自动变为空函数，零开销。
 * 开发模式下自动启用所有日志。
 *
 * 用法：
 *   import { createLogger } from '../utils/debug.js'
 *   const log = createLogger('HomePage')
 *   log.log('onBuildingClick', data)
 *
 * 控制台过滤（仅开发模式）：
 *   filterDebug('HomePage')   只显示某模块
 *   disableDebug('API')       关闭某模块
 *   disableAllDebug()         全部关闭
 */

const noop = () => {}

let createLogger
let log

if (import.meta.env.DEV) {
  // ═══════════ development: 完整实现 ═══════════

  const namespaceFilters = {}
  let globalEnabled = true

  try {
    const saved = localStorage.getItem('__debug_namespaces')
    if (saved) Object.assign(namespaceFilters, JSON.parse(saved))
    globalEnabled = localStorage.getItem('__debug_enabled') !== 'false'
  } catch {}

  function saveFilters() {
    try { localStorage.setItem('__debug_namespaces', JSON.stringify(namespaceFilters)) } catch {}
  }

  window.filterDebug = (ns) => { namespaceFilters[ns] = true; saveFilters(); console.log(`[DEBUG] 仅显示: ${ns}`) }
  window.disableDebug = (ns) => { namespaceFilters[ns] = false; saveFilters(); console.log(`[DEBUG] 隐藏: ${ns}`) }
  window.enableAllDebug = () => { globalEnabled = true; localStorage.setItem('__debug_enabled', 'true'); console.log('[DEBUG] 全部开启') }
  window.disableAllDebug = () => { globalEnabled = false; localStorage.setItem('__debug_enabled', 'false'); console.log('[DEBUG] 全部关闭') }
  window.clearDebugFilters = () => { Object.keys(namespaceFilters).forEach(k => delete namespaceFilters[k]); saveFilters(); console.log('[DEBUG] 清除过滤') }

  createLogger = (namespace) => {
    const shouldPrint = () => globalEnabled && namespaceFilters[namespace] !== false
    return {
      log: (...args) => { if (shouldPrint()) console.log(`[${namespace}]`, ...args) },
      apiReq: (method, url, body) => {
        if (!shouldPrint()) return
        const safe = body ? { ...body } : undefined
        if (safe?.password) safe.password = '***'
        if (safe?.newPassword) safe.newPassword = '***'
        if (safe?.oldPassword) safe.oldPassword = '***'
        console.log(`[API] ➡️ ${method} ${url}`, safe || '')
      },
      apiRes: (method, url, json) => {
        if (!shouldPrint()) return
        console.log(`[API] ⬅️ ${method} ${url}`, { code: json.code, message: json.message, hasData: !!json.data })
      },
      warn: (...args) => { if (shouldPrint()) console.warn(`[${namespace}]`, ...args) },
      error: (...args) => { if (shouldPrint()) console.error(`[${namespace}]`, ...args) },
    }
  }

  log = createLogger('App')
} else {
  // ═══════════ production: 空函数，零开销 ═══════════
  createLogger = () => ({ log: noop, apiReq: noop, apiRes: noop, warn: noop, error: noop })
  log = { log: noop, apiReq: noop, apiRes: noop, warn: noop, error: noop }
}

export { createLogger, log }
