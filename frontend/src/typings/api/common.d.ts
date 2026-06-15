/**
 * Namespace Api
 *
 * All backend api type
 */
declare namespace Api {
  namespace Common {
    /** common params of paginating */
    interface PaginatingCommonParams {
      /** current page number */
      current: number;
      /** page size */
      size: number;
      /** total count */
      total: number;
    }

    /** common params of paginating query list data */
    interface PaginatingQueryRecord<T = any> extends PaginatingCommonParams {
      records: T[];
    }

    /** common search params of table */
    type CommonSearchParams = Pick<Common.PaginatingCommonParams, 'current' | 'size'>;

    /**
     * enable status
     *
     * - "1": enabled
     * - "2": disabled
     */
    type EnableStatus = '1' | '2';

    /** common record */
    type CommonRecord<T = any> = {
      /** record id */
      id: number;
      /** record creator */
      createBy: string;
      /** record create time */
      createTime: string;
      /** record updater */
      updateBy: string;
      /** record update time */
      updateTime: string;
      /** record status */
      status: EnableStatus | null;
    } & T;

    // Health-specific common types

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

    /** Generic date range params */
    interface DateRangeParams {
      startDate?: string;
      endDate?: string;
    }
  }
}
