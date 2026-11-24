import { useState } from "react";
import { Link } from "react-router";
import { BookOpen, Eye, GraduationCap, Sparkles } from "lucide-react";
import { useAuth } from "../../auth/AuthContext";
import { BookCover } from "../../components/site/BookCard";
import { BookRow } from "../../components/site/BookRow";
import { compactNumber, NOVEL_GENRES, TEXTBOOK_SUBJECTS } from "../../utils/books";

/** The public front page: featured book, genres, and rows of popular books. Anyone can browse. */
export function HomePage() {
  const { user } = useAuth();
  const [featured, setFeatured] = useState(null);

  return (
    <>
      <section className="hero">
        {featured ? (
          <div className="hero-inner">
            <div className="hero-backdrop" style={featured.coverUrl ? { backgroundImage: `url("${featured.coverUrl}")` } : undefined} />
            <div className="hero-cover">
              <BookCover book={featured} large />
            </div>
            <div className="hero-text">
              <span className="hero-tag"><Sparkles size={14} /> Most read this week</span>
              <h1>{featured.title}</h1>
              <p className="hero-author">by {featured.author}</p>
              {featured.summary && <p className="hero-summary">{featured.summary}</p>}
              <p className="hero-stats"><Eye size={15} /> {compactNumber(featured.downloads)} reads</p>
              <div className="hero-actions">
                <Link to={`/read/${featured.id}`} className="btn btn-primary"><BookOpen size={18} /> Read now</Link>
                <Link to={`/book/${featured.id}`} className="btn btn-ghost hero-ghost">Details</Link>
              </div>
            </div>
          </div>
        ) : (
          <div className="hero-inner hero-welcome">
            <div className="hero-text">
              <h1>Free novels and textbooks, in one library.</h1>
              <p className="hero-summary">Thousands of classic books you can read in your browser.</p>
            </div>
          </div>
        )}
      </section>

      {!user && (
        <div className="join-banner">
          <span>Browse freely. <strong>Create a free account</strong> to read, save books and pick up where you stopped.</span>
          <Link to="/register" className="btn btn-primary btn-sm">Join free</Link>
        </div>
      )}

      <section className="genre-strip" aria-label="Genres">
        {NOVEL_GENRES.map((g) => (
          <Link key={g.topic} to={`/browse?topic=${encodeURIComponent(g.topic)}`} className="genre-chip">{g.label}</Link>
        ))}
        <Link to="/textbooks" className="genre-chip genre-chip-alt"><GraduationCap size={15} /> Textbooks</Link>
      </section>

      <BookRow title="Trending now" seeAllTo="/browse" onLoaded={(books) => setFeatured(books[0] ?? null)} />
      <BookRow title="Romance" topic="romance" seeAllTo="/browse?topic=romance" />
      <BookRow title="Adventure" topic="adventure" seeAllTo="/browse?topic=adventure" />
      <BookRow title="Mystery & detective" topic="detective" seeAllTo="/browse?topic=detective" />
      <BookRow title="Science fiction" topic="science fiction" seeAllTo="/browse?topic=science%20fiction" />
      <BookRow title="Textbooks: Mathematics" topic={TEXTBOOK_SUBJECTS[0].topic} seeAllTo="/textbooks?topic=mathematics" />
      <BookRow title="Horror" topic="horror" seeAllTo="/browse?topic=horror" />
    </>
  );
}
