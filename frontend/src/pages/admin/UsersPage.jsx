import { useEffect, useState } from "react";
import { Copy, KeyRound, Plus, UserCheck, UserX } from "lucide-react";
import { ApiError, errorMessage } from "../../api/client";
import { adminApi } from "../../api/endpoints";
import { roleLabel, useAuth } from "../../auth/AuthContext";
import { Alert, EmptyState, Field, Modal, PasswordField, Spinner } from "../../components/ui";

/** Admin only: everyone's accounts, suspend/activate, add staff, and password reset links. */
export function UsersPage() {
  const { user: me } = useAuth();
  const [users, setUsers] = useState(null);
  const [search, setSearch] = useState("");
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [resetLink, setResetLink] = useState(null); // { name, link, expiresAt }
  const [addingStaff, setAddingStaff] = useState(false);

  const load = () => adminApi.users().then(setUsers).catch((err) => setError(errorMessage(err)));
  useEffect(() => { load(); }, []);

  async function toggleStatus(u) {
    setError("");
    setNotice("");
    const next = u.status === "SUSPENDED" ? "ACTIVE" : "SUSPENDED";
    try {
      await adminApi.setStatus(u.id, next);
      setNotice(`${u.name} is now ${next === "ACTIVE" ? "active" : "suspended"}.`);
      load();
    } catch (err) {
      setError(errorMessage(err));
    }
  }

  async function makeResetLink(u) {
    setError("");
    try {
      const { token, expiresAt } = await adminApi.resetLink(u.id);
      setResetLink({ name: u.name, link: `${window.location.origin}/reset-password?token=${encodeURIComponent(token)}`, expiresAt });
    } catch (err) {
      setError(errorMessage(err));
    }
  }

  const shown = (users ?? []).filter((u) =>
    `${u.name} ${u.email}`.toLowerCase().includes(search.trim().toLowerCase()),
  );

  return (
    <>
      <div className="page-header">
        <div>
          <h1>Users</h1>
          <p className="muted">Members and staff. Create a reset link for anyone who forgot their password.</p>
        </div>
        <button className="btn btn-primary" onClick={() => setAddingStaff(true)}><Plus size={18} /> Add staff</button>
      </div>

      <div className="toolbar">
        <label className="search">
          <input type="search" placeholder="Search name or email" aria-label="Search users" value={search} onChange={(e) => setSearch(e.target.value)} />
        </label>
      </div>

      <Alert kind="success">{notice}</Alert>
      <Alert>{error}</Alert>

      {users === null ? <Spinner /> : shown.length === 0 ? <EmptyState title="No users found" /> : (
        <div className="panel table-wrap">
          <table>
            <thead>
              <tr><th>Name</th><th>Role</th><th>Status</th><th aria-label="Actions" /></tr>
            </thead>
            <tbody>
              {shown.map((u) => (
                <tr key={u.id}>
                  <td><strong>{u.name}</strong><br /><span className="muted small">{u.email}</span></td>
                  <td>{roleLabel(u.role)}</td>
                  <td>
                    <span className={`badge ${u.status === "ACTIVE" ? "badge-success" : u.status === "SUSPENDED" ? "badge-danger" : ""}`}>
                      {u.status.charAt(0) + u.status.slice(1).toLowerCase()}
                    </span>
                  </td>
                  <td>
                    <div className="row-actions">
                      <button className="btn btn-ghost btn-sm" onClick={() => makeResetLink(u)} aria-label={`Reset link for ${u.name}`}>
                        <KeyRound size={15} /> Reset link
                      </button>
                      {u.id !== me?.id && (
                        <button className="btn btn-ghost btn-sm" onClick={() => toggleStatus(u)}>
                          {u.status === "SUSPENDED" ? <><UserCheck size={15} /> Activate</> : <><UserX size={15} /> Suspend</>}
                        </button>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {resetLink && <ResetLinkModal {...resetLink} onClose={() => setResetLink(null)} />}
      {addingStaff && (
        <AddStaffModal
          onClose={() => setAddingStaff(false)}
          onCreated={(u) => { setAddingStaff(false); setNotice(`${u.name} was added as ${roleLabel(u.role)}.`); load(); }}
        />
      )}
    </>
  );
}

function ResetLinkModal({ name, link, expiresAt, onClose }) {
  const [copied, setCopied] = useState(false);
  async function copy() {
    try {
      await navigator.clipboard.writeText(link);
      setCopied(true);
    } catch {
      setCopied(false);
    }
  }
  return (
    <Modal title={`Reset link for ${name}`} onClose={onClose}>
      <p>Send this link to {name} (for example by WhatsApp). It works <strong>once</strong> and expires{" "}
        {new Date(expiresAt).toLocaleString(undefined, { dateStyle: "medium", timeStyle: "short" })}.</p>
      <div className="copy-box">
        <input readOnly value={link} aria-label="Reset link" onFocus={(e) => e.target.select()} />
        <button className="btn btn-primary" onClick={copy}><Copy size={16} /> {copied ? "Copied" : "Copy"}</button>
      </div>
    </Modal>
  );
}

function AddStaffModal({ onClose, onCreated }) {
  const [form, setForm] = useState({ name: "", email: "", password: "", role: "LIBRARIAN" });
  const [error, setError] = useState("");
  const [fieldErrors, setFieldErrors] = useState({});
  const [busy, setBusy] = useState(false);
  const set = (key) => (e) => setForm((f) => ({ ...f, [key]: e.target.value }));

  async function submit(event) {
    event.preventDefault();
    setBusy(true);
    setError("");
    setFieldErrors({});
    try {
      onCreated(await adminApi.createStaff(form.name.trim(), form.email.trim(), form.password, form.role));
    } catch (err) {
      setError(errorMessage(err));
      if (err instanceof ApiError) setFieldErrors(err.fieldErrors);
    } finally {
      setBusy(false);
    }
  }

  return (
    <Modal title="Add a staff account" onClose={onClose}>
      <form className="form" onSubmit={submit}>
        <Alert>{error}</Alert>
        <Field label="Full name" name="name" required value={form.name} onChange={set("name")} error={fieldErrors.name} />
        <Field label="Email" name="email" type="email" required value={form.email} onChange={set("email")} error={fieldErrors.email} />
        <PasswordField label="Temporary password" name="password" required minLength={8} maxLength={72}
          hint="They can change it under Account." value={form.password} onChange={set("password")} error={fieldErrors.password} />
        <div className="field">
          <label htmlFor="role">Role</label>
          <select id="role" value={form.role} onChange={set("role")}>
            <option value="LIBRARIAN">Librarian</option>
            <option value="ADMIN">Administrator</option>
          </select>
        </div>
        <div className="modal-actions">
          <button type="button" className="btn btn-ghost" onClick={onClose}>Cancel</button>
          <button className="btn btn-primary" disabled={busy}>{busy ? "Adding…" : "Add staff"}</button>
        </div>
      </form>
    </Modal>
  );
}
