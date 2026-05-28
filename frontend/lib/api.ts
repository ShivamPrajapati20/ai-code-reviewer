import axios from "axios";

// axios instance pointing to Spring Boot Project
const api = axios.create({
  baseURL: "/backend",
});

// Triggers a full PR review
// Calls POST /api/review/analyze
export const analyzePR = async (
  owner: string,
  repo: string,
  prNumber: number,
  forceRefresh: boolean = false
) => {
  const response = await api.post(
      "/api/review/analyze",
      null,
      {
          params: {
              owner,
              repo,
              prNumber,
              forceRefresh
          },
      }
  );
  return response.data;
};

// Fetches review history for a repo
// Calls GET /api/reviews?repoName=owner/repo
export const getReviews = async (repoName: string) => {
  const response = await api.get("/api/review/reviews", {
    params: { repoName },
  });
  return response.data;
};

// Fetches comments for a specific review
// Calls GET /api/reviews/{id}/comments
export const getReviewComments = async (
  reviewId: string
) => {
  const response = await api.get(
    `/api/review/${reviewId}/comments`
  );
  return response.data;
};
