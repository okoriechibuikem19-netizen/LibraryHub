export function Spinner({ label = "Loading…" }) {
  return (
    <div className="loading-row">
      <span className="spinner" aria-hidden="true" /> {label}
    </div>
  );
}
