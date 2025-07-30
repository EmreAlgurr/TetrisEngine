// ---------- Tetromino.java ----------
import java.awt.Color;
import java.util.Random;

class Tetromino {
    private int[][] shape;
    private int index;
    public static final Color[] NES_COLORS = {
        new Color(0x00FFFF), // I - cyan
        new Color(0x0000FF), // J - blue
        new Color(0xFF7F00), // L - orange
        new Color(0xFFFF00), // O - yellow
        new Color(0x00FF00), // S - green
        new Color(0xFF00FF), // T - magenta
        new Color(0xFF0000)  // Z - red
    };

    private static final int[][][] SHAPES = {
        {{0,0,0,0},{1,1,1,1},{0,0,0,0},{0,0,0,0}},
        {{2,0,0,0},{2,2,2,0},{0,0,0,0},{0,0,0,0}},
        {{0,0,3,0},{3,3,3,0},{0,0,0,0},{0,0,0,0}},
        {{4,4,0,0},{4,4,0,0},{0,0,0,0},{0,0,0,0}},
        {{0,5,5,0},{5,5,0,0},{0,0,0,0},{0,0,0,0}},
        {{0,6,0,0},{6,6,6,0},{0,0,0,0},{0,0,0,0}},
        {{7,7,0,0},{0,7,7,0},{0,0,0,0},{0,0,0,0}}
    };

    private Tetromino(int[][] shape, int idx) {
        this.shape = shape;
        this.index = idx;
    }

    public static Tetromino getRandom() {
        Random rand = new Random();
        int idx = rand.nextInt(SHAPES.length);
        int[][] shp = new int[4][4];
        for (int i = 0; i < 4; i++) shp[i] = SHAPES[idx][i].clone();
        return new Tetromino(shp, idx);
    }

    public int[][] getShape() { return shape; }
    public int getIndex() { return index; }

    public Tetromino rotateRight() {
        int[][] rot = new int[4][4];
        for (int i = 0; i < 4; i++) for (int j = 0; j < 4; j++) rot[j][3 - i] = shape[i][j];
        return new Tetromino(rot, index);
    }
}
