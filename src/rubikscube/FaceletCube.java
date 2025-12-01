package rubikscube;

/*
 Facelet parsing and the mapping from sticker layout to cubie (piece) and then
 to coordinate representations are conceptually based on standard descriptions
 of the Kociemba two-phase approach (http://kociemba.org/cube.htm) and public
 educational resources detailing corner/edge facelet ordering.
*/

import java.io.*;
import java.util.Arrays;
import static rubikscube.Colour.*;
import static rubikscube.Facelet.*;


public class FaceletCube {
	// Utilizing the two phase kociemba approach.
	// This requires parsing the Stickers to The physical Cubes often known as Cubies
	// Need to take in the state of cube through txt file and decipher the stickers then convert into Cubies
	// From there kociemba excels becasue we convert into a coordinate system which yields the fast times.
	// From Kociemba we have three levels Facelets->Cubie->Coordinate levles

    // public enum Colour {
    //     U, R, F, D, L, B
    // }

    // public enum Corner {
    // URF, UFL, ULB, UBR, DFR, DLF, DBL, DRB
    // }

    // public enum Edge {
    // UR, UF, UL, UB, DR, DF, DL, DB, FR, FL, BL, BR
    // }

    // public enum Facelet {
    //     U1, U2, U3, U4, U5, U6, U7, U8, U9, R1, R2, R3, R4, R5, R6, R7, R8, R9,
    //     F1, F2, F3, F4, F5, F6, F7, F8, F9, D1, D2, D3, D4, D5, D6, D7, D8, D9,
    //     L1, L2, L3, L4, L5, L6, L7, L8, L9, B1, B2, B3, B4, B5, B6, B7, B8, B9
    // }

	public Colour[] colours = new Colour[54];

	// Just the correct mapping for stickers to cubies
	// This is very important because kociembas requires you to transfer the physical pieces to coordinates
	public static final Facelet[][] cornerFacelet = {
		{U9, R1, F3}, {U7, F1, L3}, {U1, L1, B3}, {U3, B1, R3},
		{D3, F9, R7}, {D1, L9, F7}, {D7, B9, L7}, {D9, R9, B7}
	};
	public static final Facelet[][] edgeFacelet = {
		{U6, R2}, {U8, F2}, {U4, L2}, {U2, B2}, {D6, R8}, {D2, F8},
		{D4, L8}, {D8, B8}, {F6, R4}, {F4, L6}, {B6, L4}, {B4, R6}
	};
	public static final Colour[][] cornerColour = {
		{U, R, F}, {U, F, L}, {U, L, B}, {U, B, R}, {D, F, R},
		{D, L, F}, {D, B, L}, {D, R, B}
	};
	public static final Colour[][] edgeColour = {
		{U, R}, {U, F}, {U, L}, {U, B}, {D, R}, {D, F}, {D, L},
		{D, B}, {F, R}, {F, L}, {B, L}, {B, R}
	};

    public FaceletCube() {
        // Just allocate memory for the colors
        // In cubie cube we fill it right away anyway no need for anything else
        colours = new Colour[54];
    }
    // Takes inda string and initiallizes the face let cube on a colour level parsefave will turn it into face
	public FaceletCube(String filename) throws IOException, IncorrectFormatException {
		BufferedReader input = new BufferedReader(new FileReader(filename));
		String lines[] = new String[9];

        try {
            for( int i = 0; i < 9; i++ ){
                    lines[i] = input.readLine();
            }
            input.close();

            // reads in the input cube file line by line so 0-3 for the first line then maps it into our flat array
            parseFace(lines, 0, 3, 0);
            parseFace(lines, 3, 6, 9); // added offset because of 1d array
            parseFace(lines, 3, 3, 18);
            parseFace(lines, 6, 3, 27);
            parseFace(lines, 3, 0, 36);
            parseFace(lines, 3, 9, 45);
        } catch (Exception e) {
            throw new IncorrectFormatException("Error reading cube from file: " + e.getMessage());
        }
	}

	private void parseFace( String[] lines, int startLine, int startChar, int offset ){
		for( int i = 0; i < 3; i++){
			for( int j = 0; j < 3; j++){
				char c = lines[startLine + i].charAt(startChar + j);
				colours[offset + (i * 3)+ j] = mapColour(c); // C was stored at that spot now we just translate colour -> face and store in array
			}
		}
	}

	private Colour mapColour(char c) { // This function is extremely key for kociemba as it doesnt case for colour only for the face
        switch (c) {
            case 'O' -> {
                return U; // Orange is Up
                }
            case 'B' -> {
                return R; // Blue is Right
                }
            case 'W' -> {
                return F; // White is Front
                }
            case 'R' -> {
                return D; // Red is Down
                }
            case 'G' -> {
                return L; // Green is Left
                }
            case 'Y' -> {
                return B; // Yellow is Back
                }
            default -> throw new IllegalArgumentException("Invalid colour in file: " + c);
        }
	}
    // Now given our Sticker Cube that is translated into the faces instead of colours we will make the Pieces Cube (cubies) so we can then translate to Coords
	public CubieCube toCubieCube() {
		CubieCube PC = new CubieCube();

		// This just inits the corner and edges permutations to invalid at first so we can fill it correctly
		Arrays.fill( PC.cornerPermutation, Corner.URF );
		Arrays.fill( PC.edgePermutation, Edge.UR );

		// 1. Resolve Corners
        for (Corner i : Corner.values()) {
            int orientation;
            // Find orientationentation of this corner
            for (orientation = 0; orientation < 3; orientation++) {
                if (colours[cornerFacelet[i.ordinal()][orientation].ordinal()] == U ||
                    colours[cornerFacelet[i.ordinal()][orientation].ordinal()] == D) {
                    break;
                }
            }
            Colour col1 = colours[cornerFacelet[i.ordinal()][(orientation + 1) % 3].ordinal()];
            Colour col2 = colours[cornerFacelet[i.ordinal()][(orientation + 2) % 3].ordinal()];

            for (Corner j : Corner.values()) {
                if (col1 == cornerColour[j.ordinal()][1] && col2 == cornerColour[j.ordinal()][2]) {
                    PC.cornerPermutation[i.ordinal()] = j;
                    PC.cornerOrientation[i.ordinal()] = (byte) (orientation % 3);
                    break;
                }
            }
        }

        // 2. Resolve Edges
        for (Edge i : Edge.values()) {
            for (Edge j : Edge.values()) {
                if (colours[edgeFacelet[i.ordinal()][0].ordinal()] == edgeColour[j.ordinal()][0]
                 && colours[edgeFacelet[i.ordinal()][1].ordinal()] == edgeColour[j.ordinal()][1]) {
                    PC.edgePermutation[i.ordinal()] = j;
                    PC.edgeOrientation[i.ordinal()] = 0;
                    break;
                }
                if (colours[edgeFacelet[i.ordinal()][0].ordinal()] == edgeColour[j.ordinal()][1]
                 && colours[edgeFacelet[i.ordinal()][1].ordinal()] == edgeColour[j.ordinal()][0]) {
                    PC.edgePermutation[i.ordinal()] = j;
                    PC.edgeOrientation[i.ordinal()] = 1;
                    break;
                }
            }
        }
        return PC;
    }
}

	