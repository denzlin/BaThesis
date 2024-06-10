package reproduction;

import java.io.File;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

import org.jgrapht.alg.util.Pair;

import org.apache.commons.lang3.tuple.*;

import com.gurobi.gurobi.GRBEnv;
import com.gurobi.gurobi.GRBException;
import com.gurobi.gurobi.GRBLinExpr;
import com.gurobi.gurobi.GRBModel;
import com.gurobi.gurobi.GRBVar;
import com.gurobi.gurobi.GRB;
import com.gurobi.gurobi.GRB.IntParam;

import data.XMLData;
import heuristics.CyclePackingFormulation;

/**
 * Solves the EE formulation.
 * Gurobi model construction is not very efficient, could be improved on.
 */
public class EEFormulation {

	public static Pair<Integer, Double> run(File data, int k) throws GRBException {
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
		CyclePackingFormulation.solve(matches);

		double density = matchCount/(double) Math.pow(matches.length,2)*100/100;
		System.out.println("Data has " +matches.length+ " matchable pairs with an average density of " + density);


		return EEFormulation.solve(matches, k);
	}

	public static Pair<Integer, Double> solve(boolean[][] matches, int k) throws GRBException {
		System.out.println("\nStarting EE solve\n");

		int n = matches.length;
		//create empty graph copies

		long constructStart = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime());

		//construct gurobi model
		GRBEnv env = new GRBEnv(true);
		env.set(IntParam.OutputFlag, 1);
		env.set("logFile", "mip1.log");
		env.start();


		GRBModel model = new GRBModel(env);


		
		 model.set(GRB.DoubleParam.Heuristics, 1);
         model.set(GRB.IntParam.MIPFocus, 0);
         model.set(GRB.IntParam.SolutionLimit, Integer.MAX_VALUE);
         model.set(GRB.DoubleParam.ImproveStartGap, Double.POSITIVE_INFINITY);
         model.set(GRB.DoubleParam.ImproveStartTime, Double.POSITIVE_INFINITY);
         model.set(GRB.DoubleParam.NoRelHeurTime, Double.POSITIVE_INFINITY);
         model.set(GRB.DoubleParam.NoRelHeurWork, Double.POSITIVE_INFINITY);
         model.set(GRB.IntParam.RINS, 1);
		 


		model.set(GRB.DoubleParam.TimeLimit, 1800);

		//maps the vertices in a copy to array indices
		ArrayList<HashMap<Integer, Integer>> verticesPerCopy = new ArrayList<>();
		ArrayList<HashMap<Integer, Integer>> indicesPerCopy = new ArrayList<>();		
		ArrayList<GRBVar[][]> x = edgesPerCopy(matches, k, verticesPerCopy, indicesPerCopy, model);

		System.out.println(x.size()+" vars created in "+(TimeUnit.NANOSECONDS.toSeconds(System.nanoTime())-constructStart) +" seconds" );
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

		/*
		//restriction 9d
		for(int l = 0; l<n; l++) {
			GRBLinExpr d = new GRBLinExpr();
			for(GRBVar[] row : x.get(l)) {
				for(GRBVar var : row) {
					if(var != null) {
						d.addTerm(1, var);
					}
				}
			}
			if(d.size() != 0) {
			model.addConstr(d, GRB.LESS_EQUAL, k, "9d_"+l);
			}
		}
		 */

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

		GRBModel relaxed = model.relax();
		relaxed.optimize();
		double gap = relaxed.get(GRB.DoubleAttr.ObjVal) - model.get(GRB.DoubleAttr.ObjVal);

		//uncomment to see which cycles are chosen

		GRBVar[] vars = model.getVars();
		for(GRBVar var : vars) {
			if(var.get(GRB.DoubleAttr.X) == 1) {
				//System.out.println(var.get(GRB.StringAttr.VarName));
			}
		}


		System.out.println("EE -> Pairs matched: " + model.get(GRB.DoubleAttr.ObjVal) + " out of " + n + ". \n");
		env.dispose();
		Pair<Integer, Double> result = new Pair<Integer, Double>(T, gap);
		return result;
	}

	public static ArrayList<GRBVar[][]> edgesPerCopy(boolean[][] matches, int k,
			ArrayList<HashMap<Integer, Integer>> verticesPerCopy, ArrayList<HashMap<Integer, Integer>> indicesPerCopy, GRBModel model) throws GRBException {
		int n = matches.length;
		ArrayList<GRBVar[][]> copies =  new ArrayList<>(n);
		for(int l = 0; l<n; l++) {

			HashMap<Integer, Integer> vertices = new HashMap<>();
			HashMap<Integer, Integer> indices = new HashMap<>();
			HashSet<Pair<Integer, Integer>> copySet = new HashSet<>();
			ArrayList<Integer> route = new ArrayList<>(k);
			route.add(l);
			recursiveFind(matches, k, route, copySet);

			for(Pair<Integer, Integer> pair : copySet) {
				if(!vertices.containsKey(pair.getFirst())){
					indices.put(vertices.keySet().size(), pair.getFirst());
					vertices.put(pair.getFirst(), vertices.keySet().size());
				}
				if(!vertices.containsKey(pair.getSecond())){
					indices.put(vertices.keySet().size(), pair.getFirst());
					vertices.put(pair.getSecond(), vertices.keySet().size());
				}
			}
			verticesPerCopy.add(vertices);
			GRBVar[][] copy = new GRBVar[vertices.size()][vertices.size()];
			for(Pair<Integer, Integer> pair : copySet) {
				copy[vertices.get(pair.getFirst())][vertices.get(pair.getSecond())] = model.addVar(0, 1, 1, GRB.BINARY, "x^"+l+"_("+pair.getFirst()+","+pair.getSecond()+")");
				//System.out.println("added var "+ pair.getFirst() +"-"+pair.getSecond()+" to copy "+l);
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
			for(int i = l+1; i< matches.length; i++) {

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