package com.example.tsdb;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * TDengine 连接池，支持读写分离 + 故障自动切换。
 *
 * 架构：
 * - 1 Master + 2 Slave 节点
 * - 写操作只走 Master
 * - 读操作随机选择 Slave（未配置 Slave 时回退到 Master）
 * - 心跳检测每 30s 执行一次，自动隔离故障节点
 * - 故障节点恢复后自动重新加入池
 */
@Slf4j
@Component
public class TSDBConnectionPool {

    @Value("${tdengine.master.url:jdbc:TAOS://localhost:6030/ai_health}")
    private String masterUrl;

    @Value("${tdengine.master.username:root}")
    private String masterUsername;

    @Value("${tdengine.master.password:taosdata}")
    private String masterPassword;

    @Value("${tdengine.slave1.url:}")
    private String slave1Url;

    @Value("${tdengine.slave1.username:root}")
    private String slave1Username;

    @Value("${tdengine.slave1.password:taosdata}")
    private String slave1Password;

    @Value("${tdengine.slave2.url:}")
    private String slave2Url;

    @Value("${tdengine.slave2.username:root}")
    private String slave2Username;

    @Value("${tdengine.slave2.password:taosdata}")
    private String slave2Password;

    @Value("${tdengine.pool.max-write-connections:5}")
    private int maxWriteConnections;

    @Value("${tdengine.pool.max-read-connections-per-node:5}")
    private int maxReadConnectionsPerNode;

    @Value("${tdengine.pool.heartbeat-interval-seconds:30}")
    private int heartbeatIntervalSeconds;

    @Value("${tdengine.pool.connection-timeout-millis:5000}")
    private int connectionTimeoutMillis;

    @Value("${tdengine.enabled:false}")
    private boolean enabled;

    // ===== 写连接池（Master 专用） =====
    private final CopyOnWriteArrayList<PooledConnection> writePool = new CopyOnWriteArrayList<>();
    private final AtomicInteger writeIndex = new AtomicInteger(0);
    private volatile boolean masterAvailable = false;

    // ===== 读连接池 =====
    private final List<NodeInfo> readNodes = new CopyOnWriteArrayList<>();
    private final AtomicInteger readIndex = new AtomicInteger(0);
    private final ReentrantReadWriteLock readLock = new ReentrantReadWriteLock();

    private ScheduledExecutorService heartbeatExecutor;

    static {
        try {
            Class.forName("com.taosdata.jdbc.TSDBDriver");
        } catch (ClassNotFoundException e) {
            log.warn("TDengine JDBC driver not found, TSDB features will be disabled");
        }
    }

    @PostConstruct
    public void init() {
        if (!enabled) {
            log.info("TDengine connection pool is disabled");
            return;
        }

        log.info("Initializing TDengine connection pool...");

        // 初始化写连接（Master）
        initWritePool();

        // 初始化读连接（Slaves + Master fallback）
        initReadNodes();

        // 启动心跳检测
        startHeartbeat();

        log.info("TDengine connection pool initialized. WritePool={} ReadNodes={}",
                writePool.size(), readNodes.size());
    }

    @PreDestroy
    public void destroy() {
        if (heartbeatExecutor != null) {
            heartbeatExecutor.shutdownNow();
        }
        closeAll(writePool);
        for (NodeInfo node : readNodes) {
            closeAll(node.connections);
        }
        log.info("TDengine connection pool destroyed");
    }

    // ==================== 公共 API ====================

    /**
     * 获取写连接（仅 Master）。
     */
    public Connection getWriteConnection() throws SQLException {
        if (!enabled || !masterAvailable) {
            throw new SQLException("TDengine master is not available");
        }

        int attempts = writePool.size();
        for (int i = 0; i < attempts; i++) {
            int idx = Math.abs(writeIndex.incrementAndGet() % writePool.size());
            PooledConnection pc = writePool.get(idx);
            if (pc != null && pc.isValid()) {
                return pc.getConnection();
            }
        }
        throw new SQLException("No available write connection in TDengine pool");
    }

    /**
     * 获取读连接（优先 Slave，无 Slave 回退到 Master）。
     */
    public Connection getReadConnection() throws SQLException {
        if (!enabled) {
            throw new SQLException("TDengine is disabled");
        }

        readLock.readLock().lock();
        try {
            List<NodeInfo> availableNodes = readNodes.stream()
                    .filter(n -> n.available)
                    .toList();

            if (availableNodes.isEmpty()) {
                // 全部不可用，尝试回退到 Master 读
                if (masterAvailable) {
                    return getWriteConnection();
                }
                throw new SQLException("No available TDengine read node");
            }

            int attempts = availableNodes.size();
            for (int i = 0; i < attempts; i++) {
                int idx = Math.abs(readIndex.incrementAndGet() % availableNodes.size());
                NodeInfo node = availableNodes.get(idx);
                for (PooledConnection pc : node.connections) {
                    if (pc.isValid()) {
                        return pc.getConnection();
                    }
                }
            }
            throw new SQLException("No valid read connection in TDengine pool");
        } finally {
            readLock.readLock().unlock();
        }
    }

    /**
     * 执行写入 SQL（INSERT）。
     */
    public void executeWrite(String sql, List<Object> params) throws SQLException {
        try (Connection conn = getWriteConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            ps.executeUpdate();
        }
    }

    /**
     * 执行查询 SQL（SELECT），返回结果列表。
     */
    public List<Object[]> executeQuery(String sql, List<Object> params) throws SQLException {
        List<Object[]> results = new ArrayList<>();
        try (Connection conn = getReadConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (params != null) {
                for (int i = 0; i < params.size(); i++) {
                    ps.setObject(i + 1, params.get(i));
                }
            }
            try (ResultSet rs = ps.executeQuery()) {
                int columnCount = rs.getMetaData().getColumnCount();
                while (rs.next()) {
                    Object[] row = new Object[columnCount];
                    for (int i = 0; i < columnCount; i++) {
                        row[i] = rs.getObject(i + 1);
                    }
                    results.add(row);
                }
            }
        }
        return results;
    }

    /**
     * 查询单值（如 AVG、COUNT）。
     */
    public BigDecimal querySingleValue(String sql, List<Object> params) throws SQLException {
        try (Connection conn = getReadConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (params != null) {
                for (int i = 0; i < params.size(); i++) {
                    ps.setObject(i + 1, params.get(i));
                }
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    double val = rs.getDouble(1);
                    return rs.wasNull() ? null : BigDecimal.valueOf(val);
                }
            }
        }
        return null;
    }

    /**
     * 批量写入。
     */
    public void executeBatchWrite(String sql, List<List<Object>> batchParams) throws SQLException {
        try (Connection conn = getWriteConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (List<Object> params : batchParams) {
                for (int i = 0; i < params.size(); i++) {
                    ps.setObject(i + 1, params.get(i));
                }
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    // ==================== 时序数据查询方法 ====================

    /**
     * 查询用户某天的血糖平均值。
     */
    public BigDecimal getDailyAvgBloodSugar(Long userId, LocalDate date) {
        try {
            return querySingleValue(
                    "SELECT AVG(glucose_value) FROM blood_sugar WHERE user_id = ? AND record_date = ?",
                    List.of(userId, date.toString()));
        } catch (SQLException e) {
            log.warn("TDengine query blood_sugar avg failed userId={} date={}", userId, date, e);
            return null;
        }
    }

    /**
     * 查询用户指定天数的血糖趋势数据。
     */
    public List<Object[]> getBloodSugarTrend(Long userId, int days) {
        try {
            return executeQuery(
                    "SELECT record_date, AVG(glucose_value), MIN(glucose_value), MAX(glucose_value) " +
                    "FROM blood_sugar WHERE user_id = ? AND record_date >= ? " +
                    "GROUP BY record_date ORDER BY record_date ASC",
                    List.of(userId, LocalDate.now().minusDays(days - 1).toString()));
        } catch (SQLException e) {
            log.warn("TDengine query blood_sugar trend failed userId={} days={}", userId, days, e);
            return List.of();
        }
    }

    /**
     * 查询用户年度血糖趋势（按月汇总）。
     */
    public List<Object[]> getBloodSugarYearTrend(Long userId) {
        try {
            String startDate = LocalDate.now().minusDays(365).toString();
            return executeQuery(
                    "SELECT _wstart, AVG(glucose_value), MIN(glucose_value), MAX(glucose_value) " +
                    "FROM blood_sugar WHERE user_id = ? AND record_date >= ? " +
                    "INTERVAL(1w) ORDER BY _wstart ASC",
                    List.of(userId, startDate));
        } catch (SQLException e) {
            log.warn("TDengine query blood_sugar yearly trend failed userId={}", userId, e);
            return List.of();
        }
    }

    /**
     * 插入血糖记录到 TDengine。
     */
    public void insertBloodSugar(Long userId, LocalDate recordDate, String recordTime,
                                  String measureType, BigDecimal glucoseValue,
                                  String note, Integer abnormalFlag) {
        try {
            executeWrite(
                    "INSERT INTO blood_sugar (user_id, record_date, record_time, measure_type, glucose_value, note, abnormal_flag, create_time) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    List.of(userId, recordDate.toString(),
                            recordTime,
                            measureType, glucoseValue.doubleValue(),
                            note != null ? note : "", abnormalFlag,
                            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
        } catch (SQLException e) {
            log.warn("TDengine insert blood_sugar failed userId={} date={}", userId, recordDate, e);
        }
    }

    /**
     * 获取每日平均体重。
     */
    public BigDecimal getDailyAvgWeight(Long userId, LocalDate date) {
        try {
            return querySingleValue(
                    "SELECT AVG(weight) FROM health_record WHERE user_id = ? AND ts >= ? AND ts < ?",
                    List.of(userId,
                            date.atStartOfDay().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                            date.plusDays(1).atStartOfDay().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
        } catch (SQLException e) {
            log.warn("TDengine query weight avg failed userId={} date={}", userId, date, e);
            return null;
        }
    }

    /**
     * 获取每日运动总卡路里。
     */
    public BigDecimal getDailyExerciseCalories(Long userId, LocalDate date) {
        try {
            return querySingleValue(
                    "SELECT SUM(calories_burned) FROM exercise_record WHERE user_id = ? AND ts >= ? AND ts < ?",
                    List.of(userId,
                            date.atStartOfDay().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                            date.plusDays(1).atStartOfDay().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
        } catch (SQLException e) {
            log.warn("TDengine query exercise calories failed userId={} date={}", userId, date, e);
            return null;
        }
    }

    /**
     * 获取每日饮食总卡路里。
     */
    public BigDecimal getDailyDietCalories(Long userId, LocalDate date) {
        try {
            return querySingleValue(
                    "SELECT SUM(calories_consumed) FROM diet_record WHERE user_id = ? AND ts >= ? AND ts < ?",
                    List.of(userId,
                            date.atStartOfDay().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                            date.plusDays(1).atStartOfDay().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
        } catch (SQLException e) {
            log.warn("TDengine query diet calories failed userId={} date={}", userId, date, e);
            return null;
        }
    }

    /**
     * 查询用户某段时间的打卡天数。
     */
    public long getCheckinDays(Long userId, LocalDate start, LocalDate end) {
        try {
            BigDecimal result = querySingleValue(
                    "SELECT COUNT(*) FROM daily_checkin WHERE user_id = ? AND check_date >= ? AND check_date <= ?",
                    List.of(userId, start.toString(), end.toString()));
            return result != null ? result.longValue() : 0;
        } catch (SQLException e) {
            log.warn("TDengine query checkin days failed userId={}", userId, e);
            return 0;
        }
    }

    /**
     * 检查连接池是否可用。
     */
    public boolean isAvailable() {
        return enabled && masterAvailable;
    }

    /**
     * 获取连接池健康状态。
     */
    public String getHealthStatus() {
        if (!enabled) return "DISABLED";
        StringBuilder sb = new StringBuilder();
        sb.append("Master: ").append(masterAvailable ? "UP" : "DOWN");
        sb.append(" | WritePool: ").append(writePool.size());
        for (NodeInfo node : readNodes) {
            sb.append(" | ").append(node.name).append(": ")
              .append(node.available ? "UP" : "DOWN")
              .append(" (connections=").append(node.connections.size()).append(")");
        }
        return sb.toString();
    }

    // ==================== 内部实现 ====================

    private void initWritePool() {
        for (int i = 0; i < maxWriteConnections; i++) {
            try {
                Connection conn = createConnection(masterUrl, masterUsername, masterPassword);
                writePool.add(new PooledConnection(conn));
            } catch (SQLException e) {
                log.warn("Failed to create write connection to master ({}): {}", i, e.getMessage());
            }
        }
        masterAvailable = !writePool.isEmpty();
        if (!masterAvailable) {
            log.error("TDengine master ({}) is not reachable!", masterUrl);
        }
    }

    private void initReadNodes() {
        // Slave1
        if (slave1Url != null && !slave1Url.isBlank()) {
            registerReadNode("Slave1", slave1Url, slave1Username, slave1Password);
        }
        // Slave2
        if (slave2Url != null && !slave2Url.isBlank()) {
            registerReadNode("Slave2", slave2Url, slave2Username, slave2Password);
        }
        // 如果没有配置 Slave，将 Master 也加入读节点列表
        if (readNodes.isEmpty()) {
            log.info("No slaves configured, using master ({}) for reads as well", masterUrl);
            List<PooledConnection> conns = new CopyOnWriteArrayList<>();
            for (int i = 0; i < maxReadConnectionsPerNode; i++) {
                try {
                    Connection conn = createConnection(masterUrl, masterUsername, masterPassword);
                    conns.add(new PooledConnection(conn));
                } catch (SQLException e) {
                    log.warn("Failed to create read connection to master: {}", e.getMessage());
                }
            }
            NodeInfo node = new NodeInfo("Master-Read", masterUrl, conns);
            node.available = !conns.isEmpty();
            readNodes.add(node);
        }
    }

    private void registerReadNode(String name, String url, String username, String password) {
        List<PooledConnection> conns = new CopyOnWriteArrayList<>();
        for (int i = 0; i < maxReadConnectionsPerNode; i++) {
            try {
                Connection conn = createConnection(url, username, password);
                conns.add(new PooledConnection(conn));
            } catch (SQLException e) {
                log.warn("Failed to create read connection to {} ({})", name, e.getMessage());
            }
        }
        NodeInfo node = new NodeInfo(name, url, username, password, conns);
        node.available = !conns.isEmpty();
        readNodes.add(node);
        log.info("Registered read node: {} ({}), available={}, connections={}",
                name, url, node.available, conns.size());
    }

    private Connection createConnection(String url, String username, String password) throws SQLException {
        java.util.Properties props = new java.util.Properties();
        props.setProperty("user", username);
        props.setProperty("password", password);
        props.setProperty("charset", "UTF-8");
        props.setProperty("locale", "en_US.UTF-8");
        props.setProperty("timezone", "Asia/Shanghai");
        DriverManager.setLoginTimeout(connectionTimeoutMillis / 1000);
        return DriverManager.getConnection(url, props);
    }

    private void startHeartbeat() {
        heartbeatExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "tsdb-heartbeat");
            t.setDaemon(true);
            return t;
        });

        heartbeatExecutor.scheduleAtFixedRate(this::doHeartbeat,
                heartbeatIntervalSeconds, heartbeatIntervalSeconds, TimeUnit.SECONDS);
    }

    private void doHeartbeat() {
        // 检查 Master
        checkMasterHealth();
        // 检查读节点
        checkReadNodesHealth();
    }

    private void checkMasterHealth() {
        boolean wasAvailable = masterAvailable;
        boolean nowAvailable = testConnection(masterUrl, masterUsername, masterPassword);

        if (wasAvailable && !nowAvailable) {
            log.error("TDengine master ({}) is DOWN!", masterUrl);
            masterAvailable = false;
            // 清理所有写连接
            closeAll(writePool);
            writePool.clear();
        } else if (!wasAvailable && nowAvailable) {
            log.warn("TDengine master ({}) recovered, re-initializing write pool", masterUrl);
            initWritePool();
        } else if (wasAvailable && nowAvailable) {
            // 健康检查通过，清理失效连接
            refreshPool(writePool, masterUrl, masterUsername, masterPassword);
        }
    }

    private void checkReadNodesHealth() {
        readLock.writeLock().lock();
        try {
            for (NodeInfo node : readNodes) {
                boolean wasAvailable = node.available;
                boolean nowAvailable = testConnection(node.url, node.username, node.password);

                if (wasAvailable && !nowAvailable) {
                    log.error("TDengine read node {} ({}) is DOWN!", node.name, node.url);
                    node.available = false;
                    closeAll(node.connections);
                    node.connections.clear();
                } else if (!wasAvailable && nowAvailable) {
                    log.warn("TDengine read node {} ({}) recovered", node.name, node.url);
                    // 重新建立连接
                    for (int i = 0; i < maxReadConnectionsPerNode; i++) {
                        try {
                            Connection conn = createConnection(node.url, node.username, node.password);
                            node.connections.add(new PooledConnection(conn));
                        } catch (SQLException e) {
                            log.warn("Failed to rebuild connection to {}: {}", node.name, e.getMessage());
                        }
                    }
                    node.available = !node.connections.isEmpty();
                } else if (wasAvailable && nowAvailable) {
                    refreshPool(node.connections, node.url, node.username, node.password);
                }
            }
        } finally {
            readLock.writeLock().unlock();
        }
    }

    private void refreshPool(CopyOnWriteArrayList<PooledConnection> pool,
                              String url, String username, String password) {
        List<PooledConnection> toRemove = new ArrayList<>();
        for (PooledConnection pc : pool) {
            if (!pc.isValid()) {
                toRemove.add(pc);
                try { pc.connection.close(); } catch (SQLException ignored) {}
            }
        }
        pool.removeAll(toRemove);

        // 补充连接
        int targetSize = pool == writePool ? maxWriteConnections : maxReadConnectionsPerNode;
        while (pool.size() < targetSize) {
            try {
                Connection conn = createConnection(url, username, password);
                pool.add(new PooledConnection(conn));
            } catch (SQLException e) {
                log.warn("Failed to replenish connection: {}", e.getMessage());
                break;
            }
        }
    }

    private boolean testConnection(String url, String username, String password) {
        try {
            java.util.Properties props = new java.util.Properties();
            props.setProperty("user", username);
            props.setProperty("password", password);
            props.setProperty("charset", "UTF-8");
            props.setProperty("locale", "en_US.UTF-8");
            props.setProperty("timezone", "Asia/Shanghai");
            DriverManager.setLoginTimeout(3);
            try (Connection conn = DriverManager.getConnection(url, props);
                 Statement stmt = conn.createStatement()) {
                stmt.setQueryTimeout(2);
                stmt.execute("SELECT SERVER_VERSION()");
                return true;
            }
        } catch (SQLException e) {
            log.debug("TDengine health check failed for {}: {}", url, e.getMessage());
            return false;
        }
    }

    private void closeAll(CopyOnWriteArrayList<PooledConnection> pool) {
        for (PooledConnection pc : pool) {
            try { pc.connection.close(); } catch (SQLException ignored) {}
        }
        pool.clear();
    }

    // ==================== 内部类 ====================

    private static class PooledConnection {
        final Connection connection;

        PooledConnection(Connection connection) {
            this.connection = connection;
        }

        Connection getConnection() {
            return connection;
        }

        boolean isValid() {
            try {
                return connection != null && !connection.isClosed()
                        && connection.isValid(2);
            } catch (SQLException e) {
                return false;
            }
        }
    }

    private static class NodeInfo {
        final String name;
        final String url;
        final String username;
        final String password;
        final CopyOnWriteArrayList<PooledConnection> connections;
        volatile boolean available;

        NodeInfo(String name, String url, String username, String password,
                 CopyOnWriteArrayList<PooledConnection> connections) {
            this.name = name;
            this.url = url;
            this.username = username;
            this.password = password;
            this.connections = connections;
            this.available = false;
        }
    }
}