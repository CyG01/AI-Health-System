declare namespace Api {
  namespace Chat {
    interface Message {
      id: string;
      role: 'user' | 'assistant' | 'system';
      content: string;
      timestamp: number;
      planCard?: PlanCard;
      toolCalls?: ToolCall[];
    }

    interface Session {
      id: string;
      title: string;
      lastMessage: string;
      updatedAt: number;
      messageCount: number;
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
