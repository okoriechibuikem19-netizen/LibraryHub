import { useState } from "react";
import { ApiError, errorMessage } from "../../api/client";
import { accountApi } from "../../api/endpoints";
import { roleLabel, useAuth } from "../../auth/AuthContext";
import { Alert, Field, PasswordField } from "../../components/ui";

/** Change your name and password. */
export function AccountPage() {
  const { user, signIn } = useAuth();
  const [name, setName] = useState(user?.name ?? "");
  const [nameMsg, setNameMsg] = useState({ kind: "", text: "" });

  const [pw, setPw] = useState({ current: "", next: "", confirm: "" });
  const [pwMsg, setPwMsg] = useState({ kind: "", text: "" });
  const [pwErrors, setPwErrors] = useState({});
  const [busy, setBusy] = useState(false);

  async function saveName(event) {
    event.preventDefault();
    try {
      signIn(await accountApi.updateName(name.trim())); // update the name shown in the header
      setNameMsg({ kind: "success", text: "Name updated." });
    } catch (err) {
      setNameMsg({ kind: "error", text: errorMessage(err) });
    }
  }

  async function changePassword(event) {
    event.preventDefault();
    setPwMsg({ kind: "", text: "" });
    setPwErrors({});
    if (pw.next !== pw.confirm) {
      setPwErrors({ confirm: "Passwords don't match" });
      return;
    }
    setBusy(true);
    try {
      await accountApi.changePassword(pw.current, pw.next);
      setPw({ current: "", next: "", confirm: "" });
      setPwMsg({ kind: "success", text: "Password changed. Use the new one next time you sign in." });
    } catch (err) {
      setPwMsg({ kind: "error", text: errorMessage(err) });
      if (err instanceof ApiError) setPwErrors(err.fieldErrors);
    } finally {
      setBusy(false);
    }
  }

  const setField = (key) => (e) => setPw((p) => ({ ...p, [key]: e.target.value }));

  return (
    <div className="page-container narrow">
      <div className="browse-header">
        <h1>Account</h1>
        <p className="muted">{user?.email} · {roleLabel(user?.role)}</p>
      </div>

      <section className="panel account-panel">
        <h2>Your name</h2>
        {nameMsg.text && <Alert kind={nameMsg.kind}>{nameMsg.text}</Alert>}
        <form className="form" onSubmit={saveName}>
          <Field label="Full name" name="name" required maxLength={120} value={name} onChange={(e) => setName(e.target.value)} />
          <div><button className="btn btn-primary">Save name</button></div>
        </form>
      </section>

      <section className="panel account-panel">
        <h2>Change password</h2>
        {pwMsg.text && <Alert kind={pwMsg.kind}>{pwMsg.text}</Alert>}
        <form className="form" onSubmit={changePassword}>
          <PasswordField label="Current password" name="currentPassword" autoComplete="current-password" required
            value={pw.current} onChange={setField("current")} error={pwErrors.currentPassword} />
          <PasswordField label="New password" name="newPassword" autoComplete="new-password" required minLength={8} maxLength={72}
            hint="At least 8 characters." value={pw.next} onChange={setField("next")} error={pwErrors.newPassword} />
          <PasswordField label="Confirm new password" name="confirmPassword" autoComplete="new-password" required
            value={pw.confirm} onChange={setField("confirm")} error={pwErrors.confirm} />
          <div><button className="btn btn-primary" disabled={busy}>{busy ? "Saving…" : "Change password"}</button></div>
        </form>
      </section>
    </div>
  );
}
