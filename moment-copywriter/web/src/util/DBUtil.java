package util;

import java.sql.Connection;
import java.sql.DriverManager;

public class DBUtil {
    private static final String DRIVER =
            "com.microsoft.sqlserver.jdbc.SQLServerDriver";

    private static final String DEFAULT_URL =
            "jdbc:sqlserver://localhost\\SQL2022;"
                    + "databaseName=MomentCopywriter;"
                    + "encrypt=true;"
                    + "trustServerCertificate=true";

    private static final String URL =
            System.getenv().getOrDefault("MOMENT_DB_URL", DEFAULT_URL);

    private static final String USER =
            System.getenv().getOrDefault("MOMENT_DB_USER", "sa");

    private static final String PASSWORD =
            System.getenv("MOMENT_DB_PASSWORD");

    // 获取数据库连接
    public static Connection getConnection() throws Exception {
        if (isBlank(PASSWORD)) {
            throw new IllegalStateException(
                    "缺少 MOMENT_DB_PASSWORD 环境变量"
            );
        }

        Class.forName(DRIVER);

        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    // 判断字符串为空
    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
