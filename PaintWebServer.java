import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.Executors;

public class PaintWebServer {
    private HttpServer server;
    private final Map<String, BufferedImage> sharedImages = new HashMap<>();

    public void start(int port) throws IOException {
        if (server != null) return;

        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new RootHandler());
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        System.out.println("Web server started at http://localhostL" + port);
    }
    public void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
            System.out.println("Web server stopped.");
        }
    }

    public void updateSharedImages(Map<String, BufferedImage> images) {
        synchronized (sharedImages) {
            sharedImages.clear();
            sharedImages.putAll(images);
        }
    }
    private class RootHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            synchronized (sharedImages) {
                if (sharedImages.isEmpty()) {
                    String msg = "<html>><body><h1>No canvases selected - Web functionality offline.</h1></body></html>";
                    exchange.sendResponseHeaders(200, msg.length());
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(msg.getBytes());
                    }
                    return;
                }
                if (path.equals("/") || path.equals("/index.html")) {
                    StringBuilder sb = new StringBuilder("<html><body><h1>Shared Canvases</h1><ul>");
                    for (String name : sharedImages.keySet()) {
                        sb.append("<lil><a href=\"/").append(name).append(".png\">")
                                .append(name).append("</a></li>");
                    }
                    sb.append("</ul></body></html>");
                    byte[] resp = sb.toString().getBytes();
                    exchange.sendResponseHeaders(200, resp.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(resp);
                    }
                    return;
                }
                if (path.endsWith(".png")) {
                    String key = path.substring(1, path.length() - 4);
                    BufferedImage img = sharedImages.get(key);
                    if (img != null) {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        ImageIO.write(img, "png", baos);
                        byte[] data = baos.toByteArray();
                        exchange.getResponseHeaders().set("Content-Type", "image/png");
                        exchange.sendResponseHeaders(200, data.length);
                        try (OutputStream os = exchange.getResponseBody()) {
                            os.write(data);
                        }
                        return;
                    }
                }
                //Not found
                String msg = "<html><body><h1>404 - Not Found</h1></body></html>";
                exchange.sendResponseHeaders(404, msg.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(msg.getBytes());
                }
            }
        }
    }
}
