import { useState } from "react";
import { Eye, EyeOff } from "lucide-react";

/** A password box with an eye button to show or hide what you typed. */
export function PasswordField({ label, error, hint, id, ...input }) {
  const [visible, setVisible] = useState(false);
  const fieldId = id ?? input.name;
  return (
    <div className="field">
      <label htmlFor={fieldId}>{label}</label>
      <div className="password-box">
        <input id={fieldId} type={visible ? "text" : "password"} aria-invalid={!!error} {...input} />
        <button
          type="button"
          className="password-toggle"
          onClick={() => setVisible((v) => !v)}
          aria-label={visible ? "Hide password" : "Show password"}
          title={visible ? "Hide password" : "Show password"}
        >
          {visible ? <EyeOff size={18} /> : <Eye size={18} />}
        </button>
      </div>
      {error ? <span className="field-error">{error}</span> : hint && <span className="field-hint">{hint}</span>}
    </div>
  );
}
