package servlet;

import dao.UserDao;
import util.JsonUtil;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/api/register")
public class RegisterServlet extends BaseApiServlet {
    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        Map<String, Object> body = readBody(request);
        String username = JsonUtil.getString(request, body, "username");
        String password = JsonUtil.getString(request, body, "password");
        String phone = JsonUtil.getString(request, body, "phone");

        if (username == null || password == null || phone == null) {
            writeFail(response, HttpServletResponse.SC_BAD_REQUEST,
                    "用户名、手机号和密码不能为空");
            return;
        }

        if (!phone.matches("^1[3-9]\\d{9}$")) {
            writeFail(response, HttpServletResponse.SC_BAD_REQUEST,
                    "请输入正确的11位手机号");
            return;
        }

        UserDao userDao = new UserDao();
        boolean success = userDao.register(username, password, phone);

        if (!success) {
            writeFail(response, HttpServletResponse.SC_BAD_REQUEST,
                    "用户名或手机号已存在");
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("username", username);
        writeSuccess(response, data);
    }
}
