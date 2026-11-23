package com.libraryhub.api.controller;
import com.libraryhub.api.model.ReaderState;
import com.libraryhub.api.repository.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/api/reader")
public class ReaderController {
 private final ReaderStateRepository states;private final UserAccountRepository users;
 public ReaderController(ReaderStateRepository states,UserAccountRepository users){this.states=states;this.users=users;}
 private String id(Authentication a){return users.findByEmailIgnoreCase(a.getName()).orElseThrow().getId();}
 @GetMapping("/state") public ReaderState get(Authentication a){String id=id(a);return states.findById(id).orElseGet(()->{ReaderState s=new ReaderState();s.userId=id;return s;});}
 @PutMapping("/state") public ReaderState save(Authentication a,@RequestBody ReaderState state){state.userId=id(a);return states.save(state);}
}
