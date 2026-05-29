import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "GitHub PR Reviewer",
  description:
    "Sign in with GitHub and review open pull requests with AI-powered feedback, severity summaries, and suggested fixes.",
  icons: {
    icon: [
      {
        url: "/icon.svg",
        type: "image/svg+xml",
      },
    ],
    shortcut: "/icon.svg",
    apple: "/apple-icon.svg",
  },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en">
      <body className="min-h-full flex flex-col">{children}</body>
    </html>
  );
}
