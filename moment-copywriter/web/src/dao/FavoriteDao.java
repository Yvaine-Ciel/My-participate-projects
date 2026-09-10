package dao;

import entity.CopywritingRecord;
import util.DBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class FavoriteDao {
    public boolean add(int userId, int recordId) {
        if (!recordBelongsToUser(userId, recordId)) {
            return false;
        }

        if (exists(userId, recordId)) {
            return true;
        }

        String sql = "INSERT INTO favorites(user_id, record_id) VALUES(?, ?)";

        try (
                Connection conn = DBUtil.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setInt(1, userId);
            ps.setInt(2, recordId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    public boolean remove(int userId, int recordId) {
        if (userId <= 0 || recordId <= 0) {
            return false;
        }

        if (!recordBelongsToUser(userId, recordId) && !exists(userId, recordId)) {
            return false;
        }

        String deleteFavoriteSql = "DELETE FROM favorites WHERE user_id = ? AND record_id = ?";
        String deleteOrphanRecordSql = "DELETE FROM copywriting_records "
                + "WHERE id = ? AND user_id IS NULL "
                + "AND NOT EXISTS (SELECT 1 FROM favorites WHERE record_id = ?)";

        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false);

            try (
                    PreparedStatement deleteFavorite = conn.prepareStatement(deleteFavoriteSql);
                    PreparedStatement deleteOrphanRecord = conn.prepareStatement(deleteOrphanRecordSql)
            ) {
                deleteFavorite.setInt(1, userId);
                deleteFavorite.setInt(2, recordId);
                deleteFavorite.executeUpdate();

                deleteOrphanRecord.setInt(1, recordId);
                deleteOrphanRecord.setInt(2, recordId);
                deleteOrphanRecord.executeUpdate();

                conn.commit();
                return true;
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    public List<CopywritingRecord> listByUserId(int userId) {
        List<CopywritingRecord> list = new ArrayList<>();
        String sql = "SELECT TOP 50 r.id, r.user_id, r.scene, r.mood, r.style, r.keywords, "
                + "r.generated_content, r.ai_model, r.create_time, "
                + "1 AS favorite, f.create_time AS favorite_time "
                + "FROM favorites f "
                + "INNER JOIN copywriting_records r ON r.id = f.record_id "
                + "WHERE f.user_id = ? "
                + "ORDER BY f.create_time DESC, f.id DESC";

        try (
                Connection conn = DBUtil.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRecord(rs));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    public boolean exists(int userId, int recordId) {
        String sql = "SELECT id FROM favorites WHERE user_id = ? AND record_id = ?";

        try (
                Connection conn = DBUtil.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setInt(1, userId);
            ps.setInt(2, recordId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    private boolean recordBelongsToUser(int userId, int recordId) {
        if (userId <= 0 || recordId <= 0) {
            return false;
        }

        String sql = "SELECT id FROM copywriting_records WHERE id = ? AND user_id = ?";

        try (
                Connection conn = DBUtil.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setInt(1, recordId);
            ps.setInt(2, userId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    private CopywritingRecord mapRecord(ResultSet rs) throws Exception {
        CopywritingRecord record = new CopywritingRecord();
        record.setId(rs.getInt("id"));
        record.setUserId(rs.getInt("user_id"));
        record.setScene(rs.getString("scene"));
        record.setMood(rs.getString("mood"));
        record.setStyle(rs.getString("style"));
        record.setKeywords(rs.getString("keywords"));
        record.setGeneratedContent(rs.getString("generated_content"));
        record.setAiModel(rs.getString("ai_model"));
        record.setCreateTime(rs.getString("create_time"));
        record.setFavorite(rs.getInt("favorite") == 1);
        record.setFavoriteTime(rs.getString("favorite_time"));
        return record;
    }
}
