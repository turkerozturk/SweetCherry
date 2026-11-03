package com.turkerozturk.node.tags;
final class TagManagementProblem extends RuntimeException {
    private final String key;
    TagManagementProblem(String reason){super(reason);key="tags.manage.error."+reason;}
    String key(){return key;}
}
