import { request } from '../request';

/** Activate enterprise plan */
export function fetchActivateEnterprisePlan(params: Api.Enterprise.ActivateParams) {
  return request<Api.Enterprise.Subscription>({
    url: '/enterprise/activate',
    method: 'post',
    params
  });
}

/** Update enterprise config */
export function fetchUpdateEnterpriseConfig(params: Api.Enterprise.UpdateConfigParams) {
  return request<Api.Enterprise.Subscription>({
    url: '/enterprise/config',
    method: 'put',
    params
  });
}

/** Get enterprise config */
export function fetchGetEnterpriseConfig() {
  return request<Api.Enterprise.Subscription>({
    url: '/enterprise/config',
    method: 'get'
  });
}
