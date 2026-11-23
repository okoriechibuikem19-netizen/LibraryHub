package com.libraryhub.api.controller;

import com.libraryhub.api.dto.AuthDtos.*;
import com.libraryhub.api.repository.UserAccountRepository;
import com.libraryhub.api.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
public class AdminController {
    private final AuthService auth;
    private final UserAccountRepository users;
    public AdminController(AuthService auth,UserAccountRepository users){this.auth=auth;this.users=users;}
    @PutMapping("/{id}/status") public UserResponse status(@PathVariable String id,@RequestBody java.util.Map<String,String> input){
        var user=users.findById(id).orElseThrow();user.setStatus(com.libraryhub.api.model.UserStatus.valueOf(input.get("status")));
        return auth.response(users.save(user));
    }
    @GetMapping public List<UserResponse> all(){return users.findAll().stream().map(auth::response).toList();}
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public UserResponse createStaff(@Valid @RequestBody StaffRequest request){return auth.createStaff(request);}
}
