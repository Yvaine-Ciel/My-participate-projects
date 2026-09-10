package servlet;

import dao.CopywritingRecordDao;
import dao.CopywritingRecordStepDao;
import entity.CopywritingRecord;
import util.AiClient;
import util.JsonUtil;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/api/copywriting/optimize")
public class OptimizeCopywritingServlet extends BaseApiServlet {
    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        Map<String, Object> body = readBody(request);
        int userId = requireUserId(request, response);
        int recordId = recordId(request, body);
        String message = JsonUtil.getString(request, body, "message");

        if (userId <= 0) {
            return;
        }

        if (recordId <= 0) {
            writeFail(response, HttpServletResponse.SC_BAD_REQUEST,
                    "recordId is required");
            return;
        }

        if (message == null) {
            writeFail(response, HttpServletResponse.SC_BAD_REQUEST,
                    "优化要求不能为空");
            return;
        }

        CopywritingRecordDao recordDao = new CopywritingRecordDao();
        CopywritingRecord record = recordDao.findById(recordId, userId);
        if (record == null) {
            writeFail(response, HttpServletResponse.SC_NOT_FOUND,
                    "Record not found");
            return;
        }

        AiClient aiClient = new AiClient();
        String content;

        try {
            content = aiClient.optimizeMomentCopywriting(
                    record.getScene(),
                    record.getMood(),
                    record.getStyle(),
                    record.getKeywords(),
                    record.getGeneratedContent(),
                    message
            );
        } catch (IllegalStateException e) {
            writeFail(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    e.getMessage());
            return;
        } catch (Exception e) {
            e.printStackTrace();
            writeFail(response, HttpServletResponse.SC_BAD_GATEWAY,
                    "AI service request failed");
            return;
        }

        CopywritingRecordStepDao stepDao = new CopywritingRecordStepDao();
        int stepNo = stepDao.nextStepNo(recordId);
        stepDao.add(recordId, stepNo, message, content);
        recordDao.updateGeneratedContent(recordId, userId, content);

        Map<String, Object> data = new HashMap<>();
        data.put("recordId", recordId);
        data.put("content", content);
        data.put("stepNo", stepNo);
        data.put("steps", stepDao.listByRecordId(recordId));
        writeSuccess(response, data);
    }

    private int recordId(HttpServletRequest request, Map<String, Object> body) {
        int id = JsonUtil.getInt(request, body, "recordId", 0);
        if (id > 0) {
            return id;
        }

        return JsonUtil.getInt(request, body, "id", 0);
    }
}
