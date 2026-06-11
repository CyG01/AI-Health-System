package com.example.config;

import com.zaxxer.hikari.HikariDataSource;
import org.apache.shardingsphere.driver.api.ShardingSphereDataSourceFactory;
import org.apache.shardingsphere.infra.config.algorithm.AlgorithmConfiguration;
import org.apache.shardingsphere.infra.config.mode.ModeConfiguration;
import org.apache.shardingsphere.readwritesplitting.api.ReadwriteSplittingRuleConfiguration;
import org.apache.shardingsphere.readwritesplitting.api.rule.ReadwriteSplittingDataSourceRuleConfiguration;
import org.apache.shardingsphere.sharding.api.config.ShardingRuleConfiguration;
import org.apache.shardingsphere.sharding.api.config.rule.ShardingTableRuleConfiguration;
import org.apache.shardingsphere.sharding.api.config.strategy.sharding.StandardShardingStrategyConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.*;

/**
 * ShardingSphere-JDBC 配置（Phase 4：高并发与成本精细化）。
 *
 * 功能：
 * 1. 读写分离 — SELECT 走从库，INSERT/UPDATE/DELETE 走主库
 * 2. 数据分片 — chat_message 按 user_id % 8 分 8 张表
 * 3. 明确数据边界 — MySQL 仅保留业务数据，3 个月后下线时序表 MySQL 副本
 *
 * 通过 spring.shardingsphere.enabled=true 启用（生产环境默认开启）
 */
@Configuration
@ConditionalOnProperty(name = "spring.shardingsphere.enabled", havingValue = "true")
public class ShardingSphereConfig {

    private static final Logger log = LoggerFactory.getLogger(ShardingSphereConfig.class);

    @Value("${spring.datasource.sharding.master.url}")
    private String masterUrl;

    @Value("${spring.datasource.sharding.master.username}")
    private String masterUsername;

    @Value("${spring.datasource.sharding.master.password}")
    private String masterPassword;

    @Value("${spring.datasource.sharding.slave0.url}")
    private String slave0Url;

    @Value("${spring.datasource.sharding.slave0.username}")
    private String slave0Username;

    @Value("${spring.datasource.sharding.slave0.password}")
    private String slave0Password;

    @Value("${spring.datasource.sharding.slave1.url:#{null}}")
    private String slave1Url;

    @Value("${spring.datasource.sharding.slave1.username:#{null}}")
    private String slave1Username;

    @Value("${spring.datasource.sharding.slave1.password:#{null}}")
    private String slave1Password;

    @Bean
    @Primary
    public DataSource shardingSphereDataSource() throws SQLException {
        log.info("初始化 ShardingSphere-JDBC 数据源（读写分离 + chat_message 分片）");

        Map<String, DataSource> dataSourceMap = new HashMap<>();

        // 主库（写）
        HikariDataSource masterDs = createDataSource(masterUrl, masterUsername, masterPassword);
        dataSourceMap.put("master", masterDs);

        // 从库0（读）
        HikariDataSource slave0Ds = createDataSource(slave0Url, slave0Username, slave0Password);
        dataSourceMap.put("slave0", slave0Ds);

        // 从库1（读，可选）
        if (slave1Url != null && !slave1Url.isBlank()) {
            HikariDataSource slave1Ds = createDataSource(slave1Url, slave1Username, slave1Password);
            dataSourceMap.put("slave1", slave1Ds);
        }

        // 读写分离规则
        ReadwriteSplittingRuleConfiguration rwRule = createReadWriteSplittingRule();

        // 分片规则
        ShardingRuleConfiguration shardingRule = createShardingRule();

        // 汇总所有规则
        Collection<org.apache.shardingsphere.infra.config.rule.RuleConfiguration> rules = new ArrayList<>();
        rules.add(rwRule);
        rules.add(shardingRule);

        Properties props = new Properties();
        props.setProperty("sql-show", "false");
        props.setProperty("sql-simple", "true");

        DataSource ds = ShardingSphereDataSourceFactory.createDataSource(
                dataSourceMap, rules, props);

        log.info("ShardingSphere-JDBC 数据源初始化完成 master={} slave0={} slave1={}",
                masterUrl, slave0Url, slave1Url != null ? slave1Url : "未配置");

        return ds;
    }

    private HikariDataSource createDataSource(String url, String username, String password) {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(url);
        ds.setUsername(username);
        ds.setPassword(password);
        ds.setDriverClassName("com.mysql.cj.jdbc.Driver");
        ds.setMaximumPoolSize(20);
        ds.setMinimumIdle(5);
        ds.setIdleTimeout(300000);
        ds.setConnectionTimeout(20000);
        ds.setMaxLifetime(1200000);
        ds.setLeakDetectionThreshold(60000);
        ds.setValidationTimeout(5000);
        ds.setConnectionTestQuery("SELECT 1");
        return ds;
    }

    /**
     * 读写分离规则配置。
     * 写操作 → master，读操作 → slave0/slave1（轮询负载均衡）
     */
    private ReadwriteSplittingRuleConfiguration createReadWriteSplittingRule() {
        // 构建读库列表
        List<String> readDataSourceNames = new ArrayList<>();
        readDataSourceNames.add("slave0");
        if (slave1Url != null && !slave1Url.isBlank()) {
            readDataSourceNames.add("slave1");
        }

        ReadwriteSplittingDataSourceRuleConfiguration rwDsRule =
                new ReadwriteSplittingDataSourceRuleConfiguration(
                        "ms",                          // 读写分离数据源名称
                        "Static",                     // 静态发现类型
                        buildRwProps("master", readDataSourceNames), // 写库 + 读库列表
                        "round_robin"                 // 读库负载均衡算法
                );

        // 负载均衡算法配置
        Properties lbProps = new Properties();
        AlgorithmConfiguration lbAlgorithm = new AlgorithmConfiguration("ROUND_ROBIN", lbProps);

        return new ReadwriteSplittingRuleConfiguration(
                Collections.singleton(rwDsRule),
                Collections.singletonMap("round_robin", lbAlgorithm));
    }

    private Properties buildRwProps(String writeDataSource, List<String> readDataSources) {
        Properties props = new Properties();
        props.setProperty("write-data-source-name", writeDataSource);
        props.setProperty("read-data-source-names",
                String.join(",", readDataSources));
        return props;
    }

    /**
     * 分片规则配置。
     * chat_message 按 user_id % 8 分 8 张表：
     * chat_message → chat_message_0, chat_message_1, ..., chat_message_7
     */
    private ShardingRuleConfiguration createShardingRule() {
        ShardingRuleConfiguration config = new ShardingRuleConfiguration();

        // chat_message 分片表规则
        ShardingTableRuleConfiguration chatMessageRule =
                new ShardingTableRuleConfiguration("chat_message",
                        "ms.chat_message_$->{0..7}");

        // 按 user_id % 8 分片
        chatMessageRule.setTableShardingStrategy(
                new StandardShardingStrategyConfiguration(
                        "user_id",
                        "chat_message_mod"));

        config.getTables().add(chatMessageRule);

        // 分片算法：MOD 8
        Properties modProps = new Properties();
        modProps.setProperty("sharding-count", "8");
        AlgorithmConfiguration modAlgorithm = new AlgorithmConfiguration("MOD", modProps);
        config.getShardingAlgorithms().put("chat_message_mod", modAlgorithm);

        // 默认数据源（未配置分片的表统一走 ms 读写分离组）
        config.setDefaultDataSourceName("ms");

        log.info("chat_message 分片规则: user_id % 8, 物理表 chat_message_0 ~ chat_message_7");
        return config;
    }
}