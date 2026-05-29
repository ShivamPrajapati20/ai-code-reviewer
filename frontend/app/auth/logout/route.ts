import { NextResponse } from "next/server";
import { authCookies } from "@/lib/auth";

export function POST() {
  const response = NextResponse.json({ ok: true });
  response.cookies.delete(authCookies.session);
  return response;
}
