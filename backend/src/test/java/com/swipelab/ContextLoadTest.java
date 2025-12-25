package com.swipelab;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class ContextLoadTest {

    @Test
    void contextLoads() {
        // Sanity check to ensure the Spring application context loads successfully
        // using the 'test' profile (H2 database)
    }
}
