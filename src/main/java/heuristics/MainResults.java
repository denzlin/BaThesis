package heuristics;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

import data.WMDReader;
import data.XMLData;
import reproduction.CycleFormulation;
import reproduction.EEFormulation;
import util.CycleUtils;
import util.TimedPrintStream;

public class MainResults {

	public static void main(String[] args) throws Exception {
		System.setOut(new TimedPrintStream(System.out));
		
		//ExcelReader dr = new ExcelReader(data);
		//final boolean[][] matches = WMDReader.read(data);
		//final boolean[][] matches = SimpleDataGeneration.generate(n, density);
		//System.out.println("Simple data generated with n = "+n+" and a density of "+density);
		File folder = new File("src/main/resources/delorme");
		File[] listOfFiles = folder.listFiles();
		
		//parameters
		int k				= 4;
		int heuristicTime 	= 1800;
		int solverTime 		= 1800;
		int[] opts = new int[] {754,	727,	718,	807,	755,	788,	766,	810,	752,	749};

		for(int u = 50; u<60; u++) {
			File data = listOfFiles[u];
			System.out.println("\n");
			System.out.println("Matching " + data.getName()+ " for k = "+k+" with matheuristic using EE");
			
			XMLData reader = new XMLData(data);
			final boolean[][] matches = reader.getMatches();
			
			//final boolean[][] matches = WMDReader.read(data);
			double matchCount = 0;
			for(boolean[] row : matches) {
				for(boolean val : row) {
					if(val) {
						matchCount++;
					}
				}
			}
			
			int UB = CyclePackingFormulation.solve(matches);
			UB = opts[u-50];
			double density = matchCount/(double) Math.pow(matches.length,2)*100/100;
			System.out.println("Data has " +matches.length+ " matchable pairs with an average density of " + density);
			
			System.out.println("Starting Tabu local search");

			
			HashSet<ArrayList<Integer>> initialSolution = JumpStart.getJumpStart(matches, k);
			
			TabuLocalSearch tls = new TabuLocalSearch(matches, initialSolution, k);
			tls.run(heuristicTime, UB);
			System.out.println("final heuristic objective: "+tls.getBestObj());
			
			
			//System.out.println("EE time taken: " +EEFormulation.solve(matches, k, tls.getSolutionCycles(), UB, solverTime).getFirst()); 
			
			/*
			int startTime = (int) TimeUnit.NANOSECONDS.toSeconds(System.nanoTime());
			
			ArrayList<ArrayList<Integer>> cycles = CycleUtils.getCycles(matches, k);
			if(cycles.size() > 40000000) {
				System.out.println("too many cycles, aborting");
				continue;
			}
			System.out.println(cycles.size()+" cycles found");
			CycleFormulation.solveMinimal(cycles, matches, tls.getSolutionCycles());
			System.out.println("Cycles and Cycle formulation in "+ Math.toIntExact(TimeUnit.NANOSECONDS.toSeconds(System.nanoTime()) - startTime)+"s");
			*/
		}

	}
}
