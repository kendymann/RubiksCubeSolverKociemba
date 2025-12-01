package rubikscube;

/*
 Attributions & Academic Integrity Notice
 ---------------------------------------
 This coordinate-level representation and its move/pruning tables are adapted
 from the published Kociemba two-phase algorithm concepts (http://kociemba.org/cube.htm)
 and widely circulated educational resources explaining twist/flip/slice and
 corner/edge permutation indexing. Core retained ideas:
     - Generation of move tables by exhaustive application of 6 face moves.
     - Pruning tables built via BFS with nibble-packed storage.
     - Combination/permutation indexing (binomial + factorial encoding).

 Personal contributions / modifications:
     - Rewritten explanatory comments for constants, loops, and pruning logic.
     - Integration with custom `CubieCube` implementation and higher-level solver code.
     - Minor stylistic organization for readability.

 Permitted Use Context:
 Researching and adapting known algorithms is allowed under CMPT 225 guidelines.
 Attribution supplied here; added comments and formatting are original work.
*/

// Representation of the cube on the coordinate level
public class CoordCube {
    // Const definitions mostly taken from Kociemba Official Documentation
    // Lots of refereneces used from Official docs and online forums, to make sure the mappings are correct

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

    public short twist;
    public short flip;
    public short parity;
    public short URFtoDLF;
    public short FRtoBR;
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
    // apply move m to all coordinates
    void move(int m) {
        twist = twistMove[twist][m]; // update twist
        flip = flipMove[flip][m]; // update flip
        parity = parityMove[parity][m]; // update parity
        FRtoBR = FRtoBR_Move[FRtoBR][m]; // update slice perm
        URFtoDLF = URFtoDLF_Move[URFtoDLF][m]; // update corner perm
        URtoUL = URtoUL_Move[URtoUL][m]; // update helper UR/UF/UL
        UBtoDF = UBtoDF_Move[UBtoDF][m]; // update helper UB/DR/DF
        // merge only valid helper ranges (both < 336)
        if (URtoUL < 336 && UBtoDF < 336) {
            URtoDF = MergeURtoULandUBtoDF[URtoUL][UBtoDF]; // compute merged
        }
    }

    // corner twist move table [twist][move] -> new twist
    public static short[][] twistMove = new short[NUM_CORNER_ORIENTATIONS][NUM_MOVES];

    static {
        CubieCube cube = new CubieCube(); // temp workspace cube
        for (short i = 0; i < NUM_CORNER_ORIENTATIONS; i++) { // iterate all twists
            cube.setTwist(i); // set base twist state
            for (int j = 0; j < 6; j++) { // six face moves
                for (int k = 0; k < 3; k++) { // 3 quarter-turn variants
                    cube.multiplyCorner(CubieCube.moves[j]); // apply twist
                    twistMove[i][3 * j + k] = cube.getTwist(); // record result
                }
                cube.multiplyCorner(CubieCube.moves[j]); // 4th turn to restore
            }
        }
    }

    // edge flip move table [flip][move] -> new flip
    public static short[][] flipMove = new short[NUM_EDGE_ORIENTATIONS][NUM_MOVES];

    static {
        CubieCube cube = new CubieCube(); // temp workspace cube
        for (short i = 0; i < NUM_EDGE_ORIENTATIONS; i++) { // iterate all flips
            cube.setFlip(i); // set base flip state
            for (int j = 0; j < 6; j++) { // six face moves
                for (int k = 0; k < 3; k++) { // 3 quarter-turn variants
                    cube.multiplyEdge(CubieCube.moves[j]); // apply flip
                    flipMove[i][3 * j + k] = cube.getFlip(); // record result
                }
                cube.multiplyEdge(CubieCube.moves[j]); // 4th turn to restore
            }
        }
    }

    // parity move table: toggles based on move type
    public static short[][] parityMove = {
            {1, 0, 1, 1, 0, 1, 1, 0, 1, 1, 0, 1, 1, 0, 1, 1, 0, 1},
            {0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0}
    };

    // FR..BR slice edges move table
    public static short[][] FRtoBR_Move = new short[NUM_SLICE_EDGE_PERMUTATIONS][NUM_MOVES];

    static {
        CubieCube cube = new CubieCube(); // temp workspace cube
        for (short i = 0; i < NUM_SLICE_EDGE_PERMUTATIONS; i++) { // iterate all slice perms
            cube.setFRtoBR(i); // set base slice state
            for (int j = 0; j < 6; j++) { // six face moves
                for (int k = 0; k < 3; k++) { // 3 variants
                    cube.multiplyEdge(CubieCube.moves[j]); // apply
                    FRtoBR_Move[i][3 * j + k] = cube.getFRtoBR(); // record
                }
                cube.multiplyEdge(CubieCube.moves[j]); // restore
            }
        }
    }

    // URF..DLF corner permutation move table
    public static short[][] URFtoDLF_Move = new short[NUM_CORNER_PERMUTATIONS][NUM_MOVES];

    static {
        CubieCube cube = new CubieCube(); // temp workspace cube
        for (short i = 0; i < NUM_CORNER_PERMUTATIONS; i++) { // iterate corner perms
            cube.setURFtoDLF(i); // set base corner state
            for (int j = 0; j < 6; j++) { // six face moves
                for (int k = 0; k < 3; k++) { // 3 variants
                    cube.multiplyCorner(CubieCube.moves[j]); // apply
                    URFtoDLF_Move[i][3 * j + k] = cube.getURFtoDLF(); // record
                }
                cube.multiplyCorner(CubieCube.moves[j]); // restore
            }
        }
    }

    // UR..DF edge permutation move table (phase-2)
    public static short[][] URtoDF_Move = new short[NUM_EDGE_PERMUTATIONS_PHASE2][NUM_MOVES];

    static {
        CubieCube cube = new CubieCube(); // temp workspace cube
        for (short i = 0; i < NUM_EDGE_PERMUTATIONS_PHASE2; i++) { // iterate edge perms
            cube.setURtoDF(i); // set base edge state
            for (int j = 0; j < 6; j++) { // six face moves
                for (int k = 0; k < 3; k++) { // 3 variants
                    cube.multiplyEdge(CubieCube.moves[j]); // apply
                    URtoDF_Move[i][3 * j + k] = (short) cube.getURtoDF(); // record (phase-2 only)
                }
                cube.multiplyEdge(CubieCube.moves[j]); // restore
            }
        }
    }

    // helper: UR/UF/UL edges move table
    public static short[][] URtoUL_Move = new short[NUM_EDGE_MERGE_UR_UL][NUM_MOVES];

    static {
        CubieCube cube = new CubieCube(); // temp workspace cube
        for (short i = 0; i < NUM_EDGE_MERGE_UR_UL; i++) { // iterate helper coord
            cube.setURtoUL(i); // set base helper state
            for (int j = 0; j < 6; j++) { // six face moves
                for (int k = 0; k < 3; k++) { // 3 variants
                    cube.multiplyEdge(CubieCube.moves[j]); // apply
                    URtoUL_Move[i][3 * j + k] = cube.getURtoUL(); // record
                }
                cube.multiplyEdge(CubieCube.moves[j]); // restore
            }
        }
    }

    // helper: UB/DR/DF edges move table
    public static short[][] UBtoDF_Move = new short[NUM_EDGE_MERGE_UB_DF][NUM_MOVES];

    static {
        CubieCube cube = new CubieCube(); // temp workspace cube
        for (short i = 0; i < NUM_EDGE_MERGE_UB_DF; i++) { // iterate helper coord
            cube.setUBtoDF(i); // set base helper state
            for (int j = 0; j < 6; j++) { // six face moves
                for (int k = 0; k < 3; k++) { // 3 variants
                    cube.multiplyEdge(CubieCube.moves[j]); // apply
                    UBtoDF_Move[i][3 * j + k] = cube.getUBtoDF(); // record
                }
                cube.multiplyEdge(CubieCube.moves[j]); // restore
            }
        }
    }

    // merge helper coords -> phase-2 UR..DF edge coord
    public static short[][] MergeURtoULandUBtoDF = new short[336][336];

    static {
        // both helpers < 336 means edges outside slice; valid for merge
        for (short uRtoUL = 0; uRtoUL < 336; uRtoUL++) { // iterate first helper
            for (short uBtoDF = 0; uBtoDF < 336; uBtoDF++) { // iterate second helper
                MergeURtoULandUBtoDF[uRtoUL][uBtoDF] = (short) CubieCube.getURtoDF(uRtoUL, uBtoDF); // compute merged
            }
        }
    }

    // store pruning nibble (two entries per byte)
    public static void setPruning(byte[] table, int index, byte value) {
        if ((index & 1) == 0)
            table[index / 2] &= 0xf0 | value; // write low nibble
        else
            table[index / 2] &= 0x0f | (value << 4); // write high nibble
    }

    // read pruning nibble (two entries per byte)
    public static byte getPruning(byte[] table, int index) {
        if ((index & 1) == 0)
            return (byte) (table[index / 2] & 0x0f); // read low nibble
        else
            return (byte) ((table[index / 2] & 0xf0) >>> 4); // read high nibble
    }

    // phase-2 pruning: slice + corner + parity
    public static byte[] Slice_URFtoDLF_Parity_Prune = new byte[NUM_SLICE_PERMUTATIONS_PHASE2 * NUM_CORNER_PERMUTATIONS * NUM_PARITIES / 2];

    static {
        for (int i = 0; i < NUM_SLICE_PERMUTATIONS_PHASE2 * NUM_CORNER_PERMUTATIONS * NUM_PARITIES / 2; i++) {
            Slice_URFtoDLF_Parity_Prune[i] = -1; // init all entries to -1 (unknown)
        }
        int depth = 0; // BFS depth
        setPruning(Slice_URFtoDLF_Parity_Prune, 0, (byte) 0); // solved state at depth 0
        int done = 1; // number of filled states
        while (done != NUM_SLICE_PERMUTATIONS_PHASE2 * NUM_CORNER_PERMUTATIONS * NUM_PARITIES) { // until full table
            for (int i = 0; i < NUM_SLICE_PERMUTATIONS_PHASE2 * NUM_CORNER_PERMUTATIONS * NUM_PARITIES; i++) { // scan all states
                int parity = i % 2; // current parity
                int URFtoDLF = (i / 2) / NUM_SLICE_PERMUTATIONS_PHASE2; // corner coord
                int slice = (i / 2) % NUM_SLICE_PERMUTATIONS_PHASE2; // slice coord
                if (getPruning(Slice_URFtoDLF_Parity_Prune, i) == depth) { // frontier at this depth
                    for (int j = 0; j < 18; j++) { // try all moves
                        switch (j) { // skip redundant double turns for pruning speed
                            case 3, 5, 6, 8, 12, 14, 15, 17 -> {
                                continue; // skip
                            }
                            default -> {
                                int newSlice = FRtoBR_Move[slice][j]; // next slice
                                int newURFtoDLF = URFtoDLF_Move[URFtoDLF][j]; // next corner perm
                                int newParity = parityMove[parity][j]; // next parity
                                int idx = (NUM_SLICE_PERMUTATIONS_PHASE2 * newURFtoDLF + newSlice) * 2 + newParity; // packed index
                                if (getPruning(Slice_URFtoDLF_Parity_Prune, idx) == 0x0f) { // if not set yet
                                    setPruning(Slice_URFtoDLF_Parity_Prune, idx, (byte) (depth + 1)); // set depth+1
                                    done++; // progress
                                }
                            }
                        }
                        // skip redundant double turns for pruning speed
                                            }
                }
            }
            depth++; // next BFS layer
        }
    }

    // phase-2 pruning: slice + edge + parity
    public static byte[] Slice_URtoDF_Parity_Prune = new byte[NUM_SLICE_PERMUTATIONS_PHASE2 * NUM_EDGE_PERMUTATIONS_PHASE2 * NUM_PARITIES / 2];

    static {
        for (int i = 0; i < NUM_SLICE_PERMUTATIONS_PHASE2 * NUM_EDGE_PERMUTATIONS_PHASE2 * NUM_PARITIES / 2; i++)
            Slice_URtoDF_Parity_Prune[i] = -1; // init unknown
        int depth = 0; // BFS depth
        setPruning(Slice_URtoDF_Parity_Prune, 0, (byte) 0); // solved state
        int done = 1; // filled entries
        while (done != NUM_SLICE_PERMUTATIONS_PHASE2 * NUM_EDGE_PERMUTATIONS_PHASE2 * NUM_PARITIES) { // until full
            for (int i = 0; i < NUM_SLICE_PERMUTATIONS_PHASE2 * NUM_EDGE_PERMUTATIONS_PHASE2 * NUM_PARITIES; i++) { // scan states
                int parity = i % 2; // current parity
                int URtoDF = (i / 2) / NUM_SLICE_PERMUTATIONS_PHASE2; // edge coord
                int slice = (i / 2) % NUM_SLICE_PERMUTATIONS_PHASE2; // slice coord
                if (getPruning(Slice_URtoDF_Parity_Prune, i) == depth) { // frontier
                    for (int j = 0; j < 18; j++) { // moves
                        switch (j) { // skip redundant double turns
                            case 3, 5, 6, 8, 12, 14, 15, 17 -> {
                                continue; // skip
                            }
                            default -> {
                                int newSlice = FRtoBR_Move[slice][j]; // next slice
                                int newURtoDF = URtoDF_Move[URtoDF][j]; // next edge perm
                                int newParity = parityMove[parity][j]; // next parity
                                int idx = (NUM_SLICE_PERMUTATIONS_PHASE2 * newURtoDF + newSlice) * 2 + newParity; // packed
                                if (getPruning(Slice_URtoDF_Parity_Prune, idx) == 0x0f) { // if unset
                                    setPruning(Slice_URtoDF_Parity_Prune, idx, (byte) (depth + 1)); // set
                                    done++; // progress
                                }
                            }
                        }
                        // skip redundant double turns
                                            }
                }
            }
            depth++; // next layer
        }
    }

    // phase-1 pruning: slice position + corner twist
    public static byte[] Slice_Twist_Prune = new byte[NUM_SLICE_POSITIONS_PHASE1 * NUM_CORNER_ORIENTATIONS / 2 + 1];

    static {
        for (int i = 0; i < NUM_SLICE_POSITIONS_PHASE1 * NUM_CORNER_ORIENTATIONS / 2 + 1; i++)
            Slice_Twist_Prune[i] = -1; // init unknown

        int depth = 0; // BFS depth
        setPruning(Slice_Twist_Prune, 0, (byte) 0); // solved state
        int done = 1; // filled entries
        while (done != NUM_SLICE_POSITIONS_PHASE1 * NUM_CORNER_ORIENTATIONS) { // until full
            for (int i = 0; i < NUM_SLICE_POSITIONS_PHASE1 * NUM_CORNER_ORIENTATIONS; i++) { // scan states
                int twist = i / NUM_SLICE_POSITIONS_PHASE1; // corner twist coord
                int slice = i % NUM_SLICE_POSITIONS_PHASE1; // slice position coord
                if (getPruning(Slice_Twist_Prune, i) == depth) { // frontier
                    for (int j = 0; j < 18; j++) { // moves
                        int newSlice = FRtoBR_Move[slice * 24][j] / 24; // project to position
                        int newTwist = twistMove[twist][j]; // update twist
                        int idx = NUM_SLICE_POSITIONS_PHASE1 * newTwist + newSlice; // packed index
                        if (getPruning(Slice_Twist_Prune, idx) == 0x0f) { // if unset
                            setPruning(Slice_Twist_Prune, idx, (byte) (depth + 1)); // set
                            done++; // progress
                        }
                    }
                }
            }
            depth++; // next layer
        }
    }

    // phase-1 pruning: slice position + edge flip
    public static byte[] Slice_Flip_Prune = new byte[NUM_SLICE_POSITIONS_PHASE1 * NUM_EDGE_ORIENTATIONS / 2];

    static {
        for (int i = 0; i < NUM_SLICE_POSITIONS_PHASE1 * NUM_EDGE_ORIENTATIONS / 2; i++)
            Slice_Flip_Prune[i] = -1; // init unknown
        int depth = 0; // BFS depth
        setPruning(Slice_Flip_Prune, 0, (byte) 0); // solved state
        int done = 1; // filled entries
        while (done != NUM_SLICE_POSITIONS_PHASE1 * NUM_EDGE_ORIENTATIONS) { // until full
            for (int i = 0; i < NUM_SLICE_POSITIONS_PHASE1 * NUM_EDGE_ORIENTATIONS; i++) { // scan states
                int flip = i / NUM_SLICE_POSITIONS_PHASE1; // edge flip coord
                int slice = i % NUM_SLICE_POSITIONS_PHASE1; // slice position coord
                if (getPruning(Slice_Flip_Prune, i) == depth) { // frontier
                    for (int j = 0; j < 18; j++) { // moves
                        int newSlice = FRtoBR_Move[slice * 24][j] / 24; // project to position
                        int newFlip = flipMove[flip][j]; // update flip
                        int idx = NUM_SLICE_POSITIONS_PHASE1 * newFlip + newSlice; // packed index
                        if (getPruning(Slice_Flip_Prune, idx) == 0x0f) { // if unset
                            setPruning(Slice_Flip_Prune, idx, (byte) (depth + 1)); // set
                            done++; // progress
                        }
                    }
                }
            }
            depth++; // next layer
        }
    }
}