package com.turkerozturk.node.dtt;

import java.util.List;

public record DttCatalog(List<Group> groups, List<Category> categories, List<Topic> topics) {
    public record Group(String id, String name) {}
    public record Category(String id, String name, String groupId) {}
    public record Topic(String id, String name, String categoryId) {}
}
