package com.example.photoviewer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Sends the existing Django Post API a UTF-8 multipart form and an image file. */
public final class PostUploader {
    private PostUploader() { }

    public static final class Response {
        public final int code;
        public final String body;

        Response(int code, String body) {
            this.code = code;
            this.body = body;
        }
    }

    public static Response upload(String endpoint, String token, int author, String title,
                                  String text, String publishedDate, File image,
                                  String mimeType) throws java.io.IOException {
        String boundary = "PhotoViewer-" + UUID.randomUUID();
        if (mimeType == null || !mimeType.matches("image/[a-zA-Z0-9.+-]+")) {
            mimeType = "application/octet-stream";
        }
        String extension = "image/jpeg".equals(mimeType) ? "jpg" :
                "image/png".equals(mimeType) ? "png" : "img";
        StringBuilder form = new StringBuilder();
        appendField(form, boundary, "author", Integer.toString(author));
        appendField(form, boundary, "title", title);
        appendField(form, boundary, "text", text);
        appendField(form, boundary, "created_date", publishedDate);
        appendField(form, boundary, "published_date", publishedDate);
        form.append("--").append(boundary).append("\r\n")
                .append("Content-Disposition: form-data; name=\"image\"; filename=\"upload.")
                .append(extension).append("\"\r\n")
                .append("Content-Type: ").append(mimeType).append("\r\n\r\n");
        byte[] prefix = form.toString().getBytes(StandardCharsets.UTF_8);
        byte[] suffix = ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);
        HttpURLConnection connection = (HttpURLConnection) new URL(endpoint).openConnection();
        try {
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Authorization", "Token " + token);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(15000);
            connection.setDoOutput(true);
            // Django's development WSGI server expects a known Content-Length.
            connection.setFixedLengthStreamingMode(prefix.length + image.length() + suffix.length);
            try (OutputStream output = connection.getOutputStream();
                 InputStream input = new FileInputStream(image)) {
                output.write(prefix);
                byte[] buffer = new byte[8192];
                int count;
                while ((count = input.read(buffer)) != -1) {
                    output.write(buffer, 0, count);
                }
                output.write(suffix);
            }
            int code = connection.getResponseCode();
            InputStream response = code >= 400 ? connection.getErrorStream() : connection.getInputStream();
            return new Response(code, readResponse(response));
        } finally {
            connection.disconnect();
        }
    }

    private static void appendField(StringBuilder form, String boundary, String name, String value) {
        form.append("--").append(boundary).append("\r\n")
                .append("Content-Disposition: form-data; name=\"").append(name).append("\"\r\n")
                .append("Content-Type: text/plain; charset=UTF-8\r\n\r\n")
                .append(value).append("\r\n");
    }

    private static String readResponse(InputStream response) throws java.io.IOException {
        if (response == null) return "";
        try (InputStream input = response;
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        }
    }
}
