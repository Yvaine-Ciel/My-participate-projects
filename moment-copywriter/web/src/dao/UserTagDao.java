package dao;

import util.DBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class UserTagDao {
    public List<String> listByUserId(int userId) {
        List<String> tags = new ArrayList<>();
        String sql = "SELECT name FROM user_tags WHERE user_id = ? ORDER BY create_time ASC, id ASC";

        try (
                Connection conn = DBUtil.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tags.add(rs.getString("name"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return tags;
    }

    public boolean add(int userId, String name) {
        if (exists(userId, name)) {
            return false;
        }

        String sql = "INSERT INTO user_tags(user_id, name) VALUES(?, ?)";

        try (
                Connection conn = DBUtil.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setInt(1, userId);
            ps.setString(2, name);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    public boolean remove(int userId, String name) {
        String sql = "DELETE FROM user_tags WHERE user_id = ? AND name = ?";

        try (
                Connection conn = DBUtil.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setInt(1, userId);
            ps.setString(2, name);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    private boolean exists(int userId, String name) {
        String sql = "SELECT id FROM user_tags WHERE user_id = ? AND name = ?";

        try (
                Connection conn = DBUtil.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setInt(1, userId);
            ps.setString(2, name);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }
}
