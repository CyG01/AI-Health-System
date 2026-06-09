import request from '@/utils/request'

export function generatePlan(data) {
  return request({
    url: '/ai-plan/generate',
    method: 'post',
    data
  })
}

export function generatePlanStream(data) {
  let onMessage = null
  let onError = null
  let resolvePromise = null
  let rejectPromise = null

  const promise = new Promise((resolve, reject) => {
    resolvePromise = resolve
    rejectPromise = reject
  })

  function start() {
    const token = localStorage.getItem('accessToken')
    const url = '/api/ai-plan/generate-stream'

    fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': token ? `Bearer ${token}` : ''
      },
      body: JSON.stringify(data)
    }).then(response => {
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`)
      }
      const reader = response.body.getReader()
      const decoder = new TextDecoder()
      let buffer = ''

      function processText() {
        reader.read().then(({ done, value }) => {
          if (done) {
            resolvePromise()
            return
          }
          buffer += decoder.decode(value, { stream: true })
          const lines = buffer.split('\n')
          buffer = lines.pop() || ''

          for (const line of lines) {
            if (line.startsWith('data:')) {
              const text = line.substring(5).trim()
              if (text && onMessage) {
                onMessage(text)
              }
            }
          }
          processText()
        }).catch(err => {
          if (onError) onError(err)
          rejectPromise(err)
        })
      }
      processText()
    }).catch(err => {
      if (onError) onError(err)
      rejectPromise(err)
    })
  }

  setTimeout(start, 0)

  return {
    then: (...args) => promise.then(...args),
    catch: (...args) => promise.catch(...args),
    finally: (...args) => promise.finally(...args),
    set onMessage(fn) { onMessage = fn },
    set onError(fn) { onError = fn }
  }
}

export function getPlanList() {
  return request({
    url: '/ai-plan/list',
    method: 'get'
  })
}

export function getPlanDetail(id) {
  return request({
    url: `/ai-plan/${id}`,
    method: 'get'
  })
}

export function activePlan(id) {
  return request({
    url: `/ai-plan/${id}/active`,
    method: 'put'
  })
}

export function deletePlan(id) {
  return request({
    url: `/ai-plan/${id}`,
    method: 'delete'
  })
}
