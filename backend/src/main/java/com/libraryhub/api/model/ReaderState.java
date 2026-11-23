package com.libraryhub.api.model;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.*;
@Document("reader_states")
public class ReaderState {
 @Id public String userId;
 public Map<String,String> shelves=new HashMap<>();
 public Map<String,Integer> progress=new HashMap<>();
 public Map<String,String> bookmarks=new HashMap<>();
 public Map<String,String> notes=new HashMap<>();
 public boolean voiceEnabled=false;
 public String theme="light";
}
