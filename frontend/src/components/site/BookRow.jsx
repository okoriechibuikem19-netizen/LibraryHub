import { useEffect, useState } from "react";
import { Link } from "react-router";
import { ChevronRight } from "lucide-react";
import { catalogApi } from "../../api/endpoints";
import { BookCard, BookCardSkeleton } from "./BookCard";

/** A titled, sideways-scrolling row of books for one genre or subject. */
export function BookRow({ title, topic = "", query = "", seeAllTo, onLoaded, excludeId }) {
  const [books, setBooks] = useState(null);
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    const controller = new AbortController();
    catalogApi
      .list({ topic, q: query }, controller.signal)
      .then((all) => {
        const result = excludeId ? all.filter((b) => b.id !== excludeId) : all;
        setBooks(result);
        onLoaded?.(result);
      })
      .catch((err) => err.name !== "AbortError" && setFailed(true));
    return () => controller.abort();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [topic, query]);

  if (failed || (books && books.length === 0)) return null; // hide a row that has nothing to show

  return (
    <section className="book-row">
      <div className="book-row-header">
        <h2>{title}</h2>
        {seeAllTo && (
          <Link to={seeAllTo} className="see-all">
            See all <ChevronRight size={16} />
          </Link>
        )}
      </div>
      <div className="book-row-scroll">
        {books
          ? books.slice(0, 14).map((book) => <BookCard key={book.id} book={book} />)
          : Array.from({ length: 7 }, (_, i) => <BookCardSkeleton key={i} />)}
      </div>
    </section>
  );
}
