import { useEffect, useState } from "react";
import { Link } from "react-router";
import { errorMessage } from "../../api/client";
import { catalogApi, readerApi } from "../../api/endpoints";
import { useAuth } from "../../auth/AuthContext";
import { BookCard, BookCardSkeleton } from "../../components/site/BookCard";
import { Alert, EmptyState } from "../../components/ui";

/** The signed-in user's books: what they're reading (with progress) and what they saved. */
export function LibraryPage() {
  const { user } = useAuth();
  const [state, setState] = useState(null);
  const [books, setBooks] = useState({}); // id -> book details
  const [error, setError] = useState("");

  useEffect(() => {
    readerApi
      .state()
      .then(async (s) => {
        setState(s);
        const ids = Object.keys(s.shelves ?? {});
        const details = await Promise.allSettled(ids.map((id) => catalogApi.get(id)));
        const map = {};
        details.forEach((r, i) => r.status === "fulfilled" && (map[ids[i]] = r.value));
        setBooks(map);
      })
      .catch((err) => setError(errorMessage(err)));
  }, []);

  const shelves = state?.shelves ?? {};
  const reading = Object.keys(shelves).filter((id) => shelves[id] === "reading" || state?.progress?.[id]);
  const saved = Object.keys(shelves).filter((id) => shelves[id] === "saved" && !state?.progress?.[id]);

  const renderShelf = (ids, empty) =>
    state === null ? (
      <div className="book-grid">{Array.from({ length: 4 }, (_, i) => <BookCardSkeleton key={i} />)}</div>
    ) : ids.length === 0 ? (
      empty
    ) : (
      <div className="book-grid">
        {ids.map((id) => books[id] ? (
          <BookCard key={id} book={books[id]} progress={state.progress?.[id] ?? null} />
        ) : (
          <BookCardSkeleton key={id} />
        ))}
      </div>
    );

  return (
    <div className="page-container">
      <div className="browse-header">
        <h1>My Library</h1>
        <p className="muted">Welcome back, {user?.name.split(" ")[0]}. Your books are saved to your account.</p>
      </div>
      <Alert>{error}</Alert>

      <section className="library-section">
        <h2>Continue reading</h2>
        {renderShelf(
          reading,
          <EmptyState title="You haven't started a book yet">
            <Link to="/">Find something to read</Link>
          </EmptyState>,
        )}
      </section>

      <section className="library-section">
        <h2>Saved for later</h2>
        {renderShelf(saved, <EmptyState title="Nothing saved yet">Tap “Save to library” on any book.</EmptyState>)}
      </section>
    </div>
  );
}
