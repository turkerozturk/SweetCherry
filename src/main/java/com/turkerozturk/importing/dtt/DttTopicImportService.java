package com.turkerozturk.importing.dtt;

import com.turkerozturk.importing.CtbTreeWriter;
import com.turkerozturk.node.dtt.DttBridge;
import com.turkerozturk.node.dtt.DttCatalog;
import com.turkerozturk.node.properties.NodePropertiesService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Objects;
import org.hibernate.Session;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

@Service
public class DttTopicImportService {
    private static final DateTimeFormatter ROOT_TIME = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss", Locale.ROOT);
    @PersistenceContext
    private EntityManager entityManager;
    private final DttBridge bridge;
    private final NodePropertiesService properties;
    private final ObjectMapper json;

    public DttTopicImportService(DttBridge bridge, NodePropertiesService properties, ObjectMapper json) {
        this.bridge = bridge;
        this.properties = properties;
        this.json = json;
    }

    public record Preview(List<Group> groups, String revision, int count) {}
    public record Group(String name, List<Category> categories) {}
    public record Category(String name, List<Topic> topics) {}
    public record Topic(String name, String id) {}

    public record Plan(Preview full, Preview selected, boolean missingOnly) {}

    @Transactional(readOnly = true)
    public Plan preview(String ungrouped, String uncategorized, boolean missingOnly) {
        requireWritable();
        Preview full = preview(bridge.catalog(), ungrouped, uncategorized);
        Preview selected = missingOnly ? entityManager.unwrap(Session.class)
                .doReturningWork(c -> missing(full, topicIds(c, false))) : full;
        return new Plan(full, selected, missingOnly);
    }

    Preview missing(Preview full, Set<String> existing) {
        var groups = full.groups().stream().map(group -> new Group(group.name(), group.categories().stream()
                .map(category -> new Category(category.name(), category.topics().stream()
                        .filter(topic -> !existing.contains(topic.id())).toList()))
                .filter(category -> !category.topics().isEmpty()).toList()))
                .filter(group -> !group.categories().isEmpty()).toList();
        int count = groups.stream().flatMap(group -> group.categories().stream())
                .mapToInt(category -> category.topics().size()).sum();
        return new Preview(groups, revision(List.of(full.revision(), groups)), count);
    }

    static Set<String> topicIds(Connection connection, boolean writing) throws SQLException {
        String product = connection.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT);
        if (writing) CtbTreeWriter.lockForRootWrite(connection, product);
        boolean lockRows = writing && (product.contains("mysql") || product.contains("mariadb"));
        var ids = new HashSet<String>();
        String sql = "SELECT tags, txt, syntax FROM node WHERE LOWER(tags) LIKE '%dttid%'";
        try (var statement = connection.createStatement();
             var rows = statement.executeQuery(sql + (lockRows ? " FOR UPDATE" : ""))) {
            while (rows.next()) {
                if (DttBridge.hasTopicTag(rows.getString(1))) {
                    DttBridge.topicId(rows.getString(2), rows.getString(3)).ifPresent(ids::add);
                }
            }
        }
        return ids;
    }

    Preview preview(DttCatalog catalog, String ungrouped, String uncategorized) {
        if (catalog == null || catalog.groups() == null || catalog.categories() == null || catalog.topics() == null) {
            throw problem(HttpStatus.BAD_GATEWAY, "dttImport.invalidCatalog");
        }
        var groups = new LinkedHashMap<String, Group>();
        var categories = new LinkedHashMap<String, Category>();
        for (var item : catalog.groups()) {
            if (item == null || item.id() == null || groups.putIfAbsent(item.id(),
                    new Group(name(item.name(), item.id()), new ArrayList<>())) != null) throw invalidCatalog();
        }
        for (var item : catalog.categories()) {
            if (item == null || item.id() == null) throw invalidCatalog();
            var category = new Category(name(item.name(), item.id()), new ArrayList<>());
            if (categories.putIfAbsent(item.id(), category) != null) throw invalidCatalog();
            var group = groups.get(item.groupId());
            if (group == null) group = groups.computeIfAbsent(null, id -> new Group(ungrouped, new ArrayList<>()));
            group.categories().add(category);
        }
        var topicIds = new HashSet<String>();
        for (var item : catalog.topics()) {
            if (item == null || !validTopicId(item.id()) || !topicIds.add(item.id())) throw invalidCatalog();
            var category = categories.get(item.categoryId());
            if (category == null) {
                category = categories.get(null);
                if (category == null) {
                    category = new Category(uncategorized, new ArrayList<>());
                    categories.put(null, category);
                    groups.computeIfAbsent(null, id -> new Group(ungrouped, new ArrayList<>())).categories().add(category);
                }
            }
            category.topics().add(new Topic(name(item.name(), item.id()), item.id()));
        }
        var tree = groups.values().stream().map(group -> new Group(group.name(), group.categories().stream()
                .map(category -> new Category(category.name(), List.copyOf(category.topics()))).toList())).toList();
        return new Preview(tree, revision(catalog), catalog.topics().size());
    }

    @Transactional(rollbackFor = Exception.class)
    public CtbTreeWriter.Result write(Plan plan) {
        requireWritable();
        Instant now = Instant.now();
        var result = entityManager.unwrap(Session.class).doReturningWork(connection -> {
            Preview selected = plan.missingOnly() ? missing(plan.full(), topicIds(connection, true)) : plan.full();
            if (!selected.revision().equals(plan.selected().revision())) {
                throw problem(HttpStatus.CONFLICT, "dttImport.changed");
            }
            if (selected.count() == 0) return null;
            String base = "DailyTopicTracker-import-" + ROOT_TIME.format(now.atZone(ZoneId.systemDefault()));
            CtbTreeWriter.Result imported = CtbTreeWriter.insert(connection, entries(base, selected, now.getEpochSecond()), true);
            for (int suffix = 2; imported == null; suffix++) {
                String root = base + " (" + suffix + ")";
                imported = CtbTreeWriter.insert(connection, entries(root, selected, now.getEpochSecond()), true);
            }
            return imported;
        });
        entityManager.clear();
        return result;
    }

    static List<CtbTreeWriter.Entry> entries(String root, Preview preview, long now) {
        var entries = new ArrayList<CtbTreeWriter.Entry>();
        entries.add(entry(root, "", "", -1, 1, now));
        int groupSequence = 1;
        for (var group : preview.groups()) {
            int groupIndex = entries.size();
            entries.add(entry(group.name(), "", "", 0, groupSequence++, now));
            int categorySequence = 1;
            for (var category : group.categories()) {
                int categoryIndex = entries.size();
                entries.add(entry(category.name(), "", "", groupIndex, categorySequence++, now));
                int topicSequence = 1;
                for (var topic : category.topics()) {
                    entries.add(entry(topic.name(), topic.id(), "dttid", categoryIndex, topicSequence++, now));
                }
            }
        }
        return entries;
    }

    private void requireWritable() {
        if (!properties.writable()) throw problem(HttpStatus.FORBIDDEN, "dttImport.readonly");
    }

    private String revision(Object data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(json.writeValueAsBytes(data)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String name(String name, String id) {
        String value = Objects.toString(name, "");
        if (value.isBlank()) return "#" + id;
        return value.codePointCount(0, value.length()) <= 255 ? value : value.substring(0, value.offsetByCodePoints(0, 255));
    }

    private static boolean validTopicId(String id) {
        try {
            long number = Long.parseLong(id);
            return number > 0 && id.equals(Long.toString(number));
        } catch (NumberFormatException error) {
            return false;
        }
    }

    private static CtbTreeWriter.Entry entry(String name, String text, String tags, int parent, int sequence, long now) {
        return new CtbTreeWriter.Entry(name, text, tags, parent, sequence, 0, 0, now, now);
    }

    private static ResponseStatusException invalidCatalog() {
        return problem(HttpStatus.BAD_GATEWAY, "dttImport.invalidCatalog");
    }

    private static ResponseStatusException problem(HttpStatus status, String key) {
        return new ResponseStatusException(status, key);
    }
}
