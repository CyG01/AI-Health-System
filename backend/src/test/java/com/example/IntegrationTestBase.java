package com.example;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Testcontainers 集成测试基类。
 *
 * 启动真实 MySQL 容器，供集成测试使用。
 * 后续可扩展 Redis、TDengine 等容器。
 */
@Testcontainers
public abstract class IntegrationTestBase {

    @Container
    static MySQLContainer<?> mysqlContainer = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("ai_health_test")
            .withUsername("test")
            .withPassword("test123");

    @BeforeAll
    static void checkContainerRunning() {
        assert mysqlContainer.isRunning() : "MySQL Testcontainer 未成功启动";
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysqlContainer::getJdbcUrl);
        registry.add("spring.datasource.username", mysqlContainer::getUsername);
        registry.add("spring.datasource.password", mysqlContainer::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
    }
}