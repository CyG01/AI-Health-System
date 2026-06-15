declare namespace Api {
  namespace Community {
    interface Post {
      id: number;
      userId: number;
      username: string;
      avatar: string;
      content: string;
      images?: string[];
      likes: number;
      comments: number;
      liked: boolean;
      createdAt: string;
    }

    interface Comment {
      id: number;
      postId: number;
      userId: number;
      username: string;
      avatar: string;
      content: string;
      createdAt: string;
    }

    interface CreatePostRequest {
      content: string;
      images?: string[];
    }

    interface CreatePostParams {
      content: string;
      images?: string[];
    }

    interface CreateCommentRequest {
      postId: number;
      content: string;
    }

    interface FeedParams {
      page?: number;
      size?: number;
      type?: string;
    }

    interface RankingItem {
      userId: number;
      username: string;
      avatar: string;
      value: number;
      rank: number;
    }
  }
}
