import { useState } from "react";
import { ApiError, errorMessage } from "../../../api/client";
import { booksApi } from "../../../api/endpoints";
import { Alert, Field, Modal } from "../../../components/ui";

// The fields the backend accepts when saving a book (BookRequest.java).
function toForm(book) {
  return {
    bookId: book?.bookId ?? "",
    title: book?.title ?? "",
    author: book?.author ?? "",
    category: book?.category ?? "",
    isbn: book?.isbn ?? "",
    publisher: book?.publisher ?? "",
    yearPublished: book?.yearPublished ?? new Date().getFullYear(),
    price: book?.price ?? 0,
    availability: book?.availability ?? "Available",
    contentType: book?.contentType ?? "",
    sourceUrl: book?.sourceUrl ?? "",
    license: book?.license ?? "",
    coverUrl: book?.coverUrl ?? "",
    barcode: book?.barcode ?? "",
    totalCopies: book?.totalCopies ?? 1,
  };
}

const CATEGORY_SUGGESTIONS = ["Fiction", "Non-fiction", "Science", "Technology", "History", "Biography", "Children", "Reference"];
const FORMAT_SUGGESTIONS = ["Physical", "PDF", "Online"];
const LICENSE_SUGGESTIONS = ["Library-owned copy", "Public domain", "Licensed"];

/** Add a book (book = null) or edit one. Calls onSaved(savedBook, wasNew) when done. */
export function BookFormModal({ book, onClose, onSaved }) {
  const isNew = book === null;
  const [form, setForm] = useState(() => toForm(book));
  const [error, setError] = useState("");
  const [fieldErrors, setFieldErrors] = useState({});
  const [busy, setBusy] = useState(false);

  const text = (key) => (e) => setForm((f) => ({ ...f, [key]: e.target.value }));
  const number = (key) => (e) => setForm((f) => ({ ...f, [key]: e.target.value === "" ? 0 : Number(e.target.value) }));

  async function handleSubmit(event) {
    event.preventDefault();
    setError("");
    setFieldErrors({});
    setBusy(true);
    try {
      const payload = { ...form, coverUrl: form.coverUrl.trim(), barcode: form.barcode.trim() };
      const saved = isNew ? await booksApi.create(payload) : await booksApi.update(book.id, payload);
      onSaved(saved, isNew);
    } catch (err) {
      setError(errorMessage(err));
      if (err instanceof ApiError) setFieldErrors(err.fieldErrors);
    } finally {
      setBusy(false);
    }
  }

  const f = fieldErrors;
  return (
    <Modal title={isNew ? "Add a book" : `Edit "${book.title}"`} onClose={onClose} wide>
      <form onSubmit={handleSubmit} className="form">
        <Alert>{error}</Alert>
        <div className="form-grid">
          <Field label="Title" name="title" required maxLength={200} value={form.title} onChange={text("title")} error={f.title} />
          <Field label="Author" name="author" required maxLength={120} value={form.author} onChange={text("author")} error={f.author} />
          <Field label="Book ID" name="bookId" required maxLength={30} hint="Your library's own code, e.g. B006"
            value={form.bookId} onChange={text("bookId")} error={f.bookId} />
          <Field label="ISBN" name="isbn" required maxLength={30} value={form.isbn} onChange={text("isbn")} error={f.isbn} />
          <Field label="Category" name="category" required maxLength={80} list="category-options"
            value={form.category} onChange={text("category")} error={f.category} />
          <Field label="Publisher" name="publisher" required maxLength={120} value={form.publisher} onChange={text("publisher")} error={f.publisher} />
          <Field label="Year published" name="yearPublished" type="number" required min={1000} max={2100}
            value={form.yearPublished} onChange={number("yearPublished")} error={f.yearPublished} />
          <Field label="Price" name="price" type="number" required min={0} step="0.01"
            value={form.price} onChange={number("price")} error={f.price} />
          <Field label="Number of copies" name="totalCopies" type="number" required min={1}
            value={form.totalCopies} onChange={number("totalCopies")} error={f.totalCopies} />
          <Field label="Barcode" name="barcode" maxLength={80} hint="Optional" value={form.barcode} onChange={text("barcode")} error={f.barcode} />
          <Field label="Format" name="contentType" required list="content-type-options" hint="e.g. Physical, PDF, Online"
            value={form.contentType} onChange={text("contentType")} error={f.contentType} />
          <Field label="License" name="license" required list="license-options" hint="Only add books you have the rights to"
            value={form.license} onChange={text("license")} error={f.license} />
          <Field label="Source link" name="sourceUrl" type="url" required placeholder="https://…"
            value={form.sourceUrl} onChange={text("sourceUrl")} error={f.sourceUrl} />
          <Field label="Cover image link" name="coverUrl" type="url" placeholder="https://… (optional)"
            value={form.coverUrl} onChange={text("coverUrl")} error={f.coverUrl} />
        </div>

        {/* Suggestions that appear while typing in Category, Format and License */}
        <datalist id="category-options">
          {CATEGORY_SUGGESTIONS.map((c) => <option key={c} value={c} />)}
        </datalist>
        <datalist id="content-type-options">
          {FORMAT_SUGGESTIONS.map((c) => <option key={c} value={c} />)}
        </datalist>
        <datalist id="license-options">
          {LICENSE_SUGGESTIONS.map((c) => <option key={c} value={c} />)}
        </datalist>

        <div className="modal-actions">
          <button type="button" className="btn btn-ghost" onClick={onClose}>
            Cancel
          </button>
          <button className="btn btn-primary" disabled={busy}>
            {busy ? "Saving…" : isNew ? "Add book" : "Save changes"}
          </button>
        </div>
      </form>
    </Modal>
  );
}
