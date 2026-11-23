const API_BASE_URL =
    import.meta.env.VITE_API_URL ??
    "https://springboot-api-783009266227.us-central1.run.app";
console.log("API_BASE_URL:", API_BASE_URL);
type RequestOptions = Omit<RequestInit, "body"> & {
  body?: unknown;
};

type FormRequestOptions = Omit<RequestInit, "body" | "headers"> & {
  headers?: HeadersInit;
};

type ApiErrorResponse = {
  message?: string;
};

async function errorMessage(response: Response): Promise<string> {
  const fallback = `Request failed: ${response.status}`;

  try {
    const body = (await response.json()) as ApiErrorResponse;
    return body.message || fallback;
  } catch {
    return fallback;
  }
}

async function request<TResponse>(
  path: string,
  options: RequestOptions = {}
): Promise<TResponse> {
  const response = await fetch(`${API_BASE_URL}/api${path}`, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...options.headers,
    },
    body: options.body ? JSON.stringify(options.body) : undefined,
  });

  if (!response.ok) {
    throw new Error(await errorMessage(response));
  }

  return response.json() as Promise<TResponse>;
}

async function requestForm<TResponse>(
  path: string,
  body: FormData,
  options: FormRequestOptions = {}
): Promise<TResponse> {
  const response = await fetch(`${API_BASE_URL}/api${path}`, {
    ...options,
    headers: options.headers,
    method: options.method ?? "POST",
    body,
  });

  if (!response.ok) {
    throw new Error(await errorMessage(response));
  }

  return response.json() as Promise<TResponse>;
}

export const apiClient = {
  get: <TResponse>(path: string, options?: RequestOptions) =>
    request<TResponse>(path, { ...options, method: "GET" }),
  post: <TResponse>(path: string, body: unknown, options?: RequestOptions) =>
    request<TResponse>(path, { ...options, method: "POST", body }),
  postForm: <TResponse>(path: string, body: FormData, options?: FormRequestOptions) =>
    requestForm<TResponse>(path, body, { ...options, method: "POST" }),
  putForm: <TResponse>(path: string, body: FormData, options?: FormRequestOptions) =>
    requestForm<TResponse>(path, body, { ...options, method: "PUT" }),
  put: <TResponse>(path: string, body: unknown, options?: RequestOptions) =>
    request<TResponse>(path, { ...options, method: "PUT", body }),
  delete: <TResponse>(path: string, options?: RequestOptions) =>
    request<TResponse>(path, { ...options, method: "DELETE" }),
};
