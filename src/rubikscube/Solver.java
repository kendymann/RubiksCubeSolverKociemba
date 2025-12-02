package rubikscube;

import java.io.*;
import java.lang.invoke.MethodHandles;


public class Solver {

    // Must check if the solves are valid after such a grind. This actually caught my bad translation logic so definitely a lifesaver!
    public static boolean TestCorrectSolve( String filename, String Solution ) throws IOException, IncorrectFormatException {
        RubiksCube RC = new RubiksCube( filename );
        RC.applyMoves( Solution );
        return RC.isSolved();
    }

	public static void main(String[] args) throws IOException, IncorrectFormatException {
//		System.out.println("number of arguments: " + args.length);
//		for (int i = 0; i < args.length; i++) {
//			System.out.println(args[i]);
//		}
		if (args.length < 2) {
			System.out.println("File names are not specified");
			System.out.println("usage: java " + MethodHandles.lookup().lookupClass().getName() + " input_file output_file");
			return;
		}
		
		
		// TODO
		//File input = new File(args[0]);
		String Input_Filename = args[0];
		FaceletCube StartCube = new FaceletCube( Input_Filename ); // inits the initial cube from file
        CubieCube CC = StartCube.toCubieCube();

        // System.out.printf(" This is the file you are printing to %s", args[1] ); // had to create a check was getting file printing bugs
    
        // Pass in Cubie Cube to Search with a max depth of 20 ( According to Sources this is the number that isnt most optimal but for speed)
        // The Timer is set to max 10 seconds 
		String SolutionPath = Search.solution( CC, 21, 10);
        System.out.println("Solution: " + SolutionPath);

        // Testing that the solution algorithm spits out is actually valid
        System.out.println("Testing solution...");
        if( TestCorrectSolve( Input_Filename, SolutionPath ) == true ) {
            System.out.println("This is a valid solution!");
        } 
        else{
            System.out.println("That solution did not work :(");
        }
		
        // Write to output file

        // File output = new File( args[1] );
        try(BufferedWriter writer = new BufferedWriter( new FileWriter( args[1] ) )){
            writer.write( SolutionPath );
            System.out.printf("Solution Written to file \n");
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        
    }
		// solve...
		//File output = new File(args[1]);
}