package com.turkerozturk.node.tags;

import java.util.*;

/** Literal ASCII space separates tags. Case, Unicode and punctuation are not rewritten. */
public final class NodeTags {
    private NodeTags() { }
    public static final int MAX_INPUT_CHARACTERS=20_000;
    public static final Comparator<String> ORDER=String.CASE_INSENSITIVE_ORDER.thenComparing(Comparator.naturalOrder());
    public static List<String> split(String raw) {
        if(raw==null||raw.isEmpty())return List.of();
        var unique=new LinkedHashSet<String>();
        for(String tag:raw.split(" +"))if(!tag.isEmpty())unique.add(tag);
        return List.copyOf(unique);
    }
    public static String normalize(String raw) {
        if(raw==null||raw.length()>MAX_INPUT_CHARACTERS||raw.indexOf('\0')>=0)throw new IllegalArgumentException("Invalid tags");
        return String.join(" ",split(raw));
    }
}
