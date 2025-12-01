package rubikscube;

import java.io.*;
import java.util.Arrays;

public class RubiksCube {

    private static int FRONT = 0;
    private static int BACK = 1;
    private static int RIGHT = 2;
    private static int LEFT = 3;
    private static int UP = 4;
    private static int DONW = 5;

    // current state of the cube
    char state[][][]; // Had to make this unprivate so that we can use it in solver


    /**
     * a static method that creates the initial state of the cube
     *
     * @return
     */
    private static char[][][] createSolvedState() {
        char[][][] c = new char[6][3][3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                c[FRONT][i][j] = 'W';
                c[BACK][i][j] = 'Y';
                c[RIGHT][i][j] = 'B';
                c[LEFT][i][j] = 'G';
                c[UP][i][j] = 'O';
                c[DONW][i][j] = 'R';
            }
        }
        return c;
    }

    private static char solvedState[][][] = createSolvedState();

    /**
     * default constructor
     * Creates a Rubik's Cube in an initial state:
     */
    public RubiksCube() {
        state = RubiksCube.createSolvedState();
    }

    public RubiksCube(RubiksCube other) {
        // Create a new, empty 6x3x3 array
        this.state = new char[6][3][3];

        for (int face = 0; face < 6; face++) {
            for (int row = 0; row < 3; row++) {
                System.arraycopy(other.state[face][row], 0, this.state[face][row], 0, 3);
            }
        }
    }

    /**
     * Checks that the color is good
     * and sets state[face][i][j] = color
     */
    private void setColor(int face, int i, int j, char color) throws IncorrectFormatException{
        if (color == 'W' || color == 'Y'
                || color == 'B' || color == 'G'
                || color == 'O' || color == 'R')
            state[face][i][j] = color;
        else
            throw new IncorrectFormatException("Bad color:" + color);
    }
    /**
     * @param fileName
     * @throws IOException
     * @throws IncorrectFormatException Creates a Rubik's Cube from the description in fileName
     */
    public RubiksCube(String fileName) throws IOException, IncorrectFormatException {
        FileReader file = new FileReader(fileName);
        BufferedReader input = new BufferedReader(file);

        try {
            String s[] = new String[9];
            for (int i = 0; i < 9; i++) {
                s[i] = input.readLine();
            }

            state = new char[6][3][3];
            for (int i = 0; i < 3; i++)
                for (int j = 0; j < 3; j++) {
                    setColor(UP,i,j,s[i].charAt(j + 3));
                    setColor(LEFT,i,j,s[i + 3].charAt(j));
                    setColor(FRONT,i,j,s[i + 3].charAt(j + 3));
                    setColor(RIGHT,i,j,s[i + 3].charAt(j + 6));
                    setColor(BACK,i,j,s[i + 3].charAt(j + 9));
                    setColor(DONW,i,j,s[i + 6].charAt(j + 3));
                }
        } catch (Exception ex) {
            throw new IncorrectFormatException(ex.toString());
        }
    }

    /**
     * gets 4 squares in the cube and rotates the colors
     * -->-->-->--
     * ^         |
     * |_________|
     */
    private void rotate4(int ind1[], int ind2[], int ind3[], int ind4[]) {
        char tmp = state[ind4[0]][ind4[1]][ind4[2]];
        state[ind4[0]][ind4[1]][ind4[2]] = state[ind3[0]][ind3[1]][ind3[2]];
        state[ind3[0]][ind3[1]][ind3[2]] = state[ind2[0]][ind2[1]][ind2[2]];
        state[ind2[0]][ind2[1]][ind2[2]] = state[ind1[0]][ind1[1]][ind1[2]];
        state[ind1[0]][ind1[1]][ind1[2]] = tmp;
    }

    // rotates the side clockwise
    private void rotateOneSide(int side) {
        rotate4(new int[]{side, 0, 0}, new int[]{side, 0, 2}, new int[]{side, 2, 2}, new int[]{side, 2, 0});
        rotate4(new int[]{side, 0, 1}, new int[]{side, 1, 2}, new int[]{side, 2, 1}, new int[]{side, 1, 0});
    }

    /**
     * the six functions moveF, moveB, moveR, moveL, moveU, moveD
     */
    private void moveF() {
        rotateOneSide(FRONT);
        // update the four faces adjacent to F
        rotate4(new int[]{UP, 2, 0}, new int[]{RIGHT, 0, 0}, new int[]{DONW, 0, 2}, new int[]{LEFT, 2, 2});
        rotate4(new int[]{UP, 2, 1}, new int[]{RIGHT, 1, 0}, new int[]{DONW, 0, 1}, new int[]{LEFT, 1, 2});
        rotate4(new int[]{UP, 2, 2}, new int[]{RIGHT, 2, 0}, new int[]{DONW, 0, 0}, new int[]{LEFT, 0, 2});
    }
    private void moveB() {
        rotateOneSide(BACK);
        // update the four faces adjacent to B
        rotate4(new int[]{UP, 0, 2}, new int[]{LEFT, 0, 0}, new int[]{DONW, 2, 0}, new int[]{RIGHT, 2, 2});
        rotate4(new int[]{UP, 0, 1}, new int[]{LEFT, 1, 0}, new int[]{DONW, 2, 1}, new int[]{RIGHT, 1, 2});
        rotate4(new int[]{UP, 0, 0}, new int[]{LEFT, 2, 0}, new int[]{DONW, 2, 2}, new int[]{RIGHT, 0, 2});
    }
    private void moveR() {
        rotateOneSide(RIGHT);
        // update the four faces adjacent to R
        rotate4(new int[]{FRONT, 2, 2}, new int[]{UP, 2, 2}, new int[]{BACK, 0, 0}, new int[]{DONW, 2, 2});
        rotate4(new int[]{FRONT, 1, 2}, new int[]{UP, 1, 2}, new int[]{BACK, 1, 0}, new int[]{DONW, 1, 2});
        rotate4(new int[]{FRONT, 0, 2}, new int[]{UP, 0, 2}, new int[]{BACK, 2, 0}, new int[]{DONW, 0, 2});
    }
    private void moveL() {
        rotateOneSide(LEFT);
        // update the four faces adjacent to L
        rotate4(new int[]{BACK, 2, 2}, new int[]{UP, 0, 0}, new int[]{FRONT, 0, 0}, new int[]{DONW, 0, 0});
        rotate4(new int[]{BACK, 1, 2}, new int[]{UP, 1, 0}, new int[]{FRONT, 1, 0}, new int[]{DONW, 1, 0});
        rotate4(new int[]{BACK, 0, 2}, new int[]{UP, 2, 0}, new int[]{FRONT, 2, 0}, new int[]{DONW, 2, 0});
    }
    private void moveU() {
        rotateOneSide(UP);
        // update the four faces adjacent to U
        rotate4(new int[]{BACK, 0, 0}, new int[]{RIGHT, 0, 0}, new int[]{FRONT, 0, 0}, new int[]{LEFT, 0, 0});
        rotate4(new int[]{BACK, 0, 1}, new int[]{RIGHT, 0, 1}, new int[]{FRONT, 0, 1}, new int[]{LEFT, 0, 1});
        rotate4(new int[]{BACK, 0, 2}, new int[]{RIGHT, 0, 2}, new int[]{FRONT, 0, 2}, new int[]{LEFT, 0, 2});
    }
    private void moveD() {
        rotateOneSide(DONW);
        // update the four faces adjacent to D
        rotate4(new int[]{LEFT, 2, 0}, new int[]{FRONT, 2, 0}, new int[]{RIGHT, 2, 0}, new int[]{BACK, 2, 0});
        rotate4(new int[]{LEFT, 2, 1}, new int[]{FRONT, 2, 1}, new int[]{RIGHT, 2, 1}, new int[]{BACK, 2, 1});
        rotate4(new int[]{LEFT, 2, 2}, new int[]{FRONT, 2, 2}, new int[]{RIGHT, 2, 2}, new int[]{BACK, 2, 2});
    }   

    // Our scouting methods to essentially create the graph as we do a move
    // public RubiksCube getNeighborF() {
    //     RubiksCube neighbor = new RubiksCube(this);
    //     neighbor.moveF();                        
    //     return neighbor;                        
    // }

    // public RubiksCube getNeighborB() {
    //     RubiksCube neighbor = new RubiksCube(this);
    //     neighbor.moveB();
    //     return neighbor;
    // }

    // public RubiksCube getNeighborL() {
    //     RubiksCube neighbor = new RubiksCube(this);
    //     neighbor.moveL();
    //     return neighbor;
    // }

    // public RubiksCube getNeighborR() {
    //     RubiksCube neighbor = new RubiksCube(this);
    //     neighbor.moveR();
    //     return neighbor;
    // }

    // public RubiksCube getNeighborU() {
    //     RubiksCube neighbor = new RubiksCube(this);
    //     neighbor.moveU();
    //     return neighbor;
    // }

    // public RubiksCube getNeighborD() {
    //     RubiksCube neighbor = new RubiksCube(this);
    //     neighbor.moveD(); 
    //     return neighbor;
    // }

    // Above functions are redundant we only need 1 get neighbor function we already have a logic box makeMove
    public RubiksCube getNeighbor( char c ) {
        RubiksCube neighbor = new RubiksCube(this);
        neighbor.makeMove(c);
        return neighbor;
    }

    public void makeMove(char c) {
        if (c == 'F')
            moveF();
        else if (c == 'B')
            moveB();
        else if (c == 'R')
            moveR();
        else if (c == 'L')
            moveL();
        else if (c == 'U')
            moveU();
        else if (c == 'D')
            moveD();
        else
            throw new IllegalArgumentException("Incorrect move: " + c);
    }

    /**
     * @param moves Applies the sequence of moves on the Rubik's Cube
     */
    public void applyMoves(String moves) {
        for (char c: moves.toCharArray())
            makeMove(c);
    }

    /**
     * returns true if the current state of the Cube is solved,
     */
    public boolean isSolved() {
        for (int i = 0; i < 6; i++)
            for (int j = 0; j < 3; j++)
                for (int k = 0; k < 3; k++)
                    if (state[i][j][k] != solvedState[i][j][k])
                        return false;
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 3; i++) {
            sb.append("   ");
            sb.append("" + state[UP][i][0] + state[UP][i][1] + state[UP][i][2] + "\n");
        }

        for (int i = 0; i < 3; i++) {
            sb.append("" + state[LEFT][i][0] + state[LEFT][i][1] + state[LEFT][i][2]);
            sb.append("" + state[FRONT][i][0] + state[FRONT][i][1] + state[FRONT][i][2]);
            sb.append("" + state[RIGHT][i][0] + state[RIGHT][i][1] + state[RIGHT][i][2]);
            sb.append("" + state[BACK][i][0] + state[BACK][i][1] + state[BACK][i][2] + "\n");
        }

        for (int i = 0; i < 3; i++) {
            sb.append("   ");
            sb.append("" + state[DONW][i][0] + state[DONW][i][1] + state[DONW][i][2] + "\n");
        }

        return sb.toString();
    }


    // Initially had these just for the A star and graph searches where we used nodes that held RubiksCubes
    @Override 
    public boolean equals(Object obj) {
        if( this == obj ) return true;
        if( obj == null || getClass() != obj.getClass()) return false;
        RubiksCube that = (RubiksCube) obj;
        return Arrays.deepEquals(this.state, that.state);
    }

    @Override
    public int hashCode() {
        return Arrays.deepHashCode(this.state);
    }

    /**
     *
     * @param moves
     * @return the order of the sequence of moves
     */
    public static int order(String moves) {
        RubiksCube cube = new RubiksCube();
        cube.applyMoves(moves);
        int ord = 1;
        while (!cube.isSolved()) {
            cube.applyMoves(moves);
            ord++;
        }
        return ord;
    }

    public static void main(String[] args) {
        System.out.println(RubiksCube.order("RU"));
    }


}