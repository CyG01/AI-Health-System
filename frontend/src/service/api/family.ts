import { request } from '../request';

/** Get current user's family list */
export function fetchMyFamilies() {
  return request<Api.Family.Family[]>({
    url: '/v1/family/my-families',
    method: 'get'
  });
}

/** Get primary family */
export function fetchPrimaryFamily() {
  return request<Api.Family.Family>({
    url: '/v1/family/primary',
    method: 'get'
  });
}

/** Get family members */
export function fetchFamilyMembers(familyId: number) {
  return request<Api.Family.Member[]>({
    url: `/v1/family/${familyId}/members`,
    method: 'get'
  });
}

/** Create family group */
export function fetchCreateFamily(familyName: string) {
  return request<Api.Family.Family>({
    url: '/v1/family/create',
    method: 'post',
    params: { familyName }
  });
}

/** Invite member to family */
export function fetchInviteMember(familyId: number, phone: string, role: string) {
  return request<void>({
    url: `/v1/family/${familyId}/invite`,
    method: 'post',
    params: { phone, role }
  });
}

/** Join family via invite code */
export function fetchJoinFamily(inviteCode: string) {
  return request<void>({
    url: '/v1/family/join',
    method: 'post',
    params: { inviteCode }
  });
}

/** Leave family */
export function fetchLeaveFamily(familyId: number) {
  return request<void>({
    url: `/v1/family/${familyId}/leave`,
    method: 'post'
  });
}

/** Get viewable member IDs */
export function fetchViewableMembers() {
  return request<number[]>({
    url: '/v1/family/viewable-members',
    method: 'get'
  });
}
