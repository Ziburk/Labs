package ru.rsatu.stats;

import javax.enterprise.inject.spi.CDI;
import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

@WebFilter("/*")
public class StatsFilter implements Filter {
    private RequestStats requestStats;

    @Override
    public void init(FilterConfig filterConfig) {
        requestStats = CDI.current().select(RequestStats.class).get();
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (request instanceof HttpServletRequest) {
            HttpServletRequest req = (HttpServletRequest) request;
            String path = req.getRequestURI();          // e.g. "/stats/stats.html" or "/stats/stats"
            String ctx  = req.getContextPath();         // e.g. "/stats"
            String statsServletPath = ctx + "/stats";   // "/stats/stats"
            String statsHtmlPath    = ctx + "/stats.html"; // "/stats/stats.html"

            // Exclude exact paths: the stats servlet and the static stats.html page
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
        // Exclude common static resource extensions
        if (path.matches(".*(\\\\.css|\\\\.js|\\\\.png|\\\\.jpg|\\\\.jpeg|\\\\.gif|\\\\.ico)$")) return false;
        return true;
    }

    @Override
    public void destroy() {}
}
