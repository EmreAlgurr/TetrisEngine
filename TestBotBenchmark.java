public class TestBotBenchmark {
    public static void main(String[] args) {
        System.out.println("=== BOT TETRIS TESTİ BAŞLATILIYOR (300 Parça Simülasyonu) ===");
        
        int[][] board = new int[20][10];
        Tetromino.resetBag();
        
        Tetromino current = Tetromino.getRandom();
        Tetromino next = Tetromino.getRandom();
        
        int totalCleared = 0;
        int singles = 0;
        int doubles = 0;
        int triples = 0;
        int tetrises = 0;
        int piecesPlaced = 0;
        
        for (int p = 0; p < 1000; p++) {
            TetrisAI.Move move = TetrisAI.findBestMove(board, current, next);
            if (move == null) {
                System.out.println("Hamle bulunamadı!");
                break;
            }
            
            // Hamleyi uygula
            Tetromino pieceToPlace = current;
            for (int r = 0; r < move.rotation; r++) {
                pieceToPlace = pieceToPlace.rotateRight();
            }
            
            int[][] shape = pieceToPlace.getShape();
            
            // dropY bul
            int dropY = -1;
            for (int row = 0; row < 20; row++) {
                boolean canPlace = true;
                for (int r = 0; r < 4; r++) {
                    for (int c = 0; c < 4; c++) {
                        if (shape[r][c] != 0) {
                            int br = row + r;
                            int bc = move.column + c;
                            if (br < 0 || br >= 20 || bc < 0 || bc >= 10 || board[br][bc] != 0) {
                                canPlace = false;
                                break;
                            }
                        }
                    }
                    if (!canPlace) break;
                }
                if (canPlace) {
                    dropY = row;
                } else {
                    break;
                }
            }
            
            // Eğer sığmıyorsa Game Over
            if (dropY < 0) {
                System.out.println("Oyun Bitti! Dayanılan parça sayısı: " + piecesPlaced);
                break;
            }
            
            // Yerleştir
            for (int r = 0; r < 4; r++) {
                for (int c = 0; c < 4; c++) {
                    if (shape[r][c] != 0) {
                        int br = dropY + r;
                        int bc = move.column + c;
                        if (br >= 0 && br < 20 && bc >= 0 && bc < 10) {
                            board[br][bc] = shape[r][c];
                        }
                    }
                }
            }
            piecesPlaced++;
            
            // Satırları temizle
            int cleared = 0;
            for (int r = 19; r >= 0; r--) {
                boolean full = true;
                for (int c = 0; c < 10; c++) {
                    if (board[r][c] == 0) { full = false; break; }
                }
                if (full) {
                    cleared++;
                    for (int k = r; k > 0; k--) board[k] = board[k - 1].clone();
                    board[0] = new int[10];
                    r++;
                }
            }
            
            if (cleared > 0) {
                totalCleared += cleared;
                if (cleared == 1) singles++;
                else if (cleared == 2) doubles++;
                else if (cleared == 3) triples++;
                else if (cleared == 4) {
                    tetrises++;
                    System.out.println("🔥 [PARÇA " + piecesPlaced + "] TETRIS YAPILDI! (Toplam Tetris: " + tetrises + ")");
                }
            }
            
            current = next;
            next = Tetromino.getRandom();
        }
        
        System.out.println("\n================ TEST SONUÇLARI ================");
        System.out.println("Yerleştirilen Parça : " + piecesPlaced);
        System.out.println("Toplam Temizlenen   : " + totalCleared + " satır");
        System.out.println("Single (1x)         : " + singles);
        System.out.println("Double (2x)         : " + doubles);
        System.out.println("Triple (3x)         : " + triples);
        System.out.println("TETRIS (4x)         : " + tetrises);
        double tetrisRate = totalCleared > 0 ? ((double)(tetrises * 4) / totalCleared) * 100.0 : 0.0;
        System.out.printf("Tetris Oranı (TRT)  : %.2f%%\n", tetrisRate);
        System.out.println("================================================");
    }
}
