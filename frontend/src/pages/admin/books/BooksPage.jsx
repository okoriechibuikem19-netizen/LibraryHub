import { useCallback, useEffect, useState } from "react";
import { BookMarked, ExternalLink, Pencil, Plus, Search, Trash2 } from "lucide-react";
import { errorMessage } from "../../../api/client";
import { booksApi, myLibraryApi } from "../../../api/endpoints";
import { STAFF_ROLES, useAuth } from "../../../auth/AuthContext";
import { Alert, EmptyState, Spinner } from "../../../components/ui";
import { BookFormModal } from "./BookFormModal";

export function BooksPage() {
  const { hasRole } = useAuth();
  const isStaff = hasRole(...STAFF_ROLES);
  const isAdmin = hasRole("ADMIN");

  const [filters, setFilters] = useState({ search: "", category: "", availability: "" });
  const [search, setSearch] = useState(""); // what's typed; applied after a short pause
  const [books, setBooks] = useState(null); // null = still loading
  const [categories, setCategories] = useState([]);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [editing, setEditing] = useState(null); // null = closed, "new" = add form, a book = edit form

  const load = useCallback(
    async (signal) => {
      try {
        const result = await booksApi.list(filters, signal);
        setBooks(result);
        setError("");
        // Keep every category we've seen so the dropdown doesn't shrink while filtering.
        setCategories((known) => [...new Set([...known, ...result.map((b) => b.category)])].sort());
      } catch (err) {
        if (err.name === "AbortError") return;
        setError(errorMessage(err));
        setBooks((current) => current ?? []);
      }
    },
    [filters],
  );

  // Reload whenever the filters change. Cancel the old request if a new one starts.
  useEffect(() => {
    const controller = new AbortController();
    load(controller.signal);
    return () => controller.abort();
  }, [load]);

  // Wait until the user stops typing for 300 ms before searching.
  useEffect(() => {
    const timer = setTimeout(() => setFilters((f) => (f.search === search ? f : { ...f, search })), 300);
    return () => clearTimeout(timer);
  }, [search]);

  async function handleDelete(book) {
    if (!window.confirm(`Delete "${book.title}"? This cannot be undone.`)) return;
    setNotice("");
    try {
      await booksApi.remove(book.id);
      setNotice(`"${book.title}" was deleted.`);
      load();
    } catch (err) {
      setError(errorMessage(err));
    }
  }

  async function handleReserve(book) {
    setNotice("");
    setError("");
    try {
      await myLibraryApi.reserve(book.id);
      setNotice(`"${book.title}" is reserved. We'll email you when it's ready to collect.`);
    } catch (err) {
      setError(errorMessage(err));
    }
  }

  function handleSaved(saved, wasNew) {
    setEditing(null);
    setNotice(`"${saved.title}" was ${wasNew ? "added" : "updated"}.`);
    load();
  }

  const hasFilters = filters.search || filters.category || filters.availability;

  return (
    <>
      <div className="page-header">
        <div>
          <h1>Books</h1>
          <p className="muted">{isStaff ? "Manage the catalogue and copies." : "Browse the catalogue and reserve books."}</p>
        </div>
        {isStaff && (
          <button className="btn btn-primary" onClick={() => setEditing("new")}>
            <Plus size={18} /> Add book
          </button>
        )}
      </div>

      <div className="toolbar">
        <label className="search">
          <Search size={18} aria-hidden="true" />
          <input
            type="search"
            placeholder="Search title, author or ISBN"
            aria-label="Search books"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </label>
        <select
          aria-label="Category"
          value={filters.category}
          onChange={(e) => setFilters((f) => ({ ...f, category: e.target.value }))}
        >
          <option value="">All categories</option>
          {categories.map((category) => (
            <option key={category}>{category}</option>
          ))}
        </select>
        <select
          aria-label="Availability"
          value={filters.availability}
          onChange={(e) => setFilters((f) => ({ ...f, availability: e.target.value }))}
        >
          <option value="">Any availability</option>
          <option value="Available">Available</option>
          <option value="On Loan">On loan</option>
        </select>
      </div>

      <Alert kind="success">{notice}</Alert>
      <Alert>{error}</Alert>

      {books === null ? (
        <Spinner label="Loading books…" />
      ) : books.length === 0 ? (
        <EmptyState title="No books found">
          {hasFilters
            ? "Try a different search or clear the filters."
            : isStaff
              ? "Add your first book to start the catalogue."
              : "The catalogue is empty for now."}
        </EmptyState>
      ) : (
        <div className="panel table-wrap">
          <table>
            <thead>
              <tr>
                <th>Title</th>
                <th>Category</th>
                <th>ISBN</th>
                <th>Copies</th>
                <th>Status</th>
                <th aria-label="Actions" />
              </tr>
            </thead>
            <tbody>
              {books.map((book) => (
                <tr key={book.id}>
                  <td>
                    <div className="book-cell">
                      {book.coverUrl ? (
                        <img src={book.coverUrl} alt="" className="cover" loading="lazy" />
                      ) : (
                        <div className="cover cover-placeholder" aria-hidden="true">
                          {book.title.charAt(0)}
                        </div>
                      )}
                      <div>
                        <strong>{book.title}</strong>
                        <span className="muted">
                          {book.author} · {book.yearPublished}
                        </span>
                      </div>
                    </div>
                  </td>
                  <td>{book.category}</td>
                  <td className="mono">{book.isbn}</td>
                  <td>
                    {book.availableCopies ?? "?"} / {book.totalCopies ?? "?"}
                  </td>
                  <td>
                    <span className={`badge ${book.availability === "Available" ? "badge-success" : "badge-warning"}`}>
                      {book.availability}
                    </span>
                  </td>
                  <td>
                    <div className="row-actions">
                      {book.sourceUrl && (
                        <a className="icon-btn" href={book.sourceUrl} target="_blank" rel="noreferrer" title="Open online copy">
                          <ExternalLink size={17} />
                        </a>
                      )}
                      {!isStaff && (
                        <button className="btn btn-ghost btn-sm" onClick={() => handleReserve(book)}>
                          <BookMarked size={16} /> Reserve
                        </button>
                      )}
                      {isStaff && (
                        <button className="icon-btn" onClick={() => setEditing(book)} title="Edit" aria-label={`Edit ${book.title}`}>
                          <Pencil size={17} />
                        </button>
                      )}
                      {isAdmin && (
                        <button className="icon-btn danger" onClick={() => handleDelete(book)} title="Delete" aria-label={`Delete ${book.title}`}>
                          <Trash2 size={17} />
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

      {editing && (
        <BookFormModal
          book={editing === "new" ? null : editing}
          onClose={() => setEditing(null)}
          onSaved={handleSaved}
        />
      )}
    </>
  );
}
