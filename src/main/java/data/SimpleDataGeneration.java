package data;

import java.util.Random;

/**
 * Implements the way of generating data lined out in the paper by clmentova et al.
 * This is not the blood-type test data, but the one with differing densities.
 * @author daniel
 *
 */
public class SimpleDataGeneration {

	/**
	 * gives a simple data instance of specified size and density
	 * @param n amount of pairs
	 * @param density chance between 0 and 1 that any specific match is possible
	 * @return
	 */
	public static boolean[][] generate(int n, double density){
		
		Random r = new Random();
		boolean[][] matches = new boolean[n][n];
		for(int i = 0; i<n; i++) {
			for(int j = 0; j<n; j++) {
				if(i!=j) {
					matches[i][j] = r.nextDouble()<density;
				}
			}
		}
		return matches;
	}
}
