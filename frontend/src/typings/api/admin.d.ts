declare namespace Api {
  namespace Admin {
    /** Admin user list item — aligned with backend UserInfoVO */
    interface UserItem {
      id: number;
      username: string;
      phone: string;
      nickname: string;
      avatar: string;
      gender: number;
      age: number;
      role: string;
      status: number;
      createTime: string;
    }

    interface UserListParams {
      page?: number;
      size?: number;
      keyword?: string;
      role?: string;
      status?: string;
    }

    /** Announcement — aligned with backend SysAnnouncement entity */
    interface Announcement {
      id: number;
      title: string;
      content: string;
      adminId: number;
      status: number;
      createTime: string;
      updateTime: string;
      isDeleted: number;
      version: number;
    }

    interface AnnouncementParams {
      title: string;
      content: string;
    }

    /** AI feedback review item — aligned with backend AiFeedback entity */
    interface AiFeedback {
      id: number;
      userId: number;
      aiResponseId: string;
      rating: string;
      comment: string;
      manualReviewed: number;
      reviewerId: number;
      reviewResult: string;
      resolvedAt: string;
      createdAt: string;
      updatedAt: string;
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

    /** Admin user detail — aligned with backend AdminUserDetailVO (flat structure) */
    interface UserDetail {
      id: number;
      username: string;
      phone: string;
      avatar: string;
      role: string;
      status: number;
      createTime: string;
      lastLoginTime: string;
      // 健康档案
      height: number;
      weight: number;
      age: number;
      gender: number;
      bmiLevel: string;
      // 计划统计
      totalPlans: number;
      activePlanCount: number;
      // 打卡统计
      totalCheckinDays: number;
      consecutiveDays: number;
      lastCheckinDate: string;
      // 运动统计
      totalExerciseRecords: number;
      totalExerciseCalories: number;
      // 饮食统计
      totalDietRecords: number;
      totalDietCalories: number;
    }
  }
}
