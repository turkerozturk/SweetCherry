package com.turkerozturk.node.fragments;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public record NodeFragmentSettings(int depth,int inclusions,int characters,int htmlCharacters,int objectBytes,int objects) {
    public NodeFragmentSettings(@Value("${myapp.node-fragments.max-depth:3}") int depth,
            @Value("${myapp.node-fragments.max-inclusions:20}") int inclusions,
            @Value("${myapp.node-fragments.max-source-characters:250000}") int characters,
            @Value("${myapp.node-fragments.max-html-characters:1048576}") int htmlCharacters,
            @Value("${myapp.node-fragments.max-object-bytes:16777216}") int objectBytes,
            @Value("${myapp.node-fragments.max-objects:200}") int objects) {
        if(depth<1||depth>5||inclusions<1||inclusions>50||characters<1||characters>1000000||htmlCharacters<1024||htmlCharacters>2097152
                ||objectBytes<1024||objectBytes>33554432||objects<1||objects>500)throw new IllegalArgumentException("Invalid myapp.node-fragments limits");
        this.depth=depth;this.inclusions=inclusions;this.characters=characters;this.htmlCharacters=htmlCharacters;this.objectBytes=objectBytes;this.objects=objects;
    }
}
