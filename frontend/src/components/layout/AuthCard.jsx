import { LibraryBig } from "lucide-react";

/** The centred card used by the login, register and password pages. */
export function AuthCard({ title, subtitle, children, footer }) {
  return (
    <main className="auth-page">
      <div className="auth-card">
        <div className="brand auth-brand">
          <LibraryBig size={24} /> LibraryHub
        </div>
        <h1>{title}</h1>
        {subtitle && <p className="muted">{subtitle}</p>}
        {children}
        {footer && <div className="auth-footer">{footer}</div>}
      </div>
    </main>
  );
}
