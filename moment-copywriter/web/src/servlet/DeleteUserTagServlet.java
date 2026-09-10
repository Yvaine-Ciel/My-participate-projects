package servlet;

import dao.UserTagDao;
import util.JsonUtil;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/api/user-tags/delete")
public class DeleteUserTagServlet extends BaseApiServlet {
    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        Map<String, Object> body = readBody(request);
        int userId = requireUserId(request, response);
        String name = JsonUtil.getString(request, body, "name");

        if (userId <= 0) {
            return;
        }

        if (name == null) {
            writeFail(response, HttpServletResponse.SC_BAD_REQUEST,
                    "标签不能为空");
            return;
        }

        UserTagDao dao = new UserTagDao();
        dao.remove(userId, name);

        Map<String, Object> data = new HashMap<>();
        data.put("tags", dao.listByUserId(userId));
        writeSuccess(response, data);
    }
}
