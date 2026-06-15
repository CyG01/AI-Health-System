import { request } from '../request';

/** Activate enterprise plan */
export function fetchActivateEnterprisePlan(data: Api.Enterprise.ActivateParams) {
  return request<Api.Enterprise.Subscription>({
    url: '/enterprise/activate',
    method: 'post',
    data
  });
}

/** Update enterprise config */
export function fetchUpdateEnterpriseConfig(data: Partial<{ companyName: string; maxUsers: number; features: string[] }>) {
  return request<{ id: number; companyName: string; plan: string; maxUsers: number; currentUsers: number; features: string[]; expiresAt: string; status: string }>({
    url: '/enterprise/config',
    method: 'put',
    data
  });
}

/** Get enterprise config */
export function fetchGetEnterpriseConfig() {
  return request<{ id: number; companyName: string; plan: string; maxUsers: number; currentUsers: number; features: string[]; expiresAt: string; status: string }>({
    url: '/enterprise/config',
    method: 'get'
  });
}
