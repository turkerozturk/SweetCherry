package com.turkerozturk.image.attachments.preview;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public record FilePreviewSettings(int textBytes,int characters,int hexBytes,int archiveBytes,int expandedBytes,int entries) {
    public FilePreviewSettings(
            @Value("${myapp.file-preview.max-text-bytes:262144}") int textBytes,
            @Value("${myapp.file-preview.max-characters:200000}") int characters,
            @Value("${myapp.file-preview.hex-bytes-per-page:512}") int hexBytes,
            @Value("${myapp.file-preview.max-archive-bytes:5242880}") int archiveBytes,
            @Value("${myapp.file-preview.max-expanded-bytes:33554432}") int expandedBytes,
            @Value("${myapp.file-preview.max-entries:1000}") int entries) {
        if(textBytes<1024||textBytes>1048576||characters<1||characters>1000000||hexBytes<128||hexBytes>4096||hexBytes%16!=0
                ||archiveBytes<1024||archiveBytes>20971520||expandedBytes<1024||expandedBytes>67108864||entries<1||entries>2000)
            throw new IllegalArgumentException("Invalid myapp.file-preview limits");
        this.textBytes=textBytes;this.characters=characters;this.hexBytes=hexBytes;this.archiveBytes=archiveBytes;this.expandedBytes=expandedBytes;this.entries=entries;
    }
}
