package util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.function.Consumer;

import org.jgrapht.alg.cycle.HawickJamesSimpleCycles;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;

import com.gurobi.gurobi.GRBException;

public class CycleUtils {

	public static ArrayList<ArrayList<Integer>> getCycles(boolean[][] matches, int k) throws GRBException {

		SimpleDirectedGraph<Integer, DefaultEdge> g = new SimpleDirectedGraph<>(DefaultEdge.class);

		//add vertices
		ArrayList<Integer> vertices = new ArrayList<>(matches.length);
		for(int i = 0; i<matches.length; i++) {
			vertices.add(i);
			g.addVertex(vertices.get(i));

		}

		//add edges
		for(int i = 0; i<matches.length; i++) {
			for(int j = 0; j<matches.length; j++) {
				if(matches[i][j]) {
					g.addEdge(vertices.get(i), vertices.get(j));
				}
			}
		}


		//find cycles
		HawickJamesSimpleCycles<Integer, DefaultEdge> finder = new HawickJamesSimpleCycles<>(g);
		finder.setPathLimit(k);
		ArrayList<ArrayList<Integer>> cycles = new ArrayList<>();
		finder.findSimpleCycles(new CycleConsumer(cycles));
		return cycles;
	}

	public static class CycleConsumer implements Consumer<List<Integer>> {

		ArrayList<ArrayList<Integer>> cycles;

		public CycleConsumer(ArrayList<ArrayList<Integer>> cycles) {
			this.cycles = cycles;
		}

		@Override
		public void accept(List<Integer> t) {
			ArrayList<Integer> c = new ArrayList<>(t.size());
			c.addAll(t);
			cycles.add(c);
		}
	}

	/**
	 * Calculates the scores of each pair's donor and recipient.
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

	/**
	 * Calculates the score of all supplied cycles as defined in the thesis.
	 * @param matches possible matches
	 * @param cycles The cycles to be assigned a value
	 * @return
	 */
	public static ArrayList<Double> calculateCycles(boolean[][] matches, ArrayList<ArrayList<Integer>> cycles) {
		ArrayList<Double> values = new ArrayList<>(cycles.size());
		double[][] pairValues = calculatePairs(matches);
		for(ArrayList<Integer> cycle : cycles) {
			double value = 0;
			for(int i = 0; i<cycle.size()-1;i++) {
				value += Math.sqrt(pairValues[cycle.get(i)][0]*pairValues[cycle.get(i+1)][1]);
				//System.out.print("("+pairValues[cycle.get(i)][0]+" to "+pairValues[cycle.get(i+1)][1]+") -> ");

			}

			//take donor score of vertex i and recipient score of vertex i+1
			value += Math.sqrt(pairValues[cycle.get(0)][0]*pairValues[cycle.get(cycle.size()-1)][1]);

			//System.out.print("("+pairValues[cycle.get(0)][0]+" to "+pairValues[cycle.get(cycle.size()-1)][1]+")\n");

			value = value/(double)cycle.size();

			values.add(value);
		}
		return values;
	}

	/**
	 * Calculates the score of a single cycle as defined in the thesis.
	 * @param matches possible matches
	 * @param cycles The cycles to be assigned a value
	 * @return
	 */
	public static double calculateCycle(boolean[][] matches, ArrayList<Integer> cycle, double[][] pairValues) {

		double value = 0;
		for(int i = 0; i<cycle.size()-1;i++) {
			value += Math.sqrt(pairValues[cycle.get(i)][0]*pairValues[cycle.get(i+1)][1]);
			//System.out.print("("+pairValues[cycle.get(i)][0]+" to "+pairValues[cycle.get(i+1)][1]+") -> ");

		}

		//take donor score of vertex i and recipient score of vertex i+1
		value += Math.sqrt(pairValues[cycle.get(0)][0]*pairValues[cycle.get(cycle.size()-1)][1]);

		//System.out.print("("+pairValues[cycle.get(0)][0]+" to "+pairValues[cycle.get(cycle.size()-1)][1]+")\n");

		value = value/(double)cycle.size();



		return value;
	}
	
	/**
	 * iteratively removes unmatchable pairs
	 * @param matches
	 * @return
	 */
	public static boolean[][] reduceMatchMatrix(boolean[][] matches){
		int n = matches.length;
		
		int removed = 0;
		HashSet<Integer> toRemove = new HashSet<>(n/2);
		boolean improved = true;
		while(improved) {
			improved = false;
			

			
			rowLoop:
			for(int i = 0; i<n; i++) {
				for(int j = 0; j<n; j++) {
					//if a true value is encountered go to the next row
					if(matches[i][j]) {
						if(toRemove.contains(j)) {
							matches[i][j] = false;
							improved = true;
						}
						continue rowLoop;
					}
				}
				//else
				toRemove.add(i);

				removed++;
			}
			columnLoop:
			for(int j = 0; j<n; j++) {
				for(int i = 0; i<n; i++) {
					if(matches[i][j]) {
						if(toRemove.contains(i)) {
							matches[i][j] = false;
							improved = true;
						}
						else {
							continue columnLoop;
						}
					}
				}
				//if does not contain any true values
				toRemove.add(j);
				removed++;
			}
		}
		System.out.println("Match-matrix reduction removed " +removed+ " pairs");
		return matches;
	}
}
