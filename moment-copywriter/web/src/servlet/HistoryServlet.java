// 管理历史列表逻辑
package servlet;

import dao.CopywritingRecordDao;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/api/copywriting/history")
public class HistoryServlet extends BaseApiServlet {
    // 查询历史列表
    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        handle(request, response, new HashMap<>());
    }

    // 处理带请求体的历史查询
    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        handle(request, response, readBody(request));
    }

    // 执行历史列表查询
    private void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            Map<String, Object> body
    ) throws IOException {
        int userId = requireUserId(request, response);
        if (userId <= 0) {
            return;
        }

        writeSuccess(response, new CopywritingRecordDao().listByUserId(userId));
    }
}
