package dao;

import entity.CopywritingRecord;
import util.DBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class CopywritingRecordDao {
    public int add(CopywritingRecord record) {
        String sql = "INSERT INTO copywriting_records("
                + "user_id, scene, mood, style, keywords, generated_content, ai_model"
                + ") VALUES(?, ?, ?, ?, ?, ?, ?)";

        try (
                Connection conn = DBUtil.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
        ) {
            if (record.getUserId() > 0) {
                ps.setInt(1, record.getUserId());
            } else {
                ps.setNull(1, Types.INTEGER);
            }

            ps.setString(2, record.getScene());
            ps.setString(3, record.getMood());
            ps.setString(4, record.getStyle());
            ps.setString(5, record.getKeywords());
            ps.setString(6, record.getGeneratedContent());
            ps.setString(7, record.getAiModel());

            if (ps.executeUpdate() > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        int id = rs.getInt(1);
                        record.setId(id);
                        return id;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    public List<CopywritingRecord> listByUserId(int userId) {
        List<CopywritingRecord> list = new ArrayList<>();
        String sql = "SELECT TOP 50 r.id, r.user_id, r.scene, r.mood, r.style, r.keywords, "
                + "r.generated_content, r.ai_model, r.create_time, "
                + "CASE WHEN f.id IS NULL THEN 0 ELSE 1 END AS favorite, "
                + "f.create_time AS favorite_time "
                + "FROM copywriting_records r "
                + "LEFT JOIN favorites f ON f.record_id = r.id AND f.user_id = ? "
                + "WHERE r.user_id = ? "
                + "ORDER BY r.create_time DESC, r.id DESC";

        try (
                Connection conn = DBUtil.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setInt(1, userId);
            ps.setInt(2, userId);

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

    public CopywritingRecord findById(int id, int userId) {
        String sql = "SELECT r.id, r.user_id, r.scene, r.mood, r.style, r.keywords, "
                + "r.generated_content, r.ai_model, r.create_time, "
                + "CASE WHEN f.id IS NULL THEN 0 ELSE 1 END AS favorite, "
                + "f.create_time AS favorite_time "
                + "FROM copywriting_records r "
                + "LEFT JOIN favorites f ON f.record_id = r.id AND f.user_id = ? "
                + "WHERE r.id = ? AND r.user_id = ?";

        try (
                Connection conn = DBUtil.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setInt(1, userId);
            ps.setInt(2, id);
            ps.setInt(3, userId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRecord(rs);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public boolean updateGeneratedContent(int id, int userId, String generatedContent) {
        String sql = "UPDATE copywriting_records SET generated_content = ? "
                + "WHERE id = ? AND user_id = ?";

        try (
                Connection conn = DBUtil.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, generatedContent);
            ps.setInt(2, id);
            ps.setInt(3, userId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    public boolean deleteById(int id, int userId) {
        if (id <= 0 || userId <= 0) {
            return false;
        }

        String deleteFavoriteSql = "DELETE FROM favorites WHERE record_id = ? AND user_id = ?";
        String deleteRecordSql = "DELETE FROM copywriting_records WHERE id = ? AND user_id = ?";

        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false);

            try (
                    PreparedStatement deleteFavorite = conn.prepareStatement(deleteFavoriteSql);
                    PreparedStatement deleteRecord = conn.prepareStatement(deleteRecordSql)
            ) {
                deleteFavorite.setInt(1, id);
                deleteFavorite.setInt(2, userId);
                deleteFavorite.executeUpdate();

                deleteRecord.setInt(1, id);
                deleteRecord.setInt(2, userId);
                boolean deleted = deleteRecord.executeUpdate() > 0;

                conn.commit();
                return deleted;
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    public int deleteAllByUserId(int userId) {
        if (userId <= 0) {
            return 0;
        }

        String countHistorySql = "SELECT COUNT(1) FROM copywriting_records WHERE user_id = ?";
        String deleteStepsSql = "DELETE s FROM copywriting_record_steps s "
                + "INNER JOIN copywriting_records r ON s.record_id = r.id "
                + "WHERE r.user_id = ?";
        String deleteRecordSql = "DELETE r FROM copywriting_records r "
                + "WHERE r.user_id = ? AND NOT EXISTS ("
                + "SELECT 1 FROM favorites f WHERE f.record_id = r.id AND f.user_id = ?"
                + ")";
        String keepFavoriteRecordSql = "UPDATE r SET r.user_id = NULL "
                + "FROM copywriting_records r "
                + "WHERE r.user_id = ? AND EXISTS ("
                + "SELECT 1 FROM favorites f WHERE f.record_id = r.id AND f.user_id = ?"
                + ")";

        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false);

            try (
                    PreparedStatement countHistory = conn.prepareStatement(countHistorySql);
                    PreparedStatement deleteSteps = conn.prepareStatement(deleteStepsSql);
                    PreparedStatement deleteRecord = conn.prepareStatement(deleteRecordSql);
                    PreparedStatement keepFavoriteRecord = conn.prepareStatement(keepFavoriteRecordSql)
            ) {
                int deletedCount = 0;
                countHistory.setInt(1, userId);
                try (ResultSet rs = countHistory.executeQuery()) {
                    if (rs.next()) {
                        deletedCount = rs.getInt(1);
                    }
                }

                deleteSteps.setInt(1, userId);
                deleteSteps.executeUpdate();

                deleteRecord.setInt(1, userId);
                deleteRecord.setInt(2, userId);
                deleteRecord.executeUpdate();

                keepFavoriteRecord.setInt(1, userId);
                keepFavoriteRecord.setInt(2, userId);
                keepFavoriteRecord.executeUpdate();

                conn.commit();
                return deletedCount;
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
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
