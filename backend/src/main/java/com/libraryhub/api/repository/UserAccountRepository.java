package com.libraryhub.api.repository;
import com.libraryhub.api.model.UserAccount;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;
public interface UserAccountRepository extends MongoRepository<UserAccount,String> {
    Optional<UserAccount> findByEmailIgnoreCase(String email);
}
