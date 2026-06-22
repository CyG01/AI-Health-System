import { request } from '../request';

/** Get exercise items from library */
export function fetchGetExerciseItems(params?: { type?: string }) {
  return request<Api.Exercise.ExerciseItem[]>({
    url: '/exercise/items',
    method: 'get',
    params
  });
}

/** Submit exercise record */
export function fetchSubmitExerciseRecord(data: Api.Exercise.CreateExerciseParams) {
  return request<Api.Exercise.ExerciseRecord>({
    url: '/exercise/record',
    method: 'post',
    data
  });
}

/** Get exercise records by user ID */
export function fetchGetExerciseRecordsByUserId(params?: { limit?: number }) {
  return request<Api.Exercise.ExerciseRecord[]>({
    url: '/exercise/record/user',
    method: 'get',
    params
  });
}

/** Get exercise records by check-in ID */
export function fetchGetExerciseRecordsByCheckinId(checkinId: number) {
  return request<Api.Exercise.ExerciseRecord[]>({
    url: `/exercise/record/checkin/${checkinId}`,
    method: 'get'
  });
}

/** Get exercise records page (paginated) */
export function fetchGetExerciseRecordsPage(params?: { page?: number; size?: number; startDate?: string; endDate?: string }) {
  return request<Api.Common.PageResult<Api.Exercise.ExerciseRecord>>({
    url: '/exercise/records',
    method: 'get',
    params
  });
}

/** Get exercise records by date (paginated) */
export function fetchGetExerciseRecordsByDate(date: string, params?: { page?: number; size?: number }) {
  return request<Api.Common.PageResult<Api.Exercise.ExerciseRecord>>({
    url: `/exercise/records/${date}`,
    method: 'get',
    params
  });
}

/** Get exercise guidance */
export function fetchGetExerciseGuidance(exerciseItemId: number) {
  return request<{ exerciseId: number; exerciseName: string; steps: string[]; tips: string[]; warnings: string[]; videoUrl?: string }>({
    url: `/exercise/${exerciseItemId}/guidance`,
    method: 'get'
  });
}

/** Update exercise record */
export function fetchUpdateExerciseRecord(id: number, data: Partial<Api.Exercise.CreateExerciseParams>) {
  return request<Api.Exercise.ExerciseRecord>({
    url: `/exercise/record/${id}`,
    method: 'put',
    data
  });
}

/** Delete exercise record */
export function fetchDeleteExerciseRecord(id: number) {
  return request<void>({
    url: `/exercise/record/${id}`,
    method: 'delete'
  });
}
