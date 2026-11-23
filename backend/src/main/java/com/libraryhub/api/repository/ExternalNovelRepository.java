package com.libraryhub.api.repository;
import com.libraryhub.api.model.ExternalNovel;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.*;
public interface ExternalNovelRepository extends MongoRepository<ExternalNovel,String>{
 List<ExternalNovel> findTop100ByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCase(String title,String author);
}
