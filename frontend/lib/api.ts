import axios from "axios";

// axios instance pointing to Spring Boot Project
const api = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_URL,
});

// Triggers a full PR review
// Calls POST /api/review/analyze
export const analyzePR = async (
  owner: string,
  repo: string,
  prNumber: number
) => {
  const response = await api.post(
    "/api/review/analyze",
    null,
    {
      params: { owner, repo, prNumber },
    }
  );
  return response.data;
};

// Fetches review history for a repo
// Calls GET /api/reviews?repoName=owner/repo
export const getReviews = async (repoName: string) => {
  const response = await api.get("/api/reviews", {
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
    `/api/reviews/${reviewId}/comments`
  );
  return response.data;
};