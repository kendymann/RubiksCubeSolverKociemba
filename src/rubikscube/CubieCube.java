package rubikscube;

/*
 This file adapts the well-known two-phase Rubik's Cube solving approach as
 described by Herbert Kociemba (http://kociemba.org/cube.htm) and commonly
 referenced open-source implementations. Core ideas retained:
     - Coordinate encodings (twist, flip, slice, corner/edge permutations).
     - Move template application for the 6 faces with 3 quarter-turn variants.
     - Combination + permutation indexing using factorial / binomial logic.
*/

import java.util.Arrays; 
import static rubikscube.Corner.*;
import static rubikscube.Edge.*;

public class CubieCube {

    // Essentially these are just all hard coded move templates and the current state of the cube on a cubie level
    // Corner permutation (index aligns with Corner enum)
    // Note: cornerPermutation[position] == which corner cubie currently sits at that position.
    // e.g. cornerPermutation[URF] == DFR means the DFR cubie currently occupies the URF slot.
    public Corner[] cornerPermutation = {URF, UFL, ULB, UBR, DFR, DLF, DBL, DRB};

    // Corner orientation (0..2)
    // Note: orientations are 0..2 and the last corner orientation is implied by the first seven.
    public byte[] cornerOrientation = {0, 0, 0, 0, 0, 0, 0, 0};

    // Edge permutation (index aligns with Edge enum)
    // Note: edgePermutation[position] == which edge cubie is at position.
    public Edge[] edgePermutation = {UR, UF, UL, UB, DR, DF, DL, DB, FR, FL, BL, BR};

    // Edge orientation (0..1)
    // Note: only first 11 bits are independent; the 12th is implied to keep parity.
    public byte[] edgeOrientation = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

    // Single-face quarter-turn templates
    // U face (clockwise)
    public static Corner[] cornerPermutationUp = {UBR, URF, UFL, ULB, DFR, DLF, DBL, DRB};
    public static byte[] cornerOrientationUp = {0, 0, 0, 0, 0, 0, 0, 0};
    public static Edge[] edgePermutationUp = {UB, UR, UF, UL, DR, DF, DL, DB, FR, FL, BL, BR};
    public static byte[] edgeOrientationUp = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

    // R face (clockwise)
    public static Corner[] cornerPermutationRight = {DFR, UFL, ULB, URF, DRB, DLF, DBL, UBR};
    public static byte[] cornerOrientationRight = {2, 0, 0, 1, 1, 0, 0, 2};
    public static Edge[] edgePermutationRight = {FR, UF, UL, UB, BR, DF, DL, DB, DR, FL, BL, UR};
    public static byte[] edgeOrientationRight = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

    // F face (clockwise)
    public static Corner[] cornerPermutationFront = {UFL, DLF, ULB, UBR, URF, DFR, DBL, DRB};
    public static byte[] cornerOrientationFront = {1, 2, 0, 0, 2, 1, 0, 0};
    public static Edge[] edgePermutationFront = {UR, FL, UL, UB, DR, FR, DL, DB, UF, DF, BL, BR};
    public static byte[] edgeOrientationFront = {0, 1, 0, 0, 0, 1, 0, 0, 1, 1, 0, 0};

    // D face (clockwise)
    public static Corner[] cornerPermutationDown = {URF, UFL, ULB, UBR, DLF, DBL, DRB, DFR};
    public static byte[] cornerOrientationDown = {0, 0, 0, 0, 0, 0, 0, 0};
    public static Edge[] edgePermutationDown = {UR, UF, UL, UB, DF, DL, DB, DR, FR, FL, BL, BR};
    public static byte[] edgeOrientationDown = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

    // L face (clockwise)
    public static Corner[] cornerPermutationLeft = {URF, ULB, DBL, UBR, DFR, UFL, DLF, DRB};
    public static byte[] cornerOrientationLeft = {0, 1, 2, 0, 0, 2, 1, 0};
    public static Edge[] edgePermutationLeft = {UR, UF, BL, UB, DR, DF, FL, DB, FR, UL, DL, BR};
    public static byte[] edgeOrientationLeft = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

    // B face (clockwise)
    public static Corner[] cornerPermutationBack = {URF, UFL, UBR, DRB, DFR, DLF, ULB, DBL};
    public static byte[] cornerOrientationBack = {0, 0, 1, 2, 0, 0, 2, 1};
    public static Edge[] edgePermutationBack = {UR, UF, UL, BR, DR, DF, DL, BL, FR, FL, UB, DB};
    public static byte[] edgeOrientationBack = {0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 1, 1};

    // Store the 6 face templates (U,R,F,D,L,B)
    public static CubieCube[] moves = new CubieCube[6];

    static {
        moves[0] = new CubieCube();
        moves[0].cornerPermutation = cornerPermutationUp;
        moves[0].cornerOrientation = cornerOrientationUp;
        moves[0].edgePermutation = edgePermutationUp;
        moves[0].edgeOrientation = edgeOrientationUp;

        moves[1] = new CubieCube();
        moves[1].cornerPermutation = cornerPermutationRight;
        moves[1].cornerOrientation = cornerOrientationRight;
        moves[1].edgePermutation = edgePermutationRight;
        moves[1].edgeOrientation = edgeOrientationRight;

        moves[2] = new CubieCube();
        moves[2].cornerPermutation = cornerPermutationFront;
        moves[2].cornerOrientation = cornerOrientationFront;
        moves[2].edgePermutation = edgePermutationFront;
        moves[2].edgeOrientation = edgeOrientationFront;

        moves[3] = new CubieCube();
        moves[3].cornerPermutation = cornerPermutationDown;
        moves[3].cornerOrientation = cornerOrientationDown;
        moves[3].edgePermutation = edgePermutationDown;
        moves[3].edgeOrientation = edgeOrientationDown;

        moves[4] = new CubieCube();
        moves[4].cornerPermutation = cornerPermutationLeft;
        moves[4].cornerOrientation = cornerOrientationLeft;
        moves[4].edgePermutation = edgePermutationLeft;
        moves[4].edgeOrientation = edgeOrientationLeft;

        moves[5] = new CubieCube();
        moves[5].cornerPermutation = cornerPermutationBack;
        moves[5].cornerOrientation = cornerOrientationBack;
        moves[5].edgePermutation = edgePermutationBack;
        moves[5].edgeOrientation = edgeOrientationBack;
    }

    public CubieCube() {

    }

    // n choose k ( lol macm 101 flashbacks )
    public static int nCk(int n, int k) {
        if (n < k) {
            return 0;
        }
        if (k > n / 2) {
            k = n - k;
        }
        int p,i,j;
        for (p=1,i = n, j = 1; i != n - k; i--, j++) {
            p *= i;
            p /= j;
        }
        return p;
    }

    // Rotate corners left over [l..r]
    public static void leftRotateCorner(Corner[] corners, int l, int r) {
        Corner corner = corners[l];
        for (int i = l; i < r; i++) {
            corners[i] = corners[i + 1];
        }
        corners[r] = corner;
    }

    // Rotate corners right over [l..r]
    public static void rightRotateCorner(Corner[] corners, int l, int r) {
        Corner corner = corners[r];
        for (int i = r; i > l; i--) {
            corners[i] = corners[i - 1];
        }
        corners[l] = corner;
    }

    // Rotate edges left over [l..r]
    public static void leftRotateEdge(Edge[] edges, int l, int r) {
        Edge edge = edges[l];
        for (int i = l; i < r; i++) {
            edges[i] = edges[i + 1];
        }
        edges[r] = edge;
    }

    // Rotate edges right over [l..r]
    public static void rightRotateEdge(Edge[] edges, int l, int r) {
        Edge edge = edges[r];
        for (int i = r; i > l; i--) {
            edges[i] = edges[i - 1];
        }
        edges[l] = edge;
    }

    // Convert cubie state -> facelet state
    // maps each cubie's sticker colours into the 54 facelets using FaceletCube lookup tables.
    // orientation offsets ((k+orient)%3 or %2) rotate the sticker order for that piece.
    public FaceletCube toFaceletCube() {
        FaceletCube FaceletCube = new FaceletCube();
        for (Corner c : Corner.values()) {
            int i = c.ordinal();
            int j = cornerPermutation[i].ordinal();   // cornerCubie with index j at corner position with index i
            byte orient = cornerOrientation[i];       // orientation of this cubie
            for (int k = 0; k < 3; k++) {       // as the corner has 3 sides(use physical cube to visualize)
                FaceletCube.colours[FaceletCube.cornerFacelet[i][(k + orient) % 3].ordinal()] = FaceletCube.cornerColour[j][k];
            }
        }
        for (Edge e : Edge.values()) { // Wished java had auto like c++
            int i = e.ordinal();
            int j = edgePermutation[i].ordinal();
            byte orient = edgeOrientation[i];
            for (int k = 0; k < 2; k++) {    // as the edge has two sides(use physical cube to visualize)
                FaceletCube.colours[FaceletCube.edgeFacelet[i][(k + orient) % 2].ordinal()] = FaceletCube.edgeColour[j][k];
            }
        }
        return FaceletCube;
    }

    // Multiply this state by cubeB (corners only)
    // result corresponds to applying cubeB then this (composition order matches other code).
    // Thing to note: mirrored-corner codes (>=3) are not handled here; assume regular cubes.
    public void multiplyCorner(CubieCube cubeB) {
        Corner[] corPerm = new Corner[8];
        byte[] corOrient = new byte[8];

        for (Corner c : Corner.values()) {
            int i = c.ordinal();
            int j = cubeB.cornerPermutation[i].ordinal();

            corPerm[i] = cornerPermutation[j];

            byte orientA = cornerOrientation[j];
            byte orientB = cubeB.cornerOrientation[i];
            byte orient = 0;

            if (orientA < 3 && orientB < 3) { // both regular cubes
                orient = (byte) (orientA + orientB); // add mod 3
                if (orient >= 3) {
                    orient -= 3;
                }
            }
            corOrient[i] = orient;
        }

        for (Corner c : Corner.values()) {
            cornerPermutation[c.ordinal()] = corPerm[c.ordinal()];
            cornerOrientation[c.ordinal()] = corOrient[c.ordinal()];
        }
    }

    // Multiply this state by cubeB (edges only)
    // edge orientations compose mod 2. This is a straight-forward composition.
    public void multiplyEdge(CubieCube cubeB) {
        Edge[] edgePerm = new Edge[12];
        byte[] edgeOri = new byte[12];

        for (Edge e : Edge.values()) {
            int i = e.ordinal();
            int j = cubeB.edgePermutation[i].ordinal();

            edgePerm[i] = edgePermutation[j];

            byte orientA = edgeOrientation[j];
            byte orientB = cubeB.edgeOrientation[i];

            edgeOri[i] = (byte) ((orientA + orientB) % 2);
        }

        for (Edge e : Edge.values()) {
            edgePermutation[e.ordinal()] = edgePerm[e.ordinal()];
            edgeOrientation[e.ordinal()] = edgeOri[e.ordinal()];
        }
    }

    // Corner twist: 0 <= twist < 3^7
    public short getTwist() {
        short r = 0;
        for (int i = URF.ordinal(); i < DRB.ordinal(); i++) {
            r = (short) (3 * r + cornerOrientation[i]);    // as we have 3 sides for each corner
        }
        return r;
    }

    // Set corner twist: 0 <= twist < 3^7
    // we unpack base-3 digits into cornerOrientation; last corner is set to satisfy the orientation sum rule.
    public void setTwist(short twist) {
        int twistParity = 0;
        for (int i = DRB.ordinal() - 1; i >= URF.ordinal(); i--) {
            twistParity += cornerOrientation[i] = (byte) (twist % 3);
            twist /= 3;
        }
        cornerOrientation[DRB.ordinal()] = (byte) ((3 - twistParity % 3) % 3);
    }

    // Edge flip: 0 <= flip < 2^11
    public short getFlip() {
        short ret = 0;
        for (int i = UR.ordinal(); i < BR.ordinal(); i++)
            ret = (short) (2 * ret + edgeOrientation[i]);    // as there are 2 side for each edge
        return ret;
    }

    // Set edge flip: 0 <= flip < 2^11
    // first 11 bits of flip set edgeOrientation; the 12th is implied to make the parity consistent.
    public void setFlip(short flip) {
        int flipParity = 0;
        for (int i = BR.ordinal() - 1; i >= UR.ordinal(); i--) {
            flipParity += edgeOrientation[i] = (byte) (flip % 2);
            flip /= 2;
        }
        edgeOrientation[BR.ordinal()] = (byte) ((2 - flipParity % 2) % 2);
    }

    // Corner permutation parity
    public short cornerParity() {
        int s = 0;
        for (int i = DRB.ordinal(); i >= URF.ordinal() + 1; i--) {
            for (int j = i - 1; j >= URF.ordinal(); j--) {
                if (cornerPermutation[j].ordinal() > cornerPermutation[i].ordinal()) {
                    s++;
                }
            }
        }
        return (short) (s % 2);
    }

    // Edge permutation parity (must match corner parity for solvable states)
    public short edgeParity() {
        int s = 0;
        for (int i = BR.ordinal(); i >= UR.ordinal() + 1; i--) {
            for (int j = i - 1; j >= UR.ordinal(); j--) {
                if (edgePermutation[j].ordinal() > edgePermutation[i].ordinal())
                    s++;
            }
        }
        return (short) (s % 2);
    }

    // FR,FL,BL,BR slice edges permutation -> index
    // index = 24 * combination + permutation, where combination encodes which 4 edges are in the slice.
    public short getFRtoBR() {
        int a = 0, b = 0, x = 0;
        Edge[] edge4 = new Edge[4];

        // compute the index a < (12 choose 4) and the permutation array perm.
        for (int j = BR.ordinal(); j >= UR.ordinal(); j--) {
            if (FR.ordinal() <= edgePermutation[j].ordinal() && edgePermutation[j].ordinal() <= BR.ordinal()) {
                a += nCk(11 - j, x + 1);
                edge4[3 - x++] = edgePermutation[j];
            }
        }

        // compute the index b < 4! for the permutation in perm
        for (int j = 3; j > 0; j--) {
            int k = 0;
            while (edge4[j].ordinal() != j + 8) {
                leftRotateEdge(edge4, 0, j);
                k++;
            }
            b = (j + 1) * b + k;
        }
        return (short) (24 * a + b);     // 4! * a + b
    }

    public void setFRtoBR(short idx) {
        int x;
        Edge[] sliceEdge = {FR, FL, BL, BR};
        Edge[] otherEdge = {UR, UF, UL, UB, DR, DF, DL, DB};
        int b = idx % 24;     // Permutation
        int a = idx / 24;     // Combination
        for (Edge e : Edge.values()) {
            edgePermutation[e.ordinal()] = DB;      // Use DB to invalidate all edges.
        }

        // generate permutation from index b
        for (int j = 1, k; j < 4; j++) {
            k = b % (j + 1);
            b /= j + 1;
            while (k-- > 0) {
                rightRotateEdge(sliceEdge, 0, j);
            }
        }

        // generate combination and set slice edges
        x = 3;
        for (int j = UR.ordinal(); j <= BR.ordinal(); j++) {
            if (a - nCk(11 - j, x + 1) >= 0) {
                edgePermutation[j] = sliceEdge[3 - x];
                a -= nCk(11 - j, x-- + 1);
            }
        }

        // set the remaining edges UR..DB(their original value)
        x = 0;
        for (int j = UR.ordinal(); j <= BR.ordinal(); j++) {
            if (edgePermutation[j] == DB)
                edgePermutation[j] = otherEdge[x++];
        }

    }

    // Corner permutation (URF..DLF six corners) -> index
    // similar combination/permutation packing used in many coords (combination first, then permutation).
    public short getURFtoDLF() {
        int a = 0, x = 0;
        Corner[] corner6 = new Corner[6];

        // compute the index a < (8 choose 6) and the corner permutation.
        for (int j = URF.ordinal(); j <= DRB.ordinal(); j++) {
            if (cornerPermutation[j].ordinal() <= DLF.ordinal()) {
                a += nCk(j, x + 1);
                corner6[x++] = cornerPermutation[j];
            }
        }

        // compute the index b < 6! for the permutation in corner6
        int b = 0;
        for (int j = 5; j > 0; j--) {
            int k = 0;
            while (corner6[j].ordinal() != j) {
                leftRotateCorner(corner6, 0, j);
                k++;
            }
            b = (j + 1) * b + k;
        }
        return (short) (720 * a + b);     // 6! * a + b
    }

    public void setURFtoDLF(short idx) {
        int x;
        Corner[] corner6 = {URF, UFL, ULB, UBR, DFR, DLF};
        Corner[] otherCorner = {DBL, DRB};
        int b = idx % 720;   // Permutation
        int a = idx / 720;   // Combination
        for (Corner c : Corner.values())
            cornerPermutation[c.ordinal()] = DRB;   // Use DRB to invalidate all corners

        // generate permutation from index b
        for (int j = 1, k; j < 6; j++) {
            k = b % (j + 1);
            b /= j + 1;
            while (k-- > 0)
                rightRotateCorner(corner6, 0, j);
        }

        // generate combination and set corners
        x = 5;
        for (int j = DRB.ordinal(); j >= 0; j--) {
            if (a - nCk(j, x + 1) >= 0) {
                cornerPermutation[j] = corner6[x];
                a -= nCk(j, x-- + 1);
            }
        }

        // set the other corner permutation to their original values
        x = 0;
        for (int j = URF.ordinal(); j <= DRB.ordinal(); j++) {
            if (cornerPermutation[j] == DRB)
                cornerPermutation[j] = otherCorner[x++];
        }
    }

    // Edge permutation (UR,UF,UL,UB,DR,DF) -> index
    public int getURtoDF() {
        int a = 0, x = 0;
        Edge[] edge6 = new Edge[6];

        // compute the index a < (12 choose 6) and the edge permutation.
        for (int j = UR.ordinal(); j <= BR.ordinal(); j++) {
            if (edgePermutation[j].ordinal() <= DF.ordinal()) {
                a += nCk(j, x + 1);
                edge6[x++] = edgePermutation[j];
            }
        }

        // compute the index b < 6! for the permutation in edge6
        int b = 0;
        for (int j = 5; j > 0; j--) {
            int k = 0;
            while (edge6[j].ordinal() != j) {
                leftRotateEdge(edge6, 0, j);
                k++;
            }
            b = (j + 1) * b + k;
        }
        return 720 * a + b;
    }

    public void setURtoDF(int idx) {
        int x;
        Edge[] edge6 = {UR, UF, UL, UB, DR, DF};
        Edge[] otherEdge = {DL, DB, FR, FL, BL, BR};
        int b = idx % 720; // Permutation
        int a = idx / 720; // Combination
        for (Edge e : Edge.values())
            edgePermutation[e.ordinal()] = BR;    // Use BR to invalidate all edges

        for (int j = 1, k; j < 6; j++)// generate permutation from index b
        {
            k = b % (j + 1);
            b /= j + 1;
            while (k-- > 0)
                rightRotateEdge(edge6, 0, j);
        }

        // generate combination and set edges
        x = 5;
        for (int j = BR.ordinal(); j >= 0; j--) {
            if (a - nCk(j, x + 1) >= 0) {
                edgePermutation[j] = edge6[x];
                a -= nCk(j, x-- + 1);
            }
        }

        // set the remaining edges DL..BR
        x = 0;
        for (int j = UR.ordinal(); j <= BR.ordinal(); j++) {
            if (edgePermutation[j] == BR)
                edgePermutation[j] = otherEdge[x++];
        }
    }

    // Merge helper coords into UR..DF edge index (or -1 on collision)
    // used to combine the small helper coordinates for efficient phase-2 setup.
    public static int getURtoDF(short idx1, short idx2) {
        CubieCube a = new CubieCube();
        CubieCube b = new CubieCube();
        a.setURtoUL(idx1);
        b.setUBtoDF(idx2);
        for (int i = 0; i < 8; i++) {
            if (a.edgePermutation[i] != BR)
                if (b.edgePermutation[i] != BR)     // collision
                    return -1;
                else
                    b.edgePermutation[i] = a.edgePermutation[i];
        }
        return b.getURtoDF();
    }

    // UR,UF,UL edges -> small index
    // returns 6*a + b: a=combination, b=permutation of those three edges.
    public short getURtoUL() {
        int a = 0, x = 0;
        Edge[] edge3 = new Edge[3];

        // compute the index a < (12 choose 3) and the edge permutation.
        for (int j = UR.ordinal(); j <= BR.ordinal(); j++) {
            if (edgePermutation[j].ordinal() <= UL.ordinal()) {
                a += nCk(j, x + 1);
                edge3[x++] = edgePermutation[j];
            }
        }

        // compute the index b < 3! for the permutation in edge3
        int b = 0;
        for (int j = 2; j > 0; j--) {
            int k = 0;
            while (edge3[j].ordinal() != j) {
                leftRotateEdge(edge3, 0, j);
                k++;
            }
            b = (j + 1) * b + k;
        }
        return (short) (6 * a + b);
    }

    public void setURtoUL(short idx) {
        int x;
        Edge[] edge3 = {UR, UF, UL};
        int b = idx % 6; // Permutation
        int a = idx / 6; // Combination
        for (Edge e : Edge.values())
            edgePermutation[e.ordinal()] = BR;// Use BR to invalidate all edges

        // generate permutation from index b
        for (int j = 1, k; j < 3; j++) {
            k = b % (j + 1);
            b /= j + 1;
            while (k-- > 0)
                rightRotateEdge(edge3, 0, j);
        }

        // generate combination and set edges
        x = 2;
        for (int j = BR.ordinal(); j >= 0; j--)
            if (a - nCk(j, x + 1) >= 0) {
                edgePermutation[j] = edge3[x];
                a -= nCk(j, x-- + 1);
            }
    }

    // UB,DR,DF edges -> small index
    // analogous to getURtoUL but for a different triple.
    public short getUBtoDF() {
        int a = 0, x = 0;
        Edge[] edge3 = new Edge[3];

        // compute the index a < (12 choose 3) and the edge permutation.
        for (int j = UR.ordinal(); j <= BR.ordinal(); j++) {
            if (UB.ordinal() <= edgePermutation[j].ordinal() && edgePermutation[j].ordinal() <= DF.ordinal()) {
                a += nCk(j, x + 1);
                edge3[x++] = edgePermutation[j];
            }
        }

        // compute the index b < 3! for the permutation in edge3
        int b = 0;
        for (int j = 2; j > 0; j--) {
            int k = 0;
            while (edge3[j].ordinal() != UB.ordinal() + j) {
                leftRotateEdge(edge3, 0, j);
                k++;
            }
            b = (j + 1) * b + k;
        }
        return (short) (6 * a + b);
    }

    public void setUBtoDF(short idx) {
        int x;
        Edge[] edge3 = {UB, DR, DF};
        int b = idx % 6; // Permutation
        int a = idx / 6; // Combination
        for (Edge e : Edge.values())
            edgePermutation[e.ordinal()] = BR;// Use BR to invalidate all edges

        // generate permutation from index b
        for (int j = 1, k; j < 3; j++) {
            k = b % (j + 1);
            b /= j + 1;
            while (k-- > 0)
                rightRotateEdge(edge3, 0, j);
        }

        // generate combination and set edges
        x = 2;
        for (int j = BR.ordinal(); j >= 0; j--) {
            if (a - nCk(j, x + 1) >= 0) {
                edgePermutation[j] = edge3[x];
                a -= nCk(j, x-- + 1);
            }
        }
    }

    // Full corner permutation -> 0..8!-1
    public int getURFtoDLB() {
        Corner[] perm = Arrays.copyOf(cornerPermutation, 8); // copy current corner permutation
        int b = 0;

        // compute the index b < 8! for the permutation in perm
        for (int j = 7; j > 0; j--) {
            int k = 0;
            while (perm[j].ordinal() != j) {
                leftRotateCorner(perm, 0, j);
                k++;
            }
            b = (j + 1) * b + k;
        }
        return b;
    }

    public void setURFtoDLB(int idx) {
        Corner[] perm = {URF, UFL, ULB, UBR, DFR, DLF, DBL, DRB};
        int k;
        for (int j = 1; j < 8; j++) {
            k = idx % (j + 1);
            idx /= j + 1;
            while (k-- > 0)
                rightRotateCorner(perm, 0, j);
        }

        // set corners
        int x = 7;
        for (int j = 7; j >= 0; j--) {
            cornerPermutation[j] = perm[x--];
        }
    }

    // Full edge permutation -> 0..12!-1
    public int getURtoBR() {
        Edge[] perm = Arrays.copyOf(edgePermutation, 12); // copy current edge permutation
        int b = 0;

        // compute the index b < 12! for the permutation in perm
        for (int j = 11; j > 0; j--) {
            int k = 0;
            while (perm[j].ordinal() != j) {
                leftRotateEdge(perm, 0, j);
                k++;
            }
            b = (j + 1) * b + k;
        }
        return b;
    }

    public void setURtoBR(int idx) {
        Edge[] perm = {UR, UF, UL, UB, DR, DF, DL, DB, FR, FL, BL, BR};
        int k;
        for (int j = 1; j < 12; j++) {
            k = idx % (j + 1);
            idx /= j + 1;
            while (k-- > 0)
                rightRotateEdge(perm, 0, j);
        }

        // set edges
        int x = 11;
        for (int j = 11; j >= 0; j--) {
            edgePermutation[j] = perm[x--];
        }
    }

    // Validate solvability. Return codes: 0 OK, negatives indicate specific issue.
    // Use verify() on parsed scrambles to catch malformed inputs early.
    // The returned negative codes identify the exact category of issue.
    public int verify() {
        int sum = 0;
        int[] edgeCount = new int[12];
        for (Edge e : Edge.values()) {
            edgeCount[edgePermutation[e.ordinal()].ordinal()]++;
        }

        // all 12 edges must appear exactly once
        for (int i = 0; i < 12; i++) {
            if (edgeCount[i] != 1)
                return -2;
        }

        for (int i = 0; i < 12; i++) {
            sum += edgeOrientation[i];
        }

        // check for flip error
        if (sum % 2 != 0) {
            return -3;
        }

        int[] cornerCount = new int[8];
        for (Corner c : Corner.values()) {
            cornerCount[cornerPermutation[c.ordinal()].ordinal()]++;
        }

        // all 8 corners must appear exactly once
        for (int i = 0; i < 8; i++) {
            if (cornerCount[i] != 1)
                return -4;     // missing corners
        }

        sum = 0;
        for (int i = 0; i < 8; i++) {
            sum += cornerOrientation[i];
        }

        // check for twist error
        if (sum % 3 != 0) {
            return -5;// twisted corner
        }

        // parity of corners and edges must match
        if ((edgeParity() ^ cornerParity()) != 0) {
            return -6;// parity error
        }

        return 0;// cube ok
    }
}
