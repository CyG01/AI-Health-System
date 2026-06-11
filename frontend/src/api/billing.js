import request from '@/utils/request'

// ========== 计费与消费明细 ==========
export function getBillingSummary() {
  return request({
    url: '/billing/summary',
    method: 'get'
  })
}

export function getMonthlySummary() {
  return request({
    url: '/billing/monthly',
    method: 'get'
  })
}

export function getBillingHistory(params) {
  return request({
    url: '/billing/history',
    method: 'get',
    params
  })
}

export function getSubscription() {
  return request({
    url: '/billing/subscription',
    method: 'get'
  })
}

export function getQuotaWarning() {
  return request({
    url: '/billing/quota-warning',
    method: 'get'
  })
}

// ========== 退款管理 ==========
export function applyRefund(reason) {
  return request({
    url: '/billing/refund/apply',
    method: 'post',
    data: { reason }
  })
}

export function getRefundStatus() {
  return request({
    url: '/billing/refund/status',
    method: 'get'
  })
}

// ========== 发票管理 ==========
export function applyInvoice(data) {
  return request({
    url: '/billing/invoice/apply',
    method: 'post',
    data
  })
}

export function getInvoiceList() {
  return request({
    url: '/billing/invoice/list',
    method: 'get'
  })
}

export function getInvoiceDetail(invoiceId) {
  return request({
    url: `/billing/invoice/${invoiceId}`,
    method: 'get'
  })
}

export function cancelInvoice(invoiceId) {
  return request({
    url: `/billing/invoice/${invoiceId}/cancel`,
    method: 'post'
  })
}