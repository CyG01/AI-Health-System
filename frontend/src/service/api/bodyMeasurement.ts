// Body measurement functions are now in body.ts (using Api.Health types)
// Re-exporting for backward compatibility
export {
  fetchSubmitBodyMeasurement,
  fetchGetLatestBodyMeasurement,
  fetchGetBodyMeasurementHistory,
  fetchGetBodyMeasurementTrend,
  fetchDeleteBodyMeasurement
} from './body';
