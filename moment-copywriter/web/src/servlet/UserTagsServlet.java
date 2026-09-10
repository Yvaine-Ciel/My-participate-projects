package servlet;

import dao.UserTagDao;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/api/user-tags")
public class UserTagsServlet extends BaseApiServlet {
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
