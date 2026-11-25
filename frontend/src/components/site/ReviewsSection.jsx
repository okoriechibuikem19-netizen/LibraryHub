import { useEffect, useState } from "react";
import { Link, useLocation } from "react-router";
import { Flag, Star } from "lucide-react";
import { errorMessage } from "../../api/client";
import { reviewsApi } from "../../api/endpoints";
import { useAuth } from "../../auth/AuthContext";
import { formatDate } from "../../utils/format";
import { Alert } from "../ui";

/** Five stars, filled up to `value`. With `onPick`, the stars are buttons. */
export function Stars({ value, size = 16, onPick }) {
  return (
    <span className={`stars${onPick ? " stars-pick" : ""}`} aria-label={`${value} out of 5 stars`}>
      {[1, 2, 3, 4, 5].map((n) => {
        const icon = <Star size={size} className={n <= Math.round(value) ? "star-on" : "star-off"} aria-hidden="true" />;
        return onPick ? (
          <button key={n} type="button" onClick={() => onPick(n)} aria-label={`${n} star${n > 1 ? "s" : ""}`}>{icon}</button>
        ) : <span key={n}>{icon}</span>;
      })}
    </span>
  );
}

/** Ratings and reviews for one book: summary, my review form, and everyone's reviews. */
export function ReviewsSection({ bookId }) {
  const { user } = useAuth();
  const location = useLocation();
  const [data, setData] = useState(null);
  const [rating, setRating] = useState(0);
  const [comment, setComment] = useState("");
  const [editing, setEditing] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  async function load(signal) {
    try {
      const result = await reviewsApi.list(bookId, signal);
      setData(result);
      const mine = result.reviews.find((r) => r.mine);
      setRating(mine?.rating ?? 0);
      setComment(mine?.comment ?? "");
      setEditing(!mine);
    } catch (err) {
      if (err.name !== "AbortError") setError(errorMessage(err));
    }
  }

  useEffect(() => {
    const controller = new AbortController();
    setData(null);
    load(controller.signal);
    return () => controller.abort();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [bookId, user?.email]);

  async function submit(event) {
    event.preventDefault();
    if (!rating) return setError("Pick from 1 to 5 stars.");
    setBusy(true);
    setError("");
    try {
      await reviewsApi.save(bookId, rating, comment.trim());
      await load();
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  async function removeMine() {
    setBusy(true);
    try {
      await reviewsApi.remove(bookId);
      await load();
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  async function report(id) {
    try {
      await reviewsApi.report(bookId, id);
      await load();
    } catch (err) {
      setError(errorMessage(err));
    }
  }

  if (!data) return null;
  const mine = data.reviews.find((r) => r.mine);
  const others = data.reviews.filter((r) => !r.mine);

  return (
    <section className="reviews" aria-labelledby="reviews-title">
      <h2 id="reviews-title">Ratings &amp; reviews</h2>

      <div className="reviews-summary">
        <div className="reviews-average">
          <strong>{data.count ? data.average.toFixed(1) : "–"}</strong>
          <Stars value={data.average} size={18} />
          <span className="muted small">{data.count} {data.count === 1 ? "rating" : "ratings"}</span>
        </div>
        <div className="reviews-bars">
          {[5, 4, 3, 2, 1].map((n) => (
            <div key={n} className="reviews-bar">
              <span>{n}</span>
              <div><span style={{ width: `${data.count ? (data.stars[n - 1] / data.count) * 100 : 0}%` }} /></div>
            </div>
          ))}
        </div>
      </div>

      <Alert>{error}</Alert>

      {!user ? (
        <p className="muted">
          <Link to="/login" state={{ from: location.pathname }}>Sign in</Link> to rate this book.
        </p>
      ) : editing ? (
        <form className="review-form" onSubmit={submit}>
          <label className="review-form-label">{mine ? "Change your rating" : "Rate this book"}</label>
          <Stars value={rating} size={28} onPick={setRating} />
          <textarea
            rows={3}
            maxLength={1000}
            placeholder="What did you think? (optional)"
            value={comment}
            onChange={(e) => setComment(e.target.value)}
          />
          <div className="review-form-actions">
            <button className="btn btn-primary" disabled={busy}>{mine ? "Save changes" : "Post review"}</button>
            {mine && <button type="button" className="btn btn-ghost" onClick={() => setEditing(false)}>Cancel</button>}
          </div>
        </form>
      ) : (
        mine && (
          <div className="review review-mine">
            <div className="review-head">
              <strong>Your review</strong>
              <Stars value={mine.rating} />
            </div>
            {mine.comment && <p>{mine.comment}</p>}
            <div className="review-actions">
              <button className="link-btn" onClick={() => setEditing(true)}>Edit</button>
              <button className="link-btn" onClick={removeMine} disabled={busy}>Delete</button>
            </div>
          </div>
        )
      )}

      {others.length === 0 && !mine ? (
        <p className="muted">No reviews yet. Be the first to share what you think.</p>
      ) : (
        <ul className="review-list">
          {others.map((r) => (
            <li key={r.id} className="review">
              <div className="review-head">
                <strong>{r.name}</strong>
                <Stars value={r.rating} />
                <span className="muted small">{formatDate(r.createdAt)}</span>
              </div>
              {r.comment ? <p>{r.comment}</p> : null}
              {user && r.comment && (
                <button className="link-btn small" onClick={() => report(r.id)} title="Report this review">
                  <Flag size={12} /> Report
                </button>
              )}
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
