import { Navigate, Outlet, useLocation } from "react-router";
import { FullPageMessage } from "../components/layout/FullPageMessage";
import { useAuth } from "./AuthContext";

/** Shown while we ask the backend who is logged in, or when it can't be reached. */
function AuthGate({ children }) {
  const { status, offlineMessage, refresh } = useAuth();
  if (status === "loading") return <FullPageMessage title="Loading LibraryHub…" spinner />;
  if (status === "offline") {
    return (
      <FullPageMessage title="Can't reach the server" message={offlineMessage}>
        <button className="btn btn-primary" onClick={() => refresh()}>
          Try again
        </button>
      </FullPageMessage>
    );
  }
  return children;
}

/**
 * Pages that need a signed-in user.
 * Optional: roles={["ADMIN"]} to allow only some roles.
 */
export function RequireAuth({ roles, children }) {
  const { user, hasRole } = useAuth();
  const location = useLocation();
  return (
    <AuthGate>
      {!user ? (
        <Navigate to="/login" replace state={{ from: location.pathname }} />
      ) : roles && !hasRole(...roles) ? (
        <FullPageMessage title="No access" message="Your account doesn't have permission to open this page." />
      ) : (
        children
      )}
    </AuthGate>
  );
}

/** Login, register, etc. If someone is already signed in, send them to the dashboard. */
export function PublicOnly() {
  const { user } = useAuth();
  return <AuthGate>{user ? <Navigate to="/" replace /> : <Outlet />}</AuthGate>;
}
