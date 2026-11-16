package com.turkerozturk.image.attachments.word;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public record WordPreviewSettings(int maxBytes, int expandedBytes, int characters, int blocks, int tableRows, int tableColumns) {
    public WordPreviewSettings(
            @Value("${myapp.word-preview.max-file-bytes:5242880}") int maxBytes,
            @Value("${myapp.word-preview.max-expanded-bytes:16777216}") int expandedBytes,
            @Value("${myapp.word-preview.max-characters:200000}") int characters,
            @Value("${myapp.word-preview.max-blocks:1000}") int blocks,
            @Value("${myapp.word-preview.max-table-rows:100}") int tableRows,
            @Value("${myapp.word-preview.max-table-columns:20}") int tableColumns) {
        if (maxBytes < 1024 || maxBytes > 20971520 || expandedBytes < 1024 || expandedBytes > 67108864
                || characters < 1 || characters > 1000000 || blocks < 1 || blocks > 2000
                || tableRows < 1 || tableRows > 200 || tableColumns < 1 || tableColumns > 50)
            throw new IllegalArgumentException("Invalid myapp.word-preview limits");
        this.maxBytes=maxBytes; this.expandedBytes=expandedBytes; this.characters=characters;
        this.blocks=blocks; this.tableRows=tableRows; this.tableColumns=tableColumns;
    }
}
