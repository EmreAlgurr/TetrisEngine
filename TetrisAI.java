// ---------- TetrisAI.java ----------

public class TetrisAI {
    // Weights for evaluation function - tuned for good performance
    private static final double HEIGHT_WEIGHT = -0.510066;
    private static final double LINES_WEIGHT = 0.760666;
    private static final double HOLES_WEIGHT = -0.35663;
    private static final double BUMPINESS_WEIGHT = -0.184483;
    private static final double COMPLETE_LINES_WEIGHT = 0.8;
    
    // Special bonuses for different line clears
    private static final double SINGLE_BONUS = 0.5;     // 1 line
    private static final double DOUBLE_BONUS = 1.5;     // 2 lines  
    private static final double TRIPLE_BONUS = 3.0;     // 3 lines
    private static final double TETRIS_BONUS = 8.0;     // 4 lines - HUGE bonus!
    
    public static class Move {
        public int rotation;  // 0, 1, 2, or 3 (number of right rotations)
        public int column;    // target column position
        public double score;  // evaluation score for this move
        
        public Move(int rotation, int column, double score) {
            this.rotation = rotation;
            this.column = column;
            this.score = score;
        }
        
        @Override
        public String toString() {
            return String.format("Move(rot=%d, col=%d, score=%.3f)", rotation, column, score);
        }
    }
    
    /**
     * Find the best move for the given piece on the given board
     */
    public static Move findBestMove(int[][] board, Tetromino piece) {
        Move bestMove = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        
        // Try all 4 rotations
        Tetromino currentPiece = piece;
        for (int rotation = 0; rotation < 4; rotation++) {
            // Try all possible column positions for this rotation
            int[][] shape = currentPiece.getShape();
            
            // Find the leftmost and rightmost bounds of the piece
            int minCol = 4, maxCol = -1;
            for (int r = 0; r < 4; r++) {
                for (int c = 0; c < 4; c++) {
                    if (shape[r][c] != 0) {
                        minCol = Math.min(minCol, c);
                        maxCol = Math.max(maxCol, c);
                    }
                }
            }
            
            // Try each valid column position
            for (int col = -minCol; col <= Board.COLS - 1 - maxCol; col++) {
                // Simulate dropping the piece
                int dropY = findDropPosition(board, currentPiece, col);
                if (dropY >= 0) { // Valid position
                    // Create a copy of the board and place the piece
                    int[][] testBoard = copyBoard(board);
                    placePiece(testBoard, currentPiece, col, dropY);
                    
                    // Clear any completed lines
                    int linesCleared = clearLines(testBoard);
                    
                    // Evaluate this board state
                    double score = evaluateBoard(testBoard, linesCleared);
                    
                    if (score > bestScore) {
                        bestScore = score;
                        bestMove = new Move(rotation, col, score);
                    }
                }
            }
            
            // Rotate piece for next iteration
            currentPiece = currentPiece.rotateRight();
        }
        
        return bestMove;
    }
    
    /**
     * Find where a piece would land if dropped at the given column
     */
    private static int findDropPosition(int[][] board, Tetromino piece, int col) {
        int[][] shape = piece.getShape();
        
        // Start from the top and move down until we hit something
        for (int row = 0; row < Board.ROWS; row++) {
            if (!canPlacePiece(board, shape, col, row)) {
                return row - 1; // Return the last valid position
            }
        }
        return Board.ROWS - 1; // If we can place it all the way at the bottom
    }
    
    /**
     * Check if a piece can be placed at the given position
     */
    private static boolean canPlacePiece(int[][] board, int[][] shape, int col, int row) {
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 4; c++) {
                if (shape[r][c] != 0) {
                    int boardRow = row + r;
                    int boardCol = col + c;
                    
                    // Check bounds
                    if (boardRow < 0 || boardRow >= Board.ROWS || 
                        boardCol < 0 || boardCol >= Board.COLS) {
                        return false;
                    }
                    
                    // Check if position is occupied
                    if (board[boardRow][boardCol] != 0) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
    
    /**
     * Place a piece on the board (modifies the board)
     */
    private static void placePiece(int[][] board, Tetromino piece, int col, int row) {
        int[][] shape = piece.getShape();
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 4; c++) {
                if (shape[r][c] != 0) {
                    board[row + r][col + c] = shape[r][c];
                }
            }
        }
    }
    
    /**
     * Create a deep copy of the board
     */
    private static int[][] copyBoard(int[][] board) {
        int[][] copy = new int[board.length][];
        for (int i = 0; i < board.length; i++) {
            copy[i] = board[i].clone();
        }
        return copy;
    }
    
    /**
     * Clear completed lines and return the number cleared
     */
    private static int clearLines(int[][] board) {
        int linesCleared = 0;
        for (int row = Board.ROWS - 1; row >= 0; row--) {
            boolean fullLine = true;
            for (int col = 0; col < Board.COLS; col++) {
                if (board[row][col] == 0) {
                    fullLine = false;
                    break;
                }
            }
            
            if (fullLine) {
                linesCleared++;
                // Shift all rows above down
                for (int moveRow = row; moveRow > 0; moveRow--) {
                    board[moveRow] = board[moveRow - 1].clone();
                }
                board[0] = new int[Board.COLS];
                row++; // Check the same row again
            }
        }
        return linesCleared;
    }
    
    /**
     * Evaluate the quality of a board state
     */
    private static double evaluateBoard(int[][] board, int linesCleared) {
        double score = 0;
        
        // Base reward for clearing lines
        score += linesCleared * COMPLETE_LINES_WEIGHT;
        
        // Special bonuses based on number of lines cleared
        switch (linesCleared) {
            case 1:
                score += SINGLE_BONUS;
                break;
            case 2:
                score += DOUBLE_BONUS;
                break;
            case 3:
                score += TRIPLE_BONUS;
                break;
            case 4:
                score += TETRIS_BONUS; // MASSIVE bonus for Tetris!
                System.out.println("🎉 AI ACHIEVED TETRIS! +8.0 bonus points!");
                break;
        }
        
        // Calculate aggregate height (sum of column heights)
        double aggregateHeight = 0;
        int[] columnHeights = new int[Board.COLS];
        
        for (int col = 0; col < Board.COLS; col++) {
            for (int row = 0; row < Board.ROWS; row++) {
                if (board[row][col] != 0) {
                    columnHeights[col] = Board.ROWS - row;
                    break;
                }
            }
            aggregateHeight += columnHeights[col];
        }
        score += aggregateHeight * HEIGHT_WEIGHT;
        
        // Calculate number of holes
        int holes = 0;
        for (int col = 0; col < Board.COLS; col++) {
            boolean foundBlock = false;
            for (int row = 0; row < Board.ROWS; row++) {
                if (board[row][col] != 0) {
                    foundBlock = true;
                } else if (foundBlock) {
                    holes++;
                }
            }
        }
        score += holes * HOLES_WEIGHT;
        
        // Calculate bumpiness (variation in column heights)
        double bumpiness = 0;
        for (int col = 0; col < Board.COLS - 1; col++) {
            bumpiness += Math.abs(columnHeights[col] - columnHeights[col + 1]);
        }
        score += bumpiness * BUMPINESS_WEIGHT;
        
        // Additional Tetris setup bonus - reward having a deep well for I-pieces
        if (linesCleared == 0) {
            score += evaluateTetrisSetup(columnHeights);
        }
        
        // Exponential bonus for multiple lines (encourages waiting for better clears)
        if (linesCleared > 1) {
            score += Math.pow(linesCleared, 2.5) * LINES_WEIGHT;
        }
        
        return score;
    }
    
    /**
     * Evaluate how well the board is set up for a potential Tetris
     */
    private static double evaluateTetrisSetup(int[] columnHeights) {
        double tetrisBonus = 0;
        
        // Look for a deep well that could be used for Tetris
        for (int col = 0; col < Board.COLS; col++) {
            int currentHeight = columnHeights[col];
            int leftHeight = (col > 0) ? columnHeights[col - 1] : 0;
            int rightHeight = (col < Board.COLS - 1) ? columnHeights[col + 1] : 0;
            
            // Check if this column is significantly lower than neighbors (potential well)
            int depthLeft = leftHeight - currentHeight;
            int depthRight = rightHeight - currentHeight;
            
            // Bonus for having a well that's 4+ deep (perfect for Tetris)
            if (depthLeft >= 4 && depthRight >= 4) {
                tetrisBonus += 0.5;
            } else if (depthLeft >= 3 || depthRight >= 3) {
                tetrisBonus += 0.2;
            }
            
            // Special bonus for edge wells (easier to maintain)
            if ((col == 0 && rightHeight - currentHeight >= 4) || 
                (col == Board.COLS - 1 && leftHeight - currentHeight >= 4)) {
                tetrisBonus += 0.3;
            }
        }
        
        return tetrisBonus;
    }
    
    /**
     * Get column heights for debugging
     */
    public static int[] getColumnHeights(int[][] board) {
        int[] heights = new int[Board.COLS];
        for (int col = 0; col < Board.COLS; col++) {
            for (int row = 0; row < Board.ROWS; row++) {
                if (board[row][col] != 0) {
                    heights[col] = Board.ROWS - row;
                    break;
                }
            }
        }
        return heights;
    }
}