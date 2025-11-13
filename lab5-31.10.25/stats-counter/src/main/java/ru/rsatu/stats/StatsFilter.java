package ru.rsatu.stats;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

@WebFilter("/*")
public class StatsFilter implements Filter {
    private RequestStats requestStats;

    @Override
    public void init(FilterConfig filterConfig) {
        requestStats = new RequestStats();
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (request instanceof HttpServletRequest) {
            HttpServletRequest req = (HttpServletRequest) request;
            String path = req.getRequestURI();
            String ctx  = req.getContextPath();
            String statsServletPath = ctx + "/stats";
            String statsHtmlPath    = ctx + "/stats.html";

            if (!path.equals(statsServletPath) && !path.equals(statsServletPath + "/")
                    && !path.equals(statsHtmlPath) && shouldCount(path)) {
                String key = req.getMethod() + " " + path;
                requestStats.increment(key);
            }
        }

        chain.doFilter(request, response);
    }

    private boolean shouldCount(String path) {
        if (path == null) return false;
        if (path.matches(".*(\\\\.css|\\\\.js|\\\\.png|\\\\.jpg|\\\\.jpeg|\\\\.gif|\\\\.ico)$")) return false;
        return true;
    }

}
