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
        if(split(raw).stream().anyMatch(tag->tag.equalsIgnoreCase(com.turkerozturk.node.TrashSql.TAG)||tag.equalsIgnoreCase(com.turkerozturk.node.audit.AuditSql.TAG)))throw new IllegalArgumentException("Reserved system tag");
        return String.join(" ",split(raw));
    }
}
