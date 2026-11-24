// One place for every call to the Spring Boot backend.
//
// It takes care of three things so pages don't have to:
//  1. Sends the login cookie (credentials: "include").
//  2. Sends the CSRF token on POST/PUT/PATCH/DELETE. The backend has CSRF
//     protection switched on (SecurityConfig.java), so without this every form fails.
//  3. Turns backend errors (ApiError.java) into readable messages.

const API_URL = (import.meta.env.VITE_API_URL ?? "").replace(/\/$/, "");

export class ApiError extends Error {
  constructor(status, message, fieldErrors = {}) {
    super(message);
    this.name = "ApiError";
    this.status = status; // HTTP status code, or 0 if the server couldn't be reached
    this.fieldErrors = fieldErrors; // e.g. { title: "must not be blank" }
  }

  /** Spring Security answers 403 (not 401) when nobody is logged in. */
  get isUnauthenticated() {
    return this.status === 401 || this.status === 403;
  }
}

const UNSAFE_METHODS = new Set(["POST", "PUT", "PATCH", "DELETE"]);
let csrfToken = null;

async function loadCsrfToken() {
  const response = await fetch(`${API_URL}/api/auth/csrf`, { credentials: "include" });
  if (!response.ok) {
    throw new ApiError(response.status, "Could not start a secure session with the server.");
  }
  const data = await response.json();
  csrfToken = data.token;
  return data.token;
}

/**
 * Call the backend.
 *   path    e.g. "/api/books"
 *   options { method: "POST", body: {...}, signal, accept: "text/plain" } (all optional)
 */
export async function api(path, options = {}, isRetry = false) {
  const method = options.method ?? "GET";
  const headers = { Accept: options.accept ?? "application/json" };
  if (options.body !== undefined) headers["Content-Type"] = "application/json";

  let response;
  try {
    if (UNSAFE_METHODS.has(method)) headers["X-XSRF-TOKEN"] = csrfToken ?? (await loadCsrfToken());
    response = await fetch(`${API_URL}${path}`, {
      method,
      headers,
      credentials: "include",
      body: options.body === undefined ? undefined : JSON.stringify(options.body),
      signal: options.signal,
    });
  } catch (error) {
    if (error.name === "AbortError") throw error;
    if (error instanceof ApiError) throw error;
    throw new ApiError(0, "Cannot reach the LibraryHub server. Check that the backend is running and try again.");
  }

  // A 403 on a form submit is often an expired CSRF token: get a fresh one and try once more.
  if (response.status === 403 && UNSAFE_METHODS.has(method) && !isRetry) {
    csrfToken = null;
    return api(path, options, true);
  }

  if (!response.ok) throw await toApiError(response);
  if (response.status === 204) return undefined;

  const text = await response.text();
  if (!text) return undefined;
  const contentType = response.headers.get("content-type") ?? "";
  return contentType.includes("json") ? JSON.parse(text) : text;
}

async function toApiError(response) {
  let message = "";
  let fieldErrors = {};
  try {
    const body = await response.json();
    message = body.message ?? "";
    fieldErrors = body.validationErrors ?? {};
  } catch {
    // Empty or non-JSON body (Spring Security sends none for 403).
  }

  if (!message) {
    if (response.status === 401 || response.status === 403) {
      message = "You are not signed in, or you don't have permission to do this.";
    } else if (response.status === 404) {
      message = "We couldn't find what you were looking for.";
    } else if (response.status >= 500) {
      message = "The server had a problem. Please try again in a moment.";
    } else {
      message = `Request failed (${response.status}).`;
    }
  }
  return new ApiError(response.status, message, fieldErrors);
}

/** Use in catch blocks: always gives a message you can show to the user. */
export function errorMessage(error) {
  if (error instanceof Error) return error.message;
  return "Something went wrong.";
}
