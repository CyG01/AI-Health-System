import request from '@/utils/request'

export function createPost(data) {
  return request({ url: '/community/post', method: 'post', data })
}

export function deletePost(postId) {
  return request({ url: `/community/post/${postId}`, method: 'delete' })
}

export function getPostList(page = 1, size = 10) {
  return request({ url: '/community/posts', method: 'get', params: { page, size } })
}

export function getPostDetail(postId) {
  return request({ url: `/community/post/${postId}`, method: 'get' })
}

export function toggleLike(postId) {
  return request({ url: `/community/like/${postId}`, method: 'post' })
}

export function createComment(data) {
  return request({ url: '/community/comment', method: 'post', data })
}

export function deleteComment(commentId) {
  return request({ url: `/community/comment/${commentId}`, method: 'delete' })
}

export function getComments(postId) {
  return request({ url: `/community/comments/${postId}`, method: 'get' })
}

export function getRanking(type = 'calories', limit = 20) {
  return request({ url: '/community/ranking', method: 'get', params: { type, limit } })
}