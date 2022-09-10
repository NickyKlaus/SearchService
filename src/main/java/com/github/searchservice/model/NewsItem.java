package com.github.searchservice.model;

import lombok.Data;

@Data(staticConstructor = "of")
public class NewsItem {
    private final String title;
}
