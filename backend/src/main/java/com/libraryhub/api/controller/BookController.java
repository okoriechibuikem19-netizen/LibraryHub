package com.libraryhub.api.controller;

import com.libraryhub.api.dto.BookRequest;
import com.libraryhub.api.dto.BookResponse;
import com.libraryhub.api.model.Availability;
import com.libraryhub.api.service.BookService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/books")
public class BookController {
    private final BookService service;
    public BookController(BookService service) { this.service=service; }

    @GetMapping
    public List<BookResponse> getAll(
        @RequestParam(required=false) String search,
        @RequestParam(required=false) String category,
        @RequestParam(required=false) Availability availability) {
        return service.getAll(search,category,availability);
    }
    @GetMapping("/{id}") public BookResponse getOne(@PathVariable String id) { return service.getById(id); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public BookResponse create(@Valid @RequestBody BookRequest request) { return service.create(request); }
    @PutMapping("/{id}") public BookResponse update(@PathVariable String id,@Valid @RequestBody BookRequest request) {
        return service.update(id,request);
    }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) { service.delete(id); }
}
