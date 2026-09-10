package servlet;

import dao.CopywritingRecordDao;
import dao.CopywritingRecordStepDao;
import util.JsonUtil;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/api/copywriting/steps")
public class CopywritingRecordStepsServlet extends BaseApiServlet {
    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        handle(request, response, new HashMap<>());
    }

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        handle(request, response, readBody(request));
    }

    private void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            Map<String, Object> body
    ) throws IOException {
        int userId = requireUserId(request, response);
        int recordId = recordId(request, body);

        if (userId <= 0) {
            return;
        }

        if (recordId <= 0) {
            writeFail(response, HttpServletResponse.SC_BAD_REQUEST,
                    "recordId is required");
            return;
        }

        if (new CopywritingRecordDao().findById(recordId, userId) == null) {
            writeFail(response, HttpServletResponse.SC_NOT_FOUND,
                    "Record not found");
            return;
        }

        writeSuccess(response, new CopywritingRecordStepDao().listByRecordId(recordId));
    }

    private int recordId(HttpServletRequest request, Map<String, Object> body) {
        int id = JsonUtil.getInt(request, body, "recordId", 0);
        if (id > 0) {
            return id;
        }

        return JsonUtil.getInt(request, body, "id", 0);
    }
}
