package heuristics;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

import com.gurobi.gurobi.*;
import com.gurobi.gurobi.GRB.*;

public class CyclePackingFormulation {

	public static int solve(boolean[][] matches) throws GRBException {
		System.out.println("Starting no-k solve");

		int n = matches.length;
		//create empty graph copies

		//construct gurobi model
		GRBEnv env = new GRBEnv(true);
		env.set(IntParam.OutputFlag, 0);
		env.set("logFile", "mip1.log");
		env.start();

		
		GRBModel model = new GRBModel(env);
		model.set(GRB.IntAttr.ModelSense, -1);
		model.set(GRB.DoubleParam.TimeLimit, 1800);
		
		GRBVar[][] x = new GRBVar[n][n];
		
		for(int i = 0; i<n; i++) {
			for(int j = 0; j<n; j++) {
				if(matches[i][j]) {
					x[i][j] = model.addVar(0, 1, 1, GRB.BINARY, "x["+i+"]["+j+"]");
				}
			}
		}
		
		for(int i = 0; i<n; i++) {
			GRBLinExpr conserv = new GRBLinExpr();
			GRBLinExpr capac = new GRBLinExpr();
			
			for(int j = 0; j<n; j++) {
				if(matches[i][j]) {
					conserv.addTerm(1, x[i][j]);
					capac.addTerm(1, x[i][j]);
				}
				if(matches[j][i]) {
					conserv.addTerm(-1, x[j][i]);
				}
			}
			if(conserv.size() != 0) {
				model.addConstr(conserv, GRB.EQUAL, 0, "conservation"+i);
			}
			if(conserv.size() != 0) {
				model.addConstr(capac, GRB.LESS_EQUAL, 1, "capac"+i);
			}
		}
		
		long startTime = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime());
		model.optimize();
		System.out.println("No-k formulation solved in: "+Math.toIntExact(TimeUnit.NANOSECONDS.toSeconds(System.nanoTime()) - startTime)+" seconds");
		
		System.out.println("No-k -> Pairs matched: " + model.get(GRB.DoubleAttr.ObjVal) + " out of " + n + ". \n");
		env.dispose();
        return (int) model.get(GRB.DoubleAttr.ObjVal);
	}
	
	public static HashSet<int[]> solveWithSolution(boolean[][] matches) throws GRBException {
		System.out.println("Starting No-k solve");

		int n = matches.length;
		//create empty graph copies

		//construct gurobi model
		GRBEnv env = new GRBEnv(true);
		env.set(IntParam.OutputFlag, 0);
		env.set("logFile", "mip1.log");
		env.start();

		
		GRBModel model = new GRBModel(env);
		model.set(GRB.IntAttr.ModelSense, -1);
		model.set(GRB.DoubleParam.TimeLimit, 1800);
		
		GRBVar[][] x = new GRBVar[n][n];
		
		for(int i = 0; i<n; i++) {
			for(int j = 0; j<n; j++) {
				if(matches[i][j]) {
					x[i][j] = model.addVar(0, 1, 1, GRB.BINARY, "x["+i+"]["+j+"]");
				}
			}
		}
		
		for(int i = 0; i<n; i++) {
			GRBLinExpr conserv = new GRBLinExpr();
			GRBLinExpr capac = new GRBLinExpr();
			
			for(int j = 0; j<n; j++) {
				if(matches[i][j]) {
					conserv.addTerm(1, x[i][j]);
					capac.addTerm(1, x[i][j]);
				}
				if(matches[j][i]) {
					conserv.addTerm(-1, x[j][i]);
				}
			}
			if(conserv.size() != 0) {
				model.addConstr(conserv, GRB.EQUAL, 0, "conservation"+i);
			}
			if(conserv.size() != 0) {
				model.addConstr(capac, GRB.LESS_EQUAL, 1, "capac"+i);
			}
		}
		
		long startTime = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime());
		model.optimize();
		
		HashSet<int[]> solution = new HashSet<>((int) model.get(GRB.DoubleAttr.ObjVal));
		
		for(int i = 0; i<n; i++) {
			for(int j = 0; j<n; j++) {
				if(x[i][j] != null && x[i][j].get(DoubleAttr.X) == 1.0) {
					solution.add(new int[] {i,j});
				}
			}
		}
		
		System.out.println("No-k formulation solved in: "+Math.toIntExact(TimeUnit.NANOSECONDS.toSeconds(System.nanoTime()) - startTime)+" seconds");
		
		System.out.println("No-k -> Pairs matched: " + model.get(GRB.DoubleAttr.ObjVal) + " out of " + n + ". \n");
		env.dispose();
        return solution;
	}
	
	public static int upperBoundFromPacking(HashSet<int[]> packingSolution, int k) {
		
		System.out.println("Attempting to improve upper bound from no-k solution... (unimplemented)");
		ArrayList<LinkedList<int[]>> subSolutions = new ArrayList<>();
		while(!packingSolution.isEmpty()) {

			//get a starting edge
			int[] startEdge = null;
			for(int[] edge : packingSolution) {
				startEdge = edge;
				break;
			}
			
			packingSolution.remove(startEdge);
			
			//make new sub-solution
			LinkedList<int[]> subSol = new LinkedList<>();
			subSol.add(startEdge);
			int[] ends = new int[] {startEdge[0], startEdge[1]};
			
			//loop over solution edges until cycle is complete
			
			while(true) {
				boolean added = false;
				ArrayList<int[]> toRemove = new ArrayList<>(); //cant remove while iterating (spliterator?)
				for(int[] edge : packingSolution) {
					//add edges that fit to end or start of linkedlist
					if(edge[0] == ends[1] && !toRemove.contains(edge)) {
						toRemove.add(edge);
						subSol.addLast(edge);
						ends[1] = edge[1];
						added = true;
					}
					if(edge[1] == ends[0] && !toRemove.contains(edge)) {
						
						toRemove.add(edge);
						subSol.addFirst(edge);
						ends[0] = edge[0];
						added = true;
					}
				}
				if(!added) {
					break;
				}
				else {
					packingSolution.removeAll(toRemove);
				}
			}
			/*
			System.out.print(arrow+ "Cycle found of size " +subSol.size() +":");
			for(int[] edge : subSol) {
				System.out.print(" ["+edge[0]+", "+edge[1]+"]");
			}
			*/
		}
		
		// for every subsolution see if a k-cycle solution can be found using the subsolution's
		// nodes and/or unmatched nodes. 
		
		// note: only subsolutions of a certain size can be solved in reasonable time, and that size
		// depends on which formulation, solving software, and hardware are used. 
		// TODO: dynamically lower k depending on parameters and subsolution size to make solving
		// possible albeit with a lower chance of lowering the UB.

		//System.out.print("\n");
		return subSolutions.size();
	}
}
