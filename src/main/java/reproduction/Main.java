package reproduction;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.jgrapht.alg.connectivity.GabowStrongConnectivityInspector;
import org.jgrapht.alg.util.Pair;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;

import com.gurobi.gurobi.GRBException;

import data.XMLData;
import util.CycleFinder;
import util.PairValues;

public class Main {

	public static void main(String[] args) throws IOException, GRBException {


		final int k = 4;
		double T_average = 0;
		double gapAverage = 0;
		double cycleT_average = 0;
		ArrayList<Double> cycleValuesAll = new ArrayList<>();
		ArrayList<Double> cycleValuesSol = new ArrayList<>();
		File folder = new File("src/main/resources/delorme");
		File[] listOfFiles = folder.listFiles();
		
		int testSetSize = 10;
		for(int u = 40; u<50; u++) {
			File data = listOfFiles[u];
			
			System.out.println("["+LocalTime.now().truncatedTo(ChronoUnit.MINUTES).toString()+"] "+"Matching " + data.getName());

			//ExcelReader dr = new ExcelReader(data);
			//final boolean[][] matches = WMDReader.read(data);
			//final boolean[][] matches = SimpleDataGeneration.generate(n, density);
			//System.out.println("Simple data generated with n = "+n+" and a density of "+density);
			
			XMLData reader = new XMLData(data);
			final boolean[][] matches = reader.getMatches();
			int n = matches.length;
			
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
			long startTime = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime());
			ArrayList<ArrayList<Integer>> cycles = CycleFinder.getCycles(g, k);
			cycleT_average += TimeUnit.NANOSECONDS.toSeconds((System.nanoTime())) - startTime;
			cycleValuesAll.addAll(PairValues.calculateCycles(matches, cycles, true));
			
			System.out.println(cycles.size() + " cycles found in " +(TimeUnit.NANOSECONDS.toSeconds((System.nanoTime()))
					- startTime)+ " seconds for k = "+k);
			double density = matchCount/(double) Math.pow(matches.length,2)*100/100;
			System.out.println("Data has " +matches.length+ " matchable pairs with an average density of " + density);


			//Pair<Integer, Double> result =  EEFormulation.solve(matches, k, cycles);
			
			CycleFormulation cf = new CycleFormulation(cycles, k, n);
			Pair<Integer, Double> result = cf.solve();
			if(result==null) {
				System.out.println("too many cycles, aborting");
				continue;
			}
			
			ArrayList<ArrayList<Integer>> solutionCycles = new ArrayList<>();
			for(Integer cycle : cf.getSolutionCycles()) {
				solutionCycles.add(cycles.get(cycle));
			}
			cycleValuesSol.addAll(PairValues.calculateCycles(matches, solutionCycles, true));
			
			T_average += result.getFirst();
			gapAverage += result.getSecond();
		}
		
		//display statistics for all runs
		System.out.println("\navg cycle T: "+((double) cycleT_average)/testSetSize);
		System.out.println("avg T: "+((double) T_average)/testSetSize);
		System.out.println("avg gap: "+ ((double) gapAverage)/testSetSize);
		
		
		double max = Collections.max(cycleValuesAll);
		
		//assign cycle values to distribution buckets
		double[] distrAll = new double[20];
		for(double value : cycleValuesAll) {
			distrAll[(int) Math.ceil((value/max)*(20))-1]++;
		}
		//standardize units
		for(int i = 0; i<distrAll.length;i++) {
			distrAll[i] = distrAll[i]/(double) cycleValuesAll.size();
			distrAll[i] = Math.round(distrAll[i]*10000)/100.0;
		}
		System.out.println("\nAll cycles score distribution (in %):\n"+Arrays.toString(distrAll));
		
		double[] distrSol = new double[20];
		for(double value : cycleValuesSol) {
			distrSol[(int) Math.ceil((value/max)*(20))-1]++;
		}
		
		for(int i = 0; i<distrSol.length;i++) {
			distrSol[i] = distrSol[i]/(double) cycleValuesSol.size();
			distrSol[i] = Math.round(distrSol[i]*10000)/100.0;
		}
		System.out.println("\nSolution cycles score distribution (in %):\n"+Arrays.toString(distrSol)+"\n");
	}

	public void connectivity(SimpleDirectedGraph<Integer, DefaultEdge> g) {
		GabowStrongConnectivityInspector<Integer, DefaultEdge> insp = new GabowStrongConnectivityInspector<>(g);
		if(!insp.isStronglyConnected()) {
			System.out.println("Graph not strongly connected, analysing...");
			for(Set<Integer> set : insp.stronglyConnectedSets()) {
				System.out.println("set size: "+ set.size());
			}
		} else {
			System.out.println("graph is strongly connected");
		}
	}
}






