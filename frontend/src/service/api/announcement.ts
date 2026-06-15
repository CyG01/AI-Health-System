import { request } from '../request';

/** Get announcement list (user-facing) */
export function fetchGetAnnouncementList(params?: { page?: number; size?: number }) {
  return request<Api.Common.PageResult<Api.Admin.Announcement>>({
    url: '/announcement/list',
    method: 'get',
    params
  });
}

/** Get announcement detail */
export function fetchGetAnnouncementDetail(id: number) {
  return request<Api.Admin.Announcement>({
    url: `/announcement/${id}`,
    method: 'get'
  });
}
