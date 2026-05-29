const apiUrl = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

type RouteContext = {
  params: Promise<{
    path: string[];
  }>;
};

async function proxyRequest(request: Request, context: RouteContext) {
  const { path } = await context.params;
  const incomingUrl = new URL(request.url);
  const targetUrl = new URL(path.join("/"), `${apiUrl}/`);
  targetUrl.search = incomingUrl.search;

  const headers = new Headers(request.headers);
  headers.delete("host");
  headers.delete("content-length");

  const hasBody =
    request.method !== "GET" && request.method !== "HEAD";

  const response = await fetch(targetUrl, {
    method: request.method,
    headers,
    body: hasBody ? await request.arrayBuffer() : undefined,
  });

  const responseHeaders = new Headers(response.headers);
  responseHeaders.delete("content-encoding");
  responseHeaders.delete("content-length");

  return new Response(response.body, {
    status: response.status,
    statusText: response.statusText,
    headers: responseHeaders,
  });
}

export async function GET(
  request: Request,
  context: RouteContext
) {
  return proxyRequest(request, context);
}

export async function POST(
  request: Request,
  context: RouteContext
) {
  return proxyRequest(request, context);
}

export async function PUT(
  request: Request,
  context: RouteContext
) {
  return proxyRequest(request, context);
}

export async function DELETE(
  request: Request,
  context: RouteContext
) {
  return proxyRequest(request, context);
}

export async function OPTIONS(
  request: Request,
  context: RouteContext
) {
  return proxyRequest(request, context);
}
