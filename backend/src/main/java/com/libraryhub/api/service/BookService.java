package com.libraryhub.api.service;

import com.libraryhub.api.dto.BookRequest;
import com.libraryhub.api.dto.BookResponse;
import com.libraryhub.api.exception.DuplicateResourceException;
import com.libraryhub.api.exception.ResourceNotFoundException;
import com.libraryhub.api.model.Availability;
import com.libraryhub.api.model.Book;
import com.libraryhub.api.repository.BookRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class BookService {
    private final BookRepository repository;
    public BookService(BookRepository repository) { this.repository = repository; }

    public List<BookResponse> getAll(String search, String category, Availability availability) {
        List<Book> books;
        if (search != null && !search.isBlank()) {
            books = repository.findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCaseOrIsbnContainingIgnoreCase(search,search,search);
        } else if (category != null && !category.isBlank()) {
            books = repository.findByCategoryIgnoreCase(category);
        } else if (availability != null) {
            books = repository.findByAvailability(availability);
        } else {
            books = repository.findAll(Sort.by(Sort.Direction.ASC,"title"));
        }
        return books.stream()
            .filter(book -> category == null || category.isBlank() || book.getCategory().equalsIgnoreCase(category))
            .filter(book -> availability == null || book.getAvailability() == availability)
            .map(this::toResponse).toList();
    }

    public BookResponse getById(String id) { return toResponse(find(id)); }

    public BookResponse create(BookRequest request) {
        ensureUnique(request.bookId(),request.isbn(),null);
        return toResponse(repository.save(toEntity(new Book(),request)));
    }

    public BookResponse update(String id, BookRequest request) {
        Book existing=find(id);
        ensureUnique(request.bookId(),request.isbn(),id);
        return toResponse(repository.save(toEntity(existing,request)));
    }

    public void delete(String id) {
        Book book=find(id);
        repository.delete(book);
    }

    private Book find(String id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Book not found with id: "+id));
    }
    private void ensureUnique(String bookId,String isbn,String currentId) {
        repository.findByBookIdIgnoreCase(bookId).filter(b -> !b.getId().equals(currentId))
            .ifPresent(b -> { throw new DuplicateResourceException("Book ID already exists: "+bookId); });
        repository.findByIsbn(isbn).filter(b -> !b.getId().equals(currentId))
            .ifPresent(b -> { throw new DuplicateResourceException("ISBN already exists: "+isbn); });
    }
    private Book toEntity(Book book,BookRequest r) {
        book.setBookId(r.bookId().trim()); book.setTitle(r.title().trim()); book.setAuthor(r.author().trim());
        book.setCategory(r.category().trim()); book.setIsbn(r.isbn().trim()); book.setPublisher(r.publisher().trim());
        book.setYearPublished(r.yearPublished()); book.setPrice(r.price()); book.setAvailability(r.availability());
        book.setContentType(r.contentType()); book.setSourceUrl(r.sourceUrl()); book.setLicense(r.license());
        book.setCoverUrl(r.coverUrl());book.setBarcode(r.barcode()==null||r.barcode().isBlank()?r.bookId():r.barcode().trim());
        int previousTotal=book.getTotalCopies();int previousAvailable=book.getAvailableCopies();int total=r.totalCopies()==null?previousTotal:r.totalCopies();
        book.setTotalCopies(total);book.setAvailableCopies(Math.max(0,Math.min(total,previousAvailable+(total-previousTotal))));
        book.setAvailability(book.getAvailableCopies()>0?Availability.AVAILABLE:Availability.ON_LOAN);
        return book;
    }
    private BookResponse toResponse(Book b) {
        return new BookResponse(b.getId(),b.getBookId(),b.getTitle(),b.getAuthor(),b.getCategory(),b.getIsbn(),
            b.getPublisher(),b.getYearPublished(),b.getPrice(),b.getAvailability(),
            b.getContentType(),b.getSourceUrl(),b.getLicense(),b.getCoverUrl(),b.getBarcode(),b.getTotalCopies(),b.getAvailableCopies());
    }
}
