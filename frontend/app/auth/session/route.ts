import { NextRequest, NextResponse } from "next/server";
import {
  authCookies,
  createSessionCookie,
  readSessionCookie,
} from "@/lib/auth";

export function GET(request: NextRequest) {
  const session = readSessionCookie(
    request.cookies.get(authCookies.session)?.value
  );

  const response = NextResponse.json({
    authenticated: Boolean(session),
    user: session
      ? {
          login: session.login,
          name: session.name,
          avatarUrl: session.avatarUrl,
          profileUrl: session.profileUrl,
        }
      : null,
  });

  if (session) {
    response.cookies.set(
      authCookies.session,
      createSessionCookie({
        login: session.login,
        name: session.name,
        avatarUrl: session.avatarUrl,
        profileUrl: session.profileUrl,
      }),
      {
        httpOnly: true,
        maxAge: authCookies.maxAge,
        path: "/",
        sameSite: "lax",
      }
    );
  }

  return response;
}
