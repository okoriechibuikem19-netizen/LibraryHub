package com.libraryhub.api.controller;
import com.fasterxml.jackson.databind.*;
import com.libraryhub.api.model.ExternalNovel;
import com.libraryhub.api.repository.ExternalNovelRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.*;
@RestController @RequestMapping("/api/catalog")
public class CatalogController {
 private final ExternalNovelRepository novels;private final RestClient http=RestClient.create("https://gutendex.com");
 public CatalogController(ExternalNovelRepository novels){this.novels=novels;}
 @GetMapping("/novels") public List<ExternalNovel> search(@RequestParam(defaultValue="") String q,@RequestParam(defaultValue="1") int page){
  String term=q.trim();if(page<1||page>1000)throw new IllegalArgumentException("Invalid page");if(term.length()>100)throw new IllegalArgumentException("Search is too long");
  try {JsonNode root=http.get().uri(b->b.path("/books").queryParam("search",term).queryParam("page",page).build()).retrieve().body(JsonNode.class);
   List<ExternalNovel> imported=new ArrayList<>();
   if(root!=null)for(JsonNode n:root.path("results")){
    JsonNode formats=n.path("formats");String source="https://www.gutenberg.org/ebooks/"+n.path("id").asText();
    String html=formats.path("text/html").asText("");String text=formats.path("text/plain; charset=utf-8").asText("");
    if(html.isBlank()&&text.isBlank())continue;
    if(n.path("copyright").asBoolean(false))continue;
    ExternalNovel v=new ExternalNovel();v.id="gutenberg-"+n.path("id").asText();v.title=n.path("title").asText();v.author=n.path("authors").path(0).path("name").asText("Unknown");
    v.coverUrl=formats.path("image/jpeg").asText("");v.sourceUrl=source;v.textUrl=text;v.language=n.path("languages").path(0).asText("en");v.downloads=n.path("download_count").asLong();
    for(JsonNode subject:n.path("subjects"))if(v.genres.size()<4)v.genres.add(subject.asText());
    imported.add(v);
   }
   if(!imported.isEmpty())novels.saveAll(imported);
   return imported;
  }catch(Exception e){return term.isBlank()?novels.findAll().stream().limit(100).toList():novels.findTop100ByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCase(term,term);}
 }
 @GetMapping(value="/novels/{id}/text",produces="text/plain;charset=UTF-8") public String text(@PathVariable String id){
  ExternalNovel v=novels.findById(id).orElseThrow();
  if(v.textUrl==null||!v.textUrl.startsWith("https://www.gutenberg.org/"))throw new IllegalArgumentException("Text not available for this title");
  String content=RestClient.create().get().uri(v.textUrl).retrieve().body(String.class);
  if(content==null)throw new IllegalArgumentException("Text not available");
  return content.length()>1500000?content.substring(0,1500000):content;
 }
 @GetMapping("/novels/{id}") public ExternalNovel one(@PathVariable String id){return novels.findById(id).orElseThrow();}
}
