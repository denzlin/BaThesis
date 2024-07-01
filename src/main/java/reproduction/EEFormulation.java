package reproduction;

import java.io.File;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

import org.jgrapht.alg.util.Pair;

import com.gurobi.gurobi.GRBEnv;
import com.gurobi.gurobi.GRBException;
import com.gurobi.gurobi.GRBLinExpr;
import com.gurobi.gurobi.GRBModel;
import com.gurobi.gurobi.GRBVar;
import com.gurobi.gurobi.GRB;
import com.gurobi.gurobi.GRB.DoubleAttr;
import com.gurobi.gurobi.GRB.IntParam;

import data.SimpleDataGeneration;
import data.XMLData;
import heuristics.CyclePackingFormulation;
import heuristics.JumpStart;
import heuristics.TabuLocalSearch;

/**
 * Solves the EE formulation.
 * Gurobi model construction is not very efficient, could be improved on.
 */
public class EEFormulation {

	public static Pair<Integer, Double> run(File data, int k) throws Exception {
		System.out.println("["+LocalTime.now().truncatedTo(ChronoUnit.MINUTES).toString()+"] "+"Matching " + data.getName());

		//ExcelReader dr = new ExcelReader(data);
		//final boolean[][] matches = WMDReader.read(data);
		final boolean[][] matches = SimpleDataGeneration.generate(128, 0.7);
		//System.out.println("Simple data generated with n = "+n+" and a density of "+density);

		//XMLData reader = new XMLData(data);
		//final boolean[][] matches = reader.getMatches();

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
		
		System.out.println("Starting Tabu local search search");

		HashSet<ArrayList<Integer>> initialSolution = JumpStart.getJumpStart(matches, k);
		CyclePackingFormulation.upperBoundFromPacking(CyclePackingFormulation.solveWithSolution(matches), k);
		TabuLocalSearch tls = new TabuLocalSearch(matches, initialSolution, 4);
		tls.run(k, 1800);
		System.out.println("final objective: "+tls.getBestObj()+"\n"); 
		return EEFormulation.solve(matches, k, tls.getSolutionCycles(), UB, 1800);
	}

	public static Pair<Integer, Double> solve(boolean[][] matches, int k, ArrayList<ArrayList<Integer>> initialSolution, int UB, int solverTime) throws GRBException {
		System.out.println(" - ");
		System.out.println("Starting EE solve with initial solution");

		int n = matches.length;
		//create empty graph copies

		long constructStart = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime());

		//construct gurobi model
		GRBEnv env = new GRBEnv(true);
		env.set(IntParam.OutputFlag, 0);
		env.set("logFile", "mip1.log");
		env.start();


		GRBModel model = new GRBModel(env);

		/*
		model.set(GRB.DoubleParam.Heuristics, 0.0);
        model.set(GRB.DoubleParam.NoRelHeurTime, 0.0);
        model.set(GRB.IntParam.DegenMoves, 0);
        model.set(GRB.IntParam.MIPFocus, 1);

		*/
		
        /*
        model.set(GRB.IntParam.SolutionLimit, Integer.MAX_VALUE);
        model.set(GRB.DoubleParam.ImproveStartGap, Double.POSITIVE_INFINITY);
        model.set(GRB.DoubleParam.ImproveStartTime, Double.POSITIVE_INFINITY);
        model.set(GRB.DoubleParam.NoRelHeurWork, Double.POSITIVE_INFINITY);
        model.set(GRB.IntParam.RINS, 1);
		*/

		model.set(GRB.DoubleParam.TimeLimit, solverTime);

		//maps the vertices in a copy to array indices
		ArrayList<HashMap<Integer, Integer>> verticesPerCopy = new ArrayList<>();
		
		ArrayList<GRBVar[][]> x = edgesPerCopy(matches, k, verticesPerCopy, model);
		System.out.println(x.size()+" vars created in "+(TimeUnit.NANOSECONDS.toSeconds(System.nanoTime())-constructStart) +" seconds" );
		
		//set initial solution
		if(initialSolution != null) {
			for(ArrayList<Integer> cycle : initialSolution) {
				int min = Collections.min(cycle);
				
				for(int i = 0; i<cycle.size()-1;i++) {
					int row = verticesPerCopy.get(min).get(cycle.get(i));
					int col = verticesPerCopy.get(min).get(cycle.get(i+1));
					
					x.get(min)[row][col].set(DoubleAttr.Start, 1.0);
				}
				int row = verticesPerCopy.get(min).get(cycle.get(cycle.size()-1));
				int col = verticesPerCopy.get(min).get(cycle.get(0));
				x.get(min)[row][col].set(DoubleAttr.Start, 1.0);
			}
		}
		
		
		//create objective
		model.set(GRB.IntAttr.ModelSense, -1);

		//restrictions 9b & 9e
		for(int l = 0; l<n; l++) {
			GRBVar[][] copy = x.get(l);
			int dim = x.get(l).length;
			if(dim != 0) {
				for(int i = 0; i<dim; i++) {
					GRBLinExpr b = new GRBLinExpr();
					for(int j = 0; j<dim; j++) {
						if(copy[i][j] != null) {
							b.addTerm(-1, copy[i][j]);
						}
						if(copy[j][i] != null) {
							b.addTerm(1, copy[j][i]);
						}
					}
					if(b.size()!= 0) {
						model.addConstr(b, GRB.EQUAL, 0, "9b_"+l+"_"+i);
					}

					GRBLinExpr e = new GRBLinExpr();
					int l_index = verticesPerCopy.get(l).get(l);
					for(int j = 0; j<dim; j++) {
						if(copy[i][j] != null) {
							b.addTerm(-1, copy[l_index][j]);
						}
						if(copy[i][j] != null) {
							b.addTerm(1, copy[i][j]);
						}
					}
					if(e.size() != 0) {
						model.addConstr(e, GRB.LESS_EQUAL, 0, "9e_"+l+"_"+i);

					}
				}
			}
		}

		//restriction 9c
		for(int i = 0; i<n; i++) {
			GRBLinExpr c = new GRBLinExpr();
			for(int l = 0; l<n; l++) {
				if(verticesPerCopy.get(l).get(i) != null) {
					int i_index = verticesPerCopy.get(l).get(i);
					for(GRBVar var : x.get(l)[i_index]) {
						if(var != null) {
							c.addTerm(1, var);
						}
					}
				}

			}
			if(c.size() != 0) {
				model.addConstr(c, GRB.LESS_EQUAL, 1, "9c_"+i);
			}

		}
		System.out.println("model constructed in "+ (TimeUnit.NANOSECONDS.toSeconds(System.nanoTime())-constructStart));
		//uncomment to see which edges are chosen
		/*
		 * double[] vals = model.get(GRB.DoubleAttr.X, model.getVars()); GRBVar[] vars =
		 * model.getVars(); for(GRBVar var : vars) { if(var.get(GRB.DoubleAttr.X) == 1)
		 * { System.out.println(var.get(GRB.StringAttr.VarName)+" = 1"); } }
		 */

		long startTime = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime());
		model.optimize();
		Integer T = Math.toIntExact(TimeUnit.NANOSECONDS.toSeconds(System.nanoTime()) - startTime);
		
		/*
		GRBVar[] vars = model.getVars();
		for(GRBVar var : vars) {
			if(var.get(GRB.DoubleAttr.X) == 1) {
				//System.out.println(var.get(GRB.StringAttr.VarName));
			}
		}
		*/
		
		System.out.println("EE -> Pairs matched: " + model.get(GRB.DoubleAttr.ObjVal) + " out of " + n + "");
		System.out.println("linear relaxation bound was: "+model.get(GRB.DoubleAttr.ObjBound));
		model.dispose();
		env.dispose();
		Pair<Integer, Double> result = new Pair<Integer, Double>(T,0.0);
		return result;
	}
	
	public static Pair<Integer, Double> solveRelaxation(boolean[][] matches, int k, ArrayList<ArrayList<Integer>> initialSolution, int solverTime) throws GRBException {

		int n = matches.length;
		//create empty graph copies

		long constructStart = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime());

		//construct gurobi model
		GRBEnv env = new GRBEnv(true);
		env.set(IntParam.OutputFlag, 1);
		env.set("logFile", "mip1.log");
		env.start();


		GRBModel model = new GRBModel(env);

		model.set(GRB.DoubleParam.Heuristics, 0.0);
        model.set(GRB.DoubleParam.NoRelHeurTime, 0.0);
        model.set(GRB.IntParam.DegenMoves, 0);
        model.set(GRB.IntParam.MIPFocus, 1);
        

		model.set(GRB.DoubleParam.TimeLimit, solverTime);

		//maps the vertices in a copy to array indices
		ArrayList<HashMap<Integer, Integer>> verticesPerCopy = new ArrayList<>();
		
		ArrayList<GRBVar[][]> x = edgesPerCopy(matches, k, verticesPerCopy, model);
		System.out.println(x.size()+" vars created in "+(TimeUnit.NANOSECONDS.toSeconds(System.nanoTime())-constructStart) +" seconds" );
		
		//set initial solution
		if(!initialSolution.isEmpty()) {
			for(ArrayList<Integer> cycle : initialSolution) {
				int min = Collections.min(cycle);
				
				for(int i = 0; i<cycle.size()-1;i++) {
					int row = verticesPerCopy.get(min).get(cycle.get(i));
					int col = verticesPerCopy.get(min).get(cycle.get(i+1));
					
					x.get(min)[row][col].set(DoubleAttr.Start, 1.0);
				}
				int row = verticesPerCopy.get(min).get(cycle.get(cycle.size()-1));
				int col = verticesPerCopy.get(min).get(cycle.get(0));
				x.get(min)[row][col].set(DoubleAttr.Start, 1.0);
			}
		}
		
		//create objective
		model.set(GRB.IntAttr.ModelSense, -1);

		//restrictions 9b & 9e
		for(int l = 0; l<n; l++) {
			GRBVar[][] copy = x.get(l);
			int dim = x.get(l).length;
			if(dim != 0) {
				for(int i = 0; i<dim; i++) {
					GRBLinExpr b = new GRBLinExpr();
					for(int j = 0; j<dim; j++) {
						if(copy[i][j] != null) {
							b.addTerm(-1, copy[i][j]);
						}
						if(copy[j][i] != null) {
							b.addTerm(1, copy[j][i]);
						}
					}
					if(b.size()!= 0) {
						model.addConstr(b, GRB.EQUAL, 0, "9b_"+l+"_"+i);
					}

					GRBLinExpr e = new GRBLinExpr();
					int l_index = verticesPerCopy.get(l).get(l);
					for(int j = 0; j<dim; j++) {
						if(copy[i][j] != null) {
							b.addTerm(-1, copy[l_index][j]);
						}
						if(copy[i][j] != null) {
							b.addTerm(1, copy[i][j]);
						}
					}
					if(e.size() != 0) {
						model.addConstr(e, GRB.LESS_EQUAL, 0, "9e_"+l+"_"+i);

					}
				}
			}
		}

		//restriction 9c
		for(int i = 0; i<n; i++) {
			GRBLinExpr c = new GRBLinExpr();
			for(int l = 0; l<n; l++) {
				if(verticesPerCopy.get(l).get(i) != null) {
					int i_index = verticesPerCopy.get(l).get(i);
					for(GRBVar var : x.get(l)[i_index]) {
						if(var != null) {
							c.addTerm(1, var);
						}
					}
				}

			}
			if(c.size() != 0) {
				model.addConstr(c, GRB.LESS_EQUAL, 1, "9c_"+i);
			}
		}
		System.out.println("model constructed in "+ (TimeUnit.NANOSECONDS.toSeconds(System.nanoTime())-constructStart));

		model.update();
		long startTime = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime());
		GRBModel relaxed = model.relax();
		relaxed.optimize();
		System.out.println("Finished in "+ Math.toIntExact(TimeUnit.NANOSECONDS.toSeconds(System.nanoTime()) - startTime)+" s");

		switch(relaxed.get(GRB.IntAttr.Status)){
		case GRB.Status.OPTIMAL: 	System.out.println("Relaxation solved");
		case GRB.Status.TIME_LIMIT: System.out.println("Time limit reached");
		case GRB.Status.SUBOPTIMAL:	System.out.println("Relaxation solved suboptimally");
		}
		System.out.println("linear relaxation bound is: "+relaxed.get(GRB.DoubleAttr.ObjVal));
		relaxed.dispose();
		env.dispose();
		Pair<Integer, Double> result = new Pair<Integer, Double>(0,0.0);
		return result;
	}
	
	/**
	 * creates the edge variables and puts them in an arraylist of matrices resembling the matches matrix
	 * @param matches 
	 * @param k
	 * @param verticesPerCopy 	adds a map to this list for every copy that maps indices in the smaller copy matrix to vertices in the original graph
	 * @param indicesPerCopy	adds a map to this list for every copy that maps vertices in the original graph to indices in the smaller copy matrix
	 * @param model
	 * @return
	 * @throws GRBException
	 */
	public static ArrayList<GRBVar[][]> edgesPerCopy(boolean[][] matches, int k,
			ArrayList<HashMap<Integer, Integer>> verticesPerCopy, GRBModel model) throws GRBException {
		
		int n = matches.length;
		ArrayList<GRBVar[][]> copies =  new ArrayList<>(n);
		for(int l = 0; l<n; l++) {

			HashMap<Integer, Integer> vertices = new HashMap<>(n);
			HashSet<Pair<Integer, Integer>> copySet = new HashSet<>();
			ArrayList<Integer> route = new ArrayList<>(k);
			route.add(l);
			recursiveFind(matches, k, route, copySet);
			
			for(Pair<Integer, Integer> pair : copySet) {

				if(!vertices.containsKey(pair.getFirst())){
					vertices.put(pair.getFirst(), vertices.keySet().size());
				}
				if(!vertices.containsKey(pair.getSecond())){
					vertices.put(pair.getSecond(), vertices.keySet().size());
				}
			}
			verticesPerCopy.add(vertices);
			GRBVar[][] copy = new GRBVar[vertices.size()][vertices.size()];
			for(Pair<Integer, Integer> pair : copySet) {
				copy[vertices.get(pair.getFirst())][vertices.get(pair.getSecond())] = model.addVar(0, 1, 1, GRB.BINARY, "x^"+l+"_("+pair.getFirst()+","+pair.getSecond()+")");

			}
			copies.add(copy);
		}
		return copies;
	}

	private static void recursiveFind(boolean[][] matches, int k, ArrayList<Integer> route, HashSet<Pair<Integer, Integer>> copy) {
		int end = route.get(route.size()-1);
		int l = route.get(0);

		if(matches[end][l]) {
			for(int i = 0; i<route.size()-1; i++) {
				copy.add(new Pair<>(route.get(i), route.get(i+1)));
			}
			copy.add(new Pair<>(end, l));
		}

		if(route.size() < k) {
			for(int i = l+1; i<matches.length; i++) {

				if(matches[end][i] && !route.contains(i)) {
					ArrayList<Integer> newRoute = new ArrayList<>(k);
					newRoute.addAll(route);
					newRoute.add(i);
					recursiveFind(matches, k, newRoute, copy);
				}
			}
		}
	}
}