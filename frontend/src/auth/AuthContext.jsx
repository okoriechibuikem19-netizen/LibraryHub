import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import { ApiError, errorMessage } from "../api/client";
import { authApi } from "../api/endpoints";

// Keeps track of who is signed in, for the whole app.
// status: "loading" (asking the backend) | "ready" | "offline" (backend unreachable)

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [status, setStatus] = useState("loading");
  const [offlineMessage, setOfflineMessage] = useState("");

  // Ask the backend who is logged in (on startup, and from the "Try again" button).
  const refresh = useCallback(async () => {
    setStatus("loading");
    try {
      setUser(await authApi.me());
      setStatus("ready");
    } catch (error) {
      if (error instanceof ApiError && error.isUnauthenticated) {
        setUser(null);
        setStatus("ready");
      } else {
        setOfflineMessage(errorMessage(error));
        setStatus("offline");
      }
    }
  }, []);

  useEffect(() => {
    refresh();
  }, [refresh]);

  // Called after a successful OTP check: the backend has already set the login cookie.
  const signIn = useCallback((signedInUser) => {
    setUser(signedInUser);
    setStatus("ready");
  }, []);

  const signOut = useCallback(async () => {
    try {
      await authApi.logout();
    } finally {
      // Even if the server call fails, forget the user locally.
      setUser(null);
    }
  }, []);

  // hasRole("ADMIN", "LIBRARIAN") -> true if the user has any of those roles.
  const hasRole = useCallback((...roles) => !!user && roles.includes(user.role), [user]);

  const value = useMemo(
    () => ({ user, status, offlineMessage, signIn, signOut, refresh, hasRole }),
    [user, status, offlineMessage, signIn, signOut, refresh, hasRole],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error("useAuth must be used inside <AuthProvider>");
  return context;
}

export const STAFF_ROLES = ["ADMIN", "LIBRARIAN"];

export function roleLabel(role) {
  return { ADMIN: "Administrator", LIBRARIAN: "Librarian", MEMBER: "Member" }[role] ?? role;
}
