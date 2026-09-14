package com.archive;
import org.springframework.web.bind.annotation.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

@RestController
@RequestMapping("/api/archive")
@CrossOrigin
public class ArchiveController {
 private final Path file=Paths.get("data/archive.json");
 @GetMapping public String get() throws Exception{
   if(!Files.exists(file)) Files.writeString(file,"{\"folders\":[],\"qaItems\":[]}");
   return Files.readString(file);
 }
 @PostMapping public void save(@RequestBody String body)throws Exception{
   Files.createDirectories(file.getParent());
   Files.writeString(file,body,StandardCharsets.UTF_8);
 }
}
