package reproduction;

import java.io.IOException;

/**
 * Solves the cycle formulation of the problem
 */
public class solveCycle {

	/**
	 * initiates solve
	 * 
	 * @param data The name of the data file to be solved (without .csv)
	 * @throws IOException 
	 */
	public static void solve(String data) throws IOException {
		
		// get data
		DataReader dr = new DataReader(data);
		final int[] ids = dr.getIds();
		final boolean[][] matches = dr.getMatches();
		
		
	}
}
