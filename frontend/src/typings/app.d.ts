/** The global namespace for the app */
declare namespace App {
  /** Theme namespace */
  namespace Theme {
    type ColorPaletteNumber = import('@sa/color').ColorPaletteNumber;

    type NaiveUIThemeOverride = import('naive-ui').GlobalThemeOverrides;

    interface ThemeSetting {
      themeScheme: UnionKey.ThemeScheme;
      grayscale: boolean;
      colourWeakness: boolean;
      recommendColor: boolean;
      themeColor: string;
      themeRadius: number;
      otherColor: OtherColor;
      isInfoFollowPrimary: boolean;
      layout: {
        mode: UnionKey.ThemeLayoutMode;
        scrollMode: UnionKey.ThemeScrollMode;
      };
      page: {
        animate: boolean;
        animateMode: UnionKey.ThemePageAnimateMode;
      };
      header: {
        height: number;
        breadcrumb: {
          visible: boolean;
          showIcon: boolean;
        };
        multilingual: {
          visible: boolean;
        };
        globalSearch: {
          visible: boolean;
        };
      };
      tab: {
        visible: boolean;
        cache: boolean;
        height: number;
        mode: UnionKey.ThemeTabMode;
        closeTabByMiddleClick: boolean;
      };
      fixedHeaderAndTab: boolean;
      sider: {
        inverted: boolean;
        width: number;
        collapsedWidth: number;
        mixWidth: number;
        mixCollapsedWidth: number;
        mixChildMenuWidth: number;
        autoSelectFirstMenu: boolean;
      };
      footer: {
        visible: boolean;
        fixed: boolean;
        height: number;
        right: boolean;
      };
      watermark: {
        visible: boolean;
        text: string;
        enableUserName: boolean;
        enableTime: boolean;
        timeFormat: string;
      };
      tokens: {
        light: ThemeSettingToken;
        dark?: {
          [K in keyof ThemeSettingToken]?: Partial<ThemeSettingToken[K]>;
        };
      };
    }

    interface OtherColor {
      info: string;
      success: string;
      warning: string;
      error: string;
    }

    interface ThemeColor extends OtherColor {
      primary: string;
    }

    type ThemeColorKey = keyof ThemeColor;
    type ThemePaletteColor = {
      [key in ThemeColorKey | `${ThemeColorKey}-${ColorPaletteNumber}`]: string;
    };

    type BaseToken = Record<string, Record<string, string>>;

    interface ThemeSettingTokenColor {
      nprogress?: string;
      container: string;
      layout: string;
      inverted: string;
      'base-text': string;
    }

    interface ThemeSettingTokenBoxShadow {
      header: string;
      sider: string;
      tab: string;
    }

    interface ThemeSettingToken {
      colors: ThemeSettingTokenColor;
      boxShadow: ThemeSettingTokenBoxShadow;
    }

    type ThemeTokenColor = ThemePaletteColor & ThemeSettingTokenColor;

    type ThemeTokenCSSVars = {
      colors: ThemeTokenColor & { [key: string]: string };
      boxShadow: ThemeSettingTokenBoxShadow & { [key: string]: string };
    };
  }

  /** Global namespace */
  namespace Global {
    type VNode = import('vue').VNode;
    type RouteLocationNormalizedLoaded = import('vue-router').RouteLocationNormalizedLoaded;

    type RouterPushOptions = {
      query?: Record<string, string>;
      params?: Record<string, string>;
      force?: boolean;
    };

    interface HeaderProps {
      showLogo?: boolean;
      showMenuToggler?: boolean;
      showMenu?: boolean;
    }

    type Menu = {
      key: string;
      label: string;
      i18nKey?: I18n.I18nKey | null;
      routeKey: string;
      routePath: string;
      icon?: () => VNode;
      children?: Menu[];
    };

    type Breadcrumb = Omit<Menu, 'children'> & {
      options?: Breadcrumb[];
    };

    type TabRoute = Pick<RouteLocationNormalizedLoaded, 'name' | 'path' | 'meta'> &
      Partial<Pick<RouteLocationNormalizedLoaded, 'fullPath' | 'query' | 'matched'>>;

    type Tab = {
      id: string;
      label: string;
      newLabel?: string;
      oldLabel?: string;
      routeKey: string;
      routePath: string;
      fullPath: string;
      fixedIndex?: number | null;
      icon?: string;
      localIcon?: string;
      i18nKey?: I18n.I18nKey | null;
    };

    /** Form rule */
    type FormRule = import('naive-ui').FormItemRule;

    type DropdownKey = 'closeCurrent' | 'closeOther' | 'closeLeft' | 'closeRight' | 'closeAll' | 'pin' | 'unpin';
  }

  /**
   * I18n namespace
   *
   * Locales type
   */
  namespace I18n {
    type LangType = 'en-US' | 'zh-CN';

    type LangOption = {
      label: string;
      key: LangType;
    };

    type FormMsg = {
      required: string;
      invalid: string;
    };

    type Schema = {
      system: {
        title: string;
        updateTitle: string;
        updateContent: string;
        updateConfirm: string;
        updateCancel: string;
      };
      common: {
        action: string;
        add: string;
        addSuccess: string;
        backToHome: string;
        batchDelete: string;
        cancel: string;
        close: string;
        check: string;
        selectAll: string;
        expandColumn: string;
        columnSetting: string;
        config: string;
        confirm: string;
        delete: string;
        deleteSuccess: string;
        confirmDelete: string;
        edit: string;
        warning: string;
        error: string;
        index: string;
        keywordSearch: string;
        logout: string;
        logoutConfirm: string;
        lookForward: string;
        modify: string;
        modifySuccess: string;
        noData: string;
        operate: string;
        pleaseCheckValue: string;
        refresh: string;
        reset: string;
        search: string;
        switch: string;
        tip: string;
        trigger: string;
        update: string;
        updateSuccess: string;
        userCenter: string;
        save: string;
        saveSuccess: string;
        submit: string;
        submitSuccess: string;
        loading: string;
        success: string;
        failed: string;
        back: string;
        next: string;
        prev: string;
        total: string;
        status: string;
        enable: string;
        disable: string;
        enabled: string;
        disabled: string;
        yesOrNo: {
          yes: string;
          no: string;
        };
      };
      request: {
        logout: string;
        logoutMsg: string;
        logoutWithModal: string;
        logoutWithModalMsg: string;
        refreshToken: string;
        tokenExpired: string;
      };
      theme: {
        themeDrawerTitle: string;
        tabs: {
          appearance: string;
          layout: string;
          general: string;
          preset: string;
        };
        appearance: {
          themeSchema: { title: string } & Record<UnionKey.ThemeScheme, string>;
          grayscale: string;
          colourWeakness: string;
          themeColor: {
            title: string;
            followPrimary: string;
          } & Record<Theme.ThemeColorKey, string>;
          recommendColor: string;
          recommendColorDesc: string;
          themeRadius: {
            title: string;
          };
          preset: {
            title: string;
            apply: string;
            applySuccess: string;
            [key: string]:
              | {
                  name: string;
                  desc: string;
                }
              | string;
          };
        };
        layout: {
          layoutMode: { title: string } & Record<UnionKey.ThemeLayoutMode, string> & {
              [K in `${UnionKey.ThemeLayoutMode}_detail`]: string;
            };
          tab: {
            title: string;
            visible: string;
            cache: string;
            cacheTip: string;
            height: string;
            mode: { title: string } & Record<UnionKey.ThemeTabMode, string>;
            closeByMiddleClick: string;
            closeByMiddleClickTip: string;
          };
          header: {
            title: string;
            height: string;
            breadcrumb: {
              visible: string;
              showIcon: string;
            };
          };
          sider: {
            title: string;
            inverted: string;
            width: string;
            collapsedWidth: string;
            mixWidth: string;
            mixCollapsedWidth: string;
            mixChildMenuWidth: string;
            autoSelectFirstMenu: string;
            autoSelectFirstMenuTip: string;
          };
          footer: {
            title: string;
            visible: string;
            fixed: string;
            height: string;
            right: string;
          };
          content: {
            title: string;
            scrollMode: { title: string; tip: string } & Record<UnionKey.ThemeScrollMode, string>;
            page: {
              animate: string;
              mode: { title: string } & Record<UnionKey.ThemePageAnimateMode, string>;
            };
            fixedHeaderAndTab: string;
          };
        };
        general: {
          title: string;
          watermark: {
            title: string;
            visible: string;
            text: string;
            enableUserName: string;
            enableTime: string;
            timeFormat: string;
          };
          multilingual: {
            title: string;
            visible: string;
          };
          globalSearch: {
            title: string;
            visible: string;
          };
        };
        configOperation: {
          copyConfig: string;
          copySuccessMsg: string;
          resetConfig: string;
          resetSuccessMsg: string;
        };
      };
      route: {
        login: string;
        403: string;
        404: string;
        500: string;
        dashboard: string;
        'iframe-page': string;
        auth_register: string;
        'auth_forgot-password': string;
        auth_profile: string;
        health: string;
        health_create: string;
        health_form: string;
        health_view: string;
        health_report: string;
        'health_blood-sugar': string;
        plan: string;
        plan_generate: string;
        plan_list: string;
        plan_detail: string;
        checkin: string;
        checkin_calendar: string;
        food: string;
        food_record: string;
        exercise: string;
        exercise_record: string;
        sleep: string;
        sleep_record: string;
        water: string;
        water_record: string;
        statistics: string;
        statistics_dashboard: string;
        body: string;
        body_measurement: string;
        goal: string;
        goal_milestones: string;
        community: string;
        community_feed: string;
        chat: string;
        chat_chatbot: string;
        notification: string;
        notification_list: string;
        settings: string;
        'settings_notification-preference': string;
        billing: string;
        billing_billing: string;
        'billing_refund-invoice': string;
        enterprise: string;
        enterprise_activate: string;
        export: string;
        export_export: string;
        admin: string;
        'admin_user-manage': string;
        'admin_announcement-manage': string;
        'admin_food-manage': string;
        'admin_exercise-manage': string;
        'admin_notification-send': string;
        'admin_plan-feedback': string;
        'admin_audit-log': string;
        'admin_approval-manage': string;
        'admin_rule-suggestion': string;
        'admin_ai-feedback': string;
        'admin_llm-cost-monitor': string;
        'admin_llm-ops': string;
        settings_privacy: string;
      };
      page: {
        login: {
          common: {
            loginOrRegister: string;
            userNamePlaceholder: string;
            phonePlaceholder: string;
            codePlaceholder: string;
            passwordPlaceholder: string;
            confirmPasswordPlaceholder: string;
            codeLogin: string;
            confirm: string;
            back: string;
            validateSuccess: string;
            loginSuccess: string;
            welcomeBack: string;
          };
          pwdLogin: {
            title: string;
            rememberMe: string;
            forgetPassword: string;
            register: string;
            otherAccountLogin: string;
            otherLoginMode: string;
            superAdmin: string;
            admin: string;
            user: string;
            captchaPlaceholder: string;
            refreshCaptcha: string;
          };
          codeLogin: {
            title: string;
            getCode: string;
            reGetCode: string;
            sendCodeSuccess: string;
            imageCodePlaceholder: string;
          };
          register: {
            title: string;
            agreement: string;
            protocol: string;
            policy: string;
          };
          resetPwd: {
            title: string;
          };
          bindWeChat: {
            title: string;
          };
        };
        home: {
          branchDesc: string;
          greeting: string;
          weatherDesc: string;
          projectCount: string;
          todo: string;
          message: string;
          downloadCount: string;
          registerCount: string;
          schedule: string;
          study: string;
          work: string;
          rest: string;
          entertainment: string;
          visitCount: string;
          turnover: string;
          dealCount: string;
          projectNews: {
            title: string;
            moreNews: string;
            desc1: string;
            desc2: string;
            desc3: string;
            desc4: string;
            desc5: string;
          };
          creativity: string;
        };
        dashboard: {
          today: string;
          week: string;
          month: string;
          greeting: string;
          checkinStatus: string;
          planProgress: string;
          weightTrend: string;
          checkinChart: string;
          dietComparison: string;
          healthAssessment: string;
          goalProgress: string;
          aiRecommendations: string;
          weight: string;
          bmi: string;
          exerciseCaloriesBurned: string;
          dietCaloriesConsumed: string;
          isCheckedIn: string;
          notCheckedIn: string;
          streakDays: string;
          planName: string;
          completedTasks: string;
          exerciseRecords: string;
          dietRecords: string;
          todayTasks: string;
          taskName: string;
          taskType: string;
          taskTarget: string;
          taskStatus: string;
          sportType: string;
          dietType: string;
          completed: string;
          uncompleted: string;
          checkinDaysWeek: string;
          exerciseCaloriesWeek: string;
          dietCaloriesWeek: string;
          recordsCount: string;
          dailyDetail: string;
          date: string;
          checkedIn: string;
          notChecked: string;
          exerciseCount: string;
          dietCount: string;
          dietComparisonWeek: string;
          currentWeek: string;
          previousWeek: string;
          checkinDaysMonth: string;
          checkinRate: string;
          monthlyExerciseCalories: string;
          monthlyDietCalories: string;
          monthlyExerciseRecords: string;
          monthlyDietRecords: string;
          weeklySummary: string;
          weekLabel: string;
          checkinDaysCount: string;
          healthGoalProgress: string;
          goalRate: string;
          checkinRateLabel: string;
          exerciseRate: string;
          dietRate: string;
          weightChange: string;
          bmiLevel: string;
          healthScore: string;
          risk: string;
          noHealthData: string;
          noProgressData: string;
          aiExercise: string;
          aiFood: string;
          healthTips: string;
          aiSuggestions: string;
          noRecommend: string;
          caloriesPerHour: string;
          caloriesPer100g: string;
          proteinPer100g: string;
          retry: string;
          dataLoadFailed: string;
          days: string;
          kcal: string;
          kg: string;
          score: string;
          items: string;
          percent: string;
          recent30Days: string;
          matchScore: string;
        };
        health: {
          create: string;
          edit: string;
          view: string;
          report: string;
          bloodSugar: string;
          height: string;
          weight: string;
          bmi: string;
          goalWeight: string;
          bloodType: string;
          allergies: string;
          medicalHistory: string;
          weeklyReport: string;
          monthlyReport: string;
          fasting: string;
          afterMeal: string;
          beforeMeal: string;
          bedtime: string;
          diseaseHistory: string;
          allergyHistory: string;
          exerciseHabit: string;
          dietHabit: string;
          healthAssessment: string;
          healthRecord: string;
          gender: string;
          male: string;
          female: string;
          age: string;
          birthDate: string;
          phone: string;
          email: string;
          basicInfo: string;
          healthInfo: string;
          lifestyle: string;
          saveSuccess: string;
          noRecord: string;
          bloodPressure: string;
          heartRate: string;
          temperature: string;
        };
        plan: {
          generate: string;
          list: string;
          detail: string;
          myPlan: string;
          noPlan: string;
          generateNew: string;
          aiPlan: string;
          planType: string;
          durationDays: string;
          intensity: string;
          sport: string;
          diet: string;
          comprehensive: string;
          rehabilitation: string;
          meditation: string;
          low: string;
          medium: string;
          high: string;
          planName: string;
          planDesc: string;
          startDate: string;
          endDate: string;
          progress: string;
          tasks: string;
          completedTasks: string;
          dailyTasks: string;
          weeklyTasks: string;
          generateSuccess: string;
          generating: string;
        };
        checkin: {
          calendar: string;
          today: string;
          streak: string;
          checked: string;
          unchecked: string;
          foodRecord: string;
          exerciseRecord: string;
          sleepRecord: string;
          waterRecord: string;
          bodyMeasurement: string;
          statistics: string;
          checkinNow: string;
          checkinSuccess: string;
          missedToday: string;
          totalDays: string;
        };
        food: {
          record: string;
          addMeal: string;
          calories: string;
          protein: string;
          carbs: string;
          fat: string;
        };
        exercise: {
          record: string;
          addExercise: string;
          duration: string;
          caloriesBurned: string;
        };
        sleep: {
          record: string;
          duration: string;
          quality: string;
        };
        water: {
          record: string;
          amount: string;
          cups: string;
        };
        community: {
          feed: string;
          post: string;
          like: string;
          comment: string;
          share: string;
          collect: string;
          report: string;
          followers: string;
          following: string;
          publish: string;
          noMorePosts: string;
          loadMore: string;
          commentPlaceholder: string;
          publishSuccess: string;
          deleteConfirm: string;
        };
        chat: {
          chatbot: string;
          sendMessage: string;
          typing: string;
        };
        settings: {
          notificationPreference: string;
        };
        billing: {
          billing: string;
          refundInvoice: string;
        };
        enterprise: {
          activate: string;
        };
        export: {
          exportData: string;
        };
        admin: {
          userManage: string;
          announcementManage: string;
          foodManage: string;
          exerciseManage: string;
          notificationSend: string;
          planFeedback: string;
          auditLog: string;
          approvalManage: string;
          ruleSuggestion: string;
          aiFeedback: string;
          totalUsers: string;
          activeUsers: string;
          bannedUsers: string;
          userName: string;
          phone: string;
          email: string;
          role: string;
          createTime: string;
          userStatus: string;
          banUser: string;
          unbanUser: string;
          resetPwd: string;
          announcementTitle: string;
          announcementContent: string;
          publishTime: string;
          publish: string;
          unpublish: string;
          draft: string;
          published: string;
          foodName: string;
          foodCategory: string;
          foodCalories: string;
          foodProtein: string;
          foodCarbs: string;
          foodFat: string;
          exerciseName: string;
          exerciseCategory: string;
          exerciseCalories: string;
          exerciseDuration: string;
          notificationTitle: string;
          notificationContent: string;
          notificationType: string;
          sendTo: string;
          sendAll: string;
          sendSuccess: string;
          feedbackContent: string;
          feedbackStatus: string;
          feedbackTime: string;
          handle: string;
          handled: string;
          pending: string;
          logType: string;
          logContent: string;
          operator: string;
          operateTime: string;
          ipAddress: string;
          approve: string;
          reject: string;
          approved: string;
          rejected: string;
          pendingApproval: string;
        };
        auth: {
          loginTitle: string;
          loginDesc: string;
          registerTitle: string;
          registerDesc: string;
          forgotPasswordTitle: string;
          forgotPasswordDesc: string;
          accountLogin: string;
          phoneLogin: string;
          captchaLabel: string;
          getCode: string;
          reGetCode: string;
          sendCodeSuccess: string;
          rememberMeDesc: string;
          resetPassword: string;
          newPassword: string;
          confirmPasswordLabel: string;
          registerSuccess: string;
          resetSuccess: string;
          backToLogin: string;
          alreadyHaveAccount: string;
          noAccount: string;
          registerNow: string;
          forgotPassword: string;
          disclaimer: string;
          acceptDisclaimer: string;
          disclaimerRejectMsg: string;
          loginSuccess: string;
          phonePlaceholder: string;
          codePlaceholder: string;
          usernamePlaceholder: string;
          passwordPlaceholder: string;
          confirmPasswordPlaceholder: string;
        };
        goal: {
          title: string;
          create: string;
          milestones: string;
          targetWeight: string;
          currentProgress: string;
          deadline: string;
          goalType: string;
          loseWeight: string;
          gainMuscle: string;
          keepFit: string;
          improveSleep: string;
          reduceStress: string;
        };
        body: {
          measurement: string;
          height: string;
          weight: string;
          chest: string;
          waist: string;
          hip: string;
          arm: string;
          thigh: string;
          bodyFat: string;
          muscleMass: string;
          recordDate: string;
          trend: string;
        };
        statistics: {
          dashboard: string;
          overview: string;
          trend: string;
          comparison: string;
          dateRange: string;
          thisWeek: string;
          thisMonth: string;
          thisYear: string;
          customRange: string;
          exportReport: string;
        };
        notification: {
          list: string;
          markRead: string;
          markAllRead: string;
          unread: string;
          all: string;
          system: string;
          plan: string;
          community: string;
          noNotifications: string;
        };
      };
      stats: {
        totalCheckinRate: string;
        exerciseCompleteRate: string;
        dietCompleteRate: string;
        weightChange: string;
        currentGoal: string;
        exportData: string;
        detailedStats: string;
        weightTrend: string;
      };
      form: {
        required: string;
        userName: FormMsg;
        phone: FormMsg;
        pwd: FormMsg;
        confirmPwd: FormMsg;
        code: FormMsg;
        email: FormMsg;
      };
      dropdown: Record<Global.DropdownKey, string>;
      icon: {
        themeConfig: string;
        themeSchema: string;
        lang: string;
        fullscreen: string;
        fullscreenExit: string;
        reload: string;
        collapse: string;
        expand: string;
        pin: string;
        unpin: string;
      };
      datatable: {
        itemCount: string;
        fixed: {
          left: string;
          right: string;
          unFixed: string;
        };
      };
    };

    type GetI18nKey<T extends Record<string, unknown>, K extends keyof T = keyof T> = K extends string
      ? T[K] extends Record<string, unknown>
        ? `${K}.${GetI18nKey<T[K]>}`
        : K
      : never;

    type I18nKey = GetI18nKey<Schema>;

    type TranslateOptions<Locales extends string> = import('vue-i18n').TranslateOptions<Locales>;

    interface $T {
      (key: I18nKey): string;
      (key: I18nKey, plural: number, options?: TranslateOptions<LangType>): string;
      (key: I18nKey, defaultMsg: string, options?: TranslateOptions<I18nKey>): string;
      (key: I18nKey, list: unknown[], options?: TranslateOptions<I18nKey>): string;
      (key: I18nKey, list: unknown[], plural: number): string;
      (key: I18nKey, list: unknown[], defaultMsg: string): string;
      (key: I18nKey, named: Record<string, unknown>, options?: TranslateOptions<LangType>): string;
      (key: I18nKey, named: Record<string, unknown>, plural: number): string;
      (key: I18nKey, named: Record<string, unknown>, defaultMsg: string): string;
    }
  }

  /** Service namespace */
  namespace Service {
    interface ServiceConfigItem {
      baseURL: string;
      proxyPattern: string;
    }

    interface ServiceConfig extends ServiceConfigItem {
      other: ServiceConfigItem[];
    }

    type Response<T = unknown> = {
      code: string;
      msg: string;
      data: T;
    };
  }
}

/** Login module type */
declare type LoginModule = 'pwd-login' | 'code-login' | 'register' | 'reset-pwd' | 'bind-wechat';
