// ---------- TetrisAIGame.java (Main class) ----------
import java.awt.BorderLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class TetrisAIGame extends JFrame {
    public TetrisAIGame() {
        initUI();
    }
    
    private void initUI() {
        setTitle("NES Tetris with AI");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);
        
        JPanel container = new JPanel(new BorderLayout());
        AIBoard board = new AIBoard();
        AISidebar sidebar = new AISidebar(board);
        container.add(board, BorderLayout.CENTER);
        container.add(sidebar, BorderLayout.EAST);
        
        add(container);
        pack();
        setLocationRelativeTo(null);
        
        System.out.println("=== NES Tetris with AI ===");
        System.out.println("Press SPACE to toggle AI mode");
        System.out.println("Arrow keys to play manually");
    }
    
    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            TetrisAIGame game = new TetrisAIGame();
            game.setVisible(true);
        });
    }
}