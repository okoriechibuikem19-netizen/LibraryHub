import { useState } from "react";
import { Link, useLocation, useNavigate } from "react-router";
import { errorMessage } from "../../api/client";
import { authApi } from "../../api/endpoints";
import { STAFF_ROLES, useAuth } from "../../auth/AuthContext";
import { AuthCard } from "../../components/layout/AuthCard";
import { Alert, Field, PasswordField } from "../../components/ui";

export function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { signIn } = useAuth();
  const from = location.state?.from; // page the user wanted before being sent to login
  const notice = location.state?.notice; // e.g. "Password changed"

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  async function handleSubmit(event) {
    event.preventDefault();
    setError("");
    setBusy(true);
    try {
      const user = await authApi.login(email.trim(), password);
      signIn(user);
      const home = STAFF_ROLES.includes(user.role) ? "/admin" : "/";
      navigate(from && from !== "/login" ? from : home, { replace: true });
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  return (
    <AuthCard
      title="Sign in"
      subtitle="Welcome back to LibraryHub."
      footer={
        <>
          New here? <Link to="/register">Create an account</Link>
        </>
      }
    >
      {notice && <Alert kind="success">{notice}</Alert>}
      <Alert>{error}</Alert>
      <form onSubmit={handleSubmit} className="form">
        <Field
          label="Email"
          name="email"
          type="email"
          autoComplete="email"
          required
          value={email}
          onChange={(e) => setEmail(e.target.value)}
        />
        <PasswordField
          label="Password"
          name="password"
          autoComplete="current-password"
          required
          value={password}
          onChange={(e) => setPassword(e.target.value)}
        />
        <button className="btn btn-primary btn-block" disabled={busy}>
          {busy ? "Signing in…" : "Sign in"}
        </button>
        <p className="field-hint auth-hint">
          <Link to="/reset-password">Forgot your password?</Link>
        </p>
      </form>
    </AuthCard>
  );
}
