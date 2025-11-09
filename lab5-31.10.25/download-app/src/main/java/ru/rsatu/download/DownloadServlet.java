package ru.rsatu.download;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@WebServlet("/download")
public class DownloadServlet extends HttpServlet {

    private static final String ENV_UPLOAD_DIR = "UPLOAD_DIR";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String uploadDirPath = System.getenv(ENV_UPLOAD_DIR);
        if (uploadDirPath == null || uploadDirPath.trim().isEmpty()) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Ошибка: переменная окружения " + ENV_UPLOAD_DIR + " не найдена.");
            return;
        }

        Path uploadDir = Paths.get(uploadDirPath);
        if (!Files.exists(uploadDir) || !Files.isDirectory(uploadDir)) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Директории не существует " + uploadDir.toString());
            return;
        }


        String idParam = req.getParameter("id");
        String typeParam = req.getParameter("type");

        List<Path> matches = findMatches(uploadDir, idParam, typeParam);

        if (matches.isEmpty()) {
            if ((idParam == null || idParam.trim().isEmpty()) && (typeParam == null || typeParam.trim().isEmpty())) {
                req.getRequestDispatcher("/download.html").forward(req, resp);
                return;
            }
            resp.setContentType("text/plain; charset=utf-8");
            resp.getWriter().write("Не найдено ни одного файла с заданными параметрами!");
            return;
        }

        if (matches.size() == 1) {
            streamFile(resp, matches.get(0));
            return;
        }

        resp.setContentType("text/html; charset=utf-8");
        PrintWriter w = resp.getWriter();
        w.println("<!doctype html><html><head><meta charset='utf-8'><title>Matches</title></head><body>");
        w.println("<h3>Найдено " + matches.size() + " файлов:</h3><ul>");
        for (Path p : matches) {
            String safeName = p.getFileName().toString();
            String href = req.getContextPath() + "/download?file=" + URLEncoder.encode(safeName, "UTF-8");
            w.println("<li><a href=\"" + href + "\">" + escapeHtml(safeName) + "</a></li>");
        }
        w.println("</ul></body></html>");
    }

    private List<Path> findMatches(Path dir, String idParam, String typeParam) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            List<Path> all = new ArrayList<>();
            for (Path p : stream) {
                if (Files.isRegularFile(p)) all.add(p);
            }
            final String id = idParam != null ? idParam.trim() : null;
            final String type = typeParam != null ? typeParam.trim() : null;

            return all.stream().filter(p -> {
                String name = p.getFileName().toString();
                if (id != null && !id.isEmpty()) {
                    if (name.startsWith(id + "_") || name.startsWith(id + "-") || name.equals(id)) return true;
                    if (Pattern.compile(".*(\\A|[_\\-\\.])" + Pattern.quote(id) + "([_\\-\\.]|\\z).*").matcher(name).matches()) return true;
                }
                if (type != null && !type.isEmpty()) {
                    String lower = name.toLowerCase();
                    if (lower.endsWith("." + type.toLowerCase())) return true;
                    if (lower.contains("." + type.toLowerCase() + ".")) return true;
                }
                return false;
            }).collect(Collectors.toList());
        }
    }

    private void streamFile(HttpServletResponse resp, Path file) throws IOException {
        if (!Files.exists(file) || !Files.isRegularFile(file)) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Файл не найден");
            return;
        }

        String originalName = file.getFileName().toString();
        String contentType = Files.probeContentType(file);
        if (contentType == null) contentType = "application/octet-stream";

        resp.setContentType(contentType);
        try {
            long size = Files.size(file);
            if (size >= 0) resp.setContentLengthLong(size);
        } catch (IOException ignored) {}

        String headerValue = buildContentDispositionHeader(originalName);
        resp.setHeader("Content-Disposition", headerValue);

        try (OutputStream out = resp.getOutputStream();
             InputStream in = Files.newInputStream(file, StandardOpenOption.READ)) {

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            out.flush();
        }
    }

    private String buildContentDispositionHeader(String filename) {
        String ascii = filename.replaceAll("[^\\x20-\\x7E]", "_");
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        return "attachment; filename=\"" + ascii + "\"; filename*=UTF-8''" + encoded;
    }

    private String escapeHtml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
