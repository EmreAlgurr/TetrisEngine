// ---------- TetrisEngineServer.java (NES Tetris AI Engine Backend) ----------
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

public class TetrisEngineServer {
    public static final int PORT = 3000;
    private static final String REQ_FILE = "bridge_request.txt";
    private static final String RES_FILE = "bridge_response.txt";

    public static void main(String[] args) throws IOException {
        // 1. HTTP Server başlat (Port 3000)
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
            server.createContext("/", new RootHandler());
            server.setExecutor(Executors.newFixedThreadPool(4));
            server.start();
            System.out.println("• HTTP Sunucusu Port " + PORT + " aktif.");
        } catch (Exception e) {
            System.out.println("• (Port " + PORT + " kullanımda, dosya IPC moduyla devam ediliyor)");
        }

        System.out.println("=======================================================");
        System.out.println("   NES TETRIS AI ENGINE (STANDALONE ZERO-DEP)");
        System.out.println("=======================================================");
        System.out.println("• FCEUX için ultra-hızlı File IPC köprüsü hazır!");
        System.out.println("• FCEUX'ten 'fceux_bridge.lua' çalıştırıldığında anında bağlanacak.");
        System.out.println("=======================================================\n");

        // 2. Ultra-hızlı Dosya Tabanlı IPC Dinleyicisi
        startFileBridgeListener();
    }

    private static void startFileBridgeListener() {
        File reqFile = new File(REQ_FILE);
        String lastProcessedToken = "";

        while (true) {
            if (reqFile.exists()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(reqFile))) {
                    String line = reader.readLine();
                    if (line != null && !line.isEmpty()) {
                        String[] parts = line.split("\\|");
                        if (parts.length >= 6) {
                            String token = parts[0];
                            if (!token.equals(lastProcessedToken)) {
                                lastProcessedToken = token;
                                long startTime = System.currentTimeMillis();

                                String curPieceStr = parts[1];
                                String nextPieceStr = parts[2];
                                int level = parseIntSafe(parts[3], 19);
                                int lines = parseIntSafe(parts[4], 0);
                                String boardStr = parts[5];

                                int[][] board = decodeBoard(boardStr);
                                Tetromino curPiece = parsePiece(curPieceStr);
                                Tetromino nextPiece = parsePiece(nextPieceStr);

                                TetrisAI.Move bestMove = TetrisAI.findBestMove(board, curPiece, nextPiece);
                                long elapsed = System.currentTimeMillis() - startTime;

                                // Yanıtı yaz (token|inputs)
                                try (FileWriter writer = new FileWriter(RES_FILE)) {
                                    writer.write(token + "|" + bestMove.inputSequence + "\n");
                                }

                                System.out.printf("⚡ [Lvl %02d | Lines %03d] Taş: %-2s | Next: %-2s | Karar: Rot=%d, Col=%d | Tuşlar: %-8s | %dms\n",
                                    level, lines, curPieceStr, nextPieceStr, bestMove.rotation, bestMove.column, bestMove.inputSequence, elapsed);
                            }
                        }
                    }
                } catch (Exception ignored) {
                }
            }

            try {
                Thread.sleep(2); // 2ms hassas döngü
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    static class RootHandler implements HttpHandler {
        private String lastAsyncResult = null;

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            String query = exchange.getRequestURI().getQuery();
            Map<String, String> params = parseQueryParams(query);

            String response = "OK";
            int statusCode = 200;

            if (path.contains("get-move") || path.contains("precompute")) {
                String boardStr = params.getOrDefault("board", "");
                String curPieceStr = params.getOrDefault("currentPiece", "T");
                String nextPieceStr = params.getOrDefault("nextPiece", "I");

                int[][] board = decodeBoard(boardStr);
                Tetromino curPiece = parsePiece(curPieceStr);
                Tetromino nextPiece = parsePiece(nextPieceStr);

                TetrisAI.Move bestMove = TetrisAI.findBestMove(board, curPiece, nextPiece);

                response = String.format("defaultPlacement:%d,%d,%s\n", bestMove.rotation, bestMove.column, bestMove.inputSequence);
                lastAsyncResult = response;
            } else if (path.contains("async-result")) {
                response = (lastAsyncResult != null) ? lastAsyncResult : "No result";
            }

            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            exchange.sendResponseHeaders(statusCode, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        }
    }

    private static Map<String, String> parseQueryParams(String query) {
        Map<String, String> map = new HashMap<>();
        if (query == null || query.isEmpty()) return map;
        for (String pair : query.split("&")) {
            int idx = pair.indexOf("=");
            if (idx > 0) map.put(pair.substring(0, idx), pair.substring(idx + 1));
        }
        return map;
    }

    private static int[][] decodeBoard(String boardStr) {
        int[][] board = new int[20][10];
        if (boardStr == null || boardStr.length() < 200) return board;
        for (int i = 0; i < 200; i++) {
            board[i / 10][i % 10] = (boardStr.charAt(i) == '1') ? 1 : 0;
        }
        return board;
    }

    private static Tetromino parsePiece(String name) {
        if (name == null) return Tetromino.getPiece(0);
        switch (name.toUpperCase().trim()) {
            case "T": return Tetromino.getPiece(0);
            case "J": return Tetromino.getPiece(1);
            case "Z": return Tetromino.getPiece(2);
            case "O": return Tetromino.getPiece(3);
            case "S": return Tetromino.getPiece(4);
            case "L": return Tetromino.getPiece(5);
            case "I": return Tetromino.getPiece(6);
            default:  return Tetromino.getPiece(0);
        }
    }

    private static int parseIntSafe(String val, int def) {
        if (val == null) return def;
        try { return Integer.parseInt(val); } catch (Exception e) { return def; }
    }
}
