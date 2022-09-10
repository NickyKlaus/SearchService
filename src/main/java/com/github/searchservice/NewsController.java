package com.github.searchservice;

import com.github.searchservice.model.NewsItem;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class NewsController {

    @Secured("ROLE_USER")
    @GetMapping(path = "/news", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<NewsItem>> getAllNews() {
        return new ResponseEntity<>(List.of(NewsItem.of("first"), NewsItem.of("second")), HttpStatus.OK);
    }
}
