"use client";

import { useState } from "react";
import { analyzePR, getReviewComments } from "@/lib/api";
import ReviewCard from "@/components/ReviewCard";

type Comment = {
  id: string;
  severity: string;
  category: string;
  lineNumber: number | null;
  issue: string;
  fix: string;
  fileName: string;
};

type Review = {
  id: string;
  repoName: string;
  prNumber: number;
  prTitle: string;
  prUrl: string;
};

export default function Home() {
  const [owner, setOwner] = useState("");
  const [repo, setRepo] = useState("");
  const [prNumber, setPrNumber] = useState("");
  const [loading, setLoading] = useState(false);
  const [review, setReview] = useState<Review | null>(null);
  const [comments, setComments] = useState<Comment[]>([]);
  const [error, setError] = useState("");

  const handleAnalyze = async () => {
    if (!owner || !repo || !prNumber) {
      setError("Please fill in all fields");
      return;
    }
    setLoading(true);
    setError("");
    setReview(null);
    setComments([]);

    try {
      const reviewData = await analyzePR(
        owner, repo, parseInt(prNumber)
      );
      console.log(reviewData)
      setReview(reviewData);
      const commentsData = await getReviewComments(
        reviewData.id
      );
      console.log(commentsData)
      setComments(commentsData);
    } catch (err: any) {
      const message = err.response?.data?.message;
      if (err.response?.status === 404) {
          // Shows the specific message:
          // "GitHub user 'xyz' not found" OR
          // "Repository 'xyz/repo' not found" OR
          // "PR #42 not found in xyz/repo"
          setError(message || "Not found. Check your inputs.");
      } else {
          setError(
              message || "Something went wrong. Try again."
          );
      }
    } finally {
      setLoading(false);
    }
  };

  const critical = comments.filter(
    (c) => c.severity === "critical"
  ).length;
  const warnings = comments.filter(
    (c) => c.severity === "warning"
  ).length;
  const suggestions = comments.filter(
    (c) => c.severity === "suggestion"
  ).length;


  const classes = "w-full border border-gray-300 px-4 py-2 rounded-lg text-gray-900 placeholder-gray-400 focus:border-blue-500"
  
  return (
    <main className="min-h-screen bg-gray-50 
      px-4 py-6 sm:px-6 sm:py-8 lg:px-8">

      <div className="max-w-3xl mx-auto w-full">

        <div className="mb-6 sm:mb-8">
          <h1 className="text-2xl sm:text-3xl 
            font-bold text-gray-900">
            AI Code Reviewer
          </h1>
          <p className="text-sm sm:text-base 
            text-gray-500 mt-1">
            Enter a GitHub PR to get an instant 
            AI-powered code review
          </p>
        </div>

        {/* Input Form */}
        <div className="bg-white rounded-xl 
          shadow-sm border p-4 sm:p-6 mb-6">

          <div className="grid grid-cols-1 
            sm:grid-cols-2 lg:grid-cols-3 
            gap-3 mb-4">

            <div>
              <label className="text-xs font-medium 
                text-gray-600 mb-1 block">
                GitHub Owner
              </label>
              <input
                className={classes}
                value={owner}
                onChange={(e) => setOwner(e.target.value)}
              />
            </div>

            <div>
              <label className="text-xs font-medium 
                text-gray-600 mb-1 block">
                Repository
              </label>
              <input
                className={classes}
                value={repo}
                onChange={(e) => setRepo(e.target.value)}
              />
            </div>

            <div className="sm:col-span-2 lg:col-span-1">
              <label className="text-xs font-medium 
                text-gray-600 mb-1 block">
                PR Number
              </label>
              <input
                className={classes}
                type="number"
                value={prNumber}
                onChange={(e) => setPrNumber(e.target.value)}
              />
            </div>
          </div>

          <button
            onClick={handleAnalyze}
            disabled={loading}
            className="w-full bg-blue-600 text-white 
              rounded-lg py-2.5 text-sm sm:text-base
              font-medium hover:bg-blue-700 
              disabled:opacity-50 
              disabled:cursor-not-allowed transition"
          >
            {loading
              ? "Analyzing PR..."
              : "Analyze PR"}
          </button>

          {error && (
            <p className="text-red-500 text-sm mt-2">
              {error}
            </p>
          )}
        </div>

        {review && (
          <div>
            <div className="bg-white rounded-xl 
              shadow-sm border p-4 sm:p-6 mb-4">

              <h2 className="font-semibold 
                text-gray-800 text-sm sm:text-base 
                mb-1 break-words">
                {review.prTitle}
              </h2>

              <a
                href={review.prUrl}
                target="_blank"
                className="text-blue-500 text-sm 
                  hover:underline break-all"
              >
                View PR on GitHub
              </a>

              <div className="flex flex-wrap gap-6 mt-4">
                <div className="text-center min-w-[60px]">
                  <p className="text-2xl sm:text-3xl 
                    font-bold text-red-600">
                    {critical}
                  </p>
                  <p className="text-xs text-gray-500">
                    Critical
                  </p>
                </div>
                <div className="text-center min-w-[60px]">
                  <p className="text-2xl sm:text-3xl 
                    font-bold text-yellow-600">
                    {warnings}
                  </p>
                  <p className="text-xs text-gray-500">
                    Warnings
                  </p>
                </div>
                <div className="text-center min-w-[60px]">
                  <p className="text-2xl sm:text-3xl 
                    font-bold text-blue-600">
                    {suggestions}
                  </p>
                  <p className="text-xs text-gray-500">
                    Suggestions
                  </p>
                </div>
              </div>
            </div>

            {comments.length === 0 ? (
              <div className="bg-green-50 border 
                border-green-200 rounded-xl p-6 
                text-center">
                <p className="text-green-700 
                  font-medium text-sm sm:text-base">
                  No issues found — clean code!
                </p>
              </div>
            ) : (
              <div>
                <h3 className="font-semibold 
                  text-gray-700 text-sm sm:text-base 
                  mb-3">
                  {comments.length} Issues Found
                </h3>
                {comments.map((comment) => (
                  <ReviewCard
                    key={comment.id}
                    comment={comment}
                  />
                ))}
              </div>
            )}
          </div>
        )}
      </div>
    </main>
  );
}