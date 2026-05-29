"use client";

import { useEffect, useMemo, useState } from "react";
import {
  AlertTriangle,
  ArrowRight,
  Bot,
  CheckCircle2,
  ChevronDown,
  ChevronUp,
  Code2,
  ExternalLink,
  FileCode2,
  GitPullRequest,
  Lock,
  LogOut,
  Loader2,
  RefreshCw,
  Search,
  ShieldAlert,
  Sparkles,
  Zap,
  type LucideIcon,
} from "lucide-react";
import { analyzePR, getReviewComments } from "@/lib/api";

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
  cached: boolean;
};

type SeverityKey = "critical" | "warning" | "suggestion";

type GroupedComments = Record<SeverityKey, Comment[]>;

type AuthUser = {
  login: string;
  name: string | null;
  avatarUrl: string | null;
  profileUrl: string;
};

const emptyGrouped: GroupedComments = {
  critical: [],
  warning: [],
  suggestion: [],
};

const severityConfig = {
  critical: {
    label: "Critical",
    icon: ShieldAlert,
    pill: "bg-rose-100 text-rose-700 ring-rose-200",
    card: "border-rose-200 bg-rose-50/70",
    active: "border-rose-400 bg-rose-50 shadow-rose-100",
    dot: "bg-rose-500",
  },
  warning: {
    label: "Warnings",
    icon: AlertTriangle,
    pill: "bg-amber-100 text-amber-800 ring-amber-200",
    card: "border-amber-200 bg-amber-50/70",
    active: "border-amber-400 bg-amber-50 shadow-amber-100",
    dot: "bg-amber-500",
  },
  suggestion: {
    label: "Suggestions",
    icon: Sparkles,
    pill: "bg-sky-100 text-sky-700 ring-sky-200",
    card: "border-sky-200 bg-sky-50/70",
    active: "border-sky-400 bg-sky-50 shadow-sky-100",
    dot: "bg-sky-500",
  },
} satisfies Record<
  SeverityKey,
  {
    label: string;
    icon: LucideIcon;
    pill: string;
    card: string;
    active: string;
    dot: string;
  }
>;

const severityOrder: SeverityKey[] = ["critical", "warning", "suggestion"];

export function ReviewerDashboard() {
  const [owner, setOwner] = useState("");
  const [repo, setRepo] = useState("");
  const [prNumber, setPrNumber] = useState("");
  const [authLoading, setAuthLoading] = useState(true);
  const [user, setUser] = useState<AuthUser | null>(null);
  const [loading, setLoading] = useState(false);
  const [review, setReview] = useState<Review | null>(null);
  const [grouped, setGrouped] = useState<GroupedComments>(emptyGrouped);
  const [error, setError] = useState("");
  const [openSection, setOpenSection] = useState<SeverityKey | "">("");
  const [query, setQuery] = useState("");

  useEffect(() => {
    const loadSession = async () => {
      try {
        const response = await fetch("/auth/session");
        const session = await response.json();

        if (session.authenticated && session.user) {
          setUser(session.user);
          setOwner(session.user.login);
        } else {
          window.location.replace("/");
        }
      } finally {
        setAuthLoading(false);
      }
    };

    loadSession();
  }, []);

  useEffect(() => {
    if (!user) {
      return;
    }

    const intervalId = window.setInterval(async () => {
      const response = await fetch("/auth/session");
      const session = await response.json();

      if (!session.authenticated) {
        setUser(null);
        setOwner("");
        setReview(null);
        setGrouped(emptyGrouped);
      }
    }, 60 * 1000);

    return () => window.clearInterval(intervalId);
  }, [user]);

  const total = severityOrder.reduce(
    (sum, key) => sum + grouped[key].length,
    0
  );

  const filteredGrouped = useMemo(() => {
    const normalizedQuery = query.trim().toLowerCase();

    if (!normalizedQuery) {
      return grouped;
    }

    return severityOrder.reduce((acc, key) => {
      acc[key] = grouped[key].filter((comment) =>
        [
          comment.fileName,
          comment.category,
          comment.issue,
          comment.fix,
          String(comment.lineNumber ?? ""),
        ]
          .join(" ")
          .toLowerCase()
          .includes(normalizedQuery)
      );

      return acc;
    }, { ...emptyGrouped });
  }, [grouped, query]);

  const filteredTotal = severityOrder.reduce(
    (sum, key) => sum + filteredGrouped[key].length,
    0
  );

  const healthLabel =
    total === 0
      ? "Clean"
      : grouped.critical.length > 0
      ? "Needs attention"
      : grouped.warning.length > 0
      ? "Review advised"
      : "Nice polish";

  const handleAnalyze = async (forceRefresh = false) => {
    if (!user) {
      setError("Please sign in with GitHub first.");
      return;
    }

    if (!repo.trim() || !prNumber.trim()) {
      setError("Please fill in owner, repository, and PR number.");
      return;
    }

    setLoading(true);
    setError("");
    setReview(null);
    setGrouped(emptyGrouped);
    setQuery("");

    try {
      const reviewData = await analyzePR(
        user.login,
        repo.trim(),
        Number(prNumber),
        forceRefresh
      );

      setReview(reviewData);

      const commentsData = await getReviewComments(reviewData.id);
      const comments: Comment[] = Array.isArray(commentsData)
        ? commentsData
        : [];

      const nextGrouped = {
        critical: comments.filter((comment) => comment.severity === "critical"),
        warning: comments.filter((comment) => comment.severity === "warning"),
        suggestion: comments.filter(
          (comment) => comment.severity === "suggestion"
        ),
      };

      setGrouped(nextGrouped);
      setOpenSection(
        severityOrder.find((key) => nextGrouped[key].length > 0) ?? ""
      );
    } catch (err: unknown) {
      const message =
        typeof err === "object" &&
        err !== null &&
        "response" in err &&
        typeof err.response === "object" &&
        err.response !== null &&
        "data" in err.response &&
        typeof err.response.data === "object" &&
        err.response.data !== null &&
        "message" in err.response.data
          ? String(err.response.data.message)
          : "Could not reach the review service. Make sure the Spring Boot backend is running on port 8080.";

      setError(message);
    } finally {
      setLoading(false);
    }
  };

  const handleLogout = async () => {
    await fetch("/auth/logout", { method: "POST" });
    setUser(null);
    setOwner("");
    setReview(null);
    setGrouped(emptyGrouped);
    window.location.href = "/";
  };

  const repoLabel = owner && repo ? `${owner}/${repo}` : "Repository";

  if (authLoading) {
    return (
      <main className="grid min-h-screen place-items-center bg-[#f7f3ec] px-4 text-slate-950">
        <div className="flex items-center gap-3 rounded-2xl border border-slate-200 bg-white px-5 py-4 text-sm font-bold shadow-sm">
          <Loader2 className="h-5 w-5 animate-spin text-emerald-700" />
          Checking GitHub login
        </div>
      </main>
    );
  }

  if (!user) {
    return (
      <main className="grid min-h-screen place-items-center bg-[#f7f3ec] px-4 text-slate-950">
        <div className="flex items-center gap-3 rounded-2xl border border-slate-200 bg-white px-5 py-4 text-sm font-bold shadow-sm">
          <Loader2 className="h-5 w-5 animate-spin text-emerald-700" />
          Redirecting to sign in
        </div>
      </main>
    );
  }

  return (
    <main className="min-h-screen bg-[#f7f3ec] text-slate-950">
      <section className="border-b border-slate-200/80 bg-white">
        <div className="mx-auto grid min-h-[92vh] w-full max-w-7xl grid-cols-1 items-center gap-8 px-4 py-6 sm:px-6 lg:grid-cols-[1.02fr_0.98fr] lg:px-8">
          <div className="space-y-7">
            <div className="inline-flex items-center gap-2 rounded-full border border-emerald-200 bg-emerald-50 px-3 py-1 text-xs font-semibold text-emerald-800">
              <Bot className="h-3.5 w-3.5" />
              AI powered PR review cockpit
            </div>

            <div className="flex max-w-xl items-center justify-between gap-3 rounded-2xl border border-slate-200 bg-white p-3 shadow-sm">
              <div className="flex min-w-0 items-center gap-3">
                {user.avatarUrl ? (
                  // eslint-disable-next-line @next/next/no-img-element
                  <img
                    src={user.avatarUrl}
                    alt=""
                    className="h-11 w-11 rounded-xl"
                  />
                ) : (
                  <span className="grid h-11 w-11 place-items-center rounded-xl bg-slate-950 text-white">
                    <GitPullRequest className="h-5 w-5" />
                  </span>
                )}
                <div className="min-w-0">
                  <p className="truncate text-sm font-black text-slate-950">
                    {user.name || user.login}
                  </p>
                  <p className="truncate text-xs font-semibold text-slate-500">
                    @{user.login}
                  </p>
                </div>
              </div>

              <button
                type="button"
                onClick={handleLogout}
                className="grid h-10 w-10 shrink-0 place-items-center rounded-xl border border-slate-200 text-slate-500 transition hover:border-rose-200 hover:bg-rose-50 hover:text-rose-700"
                title="Sign out"
              >
                <LogOut className="h-4 w-4" />
              </button>
            </div>

            <div className="max-w-2xl space-y-4">
              <h1 className="text-4xl font-black tracking-normal text-slate-950 sm:text-5xl lg:text-6xl">
                Review pull requests before bugs reach production.
              </h1>
              <p className="max-w-xl text-base leading-7 text-slate-600 sm:text-lg">
                Paste a GitHub PR, run the analyzer, and get a focused issue
                board with severity, files, line numbers, and suggested fixes.
              </p>
            </div>

            <div className="grid max-w-2xl grid-cols-3 gap-3">
              <Metric label="Scan mode" value="Live" icon={Zap} />
              <Metric label="Signal" value={healthLabel} icon={Code2} />
              <Metric label="Findings" value={String(total)} icon={Search} />
            </div>
          </div>

          <div className="relative">
            <div className="absolute -inset-3 rounded-[2rem] border border-white/80 bg-white/60 shadow-2xl shadow-slate-300/50" />
            <div className="relative overflow-hidden rounded-3xl border border-slate-200 bg-slate-950 text-white shadow-2xl">
              <div className="flex items-center justify-between border-b border-white/10 px-5 py-4">
                <div className="flex items-center gap-2">
                  <span className="h-2.5 w-2.5 rounded-full bg-rose-400" />
                  <span className="h-2.5 w-2.5 rounded-full bg-amber-300" />
                  <span className="h-2.5 w-2.5 rounded-full bg-emerald-400" />
                </div>
                <span className="text-xs font-medium text-slate-400">
                  ai-reviewer.local
                </span>
              </div>

              <div className="space-y-5 p-5 sm:p-6">
                <div>
                  <div className="mb-2 flex items-center justify-between gap-3">
                    <label className="text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">
                      Pull request target
                    </label>
                    <span className="rounded-full bg-white/10 px-2.5 py-1 text-[11px] font-medium text-slate-300">
                      Cache enabled
                    </span>
                  </div>

                  <div className="grid grid-cols-1 gap-3 sm:grid-cols-[1fr_1fr_0.7fr]">
                    <Field
                      label="Owner"
                      placeholder="shivam"
                      value={owner}
                      onChange={setOwner}
                      disabled
                    />
                    <Field
                      label="Repository"
                      placeholder="ai-code-reviewer"
                      value={repo}
                      onChange={setRepo}
                    />
                    <Field
                      label="PR"
                      placeholder="42"
                      type="number"
                      value={prNumber}
                      onChange={setPrNumber}
                    />
                  </div>
                </div>

                <div className="flex flex-col gap-3 sm:flex-row">
                  <button
                    type="button"
                    onClick={() => handleAnalyze(false)}
                    disabled={loading}
                    className="group inline-flex min-h-12 flex-1 items-center justify-center gap-2 rounded-xl bg-emerald-400 px-5 py-3 text-sm font-bold text-slate-950 shadow-lg shadow-emerald-950/25 transition hover:-translate-y-0.5 hover:bg-emerald-300 disabled:translate-y-0 disabled:cursor-not-allowed disabled:opacity-60"
                  >
                    {loading ? (
                      <Loader2 className="h-4 w-4 animate-spin" />
                    ) : (
                      <GitPullRequest className="h-4 w-4" />
                    )}
                    {loading ? "Analyzing PR" : "Analyze PR"}
                    {!loading && (
                      <ArrowRight className="h-4 w-4 transition group-hover:translate-x-0.5" />
                    )}
                  </button>

                  <button
                    type="button"
                    onClick={() => handleAnalyze(true)}
                    disabled={loading}
                    className="inline-flex min-h-12 items-center justify-center gap-2 rounded-xl border border-white/15 bg-white/5 px-4 py-3 text-sm font-semibold text-slate-200 transition hover:bg-white/10 disabled:cursor-not-allowed disabled:opacity-60"
                  >
                    <RefreshCw className="h-4 w-4" />
                    Refresh
                  </button>
                </div>

                {error && (
                  <div className="rounded-xl border border-rose-400/40 bg-rose-400/10 px-4 py-3 text-sm text-rose-100">
                    {error}
                  </div>
                )}

                <div className="rounded-2xl border border-white/10 bg-white/[0.06] p-4">
                  <div className="mb-4 flex items-center justify-between gap-3">
                    <div>
                      <p className="text-xs font-semibold uppercase tracking-[0.18em] text-slate-500">
                        Preview
                      </p>
                      <p className="mt-1 text-sm font-semibold text-slate-200">
                        {repoLabel} {prNumber ? `#${prNumber}` : ""}
                      </p>
                    </div>
                    <GitPullRequest className="h-5 w-5 text-emerald-300" />
                  </div>

                  <div className="space-y-3">
                    {loading ? (
                      <>
                        <SkeletonRow />
                        <SkeletonRow />
                        <SkeletonRow />
                      </>
                    ) : (
                      severityOrder.map((key) => {
                        const config = severityConfig[key];
                        const Icon = config.icon;

                        return (
                          <div
                            key={key}
                            className="flex items-center justify-between rounded-xl border border-white/10 bg-slate-900 px-3 py-3"
                          >
                            <div className="flex items-center gap-3">
                              <span
                                className={`grid h-9 w-9 place-items-center rounded-lg ${config.pill}`}
                              >
                                <Icon className="h-4 w-4" />
                              </span>
                              <span className="text-sm font-semibold text-slate-200">
                                {config.label}
                              </span>
                            </div>
                            <span className="text-xl font-black">
                              {grouped[key].length}
                            </span>
                          </div>
                        );
                      })
                    )}
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section className="mx-auto w-full max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
        {review ? (
          <div className="space-y-5">
            <div className="flex flex-col justify-between gap-4 border-b border-slate-200 pb-5 lg:flex-row lg:items-end">
              <div className="min-w-0">
                <div className="mb-2 flex flex-wrap items-center gap-2">
                  <span className="rounded-full bg-slate-950 px-3 py-1 text-xs font-semibold text-white">
                    {review.cached ? "Cached result" : "Fresh result"}
                  </span>
                  <span className="rounded-full bg-white px-3 py-1 text-xs font-semibold text-slate-600 ring-1 ring-slate-200">
                    {review.repoName}
                  </span>
                </div>
                <h2 className="break-words text-2xl font-black text-slate-950 sm:text-3xl">
                  {review.prTitle}
                </h2>
                <a
                  href={review.prUrl}
                  target="_blank"
                  rel="noreferrer"
                  className="mt-2 inline-flex items-center gap-1.5 text-sm font-semibold text-emerald-700 hover:text-emerald-900"
                >
                  View PR on GitHub
                  <ExternalLink className="h-3.5 w-3.5" />
                </a>
              </div>

              <div className="flex flex-col gap-3 sm:flex-row">
                <div className="relative">
                  <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
                  <input
                    className="h-11 w-full rounded-xl border border-slate-200 bg-white pl-9 pr-3 text-sm font-medium text-slate-800 shadow-sm outline-none transition placeholder:text-slate-400 focus:border-emerald-400 focus:ring-4 focus:ring-emerald-100 sm:w-72"
                    placeholder="Filter files, issues, fixes"
                    value={query}
                    onChange={(event) => setQuery(event.target.value)}
                  />
                </div>

                <button
                  type="button"
                  onClick={() => handleAnalyze(true)}
                  disabled={loading}
                  className="inline-flex h-11 items-center justify-center gap-2 rounded-xl bg-slate-950 px-4 text-sm font-bold text-white shadow-sm transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-60"
                >
                  <RefreshCw className="h-4 w-4" />
                  Re-run
                </button>
              </div>
            </div>

            <div className="grid grid-cols-1 gap-3 md:grid-cols-3">
              {severityOrder.map((key) => {
                const config = severityConfig[key];
                const Icon = config.icon;
                const isOpen = openSection === key;

                return (
                  <button
                    key={key}
                    type="button"
                    onClick={() => setOpenSection(isOpen ? "" : key)}
                    className={`group rounded-2xl border bg-white p-5 text-left shadow-sm transition hover:-translate-y-0.5 hover:shadow-lg ${
                      isOpen
                        ? `${config.active} shadow-lg`
                        : "border-slate-200 hover:border-slate-300"
                    }`}
                  >
                    <div className="mb-5 flex items-center justify-between">
                      <span className={`grid h-11 w-11 place-items-center rounded-xl ${config.pill}`}>
                        <Icon className="h-5 w-5" />
                      </span>
                      <span className={`h-2.5 w-2.5 rounded-full ${config.dot}`} />
                    </div>
                    <p className="text-3xl font-black text-slate-950">
                      {grouped[key].length}
                    </p>
                    <p className="mt-1 text-sm font-bold text-slate-600">
                      {config.label}
                    </p>
                  </button>
                );
              })}
            </div>

            {total === 0 ? (
              <div className="grid min-h-64 place-items-center rounded-3xl border border-emerald-200 bg-emerald-50 p-8 text-center">
                <div className="max-w-md">
                  <span className="mx-auto grid h-14 w-14 place-items-center rounded-2xl bg-emerald-500 text-white">
                    <CheckCircle2 className="h-7 w-7" />
                  </span>
                  <h3 className="mt-4 text-2xl font-black text-emerald-950">
                    No issues found
                  </h3>
                  <p className="mt-2 text-sm leading-6 text-emerald-800">
                    This PR came back clean from the analyzer.
                  </p>
                </div>
              </div>
            ) : filteredTotal === 0 ? (
              <div className="rounded-2xl border border-slate-200 bg-white p-8 text-center text-sm font-semibold text-slate-500">
                No review comments match that filter.
              </div>
            ) : (
              <div className="space-y-4">
                {severityOrder.map((key) => {
                  const comments = filteredGrouped[key];
                  const config = severityConfig[key];
                  const Icon = config.icon;
                  const isOpen = openSection === key;

                  if (comments.length === 0) {
                    return null;
                  }

                  return (
                    <section
                      key={key}
                      className={`overflow-hidden rounded-3xl border shadow-sm ${config.card}`}
                    >
                      <button
                        type="button"
                        onClick={() => setOpenSection(isOpen ? "" : key)}
                        className="flex w-full items-center justify-between gap-4 bg-white/75 px-5 py-4 text-left transition hover:bg-white"
                      >
                        <div className="flex min-w-0 items-center gap-3">
                          <span className={`grid h-10 w-10 place-items-center rounded-xl ${config.pill}`}>
                            <Icon className="h-5 w-5" />
                          </span>
                          <div className="min-w-0">
                            <h3 className="truncate text-base font-black text-slate-950">
                              {config.label}
                            </h3>
                            <p className="text-xs font-semibold text-slate-500">
                              {comments.length} visible finding
                              {comments.length === 1 ? "" : "s"}
                            </p>
                          </div>
                        </div>
                        {isOpen ? (
                          <ChevronUp className="h-5 w-5 text-slate-500" />
                        ) : (
                          <ChevronDown className="h-5 w-5 text-slate-500" />
                        )}
                      </button>

                      {isOpen && (
                        <div className="grid gap-3 p-3 sm:p-4">
                          {comments.map((comment) => (
                            <ReviewFinding
                              key={comment.id}
                              comment={comment}
                              severityKey={key}
                            />
                          ))}
                        </div>
                      )}
                    </section>
                  );
                })}
              </div>
            )}
          </div>
        ) : (
          <div className="grid gap-4 border-b border-slate-200 pb-8 md:grid-cols-3">
            <EmptyPanel
              icon={GitPullRequest}
              title="Connect a PR"
              text="Enter owner, repository, and PR number in the analyzer above."
            />
            <EmptyPanel
              icon={ShieldAlert}
              title="Prioritize risk"
              text="Critical findings, warnings, and suggestions are separated for faster triage."
            />
            <EmptyPanel
              icon={FileCode2}
              title="Jump to files"
              text="Each finding keeps the file path, category, and line number visible."
            />
          </div>
        )}
      </section>
    </main>
  );
}

export default function Home() {
  return <LoginScreen />;
}

function LoginScreen() {
  const [authError] = useState(() => {
    if (typeof window === "undefined") {
      return "";
    }

    const params = new URLSearchParams(window.location.search);
    return params.get("authError") || "";
  });

  return (
    <main className="grid min-h-screen bg-[#f7f3ec] px-4 py-6 text-slate-950">
      <section className="mx-auto grid w-full max-w-6xl grid-cols-1 items-center gap-8 lg:grid-cols-[1fr_0.9fr]">
        <div className="space-y-6">
          <div className="inline-flex items-center gap-2 rounded-full border border-emerald-200 bg-emerald-50 px-3 py-1 text-xs font-semibold text-emerald-800">
            <GitPullRequest className="h-3.5 w-3.5" />
            GitHub sign-in required
          </div>

          <div className="max-w-2xl space-y-4">
            <h1 className="text-4xl font-black tracking-normal text-slate-950 sm:text-5xl">
              Sign in with GitHub to review your pull requests.
            </h1>
            <p className="max-w-xl text-base leading-7 text-slate-600 sm:text-lg">
              Connect your GitHub account and get focused AI feedback for
              active pull requests.
            </p>
          </div>

          <a
            href="/auth/github"
            className="inline-flex min-h-12 items-center justify-center gap-2 rounded-xl bg-slate-950 px-5 py-3 text-sm font-bold text-white shadow-lg shadow-slate-300 transition hover:-translate-y-0.5 hover:bg-slate-800"
          >
            <GitPullRequest className="h-4 w-4" />
            Continue with GitHub
            <ArrowRight className="h-4 w-4" />
          </a>

          {authError && (
            <div className="max-w-xl rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm font-semibold text-rose-700">
              {authError}
            </div>
          )}
        </div>

        <div className="relative">
          <div className="absolute -inset-3 rounded-[2rem] border border-white/80 bg-white/60 shadow-2xl shadow-slate-300/50" />
          <div className="relative overflow-hidden rounded-3xl border border-slate-200 bg-slate-950 p-6 text-white shadow-2xl">
            <div className="mb-6 flex items-center justify-between border-b border-white/10 pb-4">
              <div className="flex items-center gap-2">
                <span className="h-2.5 w-2.5 rounded-full bg-rose-400" />
                <span className="h-2.5 w-2.5 rounded-full bg-amber-300" />
                <span className="h-2.5 w-2.5 rounded-full bg-emerald-400" />
              </div>
              <Lock className="h-4 w-4 text-emerald-300" />
            </div>

            <div className="space-y-4">
              <div className="rounded-2xl border border-white/10 bg-white/[0.06] p-4">
                <p className="mb-2 text-xs font-semibold uppercase tracking-[0.18em] text-slate-500">
                  Repository
                </p>
                <div className="flex items-center justify-between rounded-xl border border-white/10 bg-white/[0.05] px-3 py-3 text-sm font-bold text-slate-300">
                  <span>Choose a repository and pull request</span>
                  <GitPullRequest className="h-4 w-4 text-emerald-300" />
                </div>
              </div>

              <div className="rounded-2xl border border-white/10 bg-white/[0.06] p-4">
                <p className="mb-2 text-xs font-semibold uppercase tracking-[0.18em] text-slate-500">
                  Review
                </p>
                <p className="text-sm leading-6 text-slate-300">
                  See severity summaries, file-level comments, and practical
                  fixes in one clean review board.
                </p>
              </div>
            </div>
          </div>
        </div>
      </section>
    </main>
  );
}

function Field({
  label,
  placeholder,
  value,
  onChange,
  type = "text",
  disabled = false,
}: {
  label: string;
  placeholder: string;
  value: string;
  onChange: (value: string) => void;
  type?: string;
  disabled?: boolean;
}) {
  return (
    <label className="block">
      <span className="mb-1.5 flex items-center gap-1.5 text-xs font-semibold text-slate-400">
        {disabled && <Lock className="h-3 w-3" />}
        <span>{label}</span>
      </span>
      <input
        type={type}
        className="h-12 w-full rounded-xl border border-white/10 bg-white/[0.08] px-3 text-sm font-semibold text-white outline-none transition placeholder:text-slate-500 focus:border-emerald-300 focus:bg-white/[0.12] focus:ring-4 focus:ring-emerald-300/10 disabled:cursor-not-allowed disabled:bg-white/[0.04] disabled:text-slate-400"
        placeholder={placeholder}
        value={value}
        disabled={disabled}
        onChange={(event) => onChange(event.target.value)}
      />
    </label>
  );
}

function Metric({
  label,
  value,
  icon: Icon,
}: {
  label: string;
  value: string;
  icon: LucideIcon;
}) {
  return (
    <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
      <Icon className="mb-3 h-5 w-5 text-emerald-700" />
      <p className="text-xl font-black text-slate-950 sm:text-2xl">{value}</p>
      <p className="mt-1 text-xs font-semibold text-slate-500">{label}</p>
    </div>
  );
}

function SkeletonRow() {
  return (
    <div className="flex items-center gap-3 rounded-xl border border-white/10 bg-slate-900 px-3 py-3">
      <div className="h-9 w-9 animate-pulse rounded-lg bg-white/10" />
      <div className="flex-1 space-y-2">
        <div className="h-3 w-28 animate-pulse rounded-full bg-white/10" />
        <div className="h-2 w-20 animate-pulse rounded-full bg-white/10" />
      </div>
      <div className="h-6 w-8 animate-pulse rounded-full bg-white/10" />
    </div>
  );
}

function ReviewFinding({
  comment,
  severityKey,
}: {
  comment: Comment;
  severityKey: SeverityKey;
}) {
  const config = severityConfig[severityKey];
  const Icon = config.icon;

  return (
    <article className="group rounded-2xl border border-slate-200 bg-white p-4 shadow-sm transition hover:-translate-y-0.5 hover:border-slate-300 hover:shadow-lg">
      <div className="mb-3 flex flex-col justify-between gap-3 lg:flex-row lg:items-start">
        <div className="flex min-w-0 items-start gap-3">
          <span className={`grid h-10 w-10 shrink-0 place-items-center rounded-xl ${config.pill}`}>
            <Icon className="h-5 w-5" />
          </span>
          <div className="min-w-0">
            <p className="break-words text-sm font-black text-slate-950">
              {comment.issue}
            </p>
            <div className="mt-2 flex flex-wrap items-center gap-2">
              <span className="rounded-full bg-slate-100 px-2.5 py-1 text-xs font-bold capitalize text-slate-600">
                {comment.category}
              </span>
              {comment.lineNumber && (
                <span className="rounded-full bg-emerald-50 px-2.5 py-1 text-xs font-bold text-emerald-800">
                  Line {comment.lineNumber}
                </span>
              )}
            </div>
          </div>
        </div>
        <span className="break-all rounded-xl bg-slate-950 px-3 py-2 font-mono text-xs font-semibold text-slate-100">
          {comment.fileName}
        </span>
      </div>

      <div className="rounded-xl border border-emerald-100 bg-emerald-50 px-4 py-3">
        <p className="mb-1 text-xs font-black uppercase tracking-[0.18em] text-emerald-700">
          Suggested fix
        </p>
        <p className="text-sm leading-6 text-emerald-950">{comment.fix}</p>
      </div>
    </article>
  );
}

function EmptyPanel({
  icon: Icon,
  title,
  text,
}: {
  icon: LucideIcon;
  title: string;
  text: string;
}) {
  return (
    <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
      <span className="grid h-11 w-11 place-items-center rounded-xl bg-slate-950 text-white">
        <Icon className="h-5 w-5" />
      </span>
      <h3 className="mt-4 text-base font-black text-slate-950">{title}</h3>
      <p className="mt-2 text-sm leading-6 text-slate-600">{text}</p>
    </div>
  );
}
