declare namespace Api {
  namespace Chat {
    interface Message {
      id: number;
      role: 'user' | 'assistant' | 'system';
      content: string;
      createTime: string;
      planCard?: PlanCard;
      toolCalls?: ToolCall[];
    }

    interface Session {
      id: number;
      title: string;
      updateTime: string;
      createTime: string;
    }

    interface PlanCard {
      type: string;
      title: string;
      items: PlanCardItem[];
    }

    interface PlanCardItem {
      label: string;
      value: string;
      unit?: string;
    }

    interface ToolCall {
      name: string;
      args: Record<string, unknown>;
      result?: string;
    }
  }
}
