package com.example.photoviewer;

import org.junit.Test;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Locale;
import static org.junit.Assert.*;

public class PostUploaderTest {
    /** A local HTTP peer checks the actual wire request without Android or extra dependencies. */
    private static class LocalServer implements AutoCloseable {
        final ServerSocket server;
        final Thread worker;
        volatile String headers, body;
        volatile Throwable failure;
        LocalServer(int code, String responseText) throws Exception {
            server = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"));
            server.setSoTimeout(5000);
            worker = new Thread(() -> {
                try (Socket socket = server.accept()) {
                    socket.setSoTimeout(5000);
                    InputStream input = socket.getInputStream();
                    ByteArrayOutputStream headerBytes = new ByteArrayOutputStream();
                    int value;
                    while ((value = input.read()) != -1) {
                        headerBytes.write(value);
                        String current = headerBytes.toString("US-ASCII");
                        if (current.endsWith("\r\n\r\n")) break;
                    }
                    headers = headerBytes.toString("US-ASCII");
                    int length = Integer.parseInt(header("Content-Length"));
                    byte[] bytes = input.readNBytes(length);
                    if (bytes.length != length) throw new AssertionError("Incomplete request body");
                    body = new String(bytes, StandardCharsets.UTF_8);
                    byte[] response = responseText.getBytes(StandardCharsets.UTF_8);
                    socket.getOutputStream().write(("HTTP/1.1 " + code + " Test\r\nContent-Type: application/json\r\nContent-Length: "
                            + response.length + "\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
                    socket.getOutputStream().write(response);
                    socket.getOutputStream().flush();
                } catch (Throwable error) {
                    failure = error;
                }
            });
            worker.setDaemon(true);
            worker.start();
        }
        String header(String name) {
            for (String line : headers.split("\r\n")) {
                if (line.toLowerCase(Locale.US).startsWith(name.toLowerCase(Locale.US) + ":"))
                    return line.substring(line.indexOf(':') + 1).trim();
            }
            return null;
        }
        String endpoint() { return "http://127.0.0.1:" + server.getLocalPort() + "/api_root/Post/"; }
        @Override public void close() throws Exception {
            server.close();
            worker.join(5000);
            if (failure != null) throw new AssertionError(failure);
        }
    }

    @Test
    public void sendsUtf8FieldsImageAndTokenWithContentLength() throws Exception {
        File image = File.createTempFile("post-uploader-test-", ".png");
        Files.write(image.toPath(), "test-image-bytes".getBytes(StandardCharsets.UTF_8));
        try (LocalServer server = new LocalServer(201, "{\"title\":\"게시 성공\"}")) {
            PostUploader.Response response = PostUploader.upload(server.endpoint(), "test-token",
                    7, "한글 제목", "본문\n두 번째 줄", "2026-10-11T12:00:00+09:00", image, "image/png");
            assertEquals(201, response.code);
            assertTrue(response.body.contains("게시 성공"));
            assertTrue(server.headers.startsWith("POST /api_root/Post/ HTTP/1.1"));
            assertEquals("Token test-token", server.header("Authorization"));
            assertEquals(Integer.parseInt(server.header("Content-Length")),
                    server.body.getBytes(StandardCharsets.UTF_8).length);
            String boundary = server.header("Content-Type").split("boundary=")[1];
            assertTrue(server.body.endsWith("\r\n--" + boundary + "--\r\n"));
            assertTrue(server.body.contains("name=\"author\""));
            assertTrue(server.body.contains("\r\n\r\n7\r\n"));
            assertTrue(server.body.contains("한글 제목"));
            assertTrue(server.body.contains("본문\n두 번째 줄"));
            assertTrue(server.body.contains("name=\"published_date\""));
            assertTrue(server.body.contains("name=\"image\"; filename=\"upload.png\""));
            assertTrue(server.body.contains("Content-Type: image/png\r\n\r\ntest-image-bytes"));
        } finally {
            Files.deleteIfExists(image.toPath());
        }
    }

    @Test
    public void preservesServerValidationError() throws Exception {
        File image = File.createTempFile("post-uploader-test-", ".png");
        Files.write(image.toPath(), new byte[]{1, 2, 3});
        try (LocalServer server = new LocalServer(400, "{\"author\":[\"Invalid pk\"]}")) {
            PostUploader.Response response = PostUploader.upload(server.endpoint(), "test-token",
                    1, "title", "body", "2026-10-11T12:00:00+09:00", image, "image/png");
            assertEquals(400, response.code);
            assertTrue(response.body.contains("Invalid pk"));
        } finally {
            Files.deleteIfExists(image.toPath());
        }
    }
}

