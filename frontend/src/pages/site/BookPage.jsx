import { useEffect, useState } from "react";
import { Link, useLocation, useNavigate, useParams } from "react-router";
import { BookmarkCheck, BookmarkPlus, BookOpen, Download, ExternalLink, Eye, Lock } from "lucide-react";
import { errorMessage } from "../../api/client";
import { catalogApi, readerApi } from "../../api/endpoints";
import { useAuth } from "../../auth/AuthContext";
import { BookCover } from "../../components/site/BookCard";
import { BookRow } from "../../components/site/BookRow";
import { ReviewsSection } from "../../components/site/ReviewsSection";
import { Alert, Spinner } from "../../components/ui";
import { compactNumber } from "../../utils/books";

/** One book: cover, description, genres, and Read / Save buttons. Public; reading needs an account. */
export function BookPage() {
  const { id } = useParams();
  const { user } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [book, setBook] = useState(null);
  const [state, setState] = useState(null); // the user's reading state (only when signed in)
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    const controller = new AbortController();
    setBook(null);
    setError("");
    catalogApi.get(id, controller.signal).then(setBook).catch((err) => err.name !== "AbortError" && setError(errorMessage(err)));
    return () => controller.abort();
  }, [id]);

  useEffect(() => {
    if (user) readerApi.state().then(setState).catch(() => setState(null));
    else setState(null);
  }, [user]);

  const goToLogin = () => navigate("/login", { state: { from: location.pathname } });

  async function toggleSaved() {
    if (!user) return goToLogin();
    setSaving(true);
    try {
      const shelves = { ...(state?.shelves ?? {}) };
      if (shelves[id]) delete shelves[id];
      else shelves[id] = "saved";
      setState(await readerApi.save({ ...state, shelves }));
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setSaving(false);
    }
  }

  if (error && !book) return <div className="page-container"><Alert>{error}</Alert></div>;
  if (!book) return <div className="page-container"><Spinner label="Loading book…" /></div>;

  const progress = state?.progress?.[id];
  const isSaved = !!state?.shelves?.[id];
  const readsHere = !!book.textUrl; // otherwise it is read (or borrowed) on the source's own site
  const source = book.source || "Project Gutenberg";
  const outsideLabel = source === "Open Library" ? "Borrow free on Open Library" : `Read free on ${source}`;

  return (
    <div className="page-container">
      <Alert>{error}</Alert>
      <article className="book-detail">
        <div className="book-detail-cover">
          <BookCover book={book} large />
        </div>
        <div className="book-detail-info">
          <h1>{book.title}</h1>
          <p className="book-detail-author">by {book.author}</p>
          <p className="book-detail-stats">
            {book.downloads > 0 && <><Eye size={16} /> {compactNumber(book.downloads)} reads</>}
            {progress != null && <span className="badge badge-success">{progress}% read</span>}
          </p>
          {book.genres?.length > 0 && (
            <div className="tag-list">
              {book.genres.map((g) => <span key={g} className="tag">{g}</span>)}
            </div>
          )}
          {book.summary ? <p className="book-detail-summary">{book.summary}</p> : (
            <p className="book-detail-summary muted">No description yet for this book.</p>
          )}

          <div className="book-detail-actions">
            {!readsHere && book.readUrl ? (
              <a href={book.readUrl} target="_blank" rel="noreferrer" className="btn btn-primary btn-lg">
                <BookOpen size={18} /> {outsideLabel} <ExternalLink size={14} />
              </a>
            ) : user ? (
              <Link to={`/read/${book.id}`} className="btn btn-primary btn-lg">
                <BookOpen size={18} /> {progress ? "Continue reading" : "Start reading"}
              </Link>
            ) : (
              <button className="btn btn-primary btn-lg" onClick={goToLogin}>
                <Lock size={18} /> Sign in to read
              </button>
            )}
            {book.pdfUrl && (
              <a href={book.pdfUrl} target="_blank" rel="noreferrer" className="btn btn-ghost btn-lg">
                <Download size={18} /> Free PDF
              </a>
            )}
            <button className="btn btn-ghost btn-lg" onClick={toggleSaved} disabled={saving}>
              {isSaved ? <><BookmarkCheck size={18} /> Saved to library</> : <><BookmarkPlus size={18} /> Save to library</>}
            </button>
          </div>
          {!readsHere && book.readUrl && (
            <p className="muted small">
              This book opens on {source}'s website{source === "Open Library" ? ", where you can borrow it free with an Open Library account" : ""}.
            </p>
          )}
          {!user && readsHere && <p className="muted small">Reading is free. You just need an account so we can remember your page.</p>}
          <p className="small muted">
            Source:{" "}
            <a href={book.sourceUrl} target="_blank" rel="noreferrer">
              {source} <ExternalLink size={12} />
            </a>{" "}
            · {source === "OpenStax" ? "Open licence (CC BY)" : readsHere ? "Public domain" : "Free library loan"}
          </p>
        </div>
      </article>

      <ReviewsSection bookId={book.id} />

      {book.genres?.[0] && <BookRow title="You may also like" topic={book.genres[0].toLowerCase()} excludeId={book.id} />}
    </div>
  );
}
