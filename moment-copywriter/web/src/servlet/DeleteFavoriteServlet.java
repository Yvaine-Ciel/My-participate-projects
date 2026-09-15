// 管理取消收藏逻辑
package servlet;

import dao.FavoriteDao;
import util.JsonUtil;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/api/copywriting/favorite/delete")
public class DeleteFavoriteServlet extends BaseApiServlet {
    // 处理取消收藏请求
    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        handle(request, response);
    }

    // 处理删除方式取消收藏
    @Override
    protected void doDelete(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        handle(request, response);
    }

    // 执行取消收藏逻辑
    private void handle(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        Map<String, Object> body = readBody(request);
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

        boolean success = new FavoriteDao().remove(userId, recordId);
        if (!success) {
            writeFail(response, HttpServletResponse.SC_NOT_FOUND,
                    "Record not found");
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("recordId", recordId);
        data.put("favorite", false);
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
}
