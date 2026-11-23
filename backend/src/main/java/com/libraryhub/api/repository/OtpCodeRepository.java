package com.libraryhub.api.repository;
import com.libraryhub.api.model.OtpCode;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;
public interface OtpCodeRepository extends MongoRepository<OtpCode,String> {
    Optional<OtpCode> findTopByEmailIgnoreCaseAndPurposeOrderByExpiresAtDesc(String email,String purpose);
    void deleteByEmailIgnoreCaseAndPurpose(String email,String purpose);
    Optional<OtpCode> findByCodeHashAndPurpose(String codeHash,String purpose);
}
