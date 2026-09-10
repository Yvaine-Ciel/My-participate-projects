package dao;

import entity.CopywritingRecordStep;
import util.DBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class CopywritingRecordStepDao {
    public boolean add(int recordId, int stepNo, String userMessage, String generatedContent) {
        String sql = "INSERT INTO copywriting_record_steps("
                + "record_id, step_no, user_message, generated_content"
                + ") VALUES(?, ?, ?, ?)";

        try (
                Connection conn = DBUtil.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setInt(1, recordId);
            ps.setInt(2, stepNo);
            ps.setString(3, userMessage);
            ps.setString(4, generatedContent);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    public List<CopywritingRecordStep> listByRecordId(int recordId) {
        List<CopywritingRecordStep> steps = new ArrayList<>();
        String sql = "SELECT id, record_id, step_no, user_message, generated_content, create_time "
                + "FROM copywriting_record_steps WHERE record_id = ? ORDER BY step_no ASC";

        try (
                Connection conn = DBUtil.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setInt(1, recordId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    steps.add(mapStep(rs));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return steps;
    }

    public int nextStepNo(int recordId) {
        String sql = "SELECT ISNULL(MAX(step_no), 0) + 1 AS next_step_no "
                + "FROM copywriting_record_steps WHERE record_id = ?";

        try (
                Connection conn = DBUtil.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setInt(1, recordId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("next_step_no");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return 1;
    }

    private CopywritingRecordStep mapStep(ResultSet rs) throws Exception {
        CopywritingRecordStep step = new CopywritingRecordStep();
        step.setId(rs.getInt("id"));
        step.setRecordId(rs.getInt("record_id"));
        step.setStepNo(rs.getInt("step_no"));
        step.setUserMessage(rs.getString("user_message"));
        step.setGeneratedContent(rs.getString("generated_content"));
        step.setCreateTime(rs.getString("create_time"));
        return step;
    }
}
