// 管理当前用户逻辑
package servlet;

import entity.User;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/api/current-user")
public class CurrentUserServlet extends BaseApiServlet {
    // 查询当前登录用户
    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        User user = currentUser(request);
        if (user == null) {
            writeFail(response, HttpServletResponse.SC_UNAUTHORIZED, "请先登录");
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("user", user);
        writeSuccess(response, data);
    }
}
