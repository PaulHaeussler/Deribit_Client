package server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class ServerHandler {

    static final int PORT = 15000;
    private MyHandler h;

    public ServerHandler(String ServerInfo) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        h = new MyHandler(ServerInfo);
        server.createContext("/marketdata.json", h);
        server.setExecutor(null); // creates a default executor
        server.start();
    }

    public void updateServerInfo(String newInfo){
        h.serverInfo = newInfo;
    }

    static class MyHandler implements HttpHandler {

        private String serverInfo;

        public MyHandler(String ServerInfo){
            super();
            serverInfo = ServerInfo;
        }

        @Override
        public void handle(HttpExchange t) throws IOException {
            String response = serverInfo;
            t.getResponseHeaders().set("Access-Control-Allow-Origin", "http://bilbosjournal.com");
            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }
}
