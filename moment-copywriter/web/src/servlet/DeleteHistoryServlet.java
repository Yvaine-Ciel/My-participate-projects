// 管理删除历史逻辑
package servlet;

import dao.CopywritingRecordDao;
import util.JsonUtil;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

@WebServlet("/api/copywriting/delete")
public class DeleteHistoryServlet extends BaseApiServlet {
    // 处理删除历史请求
    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        handle(request, response);
    }

    // 处理删除方式历史请求
    @Override
    protected void doDelete(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        handle(request, response);
    }

    // 执行删除历史逻辑
    private void handle(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        Map<String, Object> body = readBody(request);
        int id = JsonUtil.getInt(request, body, "id", 0);
        int userId = requireUserId(request, response);

        if (userId <= 0) {
            return;
        }

        if (id <= 0) {
            writeFail(response, HttpServletResponse.SC_BAD_REQUEST,
                    "记录ID不能为空");
            return;
        }

        boolean deleted = new CopywritingRecordDao().deleteById(id, userId);
        if (!deleted) {
            writeFail(response, HttpServletResponse.SC_NOT_FOUND,
                    "文案记录不存在");
            return;
        }

        writeSuccess(response, null);
    }
}
