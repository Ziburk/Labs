package ru.rsatu.upload;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.*;
import java.nio.file.*;


@WebServlet("/upload")
@MultipartConfig

public class UploadServlet extends HttpServlet {

    private static final String ENV_UPLOAD_DIR = "UPLOAD_DIR";

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String uploadDirPath = System.getenv(ENV_UPLOAD_DIR);
        if (uploadDirPath == null || uploadDirPath.trim().isEmpty()) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Ошибка: переменная окружения " + ENV_UPLOAD_DIR + " не найдена.");
            return;
        }

        Path uploadDir = Paths.get(uploadDirPath);
        try {
            Files.createDirectories(uploadDir);
        } catch (IOException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Невозможно создать дирректорию: " + e.getMessage());
            return;
        }

        try {
            Part filePart = req.getPart("file");
            if (filePart == null || filePart.getSize() == 0) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Нет файла");
                return;
            }

            String submitted = getSubmittedFileName(filePart);
            String fileName = sanitizeFilename(Paths.get(submitted).getFileName().toString());
            String finalName = System.currentTimeMillis() + "_" + fileName;
            Path target = uploadDir.resolve(finalName);

            try (InputStream in = filePart.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "Ошибка сохранения файла: " + e.getMessage());
                return;
            }

            resp.setContentType("text/plain; charset=utf-8");
            resp.getWriter().write("Загружен файл: " + target.toString());

        } catch (IllegalStateException e) {
            resp.sendError(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE,
                    "Файл слишком большой!");
        }
    }

    private static String getSubmittedFileName(Part part) {
        String cd = part.getHeader("content-disposition");
        if (cd == null) return "";
        for (String token : cd.split(";")) {
            token = token.trim();
            if (token.startsWith("filename")) {
                String[] kv = token.split("=", 2);
                if (kv.length == 2) {
                    String filename = kv[1].trim();
                    if (filename.startsWith("\"") && filename.endsWith("\"") && filename.length() >= 2) {
                        filename = filename.substring(1, filename.length() - 1);
                    }
                    return filename;
                }
            }
        }
        return "";
    }


    private String sanitizeFilename(String filename) {
        String sanitized = filename.replaceAll("\\p{Cntrl}", "")
                .replaceAll("[\\\\/]+", "")
                .replaceAll("\\s+", "_");
        if (sanitized.length() > 200) sanitized = sanitized.substring(sanitized.length() - 200);
        if (sanitized.isEmpty()) sanitized = "file";
        return sanitized;
    }
}
