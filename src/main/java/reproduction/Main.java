package reproduction;

import java.io.IOException;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;

import com.gurobi.gurobi.GRBException;

public class Main {

	public static void main(String[] args) throws IOException, GRBException {

		final String data = "Instance_XL_0.xlsx";
		final int k = 3;
		
		System.out.println("Matching " + data);

		// get data
		DataReader dr = new DataReader(data);
		final int[] ids = dr.getIds();
		final boolean[][] matches = dr.getMatches();
		double matchCount = 0;
		for(boolean[] row : matches) {
			for(boolean val : row) {
				if(val) {
					matchCount++;
				}
			}
		}

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
		System.out.println("Starting Cycle Search");
		long startTime = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime());
		ArrayList<ArrayList<Integer>> cycles = CycleFinder.getCycles(g, k);
		System.out.println(cycles.size() + " cycles found in " +(TimeUnit.NANOSECONDS.toSeconds((System.nanoTime())) - startTime)+ " seconds.");
		int n = g.vertexSet().size();
		System.out.println("Data has " +ids.length+ " pairs with an average density of " + matchCount/(double) matches.length +"%.");

		EEFormulation.solve(cycles, k, n);
		CycleFormulation.solve(cycles, k, n);




	}

}
