import { request } from '../request';

/** Export data as CSV - returns a blob for file download */
export function fetchExportCSV() {
  return request<Blob>({
    url: '/export/csv',
    method: 'get',
    responseType: 'blob'
  });
}

/** Export data as Excel - returns a blob for file download */
export function fetchExportExcel() {
  return request<Blob>({
    url: '/export/excel',
    method: 'get',
    responseType: 'blob'
  });
}

// NOTE: The backend only provides synchronous export endpoints (GET /export/csv, GET /export/excel).
// There are no async task-based export endpoints in DataExportController.
// If async export is needed in the future, the backend must add task endpoints first.
