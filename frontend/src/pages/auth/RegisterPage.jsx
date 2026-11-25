import { useState } from "react";
import { Link, useNavigate } from "react-router";
import { ApiError, errorMessage } from "../../api/client";
import { authApi } from "../../api/endpoints";
import { useAuth } from "../../auth/AuthContext";
import { AuthCard } from "../../components/layout/AuthCard";
import { GoogleButton } from "../../components/auth/GoogleButton";
import { Alert, Field, PasswordField } from "../../components/ui";

export function RegisterPage() {
  const navigate = useNavigate();
  const { signIn } = useAuth();
  const [form, setForm] = useState({ name: "", email: "", password: "", confirm: "" });
  const [error, setError] = useState("");
  const [fieldErrors, setFieldErrors] = useState({});
  const [busy, setBusy] = useState(false);

  // update("email") returns an onChange handler that saves that field.
  const update = (key) => (e) => setForm((current) => ({ ...current, [key]: e.target.value }));

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
      const user = await authApi.register(form.name.trim(), form.email.trim(), form.password);
      signIn(user);
      navigate("/", { replace: true });
    } catch (err) {
      setError(errorMessage(err));
      if (err instanceof ApiError) setFieldErrors(err.fieldErrors);
    } finally {
      setBusy(false);
    }
  }

  return (
    <AuthCard
      title="Create your account"
      subtitle="Free forever. Read novels and textbooks, save books, and pick up where you stopped."
      footer={
        <>
          Already have an account? <Link to="/login">Sign in</Link>
        </>
      }
    >
      <Alert>{error}</Alert>
      <form onSubmit={handleSubmit} className="form">
        <Field label="Full name" name="name" autoComplete="name" required maxLength={120}
          value={form.name} onChange={update("name")} error={fieldErrors.name} />
        <Field label="Email" name="email" type="email" autoComplete="email" required
          value={form.email} onChange={update("email")} error={fieldErrors.email} />
        <PasswordField label="Password" name="password" autoComplete="new-password" required
          minLength={8} maxLength={72} hint="At least 8 characters."
          value={form.password} onChange={update("password")} error={fieldErrors.password} />
        <PasswordField label="Confirm password" name="confirm" autoComplete="new-password" required
          value={form.confirm} onChange={update("confirm")} error={fieldErrors.confirm} />
        <button className="btn btn-primary btn-block" disabled={busy}>
          {busy ? "Creating account…" : "Create account"}
        </button>
      </form>
      <GoogleButton
        text="signup_with"
        onSignedIn={(user) => {
          signIn(user);
          navigate("/", { replace: true });
        }}
      />
    </AuthCard>
  );
}
