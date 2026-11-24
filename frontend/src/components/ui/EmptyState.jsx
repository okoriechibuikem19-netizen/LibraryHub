/** Shown when a list has nothing in it. */
export function EmptyState({ title, children }) {
  return (
    <div className="empty-state">
      <strong>{title}</strong>
      {children && <p className="muted">{children}</p>}
    </div>
  );
}
