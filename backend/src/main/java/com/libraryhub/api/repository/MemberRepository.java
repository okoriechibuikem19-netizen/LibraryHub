package com.libraryhub.api.repository;
import com.libraryhub.api.model.Member;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;
public interface MemberRepository extends MongoRepository<Member,String> {
    Optional<Member> findByMemberIdIgnoreCase(String memberId);
    Optional<Member> findByEmailIgnoreCase(String email);
}
