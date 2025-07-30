// ---------- AIBoard.java (Enhanced Board with AI) ----------
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.JPanel;
import javax.swing.Timer;

class AIBoard extends JPanel implements ActionListener {
    public static final int ROWS = 20;
    public static final int COLS = 10;
    public static final int CELL_SIZE = 16;
    
    private Timer timer;
    private Tetromino current;
    private Tetromino next;
    private int currX, currY;
    private int[][] grid;
    private int linesCleared = 0;
    
    // AI-related fields
    private boolean aiEnabled = false;
    private TetrisAI.Move currentAIMove = null;
    private boolean aiMoveInProgress = false;
    private int targetRotation = 0;
    private int targetColumn = 0;
    private int currentRotation = 0;
    
    public AIBoard() {
        setPreferredSize(new Dimension(COLS * CELL_SIZE + 2, ROWS * CELL_SIZE + 2));
        setBackground(new Color(0x0A0A0A));
        setFocusable(true);
        initBoard();
    }
    
    private void initBoard() {
        grid = new int[ROWS][COLS];
        next = Tetromino.getRandom();
        spawnPiece();
        timer = new Timer(200, this); // Faster for AI
        timer.start();
        
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (aiEnabled) return; // Ignore input when AI is active
                
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_LEFT: move(-1); break;
                    case KeyEvent.VK_RIGHT: move(1); break;
                    case KeyEvent.VK_DOWN: drop(); break;
                    case KeyEvent.VK_UP: rotate(); break;
                    case KeyEvent.VK_SPACE: toggleAI(); break; // Space to toggle AI
                }
                repaint();
            }
        });
    }
    
    private void toggleAI() {
        aiEnabled = !aiEnabled;
        if (aiEnabled) {
            System.out.println("AI enabled - the computer will now play!");
            currentAIMove = null;
            aiMoveInProgress = false;
        } else {
            System.out.println("AI disabled - manual control restored");
        }
    }
    
    private void spawnPiece() {
        current = next;
        next = Tetromino.getRandom();
        currX = COLS / 2 - 1;
        currY = 0;
        currentRotation = 0;
        
        // If AI is enabled, calculate the best move for the new piece
        if (aiEnabled) {
            currentAIMove = TetrisAI.findBestMove(grid, current);
            aiMoveInProgress = true;
            targetRotation = currentAIMove.rotation;
            targetColumn = currentAIMove.column;
            System.out.println("AI decision: " + currentAIMove);
        }
    }
    
    private void executeAIMove() {
        if (currentAIMove == null || !aiMoveInProgress) return;
        
        // First, handle rotation
        if (currentRotation < targetRotation) {
            rotate();
            currentRotation++;
            return;
        }
        
        // Then handle horizontal movement
        if (currX < targetColumn) {
            move(1);
        } else if (currX > targetColumn) {
            move(-1);
        } else {
            // We're in the right position, drop fast
            while (canMove(current, currX, currY + 1)) {
                currY++;
            }
            aiMoveInProgress = false;
        }
    }
    
    private void move(int dx) {
        if (canMove(current, currX + dx, currY)) currX += dx;
    }
    
    private void rotate() {
        Tetromino rotated = current.rotateRight();
        if (canMove(rotated, currX, currY)) current = rotated;
    }
    
    private void drop() {
        if (canMove(current, currX, currY + 1)) {
            currY++;
        } else {
            fixToGrid();
            linesCleared += clearLines();
            spawnPiece();
            if (!canMove(current, currX, currY)) {
                timer.stop();
                System.out.println("Game Over! Total lines cleared: " + linesCleared);
            }
        }
    }
    
    private boolean canMove(Tetromino shape, int x, int y) {
        int[][] m = shape.getShape();
        for (int r = 0; r < 4; r++) for (int c = 0; c < 4; c++) {
            if (m[r][c] != 0) {
                int nx = x + c, ny = y + r;
                if (nx < 0 || nx >= COLS || ny < 0 || ny >= ROWS) return false;
                if (grid[ny][nx] != 0) return false;
            }
        }
        return true;
    }
    
    private void fixToGrid() {
        int[][] m = current.getShape();
        for (int r = 0; r < 4; r++) for (int c = 0; c < 4; c++) {
            if (m[r][c] != 0) grid[currY + r][currX + c] = m[r][c];
        }
    }
    
    private int clearLines() {
        int cleared = 0;
        for (int i = ROWS - 1; i >= 0; i--) {
            boolean full = true;
            for (int j = 0; j < COLS; j++) if (grid[i][j] == 0) { full = false; break; }
            if (full) {
                cleared++;
                for (int k = i; k > 0; k--) grid[k] = grid[k - 1].clone();
                grid[0] = new int[COLS];
                i++;
            }
        }
        return cleared;
    }
    
    public int getLinesCleared() { return linesCleared; }
    public boolean isAIEnabled() { return aiEnabled; }
    
    public void drawNextPiece(Graphics g, int offsetX, int offsetY) {
        int[][] m = next.getShape();
        for (int r = 0; r < 4; r++) for (int c = 0; c < 4; c++) {
            if (m[r][c] != 0) {
                g.setColor(Tetromino.NES_COLORS[next.getIndex()]);
                g.fillRect(offsetX + c * CELL_SIZE, offsetY + r * CELL_SIZE, CELL_SIZE, CELL_SIZE);
                g.setColor(Color.BLACK);
                g.drawRect(offsetX + c * CELL_SIZE, offsetY + r * CELL_SIZE, CELL_SIZE, CELL_SIZE);
            }
        }
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        // Draw border
        g.setColor(new Color(0x7E7E7E));
        g.drawRect(0, 0, COLS * CELL_SIZE + 1, ROWS * CELL_SIZE + 1);
        
        // Draw grid cells
        for (int r = 0; r < ROWS; r++) for (int c = 0; c < COLS; c++) {
            if (grid[r][c] != 0) {
                g.setColor(Tetromino.NES_COLORS[grid[r][c] - 1]);
                g.fillRect(c * CELL_SIZE, r * CELL_SIZE, CELL_SIZE, CELL_SIZE);
                g.setColor(Color.BLACK);
                g.drawRect(c * CELL_SIZE, r * CELL_SIZE, CELL_SIZE, CELL_SIZE);
            }
        }
        
        // Draw current piece with AI highlight
        int[][] m = current.getShape();
        for (int r = 0; r < 4; r++) for (int c = 0; c < 4; c++) {
            if (m[r][c] != 0) {
                Color pieceColor = Tetromino.NES_COLORS[current.getIndex()];
                if (aiEnabled && aiMoveInProgress) {
                    // Add a slight brightness to AI-controlled pieces
                    pieceColor = pieceColor.brighter();
                }
                g.setColor(pieceColor);
                g.fillRect((currX + c) * CELL_SIZE, (currY + r) * CELL_SIZE, CELL_SIZE, CELL_SIZE);
                g.setColor(Color.BLACK);
                g.drawRect((currX + c) * CELL_SIZE, (currY + r) * CELL_SIZE, CELL_SIZE, CELL_SIZE);
            }
        }
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        if (aiEnabled && aiMoveInProgress) {
            executeAIMove();
        } else {
            drop();
        }
        repaint();
    }
}