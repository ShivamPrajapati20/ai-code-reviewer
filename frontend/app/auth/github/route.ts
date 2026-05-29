import { NextRequest, NextResponse } from "next/server";
import { authCookies, createOAuthState } from "@/lib/auth";

export function GET(request: NextRequest) {
  const clientId = process.env.GITHUB_CLIENT_ID;

  if (!clientId) {
    const url = new URL("/", request.url);
    url.searchParams.set(
      "authError",
      "GitHub login is not configured. Add OAuth credentials to frontend/.env.local."
    );
    return NextResponse.redirect(url);
  }

  const state = createOAuthState();
  const redirectUri = new URL("/auth/github/callback", request.url);
  const githubUrl = new URL("https://github.com/login/oauth/authorize");
  githubUrl.searchParams.set("client_id", clientId);
  githubUrl.searchParams.set("redirect_uri", redirectUri.toString());
  githubUrl.searchParams.set("scope", "read:user");
  githubUrl.searchParams.set("state", state);

  const response = NextResponse.redirect(githubUrl);
  response.cookies.set(authCookies.state, state, {
    httpOnly: true,
    maxAge: 60 * 10,
    path: "/",
    sameSite: "lax",
  });

  return response;
}
