import { NextRequest, NextResponse } from "next/server";
import { authCookies, createSessionCookie } from "@/lib/auth";

type GitHubTokenResponse = {
  access_token?: string;
  error?: string;
  error_description?: string;
};

type GitHubUserResponse = {
  login: string;
  name: string | null;
  avatar_url: string | null;
  html_url: string;
};

export async function GET(request: NextRequest) {
  const code = request.nextUrl.searchParams.get("code");
  const state = request.nextUrl.searchParams.get("state");
  const storedState = request.cookies.get(authCookies.state)?.value;
  const clientId = process.env.GITHUB_CLIENT_ID;
  const clientSecret = process.env.GITHUB_CLIENT_SECRET;
  const appUrl =
    process.env.NEXT_PUBLIC_APP_URL ?? request.nextUrl.origin;

  if (request.nextUrl.origin !== appUrl) {
    const url = new URL("/auth/github", appUrl);
    url.searchParams.set(
      "authError",
      "Please start GitHub login from the configured app URL."
    );
    return NextResponse.redirect(url);
  }

  if (!clientId || !clientSecret) {
    return authError(request, "GitHub login is not configured.");
  }

  if (!code || !state || !storedState || state !== storedState) {
    return authError(request, "GitHub login could not be verified.");
  }

  const tokenResponse = await fetch(
    "https://github.com/login/oauth/access_token",
    {
      method: "POST",
      headers: {
        Accept: "application/json",
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        client_id: clientId,
        client_secret: clientSecret,
        code,
        redirect_uri: new URL(
          "/auth/github/callback",
          appUrl
        ).toString(),
      }),
    }
  );

  const tokenData =
    (await tokenResponse.json()) as GitHubTokenResponse;

  if (!tokenResponse.ok || !tokenData.access_token) {
    return authError(
      request,
      tokenData.error_description ||
        tokenData.error ||
        "GitHub did not return an access token."
    );
  }

  const userResponse = await fetch("https://api.github.com/user", {
    headers: {
      Authorization: `Bearer ${tokenData.access_token}`,
      Accept: "application/vnd.github+json",
      "User-Agent": "ai-code-reviewer",
    },
  });

  if (!userResponse.ok) {
    return authError(request, "Could not load your GitHub profile.");
  }

  const user = (await userResponse.json()) as GitHubUserResponse;
  const response = NextResponse.redirect(new URL("/", appUrl));
  response.cookies.delete(authCookies.state);
  response.cookies.set(
    authCookies.session,
    createSessionCookie({
      login: user.login,
      name: user.name,
      avatarUrl: user.avatar_url,
      profileUrl: user.html_url,
    }),
    {
      httpOnly: true,
      maxAge: authCookies.maxAge,
      path: "/",
      sameSite: "lax",
    }
  );

  return response;
}

function authError(request: NextRequest, message: string) {
  const appUrl =
    process.env.NEXT_PUBLIC_APP_URL ?? request.nextUrl.origin;
  const url = new URL("/", appUrl);
  url.searchParams.set("authError", message);
  return NextResponse.redirect(url);
}
