package com.turkerozturk.importing.exportednodes;

import java.util.List;

/** Safe ADMIN-facing diagnostics: CTB record identities, never SQL, credentials or server paths. */
public final class CtbImportProblem extends IllegalArgumentException {
    public record Limits(long fileBytes,long dataBytes,long records,long maxBytes,int maxRecords) { }
    public record LinkIssue(long sourceId,String sourceTitle,String location,String targetId) { }
    private final Limits limits;
    private final List<LinkIssue> links;
    public CtbImportProblem(Limits limits){super("limit");this.limits=limits;this.links=List.of();}
    public CtbImportProblem(List<LinkIssue> links){super("links");this.limits=null;this.links=List.copyOf(links);}
    public Limits getLimits(){return limits;}
    public List<LinkIssue> getLinks(){return links;}
}
