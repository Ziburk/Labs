package ru.rsatu.stats;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.Map;
import com.google.gson.Gson;

@WebServlet("/stats")
public class StatsServlet extends HttpServlet {

    @Inject
    private RequestStats requestStats;

    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String format = req.getParameter("format");
        if ("json".equalsIgnoreCase(format)) {
            resp.setContentType("application/json; charset=UTF-8");
            resp.getWriter().write(gson.toJson(requestStats.snapshot()));
        } else {
            // forward to static HTML page
            req.getRequestDispatcher("/stats.html").forward(req, resp);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        if ("reset".equals(req.getParameter("action"))) {
            requestStats.resetAll();
        }
        resp.sendRedirect(req.getContextPath() + "/stats");
    }
}
