import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { Link, useParams } from "react-router";
import { ArrowLeft, ChevronLeft, ChevronRight, List, Minus, Plus, Type, X } from "lucide-react";
import { errorMessage } from "../../api/client";
import { catalogApi, readerApi } from "../../api/endpoints";
import { Alert, Spinner } from "../../components/ui";
import { splitIntoChapters } from "../../utils/books";

const THEMES = [
  { id: "light", label: "Light" },
  { id: "sepia", label: "Sepia" },
  { id: "dark", label: "Dark" },
];
const SETTINGS_KEY = "libraryhub-reader-settings";

function loadSettings() {
  try {
    return { theme: "light", size: 19, font: "serif", ...JSON.parse(localStorage.getItem(SETTINGS_KEY) ?? "{}") };
  } catch {
    return { theme: "light", size: 19, font: "serif" };
  }
}

/**
 * Full-screen reader: one chapter at a time, chapter list, light/sepia/dark, text size.
 * Your place (chapter + % read) is saved to your account, so you can continue on any device.
 */
export function ReaderPage() {
  const { id } = useParams();
  const [book, setBook] = useState(null);
  const [chapters, setChapters] = useState(null);
  const [chapter, setChapter] = useState(0);
  const [error, setError] = useState("");
  const [showContents, setShowContents] = useState(false);
  const [showSettings, setShowSettings] = useState(false);
  const [settings, setSettings] = useState(loadSettings);
  const stateRef = useRef(null); // reader state from the server
  const saveTimer = useRef(null);
  const topRef = useRef(null);

  // Load the book details, its text and where the user stopped last time.
  useEffect(() => {
    const controller = new AbortController();
    Promise.all([
      catalogApi.get(id, controller.signal),
      catalogApi.text(id, controller.signal),
      readerApi.state().catch(() => null),
    ])
      .then(([details, text, state]) => {
        setBook(details);
        const parts = splitIntoChapters(String(text ?? ""));
        setChapters(parts);
        stateRef.current = state;
        const saved = Number(state?.bookmarks?.[id] ?? 0);
        setChapter(Number.isFinite(saved) ? Math.min(Math.max(saved, 0), parts.length - 1) : 0);
      })
      .catch((err) => err.name !== "AbortError" && setError(errorMessage(err)));
    return () => controller.abort();
  }, [id]);

  // Remember reader settings on this device.
  useEffect(() => {
    try {
      localStorage.setItem(SETTINGS_KEY, JSON.stringify(settings));
    } catch {
      // Private browsing can block storage; settings then last for this visit only.
    }
  }, [settings]);

  // Save progress to the account a moment after the chapter changes.
  const saveProgress = useCallback(
    (index) => {
      if (!chapters || !stateRef.current) return;
      clearTimeout(saveTimer.current);
      saveTimer.current = setTimeout(async () => {
        const current = stateRef.current;
        const percent = Math.round(((index + 1) / chapters.length) * 100);
        const next = {
          ...current,
          bookmarks: { ...(current.bookmarks ?? {}), [id]: String(index) },
          progress: { ...(current.progress ?? {}), [id]: percent },
          shelves: { ...(current.shelves ?? {}), [id]: current.shelves?.[id] === "saved" ? "saved" : "reading" },
        };
        try {
          stateRef.current = await readerApi.save(next);
        } catch {
          // Not critical: the next chapter change tries again.
        }
      }, 800);
    },
    [chapters, id],
  );

  useEffect(() => {
    if (chapters) saveProgress(chapter);
    topRef.current?.scrollIntoView({ block: "start" });
  }, [chapter, chapters, saveProgress]);

  useEffect(() => () => clearTimeout(saveTimer.current), []);

  // Left/right arrow keys change chapter.
  useEffect(() => {
    const onKey = (e) => {
      if (!chapters || e.target.closest?.("input, select, textarea")) return;
      if (e.key === "ArrowRight") setChapter((c) => Math.min(c + 1, chapters.length - 1));
      if (e.key === "ArrowLeft") setChapter((c) => Math.max(c - 1, 0));
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [chapters]);

  const current = chapters?.[chapter];
  const percent = useMemo(() => (chapters ? Math.round(((chapter + 1) / chapters.length) * 100) : 0), [chapter, chapters]);
  const update = (patch) => setSettings((s) => ({ ...s, ...patch }));

  return (
    <div className={`reader reader-${settings.theme}`}>
      <header className="reader-bar">
        <Link to={book ? `/book/${id}` : "/"} className="icon-btn" aria-label="Back to book page">
          <ArrowLeft size={20} />
        </Link>
        <div className="reader-title">
          <strong>{book?.title ?? "Loading…"}</strong>
          {current && <span>{current.title}</span>}
        </div>
        <button className="icon-btn" onClick={() => { setShowContents((v) => !v); setShowSettings(false); }} aria-label="Chapters">
          <List size={20} />
        </button>
        <button className="icon-btn" onClick={() => { setShowSettings((v) => !v); setShowContents(false); }} aria-label="Reading settings">
          <Type size={20} />
        </button>
        <div className="reader-progress" aria-label={`${percent}% of the book`}><span style={{ width: `${percent}%` }} /></div>
      </header>

      {showSettings && (
        <div className="reader-panel reader-settings">
          <div className="reader-panel-head">
            <strong>Reading settings</strong>
            <button className="icon-btn" onClick={() => setShowSettings(false)} aria-label="Close settings"><X size={18} /></button>
          </div>
          <div className="setting-row">
            <span>Theme</span>
            <div className="segmented">
              {THEMES.map((t) => (
                <button key={t.id} className={settings.theme === t.id ? "active" : ""} onClick={() => update({ theme: t.id })}>{t.label}</button>
              ))}
            </div>
          </div>
          <div className="setting-row">
            <span>Text size</span>
            <div className="segmented">
              <button onClick={() => update({ size: Math.max(14, settings.size - 1) })} aria-label="Smaller text"><Minus size={16} /></button>
              <span className="size-value">{settings.size}</span>
              <button onClick={() => update({ size: Math.min(30, settings.size + 1) })} aria-label="Bigger text"><Plus size={16} /></button>
            </div>
          </div>
          <div className="setting-row">
            <span>Font</span>
            <div className="segmented">
              <button className={settings.font === "serif" ? "active" : ""} onClick={() => update({ font: "serif" })}>Serif</button>
              <button className={settings.font === "sans" ? "active" : ""} onClick={() => update({ font: "sans" })}>Sans</button>
            </div>
          </div>
        </div>
      )}

      {showContents && chapters && (
        <nav className="reader-panel reader-contents" aria-label="Chapters">
          <div className="reader-panel-head">
            <strong>Chapters ({chapters.length})</strong>
            <button className="icon-btn" onClick={() => setShowContents(false)} aria-label="Close chapters"><X size={18} /></button>
          </div>
          <ol>
            {chapters.map((c, i) => (
              <li key={i}>
                <button className={i === chapter ? "active" : ""} onClick={() => { setChapter(i); setShowContents(false); }}>
                  {c.title}
                </button>
              </li>
            ))}
          </ol>
        </nav>
      )}

      <main className="reader-body" ref={topRef}>
        {error ? (
          <div className="reader-message"><Alert>{error}</Alert><Link to="/" className="btn btn-ghost">Back to the library</Link></div>
        ) : !current ? (
          <div className="reader-message"><Spinner label="Opening the book… big books can take a few seconds." /></div>
        ) : (
          <article
            className={`reader-text font-${settings.font}`}
            style={{ fontSize: `${settings.size}px` }}
          >
            <h1>{current.title}</h1>
            {current.paragraphs.map((p, i) => <p key={i}>{p}</p>)}
          </article>
        )}
        {current && (
          <div className="reader-nav">
            <button className="btn btn-ghost" disabled={chapter === 0} onClick={() => setChapter(chapter - 1)}>
              <ChevronLeft size={18} /> Previous
            </button>
            <span className="muted">{chapter + 1} / {chapters.length}</span>
            <button className="btn btn-primary" disabled={chapter === chapters.length - 1} onClick={() => setChapter(chapter + 1)}>
              Next <ChevronRight size={18} />
            </button>
          </div>
        )}
      </main>
    </div>
  );
}
