package com.ds.search.model;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class SearchRequest {
    private String query;

    public SearchRequest() {}

    public SearchRequest(String query) {
        this.query = query;
    }

}
