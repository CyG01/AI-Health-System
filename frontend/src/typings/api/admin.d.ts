declare namespace Api {
  namespace Admin {
    /** Admin user list item */
    interface UserItem {
      id: number;
      username: string;
      email: string;
      phone: string;
      avatar: string;
      role: string;
      status: number;
      createdAt: string;
      lastLoginAt?: string;
    }

    interface UserListParams {
      page?: number;
      size?: number;
      keyword?: string;
      role?: string;
      status?: string;
    }

    interface Announcement {
      id: number;
      title: string;
      content: string;
      status: string;
      createdAt: string;
      updatedAt: string;
    }

    interface AnnouncementParams {
      title: string;
      content: string;
      status?: string;
    }

    /** AI feedback review item (entity: AiFeedback) */
    interface AiFeedback {
      id: number;
      userId: number;
      aiResponseId: string;
      rating: string;
      comment: string;
      reviewStatus: string;
      reviewedBy: number;
      reviewedAt: string;
      createTime: string;
    }

    /** Rule suggestion from AI analysis (entity: RuleSuggestion) */
    interface RuleSuggestion {
      id: number;
      suggestionType: string;
      ruleCategory: string;
      triggerPattern: string;
      action: string;
      priority: number;
      reason: string;
      sourceSampleIds: string;
      hitCount: number;
      status: string;
      reviewedBy: string;
      reviewedAt: string;
      createdAt: string;
    }

    /** Admin approval record (entity: AdminApproval) */
    interface Approval {
      id: number;
      operatorId: number;
      operatorName: string;
      actionType: string;
      targetDescription: string;
      requestPayload: string;
      status: string;
      approverId: number;
      approverName: string;
      approveReason: string;
      requestedAt: string;
      approvedAt: string;
      executed: number;
    }

    /** Params for approve/reject an approval request */
    interface ApprovalActionParams {
      approverName: string;
      reason?: string;
    }

    /** Admin audit log entry (entity: AdminAuditLog) */
    interface AuditLog {
      id: number;
      operatorId: number;
      operatorName: string;
      action: string;
      targetType: string;
      targetId: number;
      detail: string;
      ip: string;
      createTime: string;
    }

    /** Query params for audit log pagination */
    interface AuditLogParams {
      page?: number;
      size?: number;
      action?: string;
      operatorName?: string;
    }

    /** Plan feedback VO for admin view */
    interface PlanFeedbackVO {
      id: number;
      planId: number;
      userId: number;
      feedbackType: string;
      content: string;
      satisfactionScore: number;
      adjustmentSuggestion: string;
      isAdjusted: number;
      newPlanId: number | null;
      createTime: string;
    }

    /** Admin user detail (AdminUserDetailVO) — includes profile, plans, checkins, exercise, diet stats */
    interface UserDetail {
      userId: number;
      username: string;
      email: string;
      phone: string;
      avatar: string;
      role: string;
      status: string;
      createdAt: string;
      lastLoginAt?: string;
      planStats?: Record<string, unknown>;
      checkinStats?: Record<string, unknown>;
      exerciseStats?: Record<string, unknown>;
      dietStats?: Record<string, unknown>;
      [key: string]: unknown;
    }
  }
}
