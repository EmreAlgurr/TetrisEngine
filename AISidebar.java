// ---------- AISidebar.java (Enhanced Sidebar with AI status) ----------
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JPanel;
import javax.swing.Timer;

class AISidebar extends JPanel implements ActionListener {
    private static final int WIDTH = 8 * AIBoard.CELL_SIZE;
    private static final int HEIGHT = AIBoard.ROWS * AIBoard.CELL_SIZE;
    private AIBoard board;
    private Timer timer;
    private int score = 0;
    private int temp=0;

    public AISidebar(AIBoard board) {
        this.board = board;
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(0x071F34));
        timer = new Timer(500, this);
        timer.start();
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setFont(new Font("Monospaced", Font.BOLD, 14));
        g.setColor(Color.WHITE);
        
        // Score display
        g.drawString("SCORE", 10, 30);
        g.drawString(String.format("%06d", score), 10, 60);
        
        // Next piece display
        g.drawString("NEXT", 10, 120);
        board.drawNextPiece(g, 10, 140);
        
        // AI status display
        g.drawString("AI MODE", 10, 220);
        if (board.isAIEnabled()) {
            g.setColor(Color.GREEN);
            g.drawString("ENABLED", 10, 240);
        } else {
            g.setColor(Color.RED);
            g.drawString("DISABLED", 10, 240);
        }
        
        g.setColor(Color.WHITE);
        g.setFont(new Font("Monospaced", Font.PLAIN, 10));
        g.drawString("Press SPACE", 10, 270);
        g.drawString("to toggle AI", 10, 285);
        
        // Controls
        g.drawString("CONTROLS:", 10, 320);
        g.drawString("← → Move", 10, 335);
        g.drawString("↑ Rotate", 10, 350);
        g.drawString("↓ Drop", 10, 365);
        g.drawString("SPACE AI", 10, 380);
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        // Update score based on lines cleared
        
        switch (board.getLinesCleared()-temp) {
            case 1:
                score += 100; // Single line clear
                break;
            case 2:
                score += 300; // Double line clear
                break;
            case 3:    
                score += 500; // Triple line clear
                break;
            case 4:             
                score += 800; // Tetris (four lines clear)
           
                break;
        }
         temp= board.getLinesCleared();
        repaint();
    }
}

