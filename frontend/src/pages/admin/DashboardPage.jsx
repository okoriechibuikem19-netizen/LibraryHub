import { useEffect, useState } from "react";
import { Link } from "react-router";
import { errorMessage } from "../../api/client";
import { booksApi, loansApi, membersApi, myLibraryApi } from "../../api/endpoints";
import { STAFF_ROLES, useAuth } from "../../auth/AuthContext";
import { Alert, EmptyState, Spinner } from "../../components/ui";
import { formatDate, formatMoney, isOverdue } from "../../utils/format";

export function DashboardPage() {
  const { user, hasRole } = useAuth();
  if (!user) return null;
  return (
    <>
      <div className="page-header">
        <div>
          <h1>Welcome, {user.name.split(" ")[0]}</h1>
          <p className="muted">{new Date().toLocaleDateString(undefined, { dateStyle: "full" })}</p>
        </div>
      </div>
      {hasRole(...STAFF_ROLES) ? <StaffDashboard /> : <MemberDashboard />}
    </>
  );
}

/** Loans only store the book's id, so look up its title. */
function bookTitle(books, id) {
  return books.find((b) => b.id === id || b.bookId === id)?.title ?? id;
}

/**
 * Runs several requests at once. If one fails, the others still show
 * and the failure becomes a message. Returns [results, errorMessages].
 */
async function loadAll(requests) {
  const settled = await Promise.allSettled(requests);
  const results = settled.map((r) => (r.status === "fulfilled" ? r.value : []));
  const errors = settled.filter((r) => r.status === "rejected").map((r) => errorMessage(r.reason));
  return [results, [...new Set(errors)]];
}

function StaffDashboard() {
  const [data, setData] = useState(null);
  const [errors, setErrors] = useState([]);

  useEffect(() => {
    loadAll([booksApi.list(), loansApi.list(), membersApi.list()]).then(([[books, loans, members], errs]) => {
      setData({ books, loans, members });
      setErrors(errs);
    });
  }, []);

  if (!data) return <Spinner />;

  const { books, loans, members } = data;
  const activeLoans = loans.filter((loan) => loan.status !== "Returned");
  const overdue = activeLoans.filter(isOverdue);
  const unpaidFines = loans
    .filter((loan) => !loan.finePaid && (loan.fineAmount ?? 0) > 0)
    .reduce((sum, loan) => sum + (loan.fineAmount ?? 0), 0);
  const copiesAvailable = books.reduce((sum, book) => sum + (book.availableCopies ?? 0), 0);
  const memberName = (id) => members.find((m) => m.id === id || m.memberId === id)?.name ?? id;

  return (
    <>
      {errors.map((message) => (
        <Alert key={message}>{message}</Alert>
      ))}
      <section className="stats">
        <Stat label="Book titles" value={books.length} />
        <Stat label="Copies on the shelf" value={copiesAvailable} />
        <Stat label="Active loans" value={activeLoans.length} />
        <Stat label="Overdue" value={overdue.length} tone={overdue.length ? "danger" : undefined} />
        <Stat label="Members" value={members.length} />
        <Stat label="Unpaid fines" value={formatMoney(unpaidFines)} tone={unpaidFines ? "warning" : undefined} />
      </section>

      <section className="panel">
        <div className="panel-header">
          <h2>Overdue loans</h2>
        </div>
        {overdue.length === 0 ? (
          <EmptyState title="Nothing overdue">All borrowed books are within their due dates.</EmptyState>
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Book</th>
                  <th>Member</th>
                  <th>Due</th>
                  <th>Fine</th>
                </tr>
              </thead>
              <tbody>
                {overdue.map((loan) => (
                  <tr key={loan.id}>
                    <td>{bookTitle(books, loan.bookId)}</td>
                    <td>{memberName(loan.memberId)}</td>
                    <td className="text-danger">{formatDate(loan.dueDate)}</td>
                    <td>{formatMoney(loan.fineAmount ?? 0)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </>
  );
}

function MemberDashboard() {
  const [data, setData] = useState(null);
  const [errors, setErrors] = useState([]);

  useEffect(() => {
    loadAll([booksApi.list(), myLibraryApi.loans(), myLibraryApi.reservations()]).then(
      ([[books, loans, reservations], errs]) => {
        setData({ books, loans, reservations });
        setErrors(errs);
      },
    );
  }, []);

  if (!data) return <Spinner />;
  const current = data.loans.filter((loan) => loan.status !== "Returned");
  const openReservations = data.reservations.filter((r) => r.status === "WAITING" || r.status === "READY");

  return (
    <>
      {errors.map((message) => (
        <Alert key={message}>{message}</Alert>
      ))}
      <section className="stats">
        <Stat label="Books I have" value={current.length} />
        <Stat label="Overdue" value={current.filter(isOverdue).length} tone={current.some(isOverdue) ? "danger" : undefined} />
        <Stat label="Reservations" value={openReservations.length} />
      </section>

      <section className="panel">
        <div className="panel-header">
          <h2>My current loans</h2>
          <Link to="/books" className="btn btn-ghost">
            Browse books
          </Link>
        </div>
        {current.length === 0 ? (
          <EmptyState title="No books borrowed right now">Find something to read in the catalogue.</EmptyState>
        ) : (
          <ul className="list">
            {current.map((loan) => (
              <li key={loan.id}>
                <span>{bookTitle(data.books, loan.bookId)}</span>
                <span className={isOverdue(loan) ? "text-danger" : "muted"}>
                  {isOverdue(loan) ? "Overdue since " : "Due "}
                  {formatDate(loan.dueDate)}
                </span>
              </li>
            ))}
          </ul>
        )}
      </section>

      <section className="panel">
        <div className="panel-header">
          <h2>My reservations</h2>
        </div>
        {openReservations.length === 0 ? (
          <EmptyState title="No reservations">Reserve a book from the Books page and we'll email you when it's ready.</EmptyState>
        ) : (
          <ul className="list">
            {openReservations.map((r) => (
              <li key={r.id}>
                <span>{bookTitle(data.books, r.bookId)}</span>
                <span className={`badge ${r.status === "READY" ? "badge-success" : ""}`}>
                  {r.status === "READY" ? `Ready, collect by ${formatDate(r.expiresAt)}` : "Waiting"}
                </span>
              </li>
            ))}
          </ul>
        )}
      </section>
    </>
  );
}

/** One number card. tone: "danger" (red) | "warning" (amber) | nothing. */
function Stat({ label, value, tone }) {
  return (
    <div className={`stat${tone ? ` stat-${tone}` : ""}`}>
      <span className="stat-label">{label}</span>
      <strong className="stat-value">{value}</strong>
    </div>
  );
}
