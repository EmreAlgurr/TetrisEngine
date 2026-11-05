// ---------- TetrisAI.java (StackRabbit-Inspired NES Tetris AI Engine) ----------
import java.util.ArrayList;
import java.util.List;

public class TetrisAI {

    public static class Move {
        public int rotation;  // 0..3 (NES rotasyon durumu)
        public int column;    // Hedef x pozisyonu
        public double score;  // Heuristic puanı
        public String inputSequence; // FCEUX için tuş dizisi

        public Move(int rotation, int column, double score) {
            this.rotation = rotation;
            this.column = column;
            this.score = score;
            this.inputSequence = "";
        }

        @Override
        public String toString() {
            return String.format("Move(rot=%d, col=%d, score=%.2f)", rotation, column, score);
        }
    }

    public static class Placement {
        public int rotation;
        public int column;
        public int[][] shape;
        public int[][] boardAfter;
        public int linesCleared;

        public Placement(int rotation, int column, int[][] shape, int[][] boardAfter, int linesCleared) {
            this.rotation = rotation;
            this.column = column;
            this.shape = shape;
            this.boardAfter = boardAfter;
            this.linesCleared = linesCleared;
        }
    }

    /**
     * 2-Ply Lookahead Arama: Eldeki taş ve sıradaki taş için en iyi hamleyi bulur.
     */
    public static Move findBestMove(int[][] board, Tetromino currentPiece, Tetromino nextPiece) {
        List<Placement> firstPlacements = getPossiblePlacements(board, currentPiece);
        if (firstPlacements.isEmpty()) {
            return new Move(0, 3, -999999);
        }

        Move bestMove = null;
        double bestTotalScore = Double.NEGATIVE_INFINITY;

        for (Placement p1 : firstPlacements) {
            double p1Score = evaluateBoard(p1.boardAfter, p1.linesCleared);

            if (nextPiece != null) {
                List<Placement> secondPlacements = getPossiblePlacements(p1.boardAfter, nextPiece);
                double bestSecondScore = Double.NEGATIVE_INFINITY;

                if (secondPlacements.isEmpty()) {
                    bestSecondScore = -5000;
                } else {
                    for (Placement p2 : secondPlacements) {
                        double p2Score = evaluateBoard(p2.boardAfter, p2.linesCleared);
                        if (p2Score > bestSecondScore) {
                            bestSecondScore = p2Score;
                        }
                    }
                }

                double combinedScore = p1Score + 0.80 * bestSecondScore;
                if (combinedScore > bestTotalScore) {
                    bestTotalScore = combinedScore;
                    bestMove = new Move(p1.rotation, p1.column, combinedScore);
                }
            } else {
                if (p1Score > bestTotalScore) {
                    bestTotalScore = p1Score;
                    bestMove = new Move(p1.rotation, p1.column, p1Score);
                }
            }
        }

        if (bestMove == null) {
            bestMove = new Move(0, 3, 0);
        }

        // NES için tuş dizilimini (Inputs) hesapla
        bestMove.inputSequence = generateInputs(currentPiece, bestMove.rotation, bestMove.column);
        return bestMove;
    }

    /**
     * Taşın spawn pozisyonundan hedef (rot, col) konumuna gitmesi için tuş dizilimi üretir.
     * Orijinal NES spawn: X = 3 (veya I için 3).
     */
    public static String generateInputs(Tetromino piece, int targetRot, int targetCol) {
        StringBuilder sb = new StringBuilder();
        int spawnX = (piece.getIndex() == 6) ? 3 : 3;

        // 1. Rotasyon tuşları (A tuşu)
        int rotCount = targetRot % 4;
        for (int r = 0; r < rotCount; r++) {
            sb.append("A."); // A'ya bas ve 1 frame bırak
        }

        // 2. Yatay hareket tuşları
        if (targetCol < spawnX) {
            int leftMoves = spawnX - targetCol;
            for (int i = 0; i < leftMoves; i++) sb.append("L.");
        } else if (targetCol > spawnX) {
            int rightMoves = targetCol - spawnX;
            for (int i = 0; i < rightMoves; i++) sb.append("R.");
        }

        // 3. Hızlı düşürme (Soft drop)
        sb.append("D");
        return sb.toString();
    }

    public static List<Placement> getPossiblePlacements(int[][] board, Tetromino piece) {
        List<Placement> placements = new ArrayList<>();
        Tetromino current = piece;

        int idx = piece.getIndex();
        int maxRotations = (idx == 3) ? 1 : ((idx == 2 || idx == 4 || idx == 6) ? 2 : 4);

        for (int rot = 0; rot < maxRotations; rot++) {
            int[][] shape = current.getShape();

            int minCol = 4, maxCol = -1;
            for (int r = 0; r < 4; r++) {
                for (int c = 0; c < 4; c++) {
                    if (shape[r][c] != 0) {
                        minCol = Math.min(minCol, c);
                        maxCol = Math.max(maxCol, c);
                    }
                }
            }

            for (int col = -minCol; col <= 10 - 1 - maxCol; col++) {
                int dropY = findDropPosition(board, shape, col);
                if (dropY >= 0) {
                    int[][] testBoard = copyBoard(board);
                    placePiece(testBoard, shape, col, dropY);
                    int lines = clearLines(testBoard);
                    placements.add(new Placement(rot, col, shape, testBoard, lines));
                }
            }

            current = current.rotateRight();
        }

        return placements;
    }

    private static int findDropPosition(int[][] board, int[][] shape, int col) {
        if (!canPlacePiece(board, shape, col, 0)) {
            return -1;
        }

        int lastValid = 0;
        for (int row = 1; row < 20; row++) {
            if (canPlacePiece(board, shape, col, row)) {
                lastValid = row;
            } else {
                break;
            }
        }
        return lastValid;
    }

    private static boolean canPlacePiece(int[][] board, int[][] shape, int col, int row) {
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 4; c++) {
                if (shape[r][c] != 0) {
                    int boardRow = row + r;
                    int boardCol = col + c;
                    if (boardRow < 0 || boardRow >= 20 || boardCol < 0 || boardCol >= 10) {
                        return false;
                    }
                    if (board[boardRow][boardCol] != 0) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static void placePiece(int[][] board, int[][] shape, int col, int row) {
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 4; c++) {
                if (shape[r][c] != 0) {
                    int br = row + r;
                    int bc = col + c;
                    if (br >= 0 && br < 20 && bc >= 0 && bc < 10) {
                        board[br][bc] = shape[r][c];
                    }
                }
            }
        }
    }

    private static int[][] copyBoard(int[][] board) {
        int[][] copy = new int[board.length][];
        for (int i = 0; i < board.length; i++) {
            copy[i] = board[i].clone();
        }
        return copy;
    }

    private static int clearLines(int[][] board) {
        int linesCleared = 0;
        for (int row = 19; row >= 0; row--) {
            boolean fullLine = true;
            for (int col = 0; col < 10; col++) {
                if (board[row][col] == 0) {
                    fullLine = false;
                    break;
                }
            }
            if (fullLine) {
                linesCleared++;
                for (int moveRow = row; moveRow > 0; moveRow--) {
                    board[moveRow] = board[moveRow - 1].clone();
                }
                board[0] = new int[10];
                row++;
            }
        }
        return linesCleared;
    }

    /**
     * STACKRABBIT EVALUATION HEURISTIC:
     * 1. Flatness (pow(abs(diff), 1.5))
     * 2. HasFlatSpot (O ve L taşları için düz zemin garantisi)
     * 3. Built-Out Left (DAS güvenliği için sol sütunları yüksek tutma)
     * 4. Right-Well (Col 9 kuyu temizliği)
     * 5. Hole & Blockade cezaları
     */
    public static double evaluateBoard(int[][] board, int linesCleared) {
        int[] heights = getColumnHeights(board);

        int maxHeight = 0;
        for (int h : heights) {
            maxHeight = Math.max(maxHeight, h);
        }

        boolean isPanic = maxHeight >= 10;
        double score = 0.0;

        // 1. SATIR TEMİZLİĞİ
        if (linesCleared == 4) {
            score += 250.0; // TETRIS!
        } else if (linesCleared > 0) {
            if (isPanic) {
                score += linesCleared * 20.0; // Panikte erit
            } else {
                score -= (4 - linesCleared) * 6.0; // Sakinde tekli/ikili silme
            }
        }

        // 2. STACKRABBIT FLATNESS & FLAT SPOT (Col 0..8 arası)
        boolean hasFlatSpot = false;
        double flatnessPenalty = 0.0;
        for (int i = 0; i < 8; i++) {
            int diff = heights[i + 1] - heights[i];
            if (diff == 0) hasFlatSpot = true;
            if (diff != 0) {
                flatnessPenalty += Math.pow(Math.abs(diff), 1.5);
            }
            // 3+ Uçurum cezası (Line dependency)
            if (Math.abs(diff) >= 3) {
                score -= 20.0;
            }
        }
        score -= flatnessPenalty * 1.8;

        if (!hasFlatSpot) {
            score -= 15.0; // Düz zemin yoksa ceza!
        }

        // 3. STACKRABBIT BUILT-OUT LEFT (Sol tarafın sağ taraftan yüksek olması)
        double leftAvg = (heights[0] + heights[1] + heights[2]) / 3.0;
        double rightAvg = (heights[6] + heights[7] + heights[8]) / 3.0;
        if (leftAvg > rightAvg) {
            score += (leftAvg - rightAvg) * 3.0; // Sol taraf hafif yüksekse bonus
        } else {
            score -= (rightAvg - leftAvg) * 4.0; // Sol taraf alçaksa DAS tehlikesi!
        }

        // 4. DELİK VE BLOKAJLAR
        int holes = 0;
        int blockades = 0;
        for (int col = 0; col < 10; col++) {
            boolean blockFound = false;
            int blocksAbove = 0;
            for (int row = 0; row < 20; row++) {
                if (board[row][col] != 0) {
                    blockFound = true;
                    blocksAbove++;
                } else if (blockFound) {
                    holes++;
                    blockades += blocksAbove;
                }
            }
        }
        score -= holes * 35.0;
        score -= blockades * 3.5;

        // 5. EN SAĞ SÜTUN (COL 9) TETRIS KUYUSU
        int wellHeight = heights[9];
        int leftWall = heights[8];
        if (!isPanic) {
            if (wellHeight == 0) {
                int depth = Math.min(leftWall, 4);
                score += depth * 10.0; // Kusursuz kuyu derinliği
            } else if (linesCleared < 4) {
                // Kuyu kapandıysa (Covered Well)
                score -= Math.pow(wellHeight, 2.0) * 10.0;
            }
        } else {
            score -= wellHeight * 1.0;
        }

        // Yükseklik cezası
        int aggHeight = 0;
        for (int c = 0; c < 9; c++) aggHeight += heights[c];
        score -= aggHeight * 0.40;

        if (maxHeight > 12) {
            score -= (maxHeight - 12) * 15.0;
        }

        return score;
    }

    public static int[] getColumnHeights(int[][] board) {
        int[] heights = new int[10];
        for (int col = 0; col < 10; col++) {
            for (int row = 0; row < 20; row++) {
                if (board[row][col] != 0) {
                    heights[col] = 20 - row;
                    break;
                }
            }
        }
        return heights;
    }
}