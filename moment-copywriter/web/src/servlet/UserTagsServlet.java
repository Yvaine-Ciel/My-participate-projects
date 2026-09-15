// 管理用户标签列表逻辑
package servlet;

import dao.UserTagDao;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/api/user-tags")
public class UserTagsServlet extends BaseApiServlet {
    // 查询用户标签列表
    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        int userId = requireUserId(request, response);
        if (userId <= 0) {
            return;
        }

        writeSuccess(response, new UserTagDao().listByUserId(userId));
    }
}
