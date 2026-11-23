package com.libraryhub.api.controller;
import com.libraryhub.api.dto.*;
import com.libraryhub.api.service.LoanService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/loans")
public class LoanController {
    private final LoanService service;
    public LoanController(LoanService service){this.service=service;}
    @GetMapping public List<LoanResponse> getAll(){return service.getAll();}
    @GetMapping("/{id}") public LoanResponse getOne(@PathVariable String id){return service.getById(id);}
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public LoanResponse create(@Valid @RequestBody LoanRequest request){return service.create(request);}
    @PutMapping("/{id}/return") public LoanResponse returnBook(@PathVariable String id){return service.returnBook(id);}
    @PutMapping("/{id}/fine/pay") public LoanResponse payFine(@PathVariable String id){return service.payFine(id);}
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable String id){service.delete(id);}
}
