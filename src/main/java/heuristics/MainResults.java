package heuristics;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;

import data.XMLData;
import reproduction.EEFormulation;
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
		
		for(int u = 87; u<90; u++) {
			File data = listOfFiles[u];
			System.out.println("\n");
			System.out.println("Matching " + data.getName()+ " for k = "+k+" with matheuristic using EE");
			XMLData reader = new XMLData(data);
			final boolean[][] matches = reader.getMatches();

			double matchCount = 0;
			for(boolean[] row : matches) {
				for(boolean val : row) {
					if(val) {
						matchCount++;
					}
				}
			}
			int UB = CyclePackingFormulation.solve(matches);
			
			double density = matchCount/(double) Math.pow(matches.length,2)*100/100;
			System.out.println("Data has " +matches.length+ " matchable pairs with an average density of " + density);
			
			System.out.println("Starting Tabu local search");

			HashSet<ArrayList<Integer>> initialSolution = JumpStart.getJumpStart(matches, k);
			
			//CyclePackingFormulation.upperBoundFromPacking(CyclePackingFormulation.solveWithSolution(matches), k);
			TabuLocalSearch tls = new TabuLocalSearch(matches, initialSolution, k);
			tls.run(heuristicTime, UB);
			System.out.println("final heuristic objective: "+tls.getBestObj());
			
			//System.out.println("Solving relaxation without initial solution");
			//EEFormulation.solveRelaxation(matches, k, new ArrayList<ArrayList<Integer>>(), solverTime);
			//EEFormulation.solveRelaxation(matches, k, tls.getSolutionCycles(), solverTime);
			
			EEFormulation.solve(matches, k, tls.getSolutionCycles(), UB, solverTime);
			
			//System.out.println("EE done, starting Cycle formulation");
			//int startTime = (int) TimeUnit.NANOSECONDS.toSeconds(System.nanoTime());
			//ArrayList<ArrayList<Integer>> cycles = CycleUtils.getCycles(matches, k);
			
			//System.out.println("Cycles found in "+ Math.toIntExact(TimeUnit.NANOSECONDS.toSeconds(System.nanoTime()) - startTime) + "s");
			//CycleFormulation.solveMinimal(cycles, matches, tls.getSolutionCycles());
		}
	}
}
