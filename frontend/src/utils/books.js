// Helpers for the online books (genres, numbers, and splitting a book into chapters).

/** Genres shown on the home page and the Browse page. "topic" is what Gutendex searches for. */
export const NOVEL_GENRES = [
  { label: "Romance", topic: "romance" },
  { label: "Adventure", topic: "adventure" },
  { label: "Mystery", topic: "detective" },
  { label: "Science Fiction", topic: "science fiction" },
  { label: "Fantasy", topic: "fantasy" },
  { label: "Horror", topic: "horror" },
  { label: "Historical", topic: "historical fiction" },
  { label: "Humour", topic: "humor" },
  { label: "Children", topic: "children" },
  { label: "Poetry", topic: "poetry" },
];

/** Subjects for the Textbooks page. All are free, public-domain books. */
export const TEXTBOOK_SUBJECTS = [
  { label: "Mathematics", topic: "mathematics" },
  { label: "Physics", topic: "physics" },
  { label: "Chemistry", topic: "chemistry" },
  { label: "Biology", topic: "biology" },
  { label: "Economics", topic: "economics" },
  { label: "Philosophy", topic: "philosophy" },
  { label: "History", topic: "history" },
  { label: "Psychology", topic: "psychology" },
  { label: "Language", topic: "grammar" },
];

/** 12345 -> "12.3K" */
export function compactNumber(value) {
  return new Intl.NumberFormat("en", { notation: "compact", maximumFractionDigits: 1 }).format(value ?? 0);
}

/** Removes Project Gutenberg's licence header and footer from a book's text. */
function stripGutenberg(text) {
  const start = text.search(/\*\*\*\s*START OF (THE|THIS) PROJECT GUTENBERG[^\n]*\n/i);
  const end = text.search(/\*\*\*\s*END OF (THE|THIS) PROJECT GUTENBERG/i);
  let body = text;
  if (start !== -1) body = body.slice(body.indexOf("\n", start) + 1);
  if (end !== -1) body = body.slice(0, body.search(/\*\*\*\s*END OF (THE|THIS) PROJECT GUTENBERG/i));
  return body.replace(/\r\n/g, "\n").trim();
}

const HEADING = /^[ \t]*((chapter|book|part|letter|stave|canto|act)\b[ .]*([ivxlcdm]+|\d+|[a-z]+)\b[^\n]{0,80})$/gim;

/**
 * Splits a book into chapters: [{ title, paragraphs: [] }].
 * Uses "CHAPTER I" style headings when the book has them, otherwise even-sized parts.
 */
export function splitIntoChapters(rawText) {
  const text = stripGutenberg(rawText);
  const marks = [...text.matchAll(HEADING)].filter((m) => {
    // A real heading has a blank line before it.
    const before = text.slice(Math.max(0, m.index - 2), m.index);
    return m.index === 0 || before.endsWith("\n\n") || before === "\n";
  });

  let sections = [];
  if (marks.length >= 3) {
    if (marks[0].index > 600) sections.push({ title: "Opening", body: text.slice(0, marks[0].index) });
    marks.forEach((m, i) => {
      const end = i + 1 < marks.length ? marks[i + 1].index : text.length;
      sections.push({ title: tidyTitle(m[1]), body: text.slice(m.index + m[0].length, end) });
    });
    // A table of contents creates many tiny "chapters": fold them into the next one.
    const merged = [];
    for (const s of sections) {
      if (merged.length && merged[merged.length - 1].body.trim().length < 400) merged[merged.length - 1] = { title: s.title, body: s.body };
      else merged.push(s);
    }
    sections = merged;
  }
  if (sections.length < 2) {
    const size = 3500; // words per part
    const paragraphs = toParagraphs(text);
    let current = [];
    let count = 0;
    sections = [];
    for (const p of paragraphs) {
      current.push(p);
      count += p.split(/\s+/).length;
      if (count >= size) {
        sections.push({ title: `Part ${sections.length + 1}`, paragraphs: current });
        current = [];
        count = 0;
      }
    }
    if (current.length) sections.push({ title: `Part ${sections.length + 1}`, paragraphs: current });
    if (!sections.length && text) sections.push({ title: "Part 1", paragraphs: [text] });
    return sections;
  }
  return sections.map((s) => ({ title: s.title, paragraphs: toParagraphs(s.body) }));
}

function toParagraphs(body) {
  return body
    .split(/\n\s*\n/)
    .map((p) => p.replace(/\s*\n\s*/g, " ").trim())
    .filter(Boolean);
}

/** "CHAPTER IV. THE STORM" -> "Chapter IV. The Storm" (Roman numerals stay in capitals). */
function tidyTitle(title) {
  const t = title.replace(/[ \t]+/g, " ").trim().replace(/[.:]$/, "");
  if (t !== t.toUpperCase()) return t;
  return t
    .split(" ")
    .map((w) => (/^[IVXLCDM]+[.:]?$/.test(w) ? w : w.charAt(0) + w.slice(1).toLowerCase()))
    .join(" ");
}
