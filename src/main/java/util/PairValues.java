package util;

/**
 * Returns the values of each pair's donor and recipient.
 * The value is a double between 0 and 1 that is the proportion of donors/recipients that a recipient/donor match 
 * with respectively. 
 * @author daniel
 *
 */
public class PairValues {

	/**
	 * Calculates the values.
	 * @param matches n-by-n boolean array of the possible matches between pairs
	 * @return an n-by-2 double array where the first column is the score of the donor and
	 * the second column the score of the recipient.
	 */
	public static double[][] calculate(boolean[][] matches){
		
		int n = matches.length;
		double[][] result = new double[n][2];
		for(int i = 0; i<n; i++) {
			for(int j = 0; j<n; j++) {
				if(matches[i][j]) {
					result[i][0]++;
					result[j][1]++;
				}
			}
		}
		//get average
		for(int i = 0; i<n; i++) {
			for(int j = 0; j<2; j++) {
				result[i][j] = result[i][j]/(double) n;
			}
		}
		
		return result;
	}
}
