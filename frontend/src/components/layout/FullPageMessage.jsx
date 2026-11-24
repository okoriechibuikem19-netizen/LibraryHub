/** A centred card that fills the page: loading, errors, "page not found". */
export function FullPageMessage({ title, message, spinner = false, children }) {
  return (
    <main className="full-page">
      <div className="full-page-card">
        {spinner && <span className="spinner" aria-hidden="true" />}
        <h1>{title}</h1>
        {message && <p className="muted">{message}</p>}
        {children}
      </div>
    </main>
  );
}
