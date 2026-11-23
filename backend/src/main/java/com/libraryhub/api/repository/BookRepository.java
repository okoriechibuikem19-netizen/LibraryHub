package com.libraryhub.api.repository;

import com.libraryhub.api.model.Availability;
import com.libraryhub.api.model.Book;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface BookRepository extends MongoRepository<Book,String> {
    Optional<Book> findByBookIdIgnoreCase(String bookId);
    Optional<Book> findByIsbn(String isbn);
    List<Book> findByCategoryIgnoreCase(String category);
    List<Book> findByAvailability(Availability availability);
    List<Book> findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCaseOrIsbnContainingIgnoreCase(
        String title, String author, String isbn);
}
