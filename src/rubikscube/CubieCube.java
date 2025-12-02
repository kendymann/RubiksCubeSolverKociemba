package rubikscube;

/*
 * This class models the Rubik's Cube using Cartesian coordinates the cubies over the
 * facelets. This abstraction is necessary for the Kociemba Two-Phase Algorithm.
 * The implementation of coordinate mappings permutations to integers and the 
 * move tables follows the definitions provided in the Kociemba documentation:
 * Source: http://kociemba.org/cube.htm
 */

import java.util.Arrays;
import static rubikscube.Corner.*;
import static rubikscube.Edge.*;

public class CubieCube {

    // Corner Permutation: cp[i] is the corner currently at position i
    public Corner[] cornerPermutation = {URF, UFL, ULB, UBR, DFR, DLF, DBL, DRB};

    // Corner Orientation: co[i] is the orientation (0..2) of the corner at position i
    public byte[] cornerOrientation = {0, 0, 0, 0, 0, 0, 0, 0};

    // Edge Permutation: ep[i] is the edge currently at position i
    public Edge[] edgePermutation = {UR, UF, UL, UB, DR, DF, DL, DB, FR, FL, BL, BR};

    // Edge Orientation: eo[i] is the orientation (0..1) of the edge at position i
    public byte[] edgeOrientation = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

    // The 6 basic face moves: U, R, F, D, L, B
    public static CubieCube[] moves = new CubieCube[6];

    static {
        // Initialize the basic moves directly (U, R, F, D, L, B)
        // This avoids listing 24+ static array variables and keeps definitions self-contained.

        // UP (U)
        moves[0] = new CubieCube();
        moves[0].cornerPermutation = new Corner[]{UBR, URF, UFL, ULB, DFR, DLF, DBL, DRB};
        moves[0].cornerOrientation = new byte[]{0, 0, 0, 0, 0, 0, 0, 0};
        moves[0].edgePermutation = new Edge[]{UB, UR, UF, UL, DR, DF, DL, DB, FR, FL, BL, BR};
        moves[0].edgeOrientation = new byte[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

        // RIGHT (R)
        moves[1] = new CubieCube();
        moves[1].cornerPermutation = new Corner[]{DFR, UFL, ULB, URF, DRB, DLF, DBL, UBR};
        moves[1].cornerOrientation = new byte[]{2, 0, 0, 1, 1, 0, 0, 2};
        moves[1].edgePermutation = new Edge[]{FR, UF, UL, UB, BR, DF, DL, DB, DR, FL, BL, UR};
        moves[1].edgeOrientation = new byte[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

        // FRONT (F)
        moves[2] = new CubieCube();
        moves[2].cornerPermutation = new Corner[]{UFL, DLF, ULB, UBR, URF, DFR, DBL, DRB};
        moves[2].cornerOrientation = new byte[]{1, 2, 0, 0, 2, 1, 0, 0};
        moves[2].edgePermutation = new Edge[]{UR, FL, UL, UB, DR, FR, DL, DB, UF, DF, BL, BR};
        moves[2].edgeOrientation = new byte[]{0, 1, 0, 0, 0, 1, 0, 0, 1, 1, 0, 0};

        // DOWN (D)
        moves[3] = new CubieCube();
        moves[3].cornerPermutation = new Corner[]{URF, UFL, ULB, UBR, DLF, DBL, DRB, DFR};
        moves[3].cornerOrientation = new byte[]{0, 0, 0, 0, 0, 0, 0, 0};
        moves[3].edgePermutation = new Edge[]{UR, UF, UL, UB, DF, DL, DB, DR, FR, FL, BL, BR};
        moves[3].edgeOrientation = new byte[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

        // LEFT (L)
        moves[4] = new CubieCube();
        moves[4].cornerPermutation = new Corner[]{URF, ULB, DBL, UBR, DFR, UFL, DLF, DRB};
        moves[4].cornerOrientation = new byte[]{0, 1, 2, 0, 0, 2, 1, 0};
        moves[4].edgePermutation = new Edge[]{UR, UF, BL, UB, DR, DF, FL, DB, FR, UL, DL, BR};
        moves[4].edgeOrientation = new byte[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

        // BACK (B)
        moves[5] = new CubieCube();
        moves[5].cornerPermutation = new Corner[]{URF, UFL, UBR, DRB, DFR, DLF, ULB, DBL};
        moves[5].cornerOrientation = new byte[]{0, 0, 1, 2, 0, 0, 2, 1};
        moves[5].edgePermutation = new Edge[]{UR, UF, UL, BR, DR, DF, DL, BL, FR, FL, UB, DB};
        moves[5].edgeOrientation = new byte[]{0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 1, 1};
    }

    public CubieCube() {
        // Default constructor initializes a solved cube - already filled above.
    }

    // Math - Binomial Coefficient (n choose k)
    // Required for calculating the "rank" of a permutation.
    public static int binomial(int n, int k) {
        if (n < k) return 0;
        if (k > n / 2) k = n - k; // Optimization - C(n,k) == C(n, n-k)
        
        int result = 1;
        for (int i = 1; i <= k; i++) {
            result = result * (n - i + 1) / i;
        }
        return result;
    }

    // Array rotation helpers 
    public static void rotateLeft(Object[] arr, int l, int r) {
        Object temp = arr[l];
        System.arraycopy(arr, l + 1, arr, l, r - l);
        arr[r] = temp;
    }

    public static void rotateRight(Object[] arr, int l, int r) {
        Object temp = arr[r];
        System.arraycopy(arr, l, arr, l + 1, r - l);
        arr[l] = temp;
    }

    // Convert this Cubie representation back to facelets
    // This maps the physical pieces (Corner/Edge enums) to the visual stickers.
    public FaceletCube toFaceletCube() {
        FaceletCube fc = new FaceletCube();
        
        // Map corners
        for (Corner c : Corner.values()) {
            int i = c.ordinal(); // Position index
            int j = cornerPermutation[i].ordinal(); // Cubie at that position
            byte ori = cornerOrientation[i];
            
            for (int k = 0; k < 3; k++) {
                // Determine facelet color based on orientation
                int faceletIndex = FaceletCube.cornerFacelet[i][(k + ori) % 3].ordinal();
                fc.colours[faceletIndex] = FaceletCube.cornerColour[j][k];
            }
        }
        
        // Map edges
        for (Edge e : Edge.values()) {
            int i = e.ordinal();
            int j = edgePermutation[i].ordinal();
            byte ori = edgeOrientation[i];
            
            for (int k = 0; k < 2; k++) {
                int faceletIndex = FaceletCube.edgeFacelet[i][(k + ori) % 2].ordinal();
                fc.colours[faceletIndex] = FaceletCube.edgeColour[j][k];
            }
        }
        return fc;
    }

    // Composition - Apply a move (cubeB) to the current corner state
    // Math this.corner = this.corner * cubeB.corner
    public void multiplyCorner(CubieCube move) {
        Corner[] newPerm = new Corner[8];
        byte[] newOri = new byte[8];

        for (Corner c : Corner.values()) {
            int i = c.ordinal();
            int j = move.cornerPermutation[i].ordinal(); // Corner at position i in the move

            // Composition of permutation
            newPerm[i] = cornerPermutation[j];

            // Composition of orientation (sum modulo 3)
            byte oriA = cornerOrientation[j];
            byte oriB = move.cornerOrientation[i];
            
            newOri[i] = (byte) ((oriA + oriB) % 3);
        }
        
        // Update state
        System.arraycopy(newPerm, 0, cornerPermutation, 0, 8);
        System.arraycopy(newOri, 0, cornerOrientation, 0, 8);
    }

    // Composition - Apply a move (cubeB) to the current edge state
    public void multiplyEdge(CubieCube move) {
        Edge[] newPerm = new Edge[12];
        byte[] newOri = new byte[12];

        for (Edge e : Edge.values()) {
            int i = e.ordinal();
            int j = move.edgePermutation[i].ordinal();

            newPerm[i] = edgePermutation[j];
            
            // Composition of orientation (sum modulo 2)
            newOri[i] = (byte) ((edgeOrientation[j] + move.edgeOrientation[i]) % 2);
        }

        System.arraycopy(newPerm, 0, edgePermutation, 0, 12);
        System.arraycopy(newOri, 0, edgeOrientation, 0, 12);
    }

    /*
     * gets and sets
     * The following methods map the raw cubie state to integer coordinates used
     * for table lookups. This implementation relies on the combinatorial number system.
     */

    // Get Corner Twist (0..2186)
    public short getTwist() {
        short twist = 0;
        for (int i = URF.ordinal(); i < DRB.ordinal(); i++) {
            twist = (short) (3 * twist + cornerOrientation[i]);
        }
        return twist;
    }

    public void setTwist(short twist) {
        int twistSum = 0;
        for (int i = DRB.ordinal() - 1; i >= URF.ordinal(); i--) {
            twistSum += cornerOrientation[i] = (byte) (twist % 3);
            twist /= 3;
        }
        // Last corner orientation is determined by parity
        cornerOrientation[DRB.ordinal()] = (byte) ((3 - twistSum % 3) % 3);
    }

    // Get Edge Flip (0..2047)
    public short getFlip() {
        short flip = 0;
        for (int i = UR.ordinal(); i < BR.ordinal(); i++) {
            flip = (short) (2 * flip + edgeOrientation[i]);
        }
        return flip;
    }

    public void setFlip(short flip) {
        int flipSum = 0;
        for (int i = BR.ordinal() - 1; i >= UR.ordinal(); i--) {
            flipSum += edgeOrientation[i] = (byte) (flip % 2);
            flip /= 2;
        }
        edgeOrientation[BR.ordinal()] = (byte) ((2 - flipSum % 2) % 2);
    }

    // Calculate Parity
    public short cornerParity() {
        int s = 0;
        for (int i = DRB.ordinal(); i > URF.ordinal(); i--) {
            for (int j = i - 1; j >= URF.ordinal(); j--) {
                if (cornerPermutation[j].ordinal() > cornerPermutation[i].ordinal()) s++;
            }
        }
        return (short) (s % 2);
    }

    public short edgeParity() {
        int s = 0;
        for (int i = BR.ordinal(); i > UR.ordinal(); i--) {
            for (int j = i - 1; j >= UR.ordinal(); j--) {
                if (edgePermutation[j].ordinal() > edgePermutation[i].ordinal()) s++;
            }
        }
        return (short) (s % 2);
    }

    // Slice Coordinate: Map position of FR, FL, BL, BR edges
    public short getFRtoBR() {
        int a = 0, x = 0;
        Edge[] arr = new Edge[4];

        // Combination ranking
        for (int j = BR.ordinal(); j >= UR.ordinal(); j--) {
            if (FR.ordinal() <= edgePermutation[j].ordinal() && edgePermutation[j].ordinal() <= BR.ordinal()) {
                a += binomial(11 - j, x + 1);
                arr[3 - x++] = edgePermutation[j];
            }
        }
        
        // Permutation ranking
        int b = 0;
        for (int j = 3; j > 0; j--) {
            int k = 0;
            while (arr[j].ordinal() != j + 8) {
                rotateLeft(arr, 0, j);
                k++;
            }
            b = (j + 1) * b + k;
        }
        return (short) (24 * a + b);
    }

    public void setFRtoBR(short idx) {
        Edge[] sliceEdges = {FR, FL, BL, BR};
        Edge[] otherEdges = {UR, UF, UL, UB, DR, DF, DL, DB};
        int permIdx = idx % 24;
        int combIdx = idx / 24;

        Arrays.fill(edgePermutation, DB); // Clear edges

        // Decode permutation
        for (int j = 1; j < 4; j++) {
            int k = permIdx % (j + 1);
            permIdx /= j + 1;
            while (k-- > 0) rotateRight(sliceEdges, 0, j);
        }

        // Decode combination
        int x = 3;
        for (int j = UR.ordinal(); j <= BR.ordinal(); j++) {
            if (combIdx - binomial(11 - j, x + 1) >= 0) {
                edgePermutation[j] = sliceEdges[3 - x];
                combIdx -= binomial(11 - j, x-- + 1);
            }
        }

        // Fill remaining edges
        x = 0;
        for (int j = UR.ordinal(); j <= BR.ordinal(); j++) {
            if (edgePermutation[j] == DB) edgePermutation[j] = otherEdges[x++];
        }
    }

    // Coordinate: URF, UFL, ULB, UBR, DFR, DLF Corners
    public short getURFtoDLF() {
        int a = 0, x = 0;
        Corner[] arr = new Corner[6];

        for (int j = URF.ordinal(); j <= DRB.ordinal(); j++) {
            if (cornerPermutation[j].ordinal() <= DLF.ordinal()) {
                a += binomial(j, x + 1);
                arr[x++] = cornerPermutation[j];
            }
        }

        int b = 0;
        for (int j = 5; j > 0; j--) {
            int k = 0;
            while (arr[j].ordinal() != j) {
                rotateLeft(arr, 0, j);
                k++;
            }
            b = (j + 1) * b + k;
        }
        return (short) (720 * a + b);
    }

    public void setURFtoDLF(short idx) {
        Corner[] corners = {URF, UFL, ULB, UBR, DFR, DLF};
        Corner[] others = {DBL, DRB};
        int permIdx = idx % 720;
        int combIdx = idx / 720;

        Arrays.fill(cornerPermutation, DRB);

        for (int j = 1; j < 6; j++) {
            int k = permIdx % (j + 1);
            permIdx /= j + 1;
            while (k-- > 0) rotateRight(corners, 0, j);
        }

        int x = 5;
        for (int j = DRB.ordinal(); j >= 0; j--) {
            if (combIdx - binomial(j, x + 1) >= 0) {
                cornerPermutation[j] = corners[x];
                combIdx -= binomial(j, x-- + 1);
            }
        }

        x = 0;
        for (int j = URF.ordinal(); j <= DRB.ordinal(); j++) {
            if (cornerPermutation[j] == DRB) cornerPermutation[j] = others[x++];
        }
    }

    // Coordinate: UR to DF Edges (Phase 2)
    public int getURtoDF() {
        int a = 0, x = 0;
        Edge[] arr = new Edge[6];

        for (int j = UR.ordinal(); j <= BR.ordinal(); j++) {
            if (edgePermutation[j].ordinal() <= DF.ordinal()) {
                a += binomial(j, x + 1);
                arr[x++] = edgePermutation[j];
            }
        }

        int b = 0;
        for (int j = 5; j > 0; j--) {
            int k = 0;
            while (arr[j].ordinal() != j) {
                rotateLeft(arr, 0, j);
                k++;
            }
            b = (j + 1) * b + k;
        }
        return 720 * a + b;
    }

    public void setURtoDF(int idx) {
        Edge[] edges = {UR, UF, UL, UB, DR, DF};
        Edge[] others = {DL, DB, FR, FL, BL, BR};
        int permIdx = idx % 720;
        int combIdx = idx / 720;

        Arrays.fill(edgePermutation, BR);

        for (int j = 1; j < 6; j++) {
            int k = permIdx % (j + 1);
            permIdx /= j + 1;
            while (k-- > 0) rotateRight(edges, 0, j);
        }

        int x = 5;
        for (int j = BR.ordinal(); j >= 0; j--) {
            if (combIdx - binomial(j, x + 1) >= 0) {
                edgePermutation[j] = edges[x];
                combIdx -= binomial(j, x-- + 1);
            }
        }

        x = 0;
        for (int j = UR.ordinal(); j <= BR.ordinal(); j++) {
            if (edgePermutation[j] == BR) edgePermutation[j] = others[x++];
        }
    }

    // Helper: Merge two partial edge coordinates
    public static int getURtoDF(short idx1, short idx2) {
        CubieCube a = new CubieCube();
        CubieCube b = new CubieCube();
        a.setURtoUL(idx1);
        b.setUBtoDF(idx2);
        
        for (int i = 0; i < 8; i++) {
            if (a.edgePermutation[i] != BR) {
                if (b.edgePermutation[i] != BR) return -1; // Collision
                b.edgePermutation[i] = a.edgePermutation[i];
            }
        }
        return b.getURtoDF();
    }

    // Helper: UR, UF, UL subset
    public short getURtoUL() {
        int a = 0, x = 0;
        Edge[] arr = new Edge[3];
        for (int j = UR.ordinal(); j <= BR.ordinal(); j++) {
            if (edgePermutation[j].ordinal() <= UL.ordinal()) {
                a += binomial(j, x + 1);
                arr[x++] = edgePermutation[j];
            }
        }
        int b = 0;
        for (int j = 2; j > 0; j--) {
            int k = 0;
            while (arr[j].ordinal() != j) {
                rotateLeft(arr, 0, j);
                k++;
            }
            b = (j + 1) * b + k;
        }
        return (short) (6 * a + b);
    }

    public void setURtoUL(short idx) {
        Edge[] edges = {UR, UF, UL};
        int permIdx = idx % 6;
        int combIdx = idx / 6;
        Arrays.fill(edgePermutation, BR);

        for (int j = 1; j < 3; j++) {
            int k = permIdx % (j + 1);
            permIdx /= j + 1;
            while (k-- > 0) rotateRight(edges, 0, j);
        }

        int x = 2;
        for (int j = BR.ordinal(); j >= 0; j--) {
            if (combIdx - binomial(j, x + 1) >= 0) {
                edgePermutation[j] = edges[x];
                combIdx -= binomial(j, x-- + 1);
            }
        }
    }

    // Helper: UB, DR, DF subset
    public short getUBtoDF() {
        int a = 0, x = 0;
        Edge[] arr = new Edge[3];
        for (int j = UR.ordinal(); j <= BR.ordinal(); j++) {
            if (UB.ordinal() <= edgePermutation[j].ordinal() && edgePermutation[j].ordinal() <= DF.ordinal()) {
                a += binomial(j, x + 1);
                arr[x++] = edgePermutation[j];
            }
        }
        int b = 0;
        for (int j = 2; j > 0; j--) {
            int k = 0;
            while (arr[j].ordinal() != UB.ordinal() + j) {
                rotateLeft(arr, 0, j);
                k++;
            }
            b = (j + 1) * b + k;
        }
        return (short) (6 * a + b);
    }

    public void setUBtoDF(short idx) {
        Edge[] edges = {UB, DR, DF};
        int permIdx = idx % 6;
        int combIdx = idx / 6;
        Arrays.fill(edgePermutation, BR);

        for (int j = 1; j < 3; j++) {
            int k = permIdx % (j + 1);
            permIdx /= j + 1;
            while (k-- > 0) rotateRight(edges, 0, j);
        }

        int x = 2;
        for (int j = BR.ordinal(); j >= 0; j--) {
            if (combIdx - binomial(j, x + 1) >= 0) {
                edgePermutation[j] = edges[x];
                combIdx -= binomial(j, x-- + 1);
            }
        }
    }

    // Verify cube state validity
    public int verify() {
        // 1. Check Edge Counts
        int[] edgeCounts = new int[12];
        for (Edge e : Edge.values()) edgeCounts[edgePermutation[e.ordinal()].ordinal()]++;
        for (int c : edgeCounts) if (c != 1) return -2; // Missing or duplicate edge

        // 2. Check Corner Counts
        int[] cornerCounts = new int[8];
        for (Corner c : Corner.values()) cornerCounts[cornerPermutation[c.ordinal()].ordinal()]++;
        for (int c : cornerCounts) if (c != 1) return -4; // Missing or duplicate corner

        // 3. Check Edge Orientation Parity
        int sum = 0;
        for (byte b : edgeOrientation) sum += b;
        if (sum % 2 != 0) return -3;

        // 4. Check Corner Twist Parity
        sum = 0;
        for (byte b : cornerOrientation) sum += b;
        if (sum % 3 != 0) return -5;

        // 5. Check Permutation Parity (Swap Parity)
        if ((edgeParity() ^ cornerParity()) != 0) return -6;

        return 0; // Cube is valid
    }
}