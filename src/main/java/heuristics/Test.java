package heuristics;

import java.io.File;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;

import org.jgrapht.alg.util.Pair;

import com.gurobi.gurobi.GRBException;

import data.XMLData;
import reproduction.CycleFormulation;
import reproduction.EEFormulation;
import util.CycleUtils;
import util.TimedPrintStream;

public class Test {

	public static void main(String[] args) throws Exception {

		System.setOut(new TimedPrintStream(System.out));
		
		final int k = 4;

		File folder = new File("src/main/resources/delorme");
		File[] listOfFiles = folder.listFiles();

		for(int u = 50; u<60; u++) {

			File data = listOfFiles[u];
			System.out.println("Data set read: " + data.getName());

			//ExcelReader dr = new ExcelReader(data);
			//final boolean[][] matches = WMDReader.read(data);
			//final boolean[][] matches = SimpleDataGeneration.generate(n, density);

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

			double density = matchCount/(double) Math.pow(matches.length,2)*100/100;
			System.out.println("Data has " +matches.length+ " matchable pairs with an average density of " + density);

			tabuTest(matches, k); 
			
		}

	}

	public static void tabuTest(boolean[][] matches, int k) throws Exception {
		System.out.println("Starting Tabu local search");

		HashSet<ArrayList<Integer>> initialSolution = JumpStart.getJumpStart(matches, k);
		CyclePackingFormulation.upperBoundFromPacking(CyclePackingFormulation.solveWithSolution(matches), k);
		TabuLocalSearch tls = new TabuLocalSearch(matches, initialSolution, 4);
		tls.run(k, 1800);
		System.out.println("final objective: "+tls.getBestObj()+"\n");
	}

	public static HashSet<ArrayList<Integer>> greedyCyclesTest(boolean[][] matches, int k) throws GRBException {

		int iter = 1000;
		double aggressiveness = 0.5;
		int randomSelection = 10;
		HashSet<ArrayList<Integer>> bestSolution = null;
		int best = 0;
		System.out.println("["+LocalTime.now().truncatedTo(ChronoUnit.MINUTES).toString()+"] "+"Running greedy heuristic");

		int n = matches.length;

		ArrayList<ArrayList<Integer>> cycles = CycleUtils.getCycles(matches, k);
		GreedyCycles gc = new GreedyCycles(cycles, CycleUtils.calculateCycles(matches, cycles), aggressiveness, randomSelection, matches.length);

		for(int i = 0; i<iter; i++) {
			if((i+1)%5 == 0) {
				//System.out.println(i+1);
			}

			ArrayList<Integer> result = gc.runNoFilter();
			int pairCount = 0;
			for(Integer c : result) {
				pairCount += cycles.get(c).size();
			}
			if(pairCount>best) {
				best = pairCount;
				bestSolution = new HashSet<>(n/2);
				for(Integer c : result) {
					bestSolution.add(cycles.get(c));
				}
			}
		}
		System.out.println("Average runtime: " + gc.getAverageRunTime());
		System.out.println("Best Solution: "+best+"\n");

		return bestSolution;
	}
}
