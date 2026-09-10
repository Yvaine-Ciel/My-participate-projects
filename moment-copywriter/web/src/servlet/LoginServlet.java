package servlet;

import dao.UserDao;
import entity.User;
import util.JsonUtil;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/api/login")
public class LoginServlet extends BaseApiServlet {
    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        Map<String, Object> body = readBody(request);
        String phone = JsonUtil.getString(request, body, "phone");
        String password = JsonUtil.getString(request, body, "password");

        if (phone == null || password == null) {
            writeFail(response, HttpServletResponse.SC_BAD_REQUEST,
                    "phone and password are required");
            return;
        }

        User user = new UserDao().login(phone, password);
        if (user == null) {
            writeFail(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "Invalid phone or password");
            return;
        }

        HttpSession session = request.getSession();
        session.setAttribute("loginUser", user);

        Map<String, Object> data = new HashMap<>();
        data.put("user", user);
        writeSuccess(response, data);
    }
}
