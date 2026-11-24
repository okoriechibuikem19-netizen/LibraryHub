import { useEffect, useState } from "react";
import { useSearchParams } from "react-router";
import { errorMessage } from "../../api/client";
import { catalogApi } from "../../api/endpoints";
import { BookCard, BookCardSkeleton } from "../../components/site/BookCard";
import { Alert, EmptyState } from "../../components/ui";
import { NOVEL_GENRES, TEXTBOOK_SUBJECTS } from "../../utils/books";

/**
 * A grid of books with genre (or subject) filters and "Load more".
 * mode="novels" for /browse, mode="textbooks" for /textbooks.
 * The filters live in the address (?topic=romance&q=...), so links and the back button work.
 */
export function BrowsePage({ mode = "novels" }) {
  const [params, setParams] = useSearchParams();
  const topics = mode === "textbooks" ? TEXTBOOK_SUBJECTS : NOVEL_GENRES;
  const q = params.get("q") ?? "";
  const topic = params.get("topic") ?? (mode === "textbooks" ? topics[0].topic : "");

  const [books, setBooks] = useState(null);
  const [page, setPage] = useState(1);
  const [hasMore, setHasMore] = useState(false);
  const [loadingMore, setLoadingMore] = useState(false);
  const [error, setError] = useState("");

  // First page whenever the search or genre changes.
  useEffect(() => {
    const controller = new AbortController();
    setBooks(null);
    setError("");
    setPage(1);
    catalogApi
      .list({ q, topic }, controller.signal)
      .then((result) => {
        setBooks(result);
        setHasMore(result.length >= 30);
      })
      .catch((err) => {
        if (err.name === "AbortError") return;
        setError(errorMessage(err));
        setBooks([]);
      });
    return () => controller.abort();
  }, [q, topic]);

  async function loadMore() {
    setLoadingMore(true);
    try {
      const next = await catalogApi.list({ q, topic, page: page + 1 });
      setBooks((current) => {
        const seen = new Set(current.map((b) => b.id));
        return [...current, ...next.filter((b) => !seen.has(b.id))];
      });
      setPage(page + 1);
      setHasMore(next.length >= 30);
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setLoadingMore(false);
    }
  }

  function chooseTopic(value) {
    const next = new URLSearchParams();
    if (value) next.set("topic", value);
    setParams(next);
  }

  const activeLabel = topics.find((t) => t.topic === topic)?.label;
  const heading = q ? `Results for "${q}"` : activeLabel ?? (mode === "textbooks" ? "Textbooks" : "All novels");

  return (
    <div className="page-container">
      <div className="browse-header">
        <h1>{heading}</h1>
        <p className="muted">
          {mode === "textbooks"
            ? "Free public-domain textbooks and classic study books."
            : "Free classic novels. Sign in to read any of them online."}
        </p>
      </div>

      <div className="genre-strip browse-genres" role="tablist" aria-label={mode === "textbooks" ? "Subjects" : "Genres"}>
        {mode === "novels" && (
          <button className={`genre-chip${!topic && !q ? " active" : ""}`} onClick={() => chooseTopic("")}>All</button>
        )}
        {topics.map((t) => (
          <button
            key={t.topic}
            className={`genre-chip${topic === t.topic && !q ? " active" : ""}`}
            onClick={() => chooseTopic(t.topic)}
          >
            {t.label}
          </button>
        ))}
      </div>

      <Alert>{error}</Alert>

      {books === null ? (
        <div className="book-grid">{Array.from({ length: 12 }, (_, i) => <BookCardSkeleton key={i} />)}</div>
      ) : books.length === 0 ? (
        <EmptyState title="No books found">Try another search or genre.</EmptyState>
      ) : (
        <>
          <div className="book-grid">
            {books.map((book) => <BookCard key={book.id} book={book} />)}
          </div>
          {hasMore && (
            <div className="load-more">
              <button className="btn btn-ghost" onClick={loadMore} disabled={loadingMore}>
                {loadingMore ? "Loading…" : "Load more books"}
              </button>
            </div>
          )}
        </>
      )}
    </div>
  );
}
