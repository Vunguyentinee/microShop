package com.microshop.context;

import java.sql.Connection;
import java.sql.SQLException;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class DBContext {
    
    // jdbc:mysql - định dạng kết nối JDBC cho MySQL
    private static final String DB_URL = "jdbc:sqlserver://db:1434;databaseName=microShop;encrypt=true;trustServerCertificate=true;";
    // Cái này là tài khoản root của MySQL server trên máy của <Hưng>
    private static final String DB_USER = "sa";
    private static final String DB_PASSWORD = "YourStrongPassword123!";

    private static HikariDataSource dataSource;

    static {
        try {
            HikariConfig config = new HikariConfig();
            config.setDriverClassName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            config.setJdbcUrl(DB_URL);
            config.setUsername(DB_USER);
            config.setPassword(DB_PASSWORD);


            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

            config.setMinimumIdle(5);
            config.setMaximumPoolSize(20);
            config.setConnectionTimeout(30000);

            dataSource = new HikariDataSource(config);

            System.out.println("Connection Pool (HikariCP) da duoc khoi tao thanh cong");
        } catch (Exception e) {
            // Tạm thời chấp nhận in hết lỗi ra console
            // Có thể tính làm log chuẩn chỉ sau
            e.printStackTrace();
            throw new RuntimeException("Loi khi tao Connection Pool: " + e.getMessage());
        }
    }

    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
    
    public static void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }   

    private DBContext() {}
}
