package rubikscube;

/*
 * This class handles the Coordinate Level of the cube, which is essential for the Kociemba algorithms speed.
 * Instead of thinking about cubies, we think about coordinates like Twist, Flip, and Slice.
 * I adapted the coordinate definitions and the move table generation from http://kociemba.org/cube.htm.
 * The main idea is to pre-compute everything (Move Tables and Pruning Tables) so the search is fast.
 */

public class CoordCube {

    // Const definitions mostly taken from Kociemba Official Documentation
    // Lots of references used from Official docs and online forums, to make sure the mappings are correct

    public static final int NUM_URF_DLB = 40320;     // 8! permutations of all the corners
    public static final int NUM_UR_BR = 479001600;   // 12! permutations of all the edges

    public static final short NUM_MOVES = 18; // At most you can do 18 moves(U,D,F,B,L,R and their primes and doubles)

    public static final short NUM_CORNER_ORIENTATIONS = 2187;      // 3^7 = 2187 possible corner orientations for 8 corner orientations. 8th is chosen by the first 7
    public static final short NUM_EDGE_ORIENTATIONS = 2048;       // The number of ways 12 edges can be oriented. 12th is chosen by first 11. 2^11 = 2048
    public static final short NUM_SLICE_POSITIONS_PHASE1 = 495;      // The 4 middle layer edges, there are 12 choices for 4 positions. 12 choose 4 = 495
    public static final short NUM_SLICE_PERMUTATIONS_PHASE2 = 24;       // There are only 4! ways to permute the 4 middle layer edges once they are already in the middle layer for phase 2
    public static final short NUM_PARITIES = 2;        // There are two possible parities for corner and edge permutations ( even or odd )

    public static final short NUM_CORNER_PERMUTATIONS = 20160;  // The number of ways to arrange the first 6 corners (8! / 2! = 20,160) Used in Phase 2.
    public static final short NUM_SLICE_EDGE_PERMUTATIONS = 11880;    // Permutations of the 4 slice edges within the 12 possible slots.
    public static final short NUM_EDGE_MERGE_UR_UL = 1320;     // Helper constants for merging edge permutations.
    public static final short NUM_EDGE_MERGE_UB_DF = 1320;     // Helper constants for merging edge permutations.
    public static final short NUM_EDGE_PERMUTATIONS_PHASE2 = 20160;    // Permutations of the 8 edges in the U and D layers for Phase 2.

    // The actual coordinates for this specific cube state
    public short twist;
    public short flip;
    public short parity;
    public short FRtoBR;
    public short URFtoDLF;
    public short URtoUL;
    public short UBtoDF;
    public int URtoDF;

    // Generate a CoordCube from a CubieCube
    public CoordCube(CubieCube cubieCube) {
        twist = cubieCube.getTwist();
        flip = cubieCube.getFlip();
        parity = cubieCube.cornerParity();
        FRtoBR = cubieCube.getFRtoBR();
        URFtoDLF = cubieCube.getURFtoDLF();
        URtoUL = cubieCube.getURtoUL();
        UBtoDF = cubieCube.getUBtoDF();
        URtoDF = cubieCube.getURtoDF(); 
    }

    // PRUNING TABLES (Heuristics)
    // These tables store the minimum number of moves to reach the solved state.
    // I decided to use simple byte arrays instead of the original nibble packing 
    // to keep the code cleaner and easier to understand.

    // Phase 1 Tables
    public static byte[] Slice_Twist_Prune = new byte[NUM_SLICE_POSITIONS_PHASE1 * NUM_CORNER_ORIENTATIONS];
    public static byte[] Slice_Flip_Prune = new byte[NUM_SLICE_POSITIONS_PHASE1 * NUM_EDGE_ORIENTATIONS];

    // Phase 2 Tables
    public static byte[] Slice_URFtoDLF_Parity_Prune = new byte[NUM_SLICE_PERMUTATIONS_PHASE2 * NUM_CORNER_PERMUTATIONS * NUM_PARITIES];
    public static byte[] Slice_URtoDF_Parity_Prune = new byte[NUM_SLICE_PERMUTATIONS_PHASE2 * NUM_EDGE_PERMUTATIONS_PHASE2 * NUM_PARITIES];

    // Helpers to access the tables
    public static void setPruning(byte[] table, int index, byte value) {
        table[index] = value;
    }

    public static byte getPruning(byte[] table, int index) {
        return table[index];
    }

    // MOVE TABLES
    // These allow us to apply a move to a coordinate instantly (O(1)) without recalculating everything.
    public static short[][] twistMove = new short[NUM_CORNER_ORIENTATIONS][NUM_MOVES];
    public static short[][] flipMove = new short[NUM_EDGE_ORIENTATIONS][NUM_MOVES];
    public static short[][] parityMove = { {1, 0, 1, 1, 0, 1, 1, 0, 1, 1, 0, 1, 1, 0, 1, 1, 0, 1},
                                           {0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0} };
    public static short[][] FRtoBR_Move = new short[NUM_SLICE_EDGE_PERMUTATIONS][NUM_MOVES];
    public static short[][] URFtoDLF_Move = new short[NUM_CORNER_PERMUTATIONS][NUM_MOVES];
    public static short[][] URtoDF_Move = new short[NUM_EDGE_PERMUTATIONS_PHASE2][NUM_MOVES];
    public static short[][] URtoUL_Move = new short[NUM_EDGE_MERGE_UR_UL][NUM_MOVES];
    public static short[][] UBtoDF_Move = new short[NUM_EDGE_MERGE_UB_DF][NUM_MOVES];
    public static short[][] MergeURtoULandUBtoDF = new short[336][336];

    // STATIC INITIALIZATION
    // This runs once when the program starts to fill all the tables takes around 1-2 seconds
    static {
        // GENERATE MOVE TABLES
        // We simulate moves on a temporary cube to fill the lookup tables.
        CubieCube cc = new CubieCube();

        // Twist (Corner Orientation)
        for (short i = 0; i < NUM_CORNER_ORIENTATIONS; i++) {
            cc.setTwist(i);
            for (int m = 0; m < 6; m++) {
                for (int k = 0; k < 3; k++) {
                    cc.multiplyCorner(CubieCube.moves[m]);
                    twistMove[i][3 * m + k] = cc.getTwist();
                }
                cc.multiplyCorner(CubieCube.moves[m]); // restore
            }
        }

        // Flip (Edge Orientation)
        for (short i = 0; i < NUM_EDGE_ORIENTATIONS; i++) {
            cc.setFlip(i);
            for (int m = 0; m < 6; m++) {
                for (int k = 0; k < 3; k++) {
                    cc.multiplyEdge(CubieCube.moves[m]);
                    flipMove[i][3 * m + k] = cc.getFlip();
                }
                cc.multiplyEdge(CubieCube.moves[m]);
            }
        }

        // Slice Phase 1 Edges - Mapped via FRtoBR coordinate
        for (short i = 0; i < NUM_SLICE_EDGE_PERMUTATIONS; i++) {
            cc.setFRtoBR(i);
            for (int m = 0; m < 6; m++) {
                for (int k = 0; k < 3; k++) {
                    cc.multiplyEdge(CubieCube.moves[m]);
                    FRtoBR_Move[i][3 * m + k] = cc.getFRtoBR();
                }
                cc.multiplyEdge(CubieCube.moves[m]);
            }
        }

        // Phase 2 Corners
        for (short i = 0; i < NUM_CORNER_PERMUTATIONS; i++) {
            cc.setURFtoDLF(i);
            for (int m = 0; m < 6; m++) {
                for (int k = 0; k < 3; k++) {
                    cc.multiplyCorner(CubieCube.moves[m]);
                    URFtoDLF_Move[i][3 * m + k] = cc.getURFtoDLF();
                }
                cc.multiplyCorner(CubieCube.moves[m]);
            }
        }

        // Phase 2 Edges Main
        for (short i = 0; i < NUM_EDGE_PERMUTATIONS_PHASE2; i++) {
            cc.setURtoDF(i);
            for (int m = 0; m < 6; m++) {
                for (int k = 0; k < 3; k++) {
                    cc.multiplyEdge(CubieCube.moves[m]);
                    URtoDF_Move[i][3 * m + k] = (short) cc.getURtoDF();
                }
                cc.multiplyEdge(CubieCube.moves[m]);
            }
        }

        // Helpers for Phase 2 Edge merging
        for (short i = 0; i < NUM_EDGE_MERGE_UR_UL; i++) {
            cc.setURtoUL(i);
            for (int m = 0; m < 6; m++) {
                for (int k = 0; k < 3; k++) {
                    cc.multiplyEdge(CubieCube.moves[m]);
                    URtoUL_Move[i][3 * m + k] = cc.getURtoUL();
                }
                cc.multiplyEdge(CubieCube.moves[m]);
            }
        }
        for (short i = 0; i < NUM_EDGE_MERGE_UB_DF; i++) {
            cc.setUBtoDF(i);
            for (int m = 0; m < 6; m++) {
                for (int k = 0; k < 3; k++) {
                    cc.multiplyEdge(CubieCube.moves[m]);
                    UBtoDF_Move[i][3 * m + k] = cc.getUBtoDF();
                }
                cc.multiplyEdge(CubieCube.moves[m]);
            }
        }

        // Merge Table
        for (short u = 0; u < 336; u++) {
            for (short v = 0; v < 336; v++) {
                MergeURtoULandUBtoDF[u][v] = (short) CubieCube.getURtoDF(u, v);
            }
        }

        // GENERATE PRUNING TABLES with bfs backwards search 
        // We use Breadth-First Search to find the shortest distance from the solved state
        // to every other state in the coordinate graph.
        // Literally just open up to 18 around the current + 1 dist

        // Phase 1: Twist Pruning
        for(int i=0; i<Slice_Twist_Prune.length; i++) Slice_Twist_Prune[i] = -1; // -1 means unvisited
        setPruning(Slice_Twist_Prune, 0, (byte)0);
        int done = 1;
        int depth = 0;
        while(done < NUM_SLICE_POSITIONS_PHASE1 * NUM_CORNER_ORIENTATIONS) {
            for(int i=0; i<NUM_SLICE_POSITIONS_PHASE1 * NUM_CORNER_ORIENTATIONS; i++) {
                if(getPruning(Slice_Twist_Prune, i) == depth) {
                    int twist = i / NUM_SLICE_POSITIONS_PHASE1;
                    int slice = i % NUM_SLICE_POSITIONS_PHASE1;
                    for(int j=0; j<NUM_MOVES; j++) {
                        int newTwist = twistMove[twist][j];
                        int newSlice = FRtoBR_Move[slice * 24][j] / 24;
                        int idx = NUM_SLICE_POSITIONS_PHASE1 * newTwist + newSlice;
                        if(getPruning(Slice_Twist_Prune, idx) == -1) {
                            setPruning(Slice_Twist_Prune, idx, (byte)(depth + 1));
                            done++;
                        }
                    }
                }
            }
            depth++;
        }

        // Phase 1: Flip Pruning
        for(int i=0; i<Slice_Flip_Prune.length; i++) Slice_Flip_Prune[i] = -1;
        setPruning(Slice_Flip_Prune, 0, (byte)0);
        done = 1; depth = 0;
        while(done < NUM_SLICE_POSITIONS_PHASE1 * NUM_EDGE_ORIENTATIONS) {
            for(int i=0; i<NUM_SLICE_POSITIONS_PHASE1 * NUM_EDGE_ORIENTATIONS; i++) {
                if(getPruning(Slice_Flip_Prune, i) == depth) {
                    int flip = i / NUM_SLICE_POSITIONS_PHASE1;
                    int slice = i % NUM_SLICE_POSITIONS_PHASE1;
                    for(int j=0; j<NUM_MOVES; j++) {
                        int newFlip = flipMove[flip][j];
                        int newSlice = FRtoBR_Move[slice * 24][j] / 24;
                        int idx = NUM_SLICE_POSITIONS_PHASE1 * newFlip + newSlice;
                        if(getPruning(Slice_Flip_Prune, idx) == -1) {
                            setPruning(Slice_Flip_Prune, idx, (byte)(depth + 1));
                            done++;
                        }
                    }
                }
            }
            depth++;
        }

        // Phase 2: Corner + Slice + Parity Pruning
        for(int i=0; i<Slice_URFtoDLF_Parity_Prune.length; i++) Slice_URFtoDLF_Parity_Prune[i] = -1;
        setPruning(Slice_URFtoDLF_Parity_Prune, 0, (byte)0);
        done = 1; depth = 0;
        while(done < NUM_SLICE_PERMUTATIONS_PHASE2 * NUM_CORNER_PERMUTATIONS * NUM_PARITIES) {
            for(int i=0; i<NUM_SLICE_PERMUTATIONS_PHASE2 * NUM_CORNER_PERMUTATIONS * NUM_PARITIES; i++) {
                if(getPruning(Slice_URFtoDLF_Parity_Prune, i) == depth) {
                    int parity = i % 2;
                    int perm = (i / 2) / NUM_SLICE_PERMUTATIONS_PHASE2;
                    int slice = (i / 2) % NUM_SLICE_PERMUTATIONS_PHASE2;
                    for(int j=0; j<NUM_MOVES; j++) {
                        // Skip moves not allowed in Phase 2
                        if(j==3||j==5||j==6||j==8||j==12||j==14||j==15||j==17) continue; 
                        
                        int newSlice = FRtoBR_Move[slice][j];
                        int newPerm = URFtoDLF_Move[perm][j];
                        int newParity = parityMove[parity][j];
                        int idx = (NUM_SLICE_PERMUTATIONS_PHASE2 * newPerm + newSlice) * 2 + newParity;
                        if(getPruning(Slice_URFtoDLF_Parity_Prune, idx) == -1) {
                            setPruning(Slice_URFtoDLF_Parity_Prune, idx, (byte)(depth + 1));
                            done++;
                        }
                    }
                }
            }
            depth++;
        }

        // Phase 2: Edge + Slice + Parity Pruning
        for(int i=0; i<Slice_URtoDF_Parity_Prune.length; i++) Slice_URtoDF_Parity_Prune[i] = -1;
        setPruning(Slice_URtoDF_Parity_Prune, 0, (byte)0);
        done = 1; depth = 0;
        while(done < NUM_SLICE_PERMUTATIONS_PHASE2 * NUM_EDGE_PERMUTATIONS_PHASE2 * NUM_PARITIES) {
            for(int i=0; i<NUM_SLICE_PERMUTATIONS_PHASE2 * NUM_EDGE_PERMUTATIONS_PHASE2 * NUM_PARITIES; i++) {
                if(getPruning(Slice_URtoDF_Parity_Prune, i) == depth) {
                    int parity = i % 2;
                    int perm = (i / 2) / NUM_SLICE_PERMUTATIONS_PHASE2;
                    int slice = (i / 2) % NUM_SLICE_PERMUTATIONS_PHASE2;
                    for(int j=0; j<NUM_MOVES; j++) {
                        if(j==3||j==5||j==6||j==8||j==12||j==14||j==15||j==17) continue; 
                        
                        int newSlice = FRtoBR_Move[slice][j];
                        int newPerm = URtoDF_Move[perm][j];
                        int newParity = parityMove[parity][j];
                        int idx = (NUM_SLICE_PERMUTATIONS_PHASE2 * newPerm + newSlice) * 2 + newParity;
                        if(getPruning(Slice_URtoDF_Parity_Prune, idx) == -1) {
                            setPruning(Slice_URtoDF_Parity_Prune, idx, (byte)(depth + 1));
                            done++;
                        }
                    }
                }
            }
            depth++;
        }
    }
}