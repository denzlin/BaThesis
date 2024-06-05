package util;

import java.util.ArrayList;

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
	public static double[][] calculatePairs(boolean[][] matches){
		
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
				result[i][j] = Math.round((result[i][j]/(double) n)*1000)/1000.0;
			}
		}
		
		return result;
	}
	
	public static ArrayList<Double> calculateCycles(boolean[][] matches, ArrayList<ArrayList<Integer>> cycles, boolean averaged) {
		System.out.println();
		ArrayList<Double> values = new ArrayList<>(cycles.size());
		double[][] pairValues = PairValues.calculatePairs(matches);
		for(ArrayList<Integer> cycle : cycles) {
			double value = 0;
			for(int i = 0; i<cycle.size()-1;i++) {
				value +=(pairValues[cycle.get(i)][0]*pairValues[cycle.get(i+1)][1]);
				//System.out.print("("+pairValues[cycle.get(i)][0]+" to "+pairValues[cycle.get(i+1)][1]+") -> ");
				
			}
			
			//take donor score of vertex i and recipient score of vertex i+1
			value += pairValues[cycle.get(0)][0]*pairValues[cycle.get(cycle.size()-1)][1];
			
			//System.out.print("("+pairValues[cycle.get(0)][0]+" to "+pairValues[cycle.get(cycle.size()-1)][1]+")\n");
			if(averaged) {
				value = value/(double)cycle.size();
			}
			values.add(value);
		}
		return values;
	}
}
