import { useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router";
import { ApiError, errorMessage } from "../../api/client";
import { authApi } from "../../api/endpoints";
import { AuthCard } from "../../components/layout/AuthCard";
import { Alert, PasswordField } from "../../components/ui";

/**
 * Opened from a reset link an admin created: /reset-password?token=...
 * Without a token it explains how to get one.
 */
export function ResetPasswordPage() {
  const [params] = useSearchParams();
  const token = params.get("token") ?? "";
  const navigate = useNavigate();
  const [form, setForm] = useState({ password: "", confirm: "" });
  const [error, setError] = useState("");
  const [fieldErrors, setFieldErrors] = useState({});
  const [busy, setBusy] = useState(false);

  if (!token) {
    return (
      <AuthCard title="Forgot your password?" footer={<Link to="/login">Back to sign in</Link>}>
        <p>
          Ask a librarian or the library admin for a <strong>password reset link</strong>. When you open it, you can
          choose a new password here.
        </p>
      </AuthCard>
    );
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setError("");
    setFieldErrors({});
    if (form.password !== form.confirm) {
      setFieldErrors({ confirm: "Passwords don't match" });
      return;
    }
    setBusy(true);
    try {
      await authApi.resetPassword(token, form.password);
      navigate("/login", { replace: true, state: { notice: "Password changed. You can sign in now." } });
    } catch (err) {
      setError(errorMessage(err));
      if (err instanceof ApiError) setFieldErrors(err.fieldErrors);
    } finally {
      setBusy(false);
    }
  }

  return (
    <AuthCard title="Choose a new password" footer={<Link to="/login">Back to sign in</Link>}>
      <Alert>{error}</Alert>
      <form onSubmit={handleSubmit} className="form">
        <PasswordField label="New password" name="password" autoComplete="new-password" required minLength={8} maxLength={72}
          hint="At least 8 characters." value={form.password} error={fieldErrors.newPassword}
          onChange={(e) => setForm((f) => ({ ...f, password: e.target.value }))} />
        <PasswordField label="Confirm new password" name="confirm" autoComplete="new-password" required
          value={form.confirm} error={fieldErrors.confirm}
          onChange={(e) => setForm((f) => ({ ...f, confirm: e.target.value }))} />
        <button className="btn btn-primary btn-block" disabled={busy}>{busy ? "Saving…" : "Save new password"}</button>
      </form>
    </AuthCard>
  );
}
