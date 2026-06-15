declare namespace Api {
  /**
   * namespace Recommend
   *
   * backend api module: "recommend"
   */
  namespace Recommend {
    /** Personalized recommendation */
    interface PersonalizedRecommendation {
      foods: RecommendationItem[];
      exercises: RecommendationItem[];
      tips: string[];
    }

    /** Recommendation item */
    interface RecommendationItem {
      id: number;
      name: string;
      reason: string;
      score: number;
    }
  }
}
