package com.ds.search.model;

import lombok.Getter;
import lombok.Setter;

@Getter
public class Document {
    private final String id;
    private final String content;
    @Setter
    private double relevanceScore;

    public Document(String id, String content) {
        this.id = id;
        this.content = content;
    }
}
