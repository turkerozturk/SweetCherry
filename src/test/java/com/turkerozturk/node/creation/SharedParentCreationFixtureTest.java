package com.turkerozturk.node.creation;

import com.turkerozturk.multipledatabases.CustomPropertiesHolder;
import com.turkerozturk.node.properties.NodePropertiesService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SharedParentCreationFixtureTest {
    @Test void childBelongsToSharedOccurrenceAndNotItsMaster() throws Exception {
        try (var connection = fixture()) {
            assertThat(service(connection).create(11)).isEqualTo(20);
            assertThat(number(connection, "SELECT father_id FROM children WHERE node_id=20")).isEqualTo(11);
            assertThat(number(connection, "SELECT sequence FROM children WHERE node_id=20")).isEqualTo(4);
            assertThat(number(connection, "SELECT master_id FROM children WHERE node_id=20")).isZero();
            assertThat(number(connection, "SELECT COUNT(*) FROM node WHERE node_id=20")).isEqualTo(1);
            assertThat(number(connection, "SELECT COUNT(*) FROM children WHERE father_id=2")).isEqualTo(1);
            assertThat(number(connection, "SELECT master_id FROM children WHERE node_id=11")).isEqualTo(2);
        }
    }

    @Test void siblingOfNestedSharedOccurrenceUsesItsSharedParentAndShiftsFollowingSibling() throws Exception {
        try (var connection = fixture()) {
            try (var statement = connection.createStatement()) {
                statement.execute("UPDATE children SET father_id=11, sequence=4 WHERE node_id=9");
            }
            assertThat(service(connection).createSibling(18)).isEqualTo(20);
            assertThat(number(connection, "SELECT father_id FROM children WHERE node_id=20")).isEqualTo(11);
            assertThat(number(connection, "SELECT sequence FROM children WHERE node_id=20")).isEqualTo(4);
            assertThat(number(connection, "SELECT sequence FROM children WHERE node_id=9")).isEqualTo(5);
            assertThat(number(connection, "SELECT father_id FROM children WHERE node_id=19")).isEqualTo(18);
            assertThat(number(connection, "SELECT master_id FROM children WHERE node_id=18")).isEqualTo(1);
            assertThat(number(connection, "SELECT COUNT(*) FROM node WHERE node_id=20")).isEqualTo(1);
        }
    }

    private Connection fixture() throws Exception {
        var connection = DriverManager.getConnection("jdbc:sqlite::memory:");
        try (var script = getClass().getResourceAsStream("/fixtures/shared-node-tree.sql")) {
            assertThat(script).isNotNull();
            String sql = new String(script.readAllBytes(), StandardCharsets.UTF_8).replaceAll("(?m)^--.*$", "");
            try (var statement = connection.createStatement()) {
                for (String part : sql.split(";")) if (!part.isBlank()) statement.execute(part);
            }
        }
        connection.setAutoCommit(false);
        return connection;
    }

    private ChildNodeService service(Connection connection) {
        var properties = mock(NodePropertiesService.class);
        when(properties.writable()).thenReturn(true);
        var manager = mock(EntityManager.class);
        when(manager.createNativeQuery(anyString())).thenAnswer(call -> query(connection, call.getArgument(0)));
        var service = new ChildNodeService(properties, new CustomPropertiesHolder());
        ReflectionTestUtils.setField(service, "entityManager", manager);
        return service;
    }

    // Run the service's actual SQL against SQLite; only JPA parameter plumbing is mocked.
    private Query query(Connection connection, String sql) {
        var query = mock(Query.class);
        var parameters = new HashMap<String, Object>();
        when(query.setParameter(anyString(), any())).thenAnswer(call -> {
            parameters.put(call.getArgument(0), call.getArgument(1));
            return query;
        });
        org.mockito.stubbing.Answer<Object> execute = call -> {
            var names = new ArrayList<String>();
            var matcher = Pattern.compile(":([A-Za-z]+)").matcher(sql);
            while (matcher.find()) names.add(matcher.group(1));
            try (var statement = connection.prepareStatement(sql.replaceAll(":[A-Za-z]+", "?"))) {
                for (int i = 0; i < names.size(); i++) statement.setObject(i + 1, parameters.get(names.get(i)));
                if (call.getMethod().getName().equals("executeUpdate")) return statement.executeUpdate();
                try (var result = statement.executeQuery()) {
                    if (call.getMethod().getName().equals("getSingleResult")) {
                        assertThat(result.next()).isTrue();
                        return result.getObject(1);
                    }
                    var rows = new ArrayList<Object[]>();
                    while (result.next()) rows.add(new Object[]{result.getObject(1), result.getObject(2)});
                    return rows;
                }
            }
        };
        when(query.executeUpdate()).thenAnswer(execute);
        when(query.getSingleResult()).thenAnswer(execute);
        when(query.getResultList()).thenAnswer(execute);
        return query;
    }

    private long number(Connection connection, String sql) throws Exception {
        try (var statement = connection.createStatement(); var result = statement.executeQuery(sql)) {
            assertThat(result.next()).isTrue();
            return result.getLong(1);
        }
    }
}
