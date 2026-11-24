/** "2026-11-23" (from the backend) -> "23 Nov 2026" in the user's locale. */
export function formatDate(value) {
  if (!value) return "—";
  const date = /^\d{4}-\d{2}-\d{2}$/.test(value) ? new Date(`${value}T00:00:00`) : new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleDateString(undefined, { dateStyle: "medium" });
}

// Change the currency here if your library doesn't use Naira.
const money = new Intl.NumberFormat("en-NG", { style: "currency", currency: "NGN" });

export function formatMoney(amount) {
  return money.format(amount);
}

/** Today's date as "YYYY-MM-DD" in the user's own time zone. */
export function todayIso() {
  const now = new Date();
  return new Date(now.getTime() - now.getTimezoneOffset() * 60_000).toISOString().slice(0, 10);
}

export function isOverdue(loan) {
  return loan.status === "Overdue" || (loan.status === "Borrowed" && loan.dueDate < todayIso());
}
