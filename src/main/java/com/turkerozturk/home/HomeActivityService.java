package com.turkerozturk.home;

import com.turkerozturk.calendar.CalendarSettings;
import com.turkerozturk.multipledatabases.TenantContext;
import com.turkerozturk.multipledatabases.TenantService;
import com.turkerozturk.node.Node;
import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import javax.sql.DataSource;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

/** Reads tree occurrences, resolving shared content without borrowing the master's hierarchy. */
@Service
public class HomeActivityService {
    private static final String FROM = " FROM children c JOIN node n ON n.node_id = CASE WHEN COALESCE(c.master_id,0)=0 THEN c.node_id ELSE c.master_id END";
    private final HomeSettings settings;
    private final CalendarSettings calendar;
    private final TenantService tenants;
    public HomeActivityService(HomeSettings settings, CalendarSettings calendar, TenantService tenants) {
        this.settings = settings; this.calendar = calendar; this.tenants = tenants;
    }
    public record Crumb(long id, String name) {}
    public record Entry(long id, String name, String icon, boolean bold, String color,
                        String date, boolean today, List<Crumb> breadcrumbs) {}
    public record Activity(List<Entry> modified, List<Entry> created) {}
    private record Position(long parent, Node node) {}

    /** Explicit source: selection may still have an EntityManager bound to the previous tenant. */
    public OptionalLong latest(DataSource source) throws SQLException {
        try (var c = source.getConnection(); var q = c.prepareStatement("SELECT c.node_id" + FROM
                + " WHERE COALESCE(n.tags,'') <> '__sweetcherry_audit_v1__' ORDER BY COALESCE(n.ts_lastsave,0) DESC, CASE WHEN COALESCE(c.master_id,0)=0 THEN 0 ELSE 1 END, c.node_id ASC LIMIT 1");
             var r = q.executeQuery()) {
            return r.next() ? OptionalLong.of(r.getLong(1)) : OptionalLong.empty();
        }
    }
    public void populate(Model model) {
        model.addAttribute("homeModifiedEnabled", settings.getModifiedMinimum() > 0);
        model.addAttribute("homeCreatedEnabled", settings.getCreatedMinimum() > 0);
        model.addAttribute("homeModified", List.of()); model.addAttribute("homeCreated", List.of());
        if (settings.getModifiedMinimum() == 0 && settings.getCreatedMinimum() == 0) return;
        try {
            DataSource source = tenants.getAllTenants().get(TenantContext.getCurrentTenant());
            if (source == null) throw new SQLException("No selected data source");
            Activity activity = read(source, Clock.system(calendar.zone()));
            model.addAttribute("homeModified", activity.modified());
            model.addAttribute("homeCreated", activity.created());
        } catch (SQLException | RuntimeException failure) {
            model.addAttribute("homeActivityUnavailable", true);
            LoggerFactory.getLogger(getClass()).warn("Home activity could not be loaded ({}).", failure.getClass().getSimpleName());
        }
    }
    Activity read(DataSource source, Clock clock) throws SQLException {
        ZoneId zone = calendar.zone();
        LocalDate today = LocalDate.now(clock.withZone(zone));
        long start = today.atStartOfDay(zone).toEpochSecond();
        long end = today.plusDays(1).atStartOfDay(zone).toEpochSecond();
        try (var c = source.getConnection()) {
            List<Long> modified = ids(c, "ts_lastsave", settings.getModifiedMinimum(), start, end);
            List<Long> created = ids(c, "ts_creation", settings.getCreatedMinimum(), start, end);
            if (modified.isEmpty() && created.isEmpty()) return new Activity(List.of(), List.of());
            Map<Long, Position> positions = new HashMap<>();
            try (var q = c.prepareStatement("SELECT c.node_id,c.father_id,n.name,n.syntax,n.is_ro,n.is_richtxt,n.ts_creation,n.ts_lastsave" + FROM);
                 var r = q.executeQuery()) {
                while (r.next()) {
                    Node node = new Node(); node.setNodeId(r.getLong(1)); node.setName(Objects.requireNonNullElse(r.getString(3), ""));
                    node.setSyntax(Objects.requireNonNullElse(r.getString(4), "plain-text"));
                    node.setIsReadOnly16bit(r.getLong(5)); node.setIsRichText(r.getLong(6));
                    node.setCreationTimestamp(r.getLong(7)); node.setLastSaveTimestamp(r.getLong(8));
                    node.parseNodeIconAndReadOnlyFlag(); node.parseNodeTitleColorAndBoldnessAndTextType();
                    positions.put(node.getNodeId(), new Position(r.getLong(2), node));
                }
            }
            return new Activity(entries(modified, positions, false, start, zone), entries(created, positions, true, start, zone));
        }
    }
    private List<Long> ids(Connection c, String column, int minimum, long start, long end) throws SQLException {
        if (minimum == 0) return List.of();
        long count;
        try (var q = c.prepareStatement("SELECT COUNT(*)" + FROM + " WHERE COALESCE(n.tags,'') <> '__sweetcherry_audit_v1__' AND n." + column + " >= ? AND n." + column + " < ?")) {
            q.setLong(1, start); q.setLong(2, end);
            try (var r = q.executeQuery()) { r.next(); count = r.getLong(1); }
        }
        var ids = new ArrayList<Long>();
        try (var q = c.prepareStatement("SELECT c.node_id" + FROM + " WHERE COALESCE(n.tags,'') <> '__sweetcherry_audit_v1__' AND n." + column + " < ? ORDER BY n." + column + " DESC,c.node_id ASC LIMIT ?")) {
            q.setLong(1, end); q.setLong(2, Math.max(count, minimum));
            try (var r = q.executeQuery()) { while (r.next()) ids.add(r.getLong(1)); }
        }
        return ids;
    }
    private List<Entry> entries(List<Long> ids, Map<Long, Position> positions, boolean created, long start, ZoneId zone) {
        var entries = new ArrayList<Entry>();
        for (long id : ids) {
            Position position = positions.get(id); if (position == null) continue;
            Node node = position.node(); long time = created ? node.getCreationTimestamp() : node.getLastSaveTimestamp();
            var path = new ArrayList<Crumb>(); var visited = new HashSet<Long>();
            long current = id;
            while (current != 0 && visited.add(current) && path.size() < 200) {
                Position p = positions.get(current); if (p == null) break;
                path.add(new Crumb(current, p.node().getName())); current = p.parent();
            }
            Collections.reverse(path);
            entries.add(new Entry(id, node.getName(), node.getNodeIcon() == null ? "zero" : node.getNodeIcon().getIconName(),
                    node.isBoldnessBit(), String.format(Locale.ROOT, "#%06X", node.getTitleColor()),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(Instant.ofEpochSecond(time).atZone(zone)),
                    time >= start, List.copyOf(path)));
        }
        return List.copyOf(entries);
    }
}
