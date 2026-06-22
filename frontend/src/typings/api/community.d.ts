declare namespace Api {
  namespace Community {
    interface Post {
      id: number;
      userId: number;
      userNickname: string;
      userAvatar: string;
      content: string;
      images?: string;
      exerciseType?: string;
      exerciseDuration?: number | null;
      caloriesBurned?: number | null;
      likeCount: number;
      commentCount: number;
      isLiked: boolean;
      createTime: string;
      timeAgo: string;
    }

    interface Comment {
      id: number;
      postId: number;
      userId: number;
      userNickname: string;
      userAvatar: string;
      content: string;
      replyTo?: number | null;
      createTime: string;
      timeAgo: string;
    }

    interface CreatePostParams {
      content: string;
      images?: string[];
    }

  }
}
