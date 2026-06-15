import { request } from '../request';

/** Get food items from library */
export function fetchGetFoodItems(params?: { category?: string }) {
  return request<Api.Food.FoodItem[]>({
    url: '/food/items',
    method: 'get',
    params
  });
}

/** Submit food record */
export function fetchSubmitFoodRecord(data: Api.Food.CreateFoodParams) {
  return request<Api.Food.FoodRecord>({
    url: '/food/record',
    method: 'post',
    data
  });
}

/** Get food records by user ID */
export function fetchGetFoodRecordsByUserId(params?: { page?: number; size?: number; startDate?: string; endDate?: string }) {
  return request<Api.Common.PageResult<Api.Food.FoodRecord>>({
    url: '/food/record/user',
    method: 'get',
    params
  });
}

/** Get food records by check-in ID */
export function fetchGetFoodRecordsByCheckinId(checkinId: number) {
  return request<Api.Food.FoodRecord[]>({
    url: `/food/record/checkin/${checkinId}`,
    method: 'get'
  });
}

/** Get food records page (paginated) */
export function fetchGetFoodRecordsPage(params?: { page?: number; size?: number; startDate?: string; endDate?: string }) {
  return request<Api.Common.PageResult<Api.Food.FoodRecord>>({
    url: '/food/records',
    method: 'get',
    params
  });
}

/** Get food records by date (paginated) */
export function fetchGetFoodRecordsByDate(date: string, params?: { page?: number; size?: number }) {
  return request<Api.Common.PageResult<Api.Food.FoodRecord>>({
    url: `/food/records/${date}`,
    method: 'get',
    params
  });
}

/** Recognize food from image */
export function fetchRecognizeFood(formData: FormData) {
  return request<{ items: Array<{ foodName: string; weightG: number; calories: number }> }>({
    url: '/food/recognize',
    method: 'post',
    data: formData
  });
}

/** Get user's frequent food items */
export function fetchGetFrequentItems(params?: { limit?: number }) {
  return request<Api.Food.FoodItem[]>({
    url: '/food/items/frequent',
    method: 'get',
    params
  });
}

/** Parse food text (quick text entry) */
export function fetchParseFoodText(params: { text: string }) {
  return request<Api.Food.FoodItem>({
    url: '/food/items/parse',
    method: 'get',
    params
  });
}

/** Update food record */
export function fetchUpdateFoodRecord(id: number, data: Partial<Api.Food.CreateFoodParams>) {
  return request<Api.Food.FoodRecord>({
    url: `/food/record/${id}`,
    method: 'put',
    data
  });
}

/** Delete food record */
export function fetchDeleteFoodRecord(id: number) {
  return request<void>({
    url: `/food/record/${id}`,
    method: 'delete'
  });
}

/** Recognize food by natural language text */
export function fetchRecognizeByText(data: { text: string }) {
  return request<{ items: Array<{ foodName: string; weightG: number; calories: number }> }>({
    url: '/food/recognize-text',
    method: 'post',
    data
  });
}
