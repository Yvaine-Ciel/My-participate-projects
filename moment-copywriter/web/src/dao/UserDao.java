package dao;

import entity.User;
import util.DBUtil;
import util.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class UserDao {
    // 用户登录校验
    public User login(String phone, String password) {
        String sql = "SELECT id, username, phone, role, create_time, "
                + "password_hash, password_salt FROM users WHERE phone = ?";

        try (
                Connection conn = DBUtil.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, phone);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    boolean matched = PasswordUtil.matches(
                            password,
                            rs.getString("password_salt"),
                            rs.getString("password_hash")
                    );

                    if (matched) {
                        return mapUser(rs);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    // 注册新用户
    public boolean register(String username, String password, String phone) {
        if (usernameExists(username) || phoneExists(phone)) {
            return false;
        }

        String salt = PasswordUtil.createSalt();
        String passwordHash = PasswordUtil.hashPassword(password, salt);
        String sql = "INSERT INTO users(username, password_hash, password_salt, phone) "
                + "VALUES(?, ?, ?, ?)";

        try (
                Connection conn = DBUtil.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, username);
            ps.setString(2, passwordHash);
            ps.setString(3, salt);
            ps.setString(4, phone);

            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    // 判断手机号是否存在
    public boolean phoneExists(String phone) {
        String sql = "SELECT id FROM users WHERE phone = ?";

        try (
                Connection conn = DBUtil.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, phone);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    // 判断用户名是否存在
    public boolean usernameExists(String username) {
        String sql = "SELECT id FROM users WHERE username = ?";

        try (
                Connection conn = DBUtil.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    // 按 ID 查询用户
    public User findById(int id) {
        String sql = "SELECT id, username, phone, role, create_time FROM users WHERE id = ?";

        try (
                Connection conn = DBUtil.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapUser(rs);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    // 数据库结果转用户对象
    private User mapUser(ResultSet rs) throws Exception {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setUsername(rs.getString("username"));
        user.setPhone(rs.getString("phone"));
        user.setRole(rs.getString("role"));
        user.setCreateTime(rs.getString("create_time"));
        return user;
    }
}
