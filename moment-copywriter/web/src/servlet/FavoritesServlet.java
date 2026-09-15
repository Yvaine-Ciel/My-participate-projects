// 管理收藏列表逻辑
package servlet;

import dao.FavoriteDao;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/api/copywriting/favorites")
public class FavoritesServlet extends BaseApiServlet {
    // 查询收藏列表
    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        handle(request, response, new HashMap<>());
    }

    // 处理带请求体的收藏查询
    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        handle(request, response, readBody(request));
    }

    // 执行收藏列表查询
    private void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            Map<String, Object> body
    ) throws IOException {
        int userId = requireUserId(request, response);
        if (userId <= 0) {
            return;
        }

        writeSuccess(response, new FavoriteDao().listByUserId(userId));
    }
}
