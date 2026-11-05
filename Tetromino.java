// ---------- Tetromino.java (Authentic NES Tetris Pieces & RNG) ----------
import java.util.Random;

public class Tetromino {
    private int[][] shape;
    private int index; // 0:T, 1:J, 2:Z, 3:O, 4:S, 5:L, 6:I
    private int rotationState; // 0..3

    // Orijinal NES Parça İsimleri ve Sıralaması
    public static final String[] NAMES = { "T", "J", "Z", "O", "S", "L", "I" };

    // Geriye dönük uyumluluk renkleri
    public static final java.awt.Color[] NES_COLORS = {
        new java.awt.Color(0xD500F9), // 0: T - purple
        new java.awt.Color(0x2979FF), // 1: J - blue
        new java.awt.Color(0xFF1744), // 2: Z - red
        new java.awt.Color(0xFFEA00), // 3: O - yellow
        new java.awt.Color(0x00E676), // 4: S - green
        new java.awt.Color(0xFF9100), // 5: L - orange
        new java.awt.Color(0x00E5FF)  // 6: I - cyan
    };

    public static synchronized void resetBag() {
        resetRNG();
    }

    // NES Tetris Orijinal 4x4 / 3x3 Rotasyon Tabloları (NES NTSC)
    // 0: T (4 states)
    // 1: J (4 states)
    // 2: Z (2 states)
    // 3: O (1 state)
    // 4: S (2 states)
    // 5: L (4 states)
    // 6: I (2 states)
    private static final int[][][][] NES_SHAPES = {
        // 0: T
        {
            {{0,1,0,0},{1,1,1,0},{0,0,0,0},{0,0,0,0}},
            {{0,1,0,0},{0,1,1,0},{0,1,0,0},{0,0,0,0}},
            {{0,0,0,0},{1,1,1,0},{0,1,0,0},{0,0,0,0}},
            {{0,1,0,0},{1,1,0,0},{0,1,0,0},{0,0,0,0}}
        },
        // 1: J
        {
            {{0,2,0,0},{0,2,0,0},{2,2,0,0},{0,0,0,0}},
            {{2,0,0,0},{2,2,2,0},{0,0,0,0},{0,0,0,0}},
            {{0,2,2,0},{0,2,0,0},{0,2,0,0},{0,0,0,0}},
            {{0,0,0,0},{2,2,2,0},{0,0,2,0},{0,0,0,0}}
        },
        // 2: Z (NES 2-state)
        {
            {{3,3,0,0},{0,3,3,0},{0,0,0,0},{0,0,0,0}},
            {{0,0,3,0},{0,3,3,0},{0,3,0,0},{0,0,0,0}},
            {{3,3,0,0},{0,3,3,0},{0,0,0,0},{0,0,0,0}},
            {{0,0,3,0},{0,3,3,0},{0,3,0,0},{0,0,0,0}}
        },
        // 3: O (1-state)
        {
            {{4,4,0,0},{4,4,0,0},{0,0,0,0},{0,0,0,0}},
            {{4,4,0,0},{4,4,0,0},{0,0,0,0},{0,0,0,0}},
            {{4,4,0,0},{4,4,0,0},{0,0,0,0},{0,0,0,0}},
            {{4,4,0,0},{4,4,0,0},{0,0,0,0},{0,0,0,0}}
        },
        // 4: S (NES 2-state)
        {
            {{0,5,5,0},{5,5,0,0},{0,0,0,0},{0,0,0,0}},
            {{0,5,0,0},{0,5,5,0},{0,0,5,0},{0,0,0,0}},
            {{0,5,5,0},{5,5,0,0},{0,0,0,0},{0,0,0,0}},
            {{0,5,0,0},{0,5,5,0},{0,0,5,0},{0,0,0,0}}
        },
        // 5: L
        {
            {{6,0,0,0},{6,0,0,0},{6,6,0,0},{0,0,0,0}},
            {{0,0,0,0},{6,6,6,0},{6,0,0,0},{0,0,0,0}},
            {{6,6,0,0},{0,6,0,0},{0,6,0,0},{0,0,0,0}},
            {{0,0,6,0},{6,6,6,0},{0,0,0,0},{0,0,0,0}}
        },
        // 6: I (NES 2-state)
        {
            {{0,0,0,0},{7,7,7,7},{0,0,0,0},{0,0,0,0}},
            {{0,0,7,0},{0,0,7,0},{0,0,7,0},{0,0,7,0}},
            {{0,0,0,0},{7,7,7,7},{0,0,0,0},{0,0,0,0}},
            {{0,0,7,0},{0,0,7,0},{0,0,7,0},{0,0,7,0}}
        }
    };

    private static final Random rand = new Random();
    private static int lastPieceIdx = -1;

    public Tetromino(int idx, int rotState) {
        this.index = idx;
        this.rotationState = (rotState % 4 + 4) % 4;
        this.shape = new int[4][4];
        for (int r = 0; r < 4; r++) {
            this.shape[r] = NES_SHAPES[idx][this.rotationState][r].clone();
        }
    }

    /**
     * Orijinal NES Tetris Pseudo-Random Number Generator (RNG):
     * 1. 0..7 arası zar atılır (8 olasılık).
     * 2. Eğer sonuç 7 ise VEYA bir önceki taş ile aynıysa, 0..6 arası tekrar zar atılır.
     * Bu durum taş kuraklığını azaltırken orijinal NES dağılımını korur.
     */
    public static synchronized Tetromino getRandom() {
        int roll = rand.nextInt(8);
        if (roll == 7 || roll == lastPieceIdx) {
            roll = rand.nextInt(7);
        }
        lastPieceIdx = roll;
        return new Tetromino(roll, 0);
    }

    public static synchronized void resetRNG() {
        lastPieceIdx = -1;
    }

    public static Tetromino getPiece(int idx) {
        return new Tetromino(idx, 0);
    }

    public int[][] getShape() { return shape; }
    public int getIndex() { return index; }
    public int getRotationState() { return rotationState; }

    public Tetromino rotateRight() {
        return new Tetromino(index, rotationState + 1);
    }

    public Tetromino rotateLeft() {
        return new Tetromino(index, rotationState - 1);
    }
}
