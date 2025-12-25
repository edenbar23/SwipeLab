package com.swipelab;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("integration")
public class SchemaVerificationTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void testSchemaCreation() throws SQLException {
        try (var connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            ResultSet tables = metaData.getTables(null, "PUBLIC", null, new String[] { "TABLE" });

            Set<String> tableNames = new HashSet<>();
            while (tables.next()) {
                tableNames.add(tables.getString("TABLE_NAME").toLowerCase());
            }

            // Verify Core Tables
            assertTrue(tableNames.contains("users"), "Table 'users' should exist");
            assertTrue(tableNames.contains("images"), "Table 'images' should exist");
            assertTrue(tableNames.contains("labels"), "Table 'labels' should exist");
            assertTrue(tableNames.contains("tasks"), "Table 'tasks' should exist");
            assertTrue(tableNames.contains("classifications"), "Table 'classifications' should exist");

            // Verify Gamification Tables
            assertTrue(tableNames.contains("badges"), "Table 'badges' should exist");
            assertTrue(tableNames.contains("user_badges"), "Table 'user_badges' should exist");
            assertTrue(tableNames.contains("leaderboards"), "Table 'leaderboards' should exist");

            // Verify Gold Image Tables
            assertTrue(tableNames.contains("gold_images"), "Table 'gold_images' should exist");

            System.out.println("All required tables found: " + tableNames);
        }
    }
}
