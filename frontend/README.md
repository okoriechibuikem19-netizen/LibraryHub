# LibraryHub Frontend

React + JavaScript frontend for the LibraryHub Spring Boot backend.
Built with Vite and React Router. All data comes from the backend: nothing is stored in the browser.

## What works now

**Public website (no account needed)**
- Home page: featured book, genres, rows of popular books, textbooks
- Novels and Textbooks pages with genre/subject filters, search and "Load more"
- Book pages with cover, description, genres and number of reads

**With a free account**
- Reader: chapters, chapter list, light/sepia/dark, text size and font, saves your place to your account
- My Library: "Continue reading" (with progress) and "Saved for later"
- Account: change name and password; password boxes have a show/hide eye
- Forgot password: an admin creates a one-time reset link (valid 24 hours)

**Staff area (`/admin`)**: dashboard, library books (add/edit/delete), users (admin only: add staff, suspend, reset links)

Online books come from Project Gutenberg through the Gutendex API, so they're all public domain and free to share.

## Run it (Windows)

You need **Node.js 22.12 or newer** (check with `node -v`).

1. Start the backend first (IntelliJ, or `mvn spring-boot:run` in the `backend` folder).
   It must run on `http://localhost:8081`.
2. Open a terminal in this folder and install the packages (first time only):
   ```powershell
   npm install
   ```
3. Start the frontend:
   ```powershell
   npm run dev
   ```
4. Open **http://localhost:5173** and sign in.

During development, Vite forwards every `/api/...` request to `http://localhost:8081`
(see `vite.config.js`). The browser only sees one address, so the login cookie and CSRF token work
without any CORS setup.

## Check before committing

```powershell
npm run build
```
This builds the production files into `dist/`. If it prints errors, fix them before you commit.

## Project layout

```
src/
  main.jsx, App.jsx       Start-up and all page addresses (routes)
  api/                    client.js (fetch, cookie, CSRF, errors), endpoints.js (one function per endpoint)
  auth/                   AuthContext.jsx (who is signed in), guards.jsx (protected pages)
  components/
    site/                 SiteLayout (public header/footer), BookCard, BookRow
    layout/               AppLayout (staff sidebar), AuthCard, FullPageMessage
    ui/                   Alert, Field, PasswordField, Modal, Spinner, EmptyState
  pages/
    site/                 HomePage, BrowsePage (novels + textbooks), BookPage
    reader/               ReaderPage
    account/              LibraryPage, AccountPage
    auth/                 LoginPage, RegisterPage, ResetPasswordPage
    admin/                DashboardPage, UsersPage, books/ (BooksPage, BookFormModal)
  styles/                 One CSS file per topic; variables.css has the colours
  utils/                  books.js (genres, chapter splitting), format.js (dates, money)
```

## Adding a new page

1. Create a folder and file, e.g. `src/pages/loans/LoansPage.jsx`.
2. Add its backend calls to `src/api/endpoints.js`.
3. Add `<Route path="/loans" element={<LoansPage />} />` inside the protected block in `src/App.jsx`.
4. Add it to `NAV_ITEMS` in `src/components/layout/AppLayout.jsx`
   (use `roles: ["ADMIN", "LIBRARIAN"]` for staff-only pages).
5. If it needs its own styles, create `src/styles/loans.css` and add `@import "./loans.css";` to `src/styles/index.css`.

## Still to move over from the old frontend

- Members page (list, add, edit, suspend): `/api/members`
- Loans page (issue, return, collect fines): `/api/loans`
- Staff: reservations queue and review moderation: `/api/staff/...`
- Admin: create librarian accounts, suspend users: `/api/admin/users`
- Member: reviews and ratings, reading progress, recommendations: `/api/my-library/...`
- Profile page (change name): `PUT /api/auth/profile`
- Reports and CSV export

## Backend improvements found while building this

- **Login response doesn't say which code was sent.** `POST /api/auth/login` sends a REGISTER code when the
  email isn't verified, otherwise a LOGIN code, but the response doesn't say which. The frontend guesses from
  the message text (`src/auth/pendingOtp.js`). Better: add a `purpose` field to `MessageResponse`.
- **Signed-out requests get 403, not 401.** Spring Security has no authentication entry point configured.
  The frontend handles it, but 401 is the correct status. Add
  `.exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))`
  in `SecurityConfig`.
- **`orElseThrow()` without a message** in several controllers (Admin, Staff, Catalog, Reader) turns
  "not found" into a 500 server error. Use `ResourceNotFoundException` instead.
- **Login reveals whether an email exists** ("Account not found" vs "Incorrect email or password").
  Return the same message for both.
- **No rate limiting** on login, OTP and password-reset endpoints.

## Production

- Build with `npm run build` and host the `dist/` folder (Netlify, Vercel, Nginx…).
  Configure the host to send `index.html` for every path, so URLs like `/books` work on refresh.
- Best setup: serve the frontend and backend under the **same site** (for example
  `library.com` and `api.library.com`, or the backend behind `/api` on the same domain). The login cookie is
  `SameSite=Lax`, so it won't be sent between two unrelated domains.
- If the backend is on another address, set `VITE_API_URL` (see `.env.example`) before building, set the
  backend's `FRONTEND_URL` to the frontend address, and set `COOKIE_SECURE=true` (HTTPS only).
