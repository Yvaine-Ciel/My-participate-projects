package servlet;

import dao.UserTagDao;
import util.JsonUtil;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/api/user-tags/add")
public class AddUserTagServlet extends BaseApiServlet {
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

        if (!validName(name)) {
            writeFail(response, HttpServletResponse.SC_BAD_REQUEST,
                    "标签不能为空且不能超过12个字");
            return;
        }

        UserTagDao dao = new UserTagDao();
        boolean success = dao.add(userId, name);
        if (!success) {
            writeFail(response, HttpServletResponse.SC_BAD_REQUEST,
                    "标签已存在");
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("tags", dao.listByUserId(userId));
        writeSuccess(response, data);
    }

    private boolean validName(String name) {
        return name != null && name.length() <= 12;
    }
}
