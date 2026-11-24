import { BrowserRouter, Link, Route, Routes } from "react-router";
import { AuthProvider, STAFF_ROLES } from "./auth/AuthContext";
import { PublicOnly, RequireAuth } from "./auth/guards";
import { AppLayout } from "./components/layout/AppLayout";
import { FullPageMessage } from "./components/layout/FullPageMessage";
import { SiteLayout } from "./components/site/SiteLayout";
import { AccountPage } from "./pages/account/AccountPage";
import { LibraryPage } from "./pages/account/LibraryPage";
import { BooksPage } from "./pages/admin/books/BooksPage";
import { DashboardPage } from "./pages/admin/DashboardPage";
import { UsersPage } from "./pages/admin/UsersPage";
import { LoginPage } from "./pages/auth/LoginPage";
import { RegisterPage } from "./pages/auth/RegisterPage";
import { ResetPasswordPage } from "./pages/auth/ResetPasswordPage";
import { ReaderPage } from "./pages/reader/ReaderPage";
import { BookPage } from "./pages/site/BookPage";
import { BrowsePage } from "./pages/site/BrowsePage";
import { HomePage } from "./pages/site/HomePage";

// All the pages and their addresses.
export function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          {/* The public website: anyone can browse. */}
          <Route element={<SiteLayout />}>
            <Route path="/" element={<HomePage />} />
            <Route path="/browse" element={<BrowsePage mode="novels" />} />
            <Route path="/textbooks" element={<BrowsePage mode="textbooks" />} />
            <Route path="/book/:id" element={<BookPage />} />
            <Route path="/library" element={<RequireAuth><LibraryPage /></RequireAuth>} />
            <Route path="/account" element={<RequireAuth><AccountPage /></RequireAuth>} />
          </Route>

          {/* Reading needs an account. */}
          <Route path="/read/:id" element={<RequireAuth><ReaderPage /></RequireAuth>} />

          {/* Sign in pages (signed-in users are sent home). */}
          <Route element={<PublicOnly />}>
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<RegisterPage />} />
          </Route>
          <Route path="/reset-password" element={<ResetPasswordPage />} />

          {/* Staff area: librarians and admins. */}
          <Route
            element={
              <RequireAuth roles={STAFF_ROLES}>
                <AppLayout />
              </RequireAuth>
            }
          >
            <Route path="/admin" element={<DashboardPage />} />
            <Route path="/admin/books" element={<BooksPage />} />
            <Route path="/admin/users" element={<RequireAuth roles={["ADMIN"]}><UsersPage /></RequireAuth>} />
          </Route>

          <Route
            path="*"
            element={
              <FullPageMessage title="Page not found" message="That page doesn't exist.">
                <Link to="/" className="btn btn-primary">
                  Go to the library
                </Link>
              </FullPageMessage>
            }
          />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
}
