/** A coloured message box. kind: "error" (default) | "success" | "info". Shows nothing if empty. */
export function Alert({ kind = "error", children }) {
  if (!children) return null;
  return (
    <div className={`alert alert-${kind}`} role={kind === "error" ? "alert" : "status"}>
      {children}
    </div>
  );
}
