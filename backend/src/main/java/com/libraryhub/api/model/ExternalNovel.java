package com.libraryhub.api.model;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.*;

/** A free book from Project Gutenberg, Open Library or OpenStax. */
@Document("external_novels")
public class ExternalNovel {
 @Id public String id;
 public String title;
 public String author;
 public String coverUrl;
 public String sourceUrl;
 public String textUrl;
 public String language;
 public List<String> genres = new ArrayList<>();
 public long downloads;
 /** Short description from Gutendex, when available. */
 public String summary = "";
 /** "Project Gutenberg", "Open Library" or "OpenStax". */
 public String source = "Project Gutenberg";
 /** Where to read or borrow it on the source's own site (used when we can't show the text here). */
 public String readUrl;
 /** Free PDF download, when the source offers one (OpenStax). */
 public String pdfUrl;
 /** A second copy of the text (archive.org), used if the main one can't be reached. */
 public String backupTextUrl;
}
