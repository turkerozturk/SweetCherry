package com.turkerozturk.codebox.gallery;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
@Component
public record CodeGallerySettings(int fontSize,int previewCharacters,int previewLines,int viewerCharacters,int pageSize){
    public CodeGallerySettings(@Value("${myapp.codebox-gallery.font-size:14}")int fontSize,
            @Value("${myapp.codebox-gallery.preview-max-characters:2400}")int previewCharacters,
            @Value("${myapp.codebox-gallery.preview-max-lines:20}")int previewLines,
            @Value("${myapp.codebox-gallery.viewer-max-characters:200000}")int viewerCharacters,
            @Value("${myapp.codebox-gallery.page-size:12}")int pageSize){
        if(fontSize<10||fontSize>32||previewCharacters<1||previewCharacters>10000||previewLines<1||previewLines>100||viewerCharacters<1||viewerCharacters>1000000||pageSize<1||pageSize>36)throw new IllegalArgumentException("Invalid myapp.codebox-gallery display settings");
        this.fontSize=fontSize;this.previewCharacters=previewCharacters;this.previewLines=previewLines;this.viewerCharacters=viewerCharacters;this.pageSize=pageSize;
    }
}
