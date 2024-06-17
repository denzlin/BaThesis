package heuristics;

import java.util.ArrayList;
import java.util.HashSet;

import com.gurobi.gurobi.GRBException;

import util.CycleUtils;

public class JumpStart {

	public static HashSet<ArrayList<Integer>> getJumpStart(boolean[][] matches, int k) throws GRBException{
		
		System.out.println("Using JumpStart heuristic to find initial solution");
		
		int n = matches.length;
		int greedyRuns = 1000;
		
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
		
		
		GreedyCycles gc = new GreedyCycles(reducedCycles, reducedCycleScores, 1.0, 10, n);
		ArrayList<Integer> bestRun = null;
		int bestObj = 0;
		for(int i = 0; i<greedyRuns; i++) {
			ArrayList<Integer> runResult = gc.runNoFilter();
			int runObj = 0;
			for(Integer c : runResult) {
				runObj += reducedCycles.get(c).size();
			}
			if(runObj > bestObj) {
				bestRun = runResult;
			}
		}
		
		int matchCount = 0;
		HashSet<ArrayList<Integer>> resultSolution = new HashSet<>(pairingSolution.size()+bestRun.size());
		for(ArrayList<Integer> cycle : pairingSolution) {
			if(!matches[cycle.get(0)][cycle.get(1)]) {
				throw new IllegalArgumentException();
			}
			resultSolution.add(cycle);
			matchCount += 2;
		}
		
		for(Integer v : bestRun) {
			ArrayList<Integer> cycle = reducedCycles.get(v);
			for(int i = 0; i< cycle.size()-1; i++) {
				if(!matches[cycle.get(i)][cycle.get(i+1)]) {
					throw new IllegalArgumentException();
				}
			}
			
			if(!matches[cycle.get(cycle.size()-1)][cycle.get(0)]) {
				throw new IllegalArgumentException();
			}
			resultSolution.add(reducedCycles.get(v));
			matchCount += reducedCycles.get(v).size();
		}
		
		for(ArrayList<Integer> cycle : resultSolution) {
			for(int i = 0; i< cycle.size()-1; i++) {
				if(!matches[cycle.get(i)][cycle.get(i+1)]) {
					throw new IllegalArgumentException();
				}
			}
			
			if(!matches[cycle.get(cycle.size()-1)][cycle.get(0)]) {
				throw new IllegalArgumentException();
			}
		}
		System.out.println("JumpStart heuristic solution matched "+matchCount+" vertices" );
		return resultSolution;
	}
}
