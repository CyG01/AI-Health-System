/**
 * Namespace Api
 *
 * All backend api type
 */
declare namespace Api {
  namespace Common {
    /** Gender type */
    type Gender = 'MALE' | 'FEMALE' | 'OTHER';

    /** Record status */
    type RecordStatus = 'ACTIVE' | 'INACTIVE' | 'DELETED';

    /** AI plan status */
    type PlanStatus = 'GENERATING' | 'ACTIVE' | 'COMPLETED' | 'EXPIRED';

    /** Approval status */
    type ApprovalStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

    /** Meal type */
    type MealType = 'BREAKFAST' | 'LUNCH' | 'DINNER' | 'SNACK';

    /** Blood sugar measurement period */
    type BloodSugarPeriod = 'FASTING' | 'AFTER_MEAL' | 'BEFORE_MEAL' | 'BEDTIME';

    /** Exercise intensity */
    type ExerciseIntensity = 'LOW' | 'MEDIUM' | 'HIGH';

    /** Health report type */
    type ReportType = 'WEEKLY' | 'MONTHLY';

    /** Plan type */
    type PlanType = 'FITNESS' | 'DIET' | 'COMPREHENSIVE';

    /** Generic paginated response from backend */
    interface PageResult<T> {
      records: T[];
      total: number;
      current: number;
      size: number;
      pages: number;
    }
  }
}
