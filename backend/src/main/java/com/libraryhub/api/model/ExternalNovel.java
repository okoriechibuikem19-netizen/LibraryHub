package com.libraryhub.api.model;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.*;
@Document("external_novels")
public class ExternalNovel {
 @Id public String id;public String title;public String author;public String coverUrl;public String sourceUrl;public String textUrl;public String language;public List<String> genres=new ArrayList<>();public long downloads;
}
