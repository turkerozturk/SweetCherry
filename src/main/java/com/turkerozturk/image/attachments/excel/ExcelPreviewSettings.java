package com.turkerozturk.image.attachments.excel;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public record ExcelPreviewSettings(int maxBytes, int expandedBytes, int rows, int columns, int cellCharacters) {
    public ExcelPreviewSettings(
            @Value("${myapp.excel-preview.max-file-bytes:5242880}") int maxBytes,
            @Value("${myapp.excel-preview.max-expanded-bytes:16777216}") int expandedBytes,
            @Value("${myapp.excel-preview.rows-per-page:100}") int rows,
            @Value("${myapp.excel-preview.max-columns:50}") int columns,
            @Value("${myapp.excel-preview.max-cell-characters:2000}") int cellCharacters) {
        if (maxBytes < 1024 || maxBytes > 20971520 || expandedBytes < 1024 || expandedBytes > 67108864
                || rows < 1 || rows > 200 || columns < 1 || columns > 100 || cellCharacters < 1 || cellCharacters > 4000)
            throw new IllegalArgumentException("Invalid myapp.excel-preview limits");
        this.maxBytes = maxBytes; this.expandedBytes = expandedBytes; this.rows = rows;
        this.columns = columns; this.cellCharacters = cellCharacters;
    }
}
