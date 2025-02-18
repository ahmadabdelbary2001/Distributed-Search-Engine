package com.ds.search.util;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Tokenizer {

    public static List<String> tokenize(String text) {
        // Convert to lowercase, remove punctuation, and split by spaces
        return Arrays.stream(text.toLowerCase().replaceAll("[^a-zA-Z0-9\\s]", "").split("\\s+"))
                .collect(Collectors.toList());
    }
}
