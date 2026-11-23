package com.libraryhub.api.catalog;

import com.libraryhub.api.model.ExternalNovel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A built-in shelf of well-known public-domain books from Project Gutenberg.
 * Used when the Gutendex service is slow or unreachable, so the site never looks empty.
 * Covers and texts come straight from gutenberg.org.
 */
public final class StarterCatalog {
    private StarterCatalog() {}

    private static final String[][] BOOKS = {
        // id, title, author, tags (comma separated, lower case)
        {"1342", "Pride and Prejudice", "Jane Austen", "romance,love stories,classics"},
        {"1260", "Jane Eyre", "Charlotte Brontë", "romance,love stories,classics"},
        {"768", "Wuthering Heights", "Emily Brontë", "romance,love stories,classics"},
        {"158", "Emma", "Jane Austen", "romance,humor,classics"},
        {"161", "Sense and Sensibility", "Jane Austen", "romance,classics"},
        {"105", "Persuasion", "Jane Austen", "romance,classics"},
        {"64317", "The Great Gatsby", "F. Scott Fitzgerald", "romance,classics"},
        {"514", "Little Women", "Louisa May Alcott", "romance,children,classics"},
        {"45", "Anne of Green Gables", "L. M. Montgomery", "children,humor,classics"},
        {"1399", "Anna Karenina", "Leo Tolstoy", "romance,classics"},
        {"120", "Treasure Island", "Robert Louis Stevenson", "adventure,children,pirates"},
        {"1184", "The Count of Monte Cristo", "Alexandre Dumas", "adventure,historical fiction"},
        {"1257", "The Three Musketeers", "Alexandre Dumas", "adventure,historical fiction"},
        {"2701", "Moby Dick", "Herman Melville", "adventure,sea stories,classics"},
        {"103", "Around the World in Eighty Days", "Jules Verne", "adventure,science fiction"},
        {"164", "Twenty Thousand Leagues under the Sea", "Jules Verne", "adventure,science fiction"},
        {"76", "Adventures of Huckleberry Finn", "Mark Twain", "adventure,humor,children"},
        {"74", "The Adventures of Tom Sawyer", "Mark Twain", "adventure,humor,children"},
        {"236", "The Jungle Book", "Rudyard Kipling", "adventure,children"},
        {"1661", "The Adventures of Sherlock Holmes", "Arthur Conan Doyle", "detective,mystery"},
        {"2852", "The Hound of the Baskervilles", "Arthur Conan Doyle", "detective,mystery"},
        {"244", "A Study in Scarlet", "Arthur Conan Doyle", "detective,mystery"},
        {"2097", "The Sign of the Four", "Arthur Conan Doyle", "detective,mystery"},
        {"834", "The Memoirs of Sherlock Holmes", "Arthur Conan Doyle", "detective,mystery"},
        {"108", "The Return of Sherlock Holmes", "Arthur Conan Doyle", "detective,mystery"},
        {"84", "Frankenstein", "Mary Shelley", "horror,science fiction,gothic"},
        {"345", "Dracula", "Bram Stoker", "horror,gothic"},
        {"43", "The Strange Case of Dr. Jekyll and Mr. Hyde", "Robert Louis Stevenson", "horror,gothic"},
        {"174", "The Picture of Dorian Gray", "Oscar Wilde", "horror,gothic,classics"},
        {"1952", "The Yellow Wallpaper", "Charlotte Perkins Gilman", "horror"},
        {"35", "The Time Machine", "H. G. Wells", "science fiction"},
        {"36", "The War of the Worlds", "H. G. Wells", "science fiction"},
        {"5230", "The Invisible Man", "H. G. Wells", "science fiction,horror"},
        {"1250", "Anthem", "Ayn Rand", "science fiction"},
        {"11", "Alice's Adventures in Wonderland", "Lewis Carroll", "fantasy,children"},
        {"16", "Peter Pan", "J. M. Barrie", "fantasy,children"},
        {"55", "The Wonderful Wizard of Oz", "L. Frank Baum", "fantasy,children"},
        {"2591", "Grimms' Fairy Tales", "Jacob and Wilhelm Grimm", "fantasy,children"},
        {"113", "The Secret Garden", "Frances Hodgson Burnett", "children"},
        {"98", "A Tale of Two Cities", "Charles Dickens", "historical fiction,classics"},
        {"135", "Les Misérables", "Victor Hugo", "historical fiction,classics"},
        {"2600", "War and Peace", "Leo Tolstoy", "historical fiction,classics"},
        {"2610", "Notre-Dame de Paris", "Victor Hugo", "historical fiction,classics"},
        {"1400", "Great Expectations", "Charles Dickens", "classics"},
        {"730", "Oliver Twist", "Charles Dickens", "classics"},
        {"46", "A Christmas Carol", "Charles Dickens", "classics,fantasy"},
        {"844", "The Importance of Being Earnest", "Oscar Wilde", "humor,drama"},
        {"2554", "Crime and Punishment", "Fyodor Dostoyevsky", "classics,psychology"},
        {"5200", "Metamorphosis", "Franz Kafka", "classics"},
        {"1727", "The Odyssey", "Homer", "poetry,adventure"},
        {"6130", "The Iliad", "Homer", "poetry,history"},
        {"8800", "The Divine Comedy", "Dante Alighieri", "poetry"},
        {"1322", "Leaves of Grass", "Walt Whitman", "poetry"},
        {"16328", "Beowulf", "Unknown", "poetry,adventure,fantasy"},
        {"1513", "Romeo and Juliet", "William Shakespeare", "romance,drama,poetry"},
        {"1524", "Hamlet", "William Shakespeare", "drama,poetry"},
        // Textbooks and non-fiction
        {"33283", "Calculus Made Easy", "Silvanus P. Thompson", "mathematics"},
        {"41568", "An Introduction to Mathematics", "Alfred North Whitehead", "mathematics"},
        {"201", "Flatland", "Edwin A. Abbott", "mathematics,science fiction"},
        {"5001", "Relativity: The Special and General Theory", "Albert Einstein", "physics,science"},
        {"33504", "Opticks", "Isaac Newton", "physics,science"},
        {"14474", "The Chemical History of a Candle", "Michael Faraday", "chemistry,science"},
        {"1228", "On the Origin of Species", "Charles Darwin", "biology,science"},
        {"3300", "The Wealth of Nations", "Adam Smith", "economics"},
        {"833", "The Theory of the Leisure Class", "Thorstein Veblen", "economics"},
        {"61", "The Communist Manifesto", "Karl Marx and Friedrich Engels", "economics,philosophy,history"},
        {"1497", "The Republic", "Plato", "philosophy"},
        {"2680", "Meditations", "Marcus Aurelius", "philosophy"},
        {"1232", "The Prince", "Niccolò Machiavelli", "philosophy,history"},
        {"4363", "Beyond Good and Evil", "Friedrich Nietzsche", "philosophy"},
        {"5827", "The Problems of Philosophy", "Bertrand Russell", "philosophy"},
        {"34901", "On Liberty", "John Stuart Mill", "philosophy"},
        {"3207", "Leviathan", "Thomas Hobbes", "philosophy,history"},
        {"132", "The Art of War", "Sun Tzu", "philosophy,history"},
        {"205", "Walden", "Henry David Thoreau", "philosophy"},
        {"35461", "A Short History of the World", "H. G. Wells", "history"},
        {"7142", "History of the Peloponnesian War", "Thucydides", "history"},
        {"1404", "The Federalist Papers", "Hamilton, Madison and Jay", "history"},
        {"147", "Common Sense", "Thomas Paine", "history"},
        {"23", "Narrative of the Life of Frederick Douglass", "Frederick Douglass", "history"},
        {"408", "The Souls of Black Folk", "W. E. B. Du Bois", "history"},
        {"15399", "The Interesting Narrative of the Life of Olaudah Equiano", "Olaudah Equiano", "history"},
        {"621", "The Varieties of Religious Experience", "William James", "psychology,philosophy"},
        {"37134", "The Elements of Style", "William Strunk Jr.", "grammar,language"},
    };

    private static final List<ExternalNovel> ALL = build();

    private static List<ExternalNovel> build() {
        List<ExternalNovel> list = new ArrayList<>();
        for (String[] b : BOOKS) {
            ExternalNovel v = new ExternalNovel();
            v.id = "gutenberg-" + b[0];
            v.title = b[1];
            v.author = b[2];
            v.coverUrl = "https://www.gutenberg.org/cache/epub/" + b[0] + "/pg" + b[0] + ".cover.medium.jpg";
            v.sourceUrl = "https://www.gutenberg.org/ebooks/" + b[0];
            v.textUrl = "https://www.gutenberg.org/cache/epub/" + b[0] + "/pg" + b[0] + ".txt";
            v.language = "en";
            v.downloads = 0L;
            v.summary = "";
            for (String tag : b[3].split(",")) {
                String t = tag.trim();
                v.genres.add(Character.toUpperCase(t.charAt(0)) + t.substring(1));
            }
            list.add(v);
        }
        return List.copyOf(list);
    }

    public static List<ExternalNovel> all() { return ALL; }

    /** Books whose title, author or tags contain the words. Blank search returns everything. */
    public static List<ExternalNovel> search(String q, String topic) {
        String term = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        String subject = topic == null ? "" : topic.trim().toLowerCase(Locale.ROOT);
        return ALL.stream().filter(v -> {
            String tags = String.join(",", v.genres).toLowerCase(Locale.ROOT);
            boolean topicOk = subject.isEmpty() || tags.contains(subject);
            boolean termOk = term.isEmpty() || v.title.toLowerCase(Locale.ROOT).contains(term)
                    || v.author.toLowerCase(Locale.ROOT).contains(term) || tags.contains(term);
            return topicOk && termOk;
        }).toList();
    }

    public static ExternalNovel find(String id) {
        return ALL.stream().filter(v -> v.id.equals(id)).findFirst().orElse(null);
    }
}
