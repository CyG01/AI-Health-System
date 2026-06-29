import { request } from '../request';

/** Get current user's data consent status */
export function fetchGetPrivacyConsent() {
  return request<Api.Privacy.ConsentStatus>({
    url: '/privacy/consent',
    method: 'get'
  });
}

/** Update user's data consent preferences */
export function fetchUpdatePrivacyConsent(data: Api.Privacy.ConsentUpdateParams) {
  return request<Api.Privacy.ConsentStatus>({
    url: '/privacy/consent',
    method: 'put',
    data
  });
}
