package com.geekylikes.app.controllers;

import com.geekylikes.app.payload.api.response.NewsResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@CrossOrigin
@RestController
@RequestMapping("/api/test")
public class TestController {

    @Autowired
    RestTemplate restTemplate;

    @Value("${geekylikes.app.newsApiKey}")
    private String apiKey;

    @GetMapping("/all")
    public String allAccess() {
        return "public content";
    }

    @GetMapping("/user")
    //restricts routes
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public String userAccess() {
        return "User content";
    }

    @GetMapping("/mod")
    @PreAuthorize("hasRole('MODERATOR') or hasRole('ADMIN')")
    public String modAccess() {
        return "mod content";
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminAccess() {
        return "admin content";
    }

    @GetMapping("/news/{q}")
    public ResponseEntity<?> getNewsArticles(@PathVariable String q) {
        String uri = "https://newsapi.org/v2/everything?sortBy=popularity&apiKey=" + apiKey + "&q=" + q;

        NewsResponse response = restTemplate.getForObject(uri, NewsResponse.class);

        return ResponseEntity.ok(response.getArticles());
    }

    @GetMapping("/newsCategory/{category}")
    public ResponseEntity<?> getArticlesByCategory(@PathVariable String category) {
        String uri = "https://newsapi.org/v2/everything?sortBy=popularity&apiKey=" + apiKey + "&q=" + category;

        NewsResponse response = restTemplate.getForObject(uri, NewsResponse.class);

        return ResponseEntity.ok(response.getArticles());
    }


}
