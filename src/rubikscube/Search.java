package rubikscube;

/*
 Attributions & Academic Integrity Notice
 ---------------------------------------
 This search implementation follows the structure of the Kociemba two-phase
 algorithm: phase-1 IDA* to reach subgroup H (with twist/flip/slice heuristics)
 and phase-2 restricted moves using parity + permutation pruning tables.
*/

import java.io.*;


// Class Search implements the Two-Phase-Algorithm
// I like to think of this as the brain it runs two-stage IDA* searches
// and uses a bunch of precomputed tables in CoordCube to make things fast.
public class Search {

    // search trace storage 
    // We store the axis (U,R,F,D,L,B -> 0 to 5) and the power (1=90deg,2=180deg,3=270deg) makes it easy for translation later
    static int[] axis = new int[31];
    static int[] power = new int[31];

    // phase-1 coordinates kept along the search path
    static int[] flip = new int[31];   // edge flip coordinate
    static int[] twist = new int[31];  // corner twist coordinate
    static int[] slice = new int[31];  // slice coordinate (coarse edge grouping)

    // phase-2 coordinates (used after reaching subgroup H)
    static int[] parity = new int[31];   // corner/edge parity
    static int[] URFtoDLF = new int[31]; // URF-to-DLF corner index
    static int[] FRtoBR = new int[31];   // FR-to-BR edge index
    static int[] URtoUL = new int[31];   // UR-to-UL edge index
    static int[] UBtoDF = new int[31];   // UB-to-DF edge index
    static int[] URtoDF = new int[31];   // merged UR-to-DF index used for pruning

    // IDA* heuristic estimates (from pruning tables in CoordCube)
    static int[] minDistPhase1 = new int[31];
    static int[] minDistPhase2 = new int[31];

    // generate the solution string from the axis/power arrays
    // also translate F' -> FFF and F2 -> FF to keep a simple move alphabet
    static String solutionToString(int length) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < length; i++) {
            switch (power[i]) {
                case 1 -> {
                    switch(axis[i]) {
                        case 0 -> s.append("U");
                        case 1 -> s.append("R");
                        case 2 -> s.append("F");
                        case 3 -> s.append("D");
                        case 4 -> s.append("L");
                        case 5 -> s.append("B");
                    }
                }
                    
                case 2 -> {
                    switch(axis[i]) {
                        case 0 -> s.append("UU");
                        case 1 -> s.append("RR");
                        case 2 -> s.append("FF");
                        case 3 -> s.append("DD");
                        case 4 -> s.append("LL");
                        case 5 -> s.append("BB");
                    }
                }
                   
                case 3 -> {
                    switch(axis[i]) {
                        case 0 -> s.append("UUU");
                        case 1 -> s.append("RRR");
                        case 2 -> s.append("FFF");
                        case 3 -> s.append("DDD");
                        case 4 -> s.append("LLL");
                        case 5 -> s.append("BBB");
                    }
                }
                
        }
    }
    return s.toString();
}

    // Compute the solver string for a given cubie-level cube state
    // maxDepth caps the total allowed moves (phase1 + phase2)
    // timeOut in seconds limits the total runtime (safety valve)
    // Returns a move string or "Error X" codes on malformed / unsolvable inputs
    public static String solution(CubieCube CC, int maxDepth, long timeOut) throws IOException, IncorrectFormatException {
        int s;

        // Quick sanity check (structure and parity). Returns Error codes if the cube is malformed.
        if ((s = CC.verify()) != 0)
            return "Error " + Math.abs(s);

        // initialize coordinates from the cubie-level state
        CoordCube c = new CoordCube(CC);

        // prime the search arrays with the starting coordinates
        power[0] = 0;
        axis[0] = 0;
        flip[0] = c.flip;
        twist[0] = c.twist;
        parity[0] = c.parity;
        slice[0] = c.FRtoBR / 24;
        URFtoDLF[0] = c.URFtoDLF;
        FRtoBR[0] = c.FRtoBR;
        URtoUL[0] = c.URtoUL;
        UBtoDF[0] = c.UBtoDF;

        // small safety: ensures IDA* doesn't instantly fail for depth=1
        minDistPhase1[1] = 1;
        int mv, n = 0;
        boolean busy = false;
        int depthPhase1 = 1;

        long tStart = System.currentTimeMillis(); // for timrout

        // Main loop for phase-1
        // We iterate over increasing depthPhase1 until we find a depth where phase-2 can finish.
        // Also note this ida star algorithm is more optimized for speed because we dont actually use nodes or a stack we do Structure of arrays
        // Its like a manual stack in a way but we just keep track of the depth and arrays of coords in the indexes of arrays like parallel lists
        do {
            do {
                if ((depthPhase1 - n > minDistPhase1[n + 1]) && !busy) {

                    // initialize next move: alternate axes to avoid repeating same face immediately
                    if (axis[n] == 0 || axis[n] == 3) { // U or D were last
                        axis[++n] = 1; // start with R
                    } else {
                        axis[++n] = 0; // otherwise start with U
                    }
                    power[n] = 1; // start with quarter-turn
                } else if (++power[n] > 3) {
                    // power overflow so increment axis
                    do {
                        if (++axis[n] > 5) {

                            // timeout check in s
                            if (System.currentTimeMillis() - tStart > timeOut << 10)
                                return "Error 8";

                            if (n == 0) {
                                if (depthPhase1 >= maxDepth)
                                    return "Error 7"; // depth exceeded
                                else {
                                    depthPhase1++;
                                    axis[n] = 0;
                                    power[n] = 1;
                                    busy = false;
                                    break;
                                }
                            } else {
                                n--;
                                busy = true;
                                break;
                            }

                        } else {
                            power[n] = 1;
                            busy = false;
                        }
                    } while (n != 0 && (axis[n - 1] == axis[n] || axis[n - 1] - 3 == axis[n]));
                } else
                    busy = false;
            } while (busy);

            // compute new coordinates after appending the chosen move
            mv = 3 * axis[n] + power[n] - 1;
            flip[n + 1] = CoordCube.flipMove[flip[n]][mv];
            twist[n + 1] = CoordCube.twistMove[twist[n]][mv];
            slice[n + 1] = CoordCube.FRtoBR_Move[slice[n] * 24][mv] / 24;

            // heuristic is combine flip and twist pruning values then take the max
            minDistPhase1[n + 1] = Math.max(
                    CoordCube.getPruning(CoordCube.Slice_Flip_Prune, CoordCube.NUM_SLICE_POSITIONS_PHASE1 * flip[n + 1] + slice[n + 1]),
                    CoordCube.getPruning(CoordCube.Slice_Twist_Prune, CoordCube.NUM_SLICE_POSITIONS_PHASE1 * twist[n + 1] + slice[n + 1]));

            // If we reached the H subgroup minDist==0 and are near the current depth, try phase-2
            if (minDistPhase1[n + 1] == 0 && n >= depthPhase1 - 5) {
                minDistPhase1[n + 1] = 10; // bump so we don't repeatedly trigger here
                if (n == depthPhase1 - 1 && (s = totalDepth(depthPhase1, maxDepth)) >= 0) {
                    if (s == depthPhase1 || (axis[depthPhase1 - 1] != axis[depthPhase1] && axis[depthPhase1 - 1] != axis[depthPhase1] + 3))
                        return solutionToString(s);
                }

            }
        } while (true);
    }

    // Apply phase2 of algorithm and return the combined phase1 and phase2 depth. In phase2, only the moves
    // U,D,R2,F2,L2 and B2 are allowed.
    public static int totalDepth(int depthPhase1, int maxDepth) {
        int mv, d1, d2;
        int maxDepthPhase2 = Math.min(10, maxDepth - depthPhase1);    // Allow only max 10 moves in phase2
        for (int i = 0; i < depthPhase1; i++) {
            mv = 3 * axis[i] + power[i] - 1;
            URFtoDLF[i + 1] = CoordCube.URFtoDLF_Move[URFtoDLF[i]][mv];
            FRtoBR[i + 1] = CoordCube.FRtoBR_Move[FRtoBR[i]][mv];
            parity[i + 1] = CoordCube.parityMove[parity[i]][mv];
        }

        if ((d1 = CoordCube.getPruning(CoordCube.Slice_URFtoDLF_Parity_Prune,
                (CoordCube.NUM_SLICE_PERMUTATIONS_PHASE2 * URFtoDLF[depthPhase1] + FRtoBR[depthPhase1]) * 2 + parity[depthPhase1])) > maxDepthPhase2)
            return -1;

        for (int i = 0; i < depthPhase1; i++) {
            mv = 3 * axis[i] + power[i] - 1;
            URtoUL[i + 1] = CoordCube.URtoUL_Move[URtoUL[i]][mv];
            UBtoDF[i + 1] = CoordCube.UBtoDF_Move[UBtoDF[i]][mv];
        }
        URtoDF[depthPhase1] = CoordCube.MergeURtoULandUBtoDF[URtoUL[depthPhase1]][UBtoDF[depthPhase1]];

        if ((d2 = CoordCube.getPruning(CoordCube.Slice_URtoDF_Parity_Prune,
                (CoordCube.NUM_SLICE_PERMUTATIONS_PHASE2 * URtoDF[depthPhase1] + FRtoBR[depthPhase1]) * 2 + parity[depthPhase1])) > maxDepthPhase2)
            return -1;

        if ((minDistPhase2[depthPhase1] = Math.max(d1, d2)) == 0)// already solved
            return depthPhase1;

        // now set up search

        int depthPhase2 = 1;
        int n = depthPhase1;
        boolean busy = false;
        power[depthPhase1] = 0;
        axis[depthPhase1] = 0;
        minDistPhase2[n + 1] = 1;    // else failure for depthPhase2=1, n=0

        do {
            do {
                if ((depthPhase1 + depthPhase2 - n > minDistPhase2[n + 1]) && !busy) {

                    if (axis[n] == 0 || axis[n] == 3) {  // Initialize next move
                        axis[++n] = 1;
                        power[n] = 2;
                    } else {
                        axis[++n] = 0;
                        power[n] = 1;
                    }
                } else if ((axis[n] == 0 || axis[n] == 3) ? (++power[n] > 3) : ((power[n] = power[n] + 2) > 3)) {
                    do {            // increment axis
                        if (++axis[n] > 5) {
                            if (n == depthPhase1) {
                                if (depthPhase2 >= maxDepthPhase2)
                                    return -1;
                                else {
                                    depthPhase2++;
                                    axis[n] = 0;
                                    power[n] = 1;
                                    busy = false;
                                    break;
                                }
                            } else {
                                n--;
                                busy = true;
                                break;
                            }

                        } else {
                            if (axis[n] == 0 || axis[n] == 3)
                                power[n] = 1;
                            else
                                power[n] = 2;
                            busy = false;
                        }
                    } while (n != depthPhase1 && (axis[n - 1] == axis[n] || axis[n - 1] - 3 == axis[n]));
                } else
                    busy = false;
            } while (busy);

            // compute new coordinates and new minDist
            mv = 3 * axis[n] + power[n] - 1;

            URFtoDLF[n + 1] = CoordCube.URFtoDLF_Move[URFtoDLF[n]][mv];
            FRtoBR[n + 1] = CoordCube.FRtoBR_Move[FRtoBR[n]][mv];
            parity[n + 1] = CoordCube.parityMove[parity[n]][mv];
            URtoDF[n + 1] = CoordCube.URtoDF_Move[URtoDF[n]][mv];

            minDistPhase2[n + 1] = Math.max(CoordCube.getPruning(CoordCube.Slice_URtoDF_Parity_Prune, (CoordCube.NUM_SLICE_PERMUTATIONS_PHASE2
                    * URtoDF[n + 1] + FRtoBR[n + 1])
                    * 2 + parity[n + 1]), CoordCube.getPruning(CoordCube.Slice_URFtoDLF_Parity_Prune, (CoordCube.NUM_SLICE_PERMUTATIONS_PHASE2
                    * URFtoDLF[n + 1] + FRtoBR[n + 1])
                    * 2 + parity[n + 1]));

        } while (minDistPhase2[n + 1] != 0);
        return depthPhase1 + depthPhase2;
    }
}
