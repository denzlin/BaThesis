package heuristics;

import java.io.File;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

import org.jgrapht.alg.util.Pair;

import com.gurobi.gurobi.GRBException;

import data.XMLData;
import reproduction.CycleFormulation;
import reproduction.EEFormulation;
import util.CycleUtils;
import util.TimedPrintStream;

public class Test {

	public static void main(String[] args) throws Exception {		
		final int k = 4;

		File folder = new File("src/main/resources/delorme");
		File[] listOfFiles = folder.listFiles();

		double avg = 0.0;
		for(int u = 30; u<40; u++) {

			File data = listOfFiles[u];
			System.out.println("Matching " + data.getName()+ " for k = "+k+" with matheuristic using EE");
			
			XMLData reader = new XMLData(data);
			final boolean[][] matches = reader.getMatches();
			int n = matches.length;
			int startTime = Math.toIntExact(TimeUnit.NANOSECONDS.toSeconds(System.nanoTime()));
			EEFormulation.solve(matches, 4, null, 10000, 1800);
			/*
			ArrayList<ArrayList<Integer>> cycles = CycleUtils.getCycles(matches, k);
			CycleFormulation.solveMinimal(cycles, matches, new ArrayList<>());
			*/
			avg += Math.toIntExact(TimeUnit.NANOSECONDS.toSeconds(System.nanoTime()))-startTime;
	
		}
		System.out.println("avg solve time : "+avg/10.0);
		
		/*
		double max = Collections.max(cycleValuesAll);

		//assign cycle values to distribution buckets
		double[] distrAll = new double[50];
		for(double value : cycleValuesAll) {
			distrAll[(int) Math.ceil((value/max)*(50))-1]++;
		}
		//standardize units
		for(int i = 0; i<distrAll.length;i++) {
			distrAll[i] = distrAll[i]/(double) cycleValuesAll.size();
			distrAll[i] = Math.round(distrAll[i]*1000)/10.0;
		}
		System.out.println("\nAll cycles score distribution (in %):\n"+Arrays.toString(distrAll));

		double[] distrSol = new double[50];
		for(double value : cycleValuesSol) {
			distrSol[(int) Math.ceil((value/max)*(50))-1]++;
		}

		for(int i = 0; i<distrSol.length;i++) {
			distrSol[i] = distrSol[i]/(double) cycleValuesSol.size();
			distrSol[i] = Math.round(distrSol[i]*1000)/10.0;
		}
		System.out.println("\nSolution cycles score distribution (in %):\n"+Arrays.toString(distrSol)+"\n");
		*/
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
