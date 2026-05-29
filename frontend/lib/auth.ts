import crypto from "node:crypto";

export type AuthSession = {
  login: string;
  name: string | null;
  avatarUrl: string | null;
  profileUrl: string;
  expiresAt: number;
};

const sessionCookieName = "ai-reviewer-session";
const stateCookieName = "ai-reviewer-oauth-state";
const idleTimeoutSeconds = 60 * 30;

export const authCookies = {
  session: sessionCookieName,
  state: stateCookieName,
  maxAge: idleTimeoutSeconds,
};

export function createOAuthState() {
  return crypto.randomBytes(24).toString("base64url");
}

export function getAuthSecret() {
  return (
    process.env.AUTH_SECRET ||
    process.env.GITHUB_CLIENT_SECRET ||
    "local-development-auth-secret"
  );
}

export function createSessionCookie(session: Omit<AuthSession, "expiresAt">) {
  const payload: AuthSession = {
    ...session,
    expiresAt: Date.now() + idleTimeoutSeconds * 1000,
  };
  const encodedPayload = Buffer.from(JSON.stringify(payload)).toString(
    "base64url"
  );
  const signature = crypto
    .createHmac("sha256", getAuthSecret())
    .update(encodedPayload)
    .digest("base64url");

  return `${encodedPayload}.${signature}`;
}

export function readSessionCookie(value?: string): AuthSession | null {
  if (!value) {
    return null;
  }

  const [encodedPayload, signature] = value.split(".");

  if (!encodedPayload || !signature) {
    return null;
  }

  const expectedSignature = crypto
    .createHmac("sha256", getAuthSecret())
    .update(encodedPayload)
    .digest("base64url");

  const signatureBuffer = Buffer.from(signature);
  const expectedSignatureBuffer = Buffer.from(expectedSignature);

  if (
    signatureBuffer.length !== expectedSignatureBuffer.length ||
    !crypto.timingSafeEqual(signatureBuffer, expectedSignatureBuffer)
  ) {
    return null;
  }

  const session = JSON.parse(
    Buffer.from(encodedPayload, "base64url").toString("utf8")
  ) as AuthSession;

  if (session.expiresAt < Date.now()) {
    return null;
  }

  return session;
}
