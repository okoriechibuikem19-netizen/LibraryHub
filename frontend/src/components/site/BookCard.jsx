import { useState } from "react";
import { Link } from "react-router";
import { Eye } from "lucide-react";
import { compactNumber } from "../../utils/books";

/** A book cover with title, author and how many people read it. Links to the book's page. */
export function BookCard({ book, progress }) {
  return (
    <Link to={`/book/${book.id}`} className="book-card">
      <BookCover book={book} />
      {progress != null && (
        <div className="card-progress" aria-label={`${progress}% read`}>
          <span style={{ width: `${progress}%` }} />
        </div>
      )}
      <strong className="book-card-title">{book.title}</strong>
      <span className="book-card-author">{book.author}</span>
      <span className="book-card-reads">
        {book.downloads > 0 ? <><Eye size={13} aria-hidden="true" /> {compactNumber(book.downloads)} reads</> : (book.source && book.source !== book.author ? book.source : "Free")}
      </span>
    </Link>
  );
}

/** The cover image, or a coloured placeholder with the title when there is none. */
export function BookCover({ book, large = false }) {
  // If the picture can't load (blocked or missing), show the coloured placeholder instead of a broken image.
  const [failedUrl, setFailedUrl] = useState(null);
  if (book.coverUrl && failedUrl !== book.coverUrl) {
    return (
      <img src={book.coverUrl} alt={`Cover of ${book.title}`} className={`cover-img${large ? " cover-large" : ""}${book.source === "OpenStax" ? " cover-contain" : ""}`}
        loading="lazy" onError={() => setFailedUrl(book.coverUrl)} />
    );
  }
  // Pick a colour from the title so each book keeps the same one.
  const hue = [...book.title].reduce((h, c) => (h * 31 + c.charCodeAt(0)) % 360, 7);
  return (
    <div className={`cover-img cover-fallback${large ? " cover-large" : ""}`} style={{ "--hue": hue }} aria-hidden="true">
      <span>{book.title}</span>
    </div>
  );
}

/** Grey boxes shown while books load. */
export function BookCardSkeleton() {
  return (
    <div className="book-card skeleton-card" aria-hidden="true">
      <div className="cover-img skeleton" />
      <div className="skeleton skeleton-line" />
      <div className="skeleton skeleton-line short" />
    </div>
  );
}
