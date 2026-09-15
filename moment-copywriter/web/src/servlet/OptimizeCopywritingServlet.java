// 管理文案优化逻辑
package servlet;

import dao.CopywritingRecordDao;
import dao.CopywritingRecordStepDao;
import dao.UserTagDao;
import entity.CopywritingRecord;
import util.AiClient;
import util.JsonUtil;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/api/copywriting/optimize")
public class OptimizeCopywritingServlet extends BaseApiServlet {
    // 处理文案优化请求
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
                    "文案记录ID不能为空");
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
                    "文案记录不存在");
            return;
        }

        AiClient aiClient = new AiClient();
        String content;
        String promptKeywords = appendUserTags(
                record.getKeywords(),
                new UserTagDao().listByUserId(userId)
        );

        try {
            content = aiClient.optimizeAiCopywriting(
                    record.getScene(),
                    record.getMood(),
                    record.getStyle(),
                    promptKeywords,
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
                    "AI 服务请求失败");
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

    // 读取文案记录 ID
    private int recordId(HttpServletRequest request, Map<String, Object> body) {
        int id = JsonUtil.getInt(request, body, "recordId", 0);
        if (id > 0) {
            return id;
        }

        return JsonUtil.getInt(request, body, "id", 0);
    }

    // 拼接用户标签关键词
    private String appendUserTags(String keywords, List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return keywords;
        }

        if (keywords != null && keywords.contains("用户标签")) {
            return keywords;
        }

        String tagText = String.join("、", tags);
        if (keywords == null || keywords.trim().isEmpty()) {
            return "用户标签：" + tagText;
        }

        return keywords.trim() + "；用户标签：" + tagText;
    }
}
