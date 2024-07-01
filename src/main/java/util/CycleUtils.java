package util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.jgrapht.alg.connectivity.GabowStrongConnectivityInspector;
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
				//value += Math.max(pairValues[cycle.get(i)][0],pairValues[cycle.get(i+1)][1]);
				//value += pairValues[cycle.get(i)][0] + pairValues[cycle.get(i+1)][1];
				value += Math.sqrt(pairValues[cycle.get(i)][0]*pairValues[cycle.get(i+1)][1]);
			}

			//take donor score of vertex i and recipient score of vertex i+1
			//value += Math.max(pairValues[cycle.get(0)][0], pairValues[cycle.get(cycle.size()-1)][1]);
			//value += pairValues[cycle.get(0)][0]+pairValues[cycle.get(cycle.size()-1)][1];
			value += Math.sqrt(pairValues[cycle.get(0)][0]*pairValues[cycle.get(cycle.size()-1)][1]);

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
			//value += Math.max(pairValues[cycle.get(i)][0],pairValues[cycle.get(i+1)][1]);
			//value += pairValues[cycle.get(i)][0]+pairValues[cycle.get(i+1)][1];
			value += Math.sqrt(pairValues[cycle.get(i)][0]*pairValues[cycle.get(i+1)][1]);
		}

		//take donor score of vertex i and recipient score of vertex i+1
		//value += Math.max(pairValues[cycle.get(0)][0],pairValues[cycle.get(cycle.size()-1)][1]);
		//value += pairValues[cycle.get(0)][0]+pairValues[cycle.get(cycle.size()-1)][1];
		value += Math.sqrt(pairValues[cycle.get(0)][0]*pairValues[cycle.get(cycle.size()-1)][1]);

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
		
		HashSet<Integer> toRemove = new HashSet<>(n/2);
		boolean improved = true;
		boolean encounter = false;
		while(improved) {
			improved = false;
			
			for(int i = 0; i<n; i++) {
				encounter = false;
				for(int j = 0; j<n; j++) {
					
					//if a true value is encountered go to the next row
					if(matches[i][j]) {
						if(toRemove.contains(j)) {
							matches[i][j] = false;
							improved = true;
						}
						else {
							encounter = true;
						}
					}
					
				}
				if(encounter == false && !toRemove.contains(i)){
					toRemove.add(i);
					improved = true;
				}
			}
			

			for(int j = 0; j<n; j++) {
				encounter = false;
				for(int i = 0; i<n; i++) {
					
					if(matches[i][j]) {
						if(toRemove.contains(i)) {
							matches[i][j] = false;
							improved = true;
						}
						else {
							encounter = true;
						}
					}
					
				}
				if(encounter == false && !toRemove.contains(j)){
					toRemove.add(j);
					improved = true;
				}
			}
		}
		
		for(int i = 0; i<n; i++) {
			for(int j = 0; j<n; j++) {
				if(toRemove.contains(i) || toRemove.contains(j)) {
					matches[i][j] = false;
				}
			}
		}
		
		System.out.println("Match-matrix reduction removed " +toRemove.size()+ " pairs");
		return matches;
	}
	
	public static void connectivity(boolean[][] matches, int k) {
		
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
		
		GabowStrongConnectivityInspector<Integer, DefaultEdge> insp = new GabowStrongConnectivityInspector<>(g);
		if(!insp.isStronglyConnected()) {
			System.out.println("Graph not strongly connected, analysing...");
			for(Set<Integer> set : insp.stronglyConnectedSets()) {
				System.out.println("set size: "+ set.size());
				System.out.println(Arrays.toString(set.toArray())); 
			}
		} else {
			System.out.println("graph is strongly connected");
		}
	}
	
	
	// this will not be efficient
	/**
	 * orders matrix by degree
	 * mode 0 is descending, mode 1 is ascending
	 * @param matches
	 * @param mode
	 * @return
	 */
	public static boolean[][] orderMatrixByDegree(boolean[][] matches, int mode){
		
		int n = matches.length;
		int[] degrees = new int[n];
		HashMap<Integer, Integer> oldToNew = new HashMap<>(n);
		for(int i = 0; i<n; i++) {
			for(int j = 0; j<n; j++) {
				if(matches[i][j]) {
					degrees[i]++;
					degrees[j]++;
				}
			}
		}
		while(oldToNew.size()<n) {
			if(mode == 0) {
				int max = -1;
				int maxVal = -1;
				for(int i = 0; i<n; i++) {
					if(degrees[i]>maxVal) {
						max = i;
						maxVal = degrees[i];
					}
				}
				oldToNew.put(max, oldToNew.size());
				degrees[max] = -1;
			}
			
			if(mode == 1) {
				int min = -1;
				int minVal = Integer.MAX_VALUE;
				for(int i = 0; i<n; i++) {
					if(degrees[i]<minVal) {
						min = i;
						minVal = degrees[i];
					}
				}
				oldToNew.put(min, oldToNew.size());
				degrees[min] = Integer.MAX_VALUE;
			}
		}
		
		boolean[][] toReturn = new boolean[n][n];
		for(int i = 0; i<n; i++) {
			for(int j = 0; j<n; j++) {
				if(matches[i][j]) {
					toReturn[oldToNew.get(i)][oldToNew.get(j)] = true;
				}
			}
		}
		int[] newDegrees = new int[n];
		for(int i = 0; i<n; i++) {
			for(int j = 0; j<n; j++) {
				if(toReturn[i][j]) {
					newDegrees[i]++;
					newDegrees[j]++;
				}
			}
		}
		return toReturn;
	}
}
