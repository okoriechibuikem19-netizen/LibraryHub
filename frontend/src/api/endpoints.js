// One function per backend endpoint. Pages import from here instead of calling fetch.
//
// What the backend sends back (see backend/.../dto and model):
//   User        { id, name, email, role: "ADMIN"|"LIBRARIAN"|"MEMBER", status, emailVerified }
//   OnlineBook  { id: "gutenberg-84", title, author, coverUrl, sourceUrl, language, genres: [], downloads, summary }
//   ReaderState { shelves: {bookId: "reading"|"saved"}, progress: {bookId: percent},
//                 bookmarks: {bookId: chapterIndex}, theme }
//   Book        { id, bookId, title, author, category, isbn, publisher, yearPublished, price,
//                 availability: "Available"|"On Loan", contentType, sourceUrl, license,
//                 coverUrl, barcode, totalCopies, availableCopies }       (physical library books)
//   Loan        { id, bookId, memberId, borrowedOn, dueDate, returnedOn,
//                 status: "Borrowed"|"Returned"|"Overdue", fineAmount, finePaid }
//   Member      { id, memberId, name, email, phone, joined, status: "Active"|"Suspended" }
//   Reservation { id, bookId, userEmail, status: "WAITING"|"READY"|"FULFILLED"|"CANCELLED", createdAt, expiresAt }

import { api } from "./client";

export const authApi = {
  me: () => api("/api/auth/me"),
  // Both return the signed-in User; the backend sets the login cookie at the same time.
  login: (email, password) => api("/api/auth/login", { method: "POST", body: { email, password } }),
  register: (name, email, password) =>
    api("/api/auth/register", { method: "POST", body: { name, email, password } }),
  logout: () => api("/api/auth/logout", { method: "POST" }),
  /** "Continue with Google": sends Google's token; returns the signed-in User. */
  google: (credential) => api("/api/auth/google", { method: "POST", body: { credential } }),
  /** Uses a reset link an admin created. */
  resetPassword: (token, newPassword) =>
    api("/api/auth/reset-password", { method: "POST", body: { token, newPassword } }),
};

export const reviewsApi = {
  /** { average, count, stars: [1★..5★ counts], reviews: [{ id, name, rating, comment, createdAt, mine }] } */
  list: (bookId, signal) => api(`/api/catalog/novels/${encodeURIComponent(bookId)}/reviews`, { signal }),
  save: (bookId, rating, comment) =>
    api(`/api/catalog/novels/${encodeURIComponent(bookId)}/reviews`, { method: "PUT", body: { rating, comment } }),
  remove: (bookId) => api(`/api/catalog/novels/${encodeURIComponent(bookId)}/reviews`, { method: "DELETE" }),
  report: (bookId, reviewId) =>
    api(`/api/catalog/novels/${encodeURIComponent(bookId)}/reviews/${encodeURIComponent(reviewId)}/report`, { method: "POST" }),
};

export const accountApi = {
  changePassword: (currentPassword, newPassword) =>
    api("/api/auth/password", { method: "PUT", body: { currentPassword, newPassword } }),
  updateName: (name) => api("/api/auth/profile", { method: "PUT", body: { name } }),
};

// Free online books (Project Gutenberg). Browsing is public; the full text needs a signed-in user.
export const catalogApi = {
  /** { q, topic, page } — all optional */
  list: ({ q = "", topic = "", page = 1 } = {}, signal) => {
    const params = new URLSearchParams({ page: String(page) });
    if (q.trim()) params.set("q", q.trim());
    if (topic) params.set("topic", topic);
    return api(`/api/catalog/novels?${params}`, { signal });
  },
  get: (id, signal) => api(`/api/catalog/novels/${encodeURIComponent(id)}`, { signal }),
  text: (id, signal) => api(`/api/catalog/novels/${encodeURIComponent(id)}/text`, { signal, accept: "text/plain" }),
};

// What the signed-in user is reading, has saved, and where they stopped.
export const readerApi = {
  state: () => api("/api/reader/state"),
  save: (state) => api("/api/reader/state", { method: "PUT", body: state }),
};

export const booksApi = {
  /** filters: { search, category, availability } — all optional */
  list: (filters = {}, signal) => {
    const params = new URLSearchParams();
    if (filters.search?.trim()) params.set("search", filters.search.trim());
    if (filters.category) params.set("category", filters.category);
    if (filters.availability) params.set("availability", filters.availability);
    const query = params.toString();
    return api(`/api/books${query ? `?${query}` : ""}`, { signal });
  },
  create: (book) => api("/api/books", { method: "POST", body: book }),
  update: (id, book) => api(`/api/books/${encodeURIComponent(id)}`, { method: "PUT", body: book }),
  remove: (id) => api(`/api/books/${encodeURIComponent(id)}`, { method: "DELETE" }),
};

// Staff-only (Admin or Librarian).
export const loansApi = { list: () => api("/api/loans") };
export const membersApi = { list: () => api("/api/members") };

// Admin only.
export const adminApi = {
  users: () => api("/api/admin/users"),
  setStatus: (id, status) => api(`/api/admin/users/${encodeURIComponent(id)}/status`, { method: "PUT", body: { status } }),
  createStaff: (name, email, password, role) =>
    api("/api/admin/users", { method: "POST", body: { name, email, password, role } }),
  /** Returns { token, expiresAt }. The page turns it into a link. */
  resetLink: (id) => api(`/api/admin/users/${encodeURIComponent(id)}/reset-link`, { method: "POST" }),
  /** Website numbers: users, sign-ups (last 14 days), reading, reviews, most-read and top-rated books. */
  stats: () => api("/api/admin/stats"),
};

// The signed-in user's physical library loans and reservations.
export const myLibraryApi = {
  loans: () => api("/api/my-library/loans"),
  reservations: () => api("/api/my-library/reservations"),
  reserve: (bookId) => api("/api/my-library/reservations", { method: "POST", body: { bookId } }),
};
