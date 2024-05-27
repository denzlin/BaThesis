package reproduction;

import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.jgrapht.alg.connectivity.GabowStrongConnectivityInspector;
import org.jgrapht.alg.util.Pair;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;

import com.gurobi.gurobi.GRBException;

import data_read_write.ExcelReader;
import data_read_write.WMDReader;
import util.CycleFinder;
import util.PairValues;

public class Main {

	public static void main(String[] args) throws IOException, GRBException {


		final int k = 3;
		double T_average = 0;
		double gapAverage = 0;
		double cycleT_average = 0;
		
		File folder = new File("src/main/resources/preflib");
		File[] listOfFiles = folder.listFiles();
		
		for(int u = 71; u<72; u++) {
			File data = listOfFiles[u];
			
			System.out.println("Matching " + data.getName());

			// ExcelReader dr = new ExcelReader(data);
			final boolean[][] matches = WMDReader.read(data);
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
			
			//uncomment to see if disjoint sets exist
			/*
			GabowStrongConnectivityInspector<Integer, DefaultEdge> insp = new GabowStrongConnectivityInspector(g);
			if(!insp.isStronglyConnected()) {
				System.out.println("Graph not strongly connected, analysing...");
				for(Set<Integer> set : insp.stronglyConnectedSets()) {
					System.out.println("set size: "+ set.size());
				}
			} else {
				System.out.println("graph is strongly connected");
			}
			*/
			
			//find cycles
			System.out.println("Starting Cycle Search");
			long startTime = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime());
			ArrayList<ArrayList<Integer>> cycles = CycleFinder.getCycles(g, k);
			cycleT_average += TimeUnit.NANOSECONDS.toSeconds((System.nanoTime())) - startTime;
		
			System.out.println(cycles.size() + " cycles found in " +(TimeUnit.NANOSECONDS.toSeconds((System.nanoTime())) - startTime)+ " seconds.");
			int n = g.vertexSet().size();
			System.out.println("Data has " +matches.length+ " pairs with an average density of " + matchCount/(double) Math.pow(matches.length,2));

			//EEFormulation.solve(cycles, k, n);
			Pair<Integer, Double> result = CycleFormulation.solve(cycles, k, n);
			T_average += result.getFirst();
			gapAverage += result.getSecond();
		}
		System.out.println("\navg cycle T: "+((double) cycleT_average)/10.0);
		System.out.println("avg T: "+((double) T_average)/10.0);
		System.out.println("avg gap: "+ ((double) gapAverage)/10.0);
	}

}
