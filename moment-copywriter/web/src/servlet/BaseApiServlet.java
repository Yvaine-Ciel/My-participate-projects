// 管理 API 通用逻辑
package servlet;

import entity.User;
import util.CorsUtil;
import util.JsonUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Map;

public abstract class BaseApiServlet extends HttpServlet {
    // 处理跨域预检请求
    @Override
    protected void doOptions(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        CorsUtil.allowCors(request, response);
        response.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }

    // 读取请求体
    protected Map<String, Object> readBody(HttpServletRequest request)
            throws IOException {
        return JsonUtil.readJsonObject(request);
    }

    // 写入成功响应
    protected void writeSuccess(HttpServletResponse response, Object data)
            throws IOException {
        JsonUtil.writeJson(response, JsonUtil.success(data));
    }

    // 写入失败响应
    protected void writeFail(HttpServletResponse response, int status, String message)
            throws IOException {
        response.setStatus(status);
        JsonUtil.writeJson(response, JsonUtil.fail(message));
    }

    // 设置请求通用处理
    @Override
    protected void service(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        CorsUtil.allowCors(request, response);
        super.service(request, response);
    }

    // 获取当前登录用户
    protected User currentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            Object loginUser = session.getAttribute("loginUser");
            if (loginUser instanceof User) {
                return (User) loginUser;
            }
        }

        return null;
    }

    // 获取当前用户 ID
    protected int currentUserId(HttpServletRequest request) {
        User user = currentUser(request);
        return user == null ? 0 : user.getId();
    }

    // 要求用户已登录
    protected int requireUserId(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        int userId = currentUserId(request);
        if (userId <= 0) {
            writeFail(response, HttpServletResponse.SC_UNAUTHORIZED, "请先登录");
        }

        return userId;
    }
}
