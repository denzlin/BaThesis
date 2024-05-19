package reproduction;

import java.io.IOException;
import java.util.ArrayList;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;

/**
 * Solves the cycle formulation of the problem
 */
public class CycleFormulation {

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
		
		Graph<Integer, DefaultEdge> g = new SimpleDirectedGraph<>(DefaultEdge.class);
		
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
		ArrayList<ArrayList<Integer>> cycles = CycleFinder.getCycles(g, 3);
		System.out.println("test");
		
	}
}
