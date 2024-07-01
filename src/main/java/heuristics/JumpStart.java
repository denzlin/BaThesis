package heuristics;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

import com.gurobi.gurobi.GRBException;

import util.CycleUtils;

public class JumpStart {

	public static HashSet<ArrayList<Integer>> getJumpStart(boolean[][] matches, int k) throws GRBException{
		
		System.out.println("Using JumpStart heuristic to find initial solution");
		
		int n = matches.length;
		int greedyTime = 60;
		
		//solve pairing formulation which gives the optimal 2-cycle matching with the lowest score per cycle
		HashSet<ArrayList<Integer>> pairingSolution = PairingFormulation.solve(matches);

		boolean[][] reducedMatches = new boolean[n][n];
		
		for(int i = 0; i<n; i++) {
			for(int j = 0; j<n; j++) {
				reducedMatches[i][j] = matches[i][j];
			}
		}
		for(ArrayList<Integer> cycle : pairingSolution) {
			for(Integer vertex : cycle) {
				reducedMatches[vertex] = new boolean[n];
				for(boolean[] row : reducedMatches) {
					row[vertex] = false;
				}
			}
		}
		
		ArrayList<ArrayList<Integer>> reducedCycles = CycleUtils.getCycles(reducedMatches, k);
		ArrayList<Double> reducedCycleScores = CycleUtils.calculateCycles(reducedMatches, reducedCycles);
		
		GreedyCycles gc = new GreedyCycles(reducedCycles, reducedCycleScores, 1.0, 50, n);
		ArrayList<Integer> bestRun = new ArrayList<>();
		int bestObj = 0;
		int startTime = Math.toIntExact(TimeUnit.NANOSECONDS.toSeconds(System.nanoTime()));
		while(Math.toIntExact(TimeUnit.NANOSECONDS.toSeconds(System.nanoTime())) < startTime+greedyTime) {
			ArrayList<Integer> runResult = gc.runNoFilter();
			int runObj = 0;
			for(Integer c : runResult) {
				runObj += reducedCycles.get(c).size();
			}
			if(runObj > bestObj) {
				bestRun = runResult;
				bestObj = runObj;
			}
		}
		int matchCount = 0;
		HashSet<ArrayList<Integer>> resultSolution = new HashSet<>(pairingSolution.size()+bestRun.size());
		for(ArrayList<Integer> cycle : pairingSolution) {
			resultSolution.add(cycle);
			matchCount += 2;
		}
		
		for(Integer v : bestRun) {
			resultSolution.add(reducedCycles.get(v));
			matchCount += reducedCycles.get(v).size();
		}
		System.out.println("pairing matched "+pairingSolution.size()*2+ " vertices, greedy added "+bestObj);
		System.out.println("JumpStart heuristic solution matched "+matchCount+" vertices" );
		return resultSolution;
	}
	
	public static HashSet<ArrayList<Integer>> getGreedySolution(boolean[][] matches, int k) throws GRBException{
		
		int n = matches.length;
		int greedyRuns = 1000;
		
		ArrayList<ArrayList<Integer>> cycles = CycleUtils.getCycles(matches, k);
		ArrayList<Double> cycleScores = CycleUtils.calculateCycles(matches, cycles);
		
		GreedyCycles gc = new GreedyCycles(cycles, cycleScores, 1.0, 10, n);
		ArrayList<Integer> bestRun = new ArrayList<>();
		int bestObj = 0;
		for(int i = 0; i<greedyRuns; i++) {
			ArrayList<Integer> runResult = gc.runNoFilter();
			int runObj = 0;
			for(Integer c : runResult) {
				runObj += cycles.get(c).size();
			}
			if(runObj > bestObj) {
				bestRun = runResult;
				bestObj = runObj;
			}
		}
		int matchCount = 0;
		HashSet<ArrayList<Integer>> resultSolution = new HashSet<>(bestRun.size());
	
	
		for(Integer v : bestRun) {
			resultSolution.add(cycles.get(v));
			matchCount += cycles.get(v).size();
		}
		System.out.println("only using greedy matched "+matchCount+" vertices" );
		return resultSolution;
	}
}
