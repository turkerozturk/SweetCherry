package com.turkerozturk.grid.gallery;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Numeric display configuration, validated before use in CSS or query limits. */
@Component
public record TableGallerySettings(int cellWidth, int cellPadding, int fontSize, int cellMaxHeight,
                                   int previewHeight, int previewRows, int previewColumns, int viewerRows, int pageSize) {
    public TableGallerySettings(
            @Value("${myapp.table-gallery.cell-width:160}") int cellWidth,
            @Value("${myapp.table-gallery.cell-padding:6}") int cellPadding,
            @Value("${myapp.table-gallery.font-size:14}") int fontSize,
            @Value("${myapp.table-gallery.cell-max-height:160}") int cellMaxHeight,
            @Value("${myapp.table-gallery.preview-height:260}") int previewHeight,
            @Value("${myapp.table-gallery.preview-rows:5}") int previewRows,
            @Value("${myapp.table-gallery.preview-columns:5}") int previewColumns,
            @Value("${myapp.table-gallery.viewer-rows:200}") int viewerRows,
            @Value("${myapp.table-gallery.page-size:12}") int pageSize) {
        check(cellWidth,20,480); check(cellPadding,0,20); check(fontSize,10,32);
        check(cellMaxHeight,20,800); check(previewHeight,20,800); check(previewRows,1,20);
        check(previewColumns,1,12); check(viewerRows,1,1000); check(pageSize,1,36);
        this.cellWidth=cellWidth;this.cellPadding=cellPadding;this.fontSize=fontSize;
        this.cellMaxHeight=cellMaxHeight;this.previewHeight=previewHeight;this.previewRows=previewRows;
        this.previewColumns=previewColumns;this.viewerRows=viewerRows;this.pageSize=pageSize;
    }
    private static void check(int value,int min,int max) {
        if(value<min||value>max)throw new IllegalArgumentException("Invalid myapp.table-gallery display setting: " + value);
    }
}
