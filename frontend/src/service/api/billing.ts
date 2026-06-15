import { request } from '../request';

/** Get billing summary */
export function fetchGetBillingSummary() {
  return request<Api.Billing.Balance>({
    url: '/billing/summary',
    method: 'get'
  });
}

/** Get monthly billing summary */
export function fetchGetMonthlySummary() {
  return request<{ month: string; totalSpent: number; aiCalls: number; reportsGenerated: number; plansGenerated: number }>({
    url: '/billing/monthly',
    method: 'get'
  });
}

/** Get billing history (usage records for last N days) */
export function fetchGetBillingHistory(params?: { days?: number }) {
  return request<Api.Billing.UserUsage[]>({
    url: '/billing/history',
    method: 'get',
    params
  });
}

/** Get current subscription */
export function fetchGetSubscription() {
  return request<{ id: number; userId: number; plan: string; status: string; startDate: string; endDate: string; autoRenew: boolean; price: number }>({
    url: '/billing/subscription',
    method: 'get'
  });
}

/** Get quota warning info */
export function fetchGetQuotaWarning() {
  return request<{ isWarning: boolean; remainingQuota: number; totalQuota: number; usagePercent: number; message: string }>({
    url: '/billing/quota-warning',
    method: 'get'
  });
}

/** Apply for a refund */
export function fetchApplyRefund(reason: string) {
  return request<Api.Billing.RefundRequest>({
    url: '/billing/refund/apply',
    method: 'post',
    data: { reason }
  });
}

/** Get refund status */
export function fetchGetRefundStatus() {
  return request<Api.Billing.RefundRequest>({
    url: '/billing/refund/status',
    method: 'get'
  });
}

/** Apply for an invoice */
export function fetchApplyInvoice(data: { orderNo: string; invoiceType: string; invoiceTitle: string; taxNumber?: string }) {
  return request<Api.Billing.Invoice>({
    url: '/billing/invoice/apply',
    method: 'post',
    data
  });
}

/** Get invoice list */
export function fetchGetInvoiceList() {
  return request<Api.Billing.Invoice[]>({
    url: '/billing/invoice/list',
    method: 'get'
  });
}

/** Get invoice detail */
export function fetchGetInvoiceDetail(invoiceId: number) {
  return request<Api.Billing.Invoice>({
    url: `/billing/invoice/${invoiceId}`,
    method: 'get'
  });
}

/** Cancel an invoice */
export function fetchCancelInvoice(invoiceId: number) {
  return request<void>({
    url: `/billing/invoice/${invoiceId}/cancel`,
    method: 'post'
  });
}
