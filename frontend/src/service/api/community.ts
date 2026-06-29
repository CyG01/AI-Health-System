import { request } from '../request';

/** Create a community post */
export function fetchCreatePost(data: Api.Community.CreatePostParams) {
  return request<Api.Community.Post>({
    url: '/community/post',
    method: 'post',
    data
  });
}

/** Delete a community post */
export function fetchDeletePost(postId: number) {
  return request<void>({
    url: `/community/post/${postId}`,
    method: 'delete'
  });
}

/** Get post list (backend returns plain list, not paginated) */
export function fetchGetPostList(page: number = 1, size: number = 10) {
  return request<Api.Community.Post[]>({
    url: '/community/posts',
    method: 'get',
    params: { page, size }
  });
}

/** Get post detail */
export function fetchGetPostDetail(postId: number) {
  return request<Api.Community.Post>({
    url: `/community/post/${postId}`,
    method: 'get'
  });
}

/** Toggle like on a post — returns { isLiked, likeCount } */
export function fetchToggleLike(postId: number) {
  return request<Record<string, unknown>>({
    url: `/community/like/${postId}`,
    method: 'post'
  });
}

/** Create a comment */
export function fetchCreateComment(data: { postId: number; content: string }) {
  return request<Api.Community.Comment>({
    url: '/community/comment',
    method: 'post',
    data
  });
}

/** Delete a comment */
export function fetchDeleteComment(commentId: number) {
  return request<void>({
    url: `/community/comment/${commentId}`,
    method: 'delete'
  });
}

/** Get comments for a post */
export function fetchGetComments(postId: number) {
  return request<Api.Community.Comment[]>({
    url: `/community/comments/${postId}`,
    method: 'get'
  });
}

/** Get community ranking */
export function fetchGetRanking(type: string = 'calories', limit: number = 20) {
  return request<Array<{ userId: number; nickname: string; avatar: string; calories: number }>>({
    url: '/community/ranking',
    method: 'get',
    params: { type, limit }
  });
}
