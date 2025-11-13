package ru.rsatu.stats;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import com.google.gson.Gson;

@WebServlet("/stats")
public class StatsServlet extends HttpServlet {
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        RequestStats requestStats = (RequestStats) getServletContext()
                .getAttribute("requestStats");

        String format = req.getParameter("format");
        if ("json".equalsIgnoreCase(format)) {
            resp.setContentType("application/json; charset=UTF-8");
            resp.getWriter().write(gson.toJson(requestStats.snapshot()));
        } else {
            req.getRequestDispatcher("/stats.html").forward(req, resp);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        RequestStats requestStats = (RequestStats) getServletContext()
                .getAttribute("requestStats");

        if ("reset".equals(req.getParameter("action"))) {
            requestStats.resetAll();
        }
        resp.sendRedirect(req.getContextPath() + "/stats");
    }
}
