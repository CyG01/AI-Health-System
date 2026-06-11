import request from '@/utils/request'

/**
 * 导出 CSV（饮食、运动、体重、打卡）
 * 后端直接返回文件流，前端需要处理 blob 下载
 */
export function exportCSV() {
  return request({
    url: '/export/csv',
    method: 'get',
    responseType: 'blob'
  })
}

/**
 * 导出 Excel（饮食、运动、体重、打卡）
 * 后端直接返回文件流，前端需要处理 blob 下载
 */
export function exportExcel() {
  return request({
    url: '/export/excel',
    method: 'get',
    responseType: 'blob'
  })
}