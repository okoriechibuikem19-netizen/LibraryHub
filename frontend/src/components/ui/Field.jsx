/**
 * A labelled input that can show the backend's validation message for that field.
 * Any normal <input> props (type, required, value, onChange…) are passed through.
 */
export function Field({ label, error, hint, id, ...input }) {
  const fieldId = id ?? input.name;
  return (
    <div className="field">
      <label htmlFor={fieldId}>{label}</label>
      <input id={fieldId} aria-invalid={!!error} {...input} />
      {error ? <span className="field-error">{error}</span> : hint && <span className="field-hint">{hint}</span>}
    </div>
  );
}
