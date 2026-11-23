package com.libraryhub.api.repository;
import com.libraryhub.api.model.ReaderState;
import org.springframework.data.mongodb.repository.MongoRepository;
public interface ReaderStateRepository extends MongoRepository<ReaderState,String>{}
