package reproduction;

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

/**
 * Solves the EE formulation.
 * Gurobi model construction is not very efficient, could be improved on.
 */
public class EEFormulation {

	public static Pair<Integer, Double> solve(boolean[][] matches, int k, ArrayList<ArrayList<Integer>> cycles) throws GRBException {
		System.out.println("\nStarting EE solve\n");

		int n = matches.length;
		//create empty graph copies
		
		long reductionStart = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime());

		ArrayList<HashSet<Pair<Integer, Integer>>> copies = edgesPerCopy(matches, k);
		/*
		ArrayList<HashSet<Pair<Integer, Integer>>> copies = new ArrayList<>();
		for(int l = 0; l<n; l++) {

			HashSet<Pair<Integer, Integer>> copy = new HashSet<>();
			copies.add(copy);
		}
		//edge reduction: only add existing cycles to copies
		for(ArrayList<Integer> c : cycles) {
			int min = c.stream().mapToInt(Integer::valueOf).min().getAsInt();

			for(int v = 0; v<c.size()-1; v++) {
				copies.get(min).add(new Pair<>(c.get(v), c.get(v+1)));
			}

			copies.get(min).add(new Pair<>(c.get(c.size()-1), c.get(0)));
		}
		*/
		System.out.println("reduction performed in "+ (TimeUnit.NANOSECONDS.toSeconds(System.nanoTime())-reductionStart) + " seconds");

		//construct gurobi model
		GRBEnv env = new GRBEnv(true);
		env.set(IntParam.OutputFlag, 0);
		env.set("logFile", "mip1.log");
		env.start();

		GRBModel model = new GRBModel(env);

		HashMap<Triple<Integer, Integer, Integer>, GRBVar> x = new HashMap<>();

		long constructStart = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime());
		//reduce size
		for(int l = 0; l<n; l++) {
			for(Pair<Integer, Integer> edge : copies.get(l)) {
				Integer first = edge.getFirst();
				Integer second = edge.getSecond();
				Triple<Integer, Integer, Integer> key = new ImmutableTriple<>(l, first, second);
				if(x.get(key) == null){
					x.put(key, model.addVar(0, 1, 0, GRB.BINARY, "x^"+l+"_("+first+","+second+")"));
				}
			}
		}
		System.out.println(x.size()+" vars created in "+(TimeUnit.NANOSECONDS.toSeconds(System.nanoTime())-constructStart) +" seconds" );
		//create objective
		GRBLinExpr obj = new GRBLinExpr();

		for(GRBVar var : x.values()) {
			obj.addTerm(1, var);
		}
		model.setObjective(obj, GRB.MAXIMIZE);



		//restriction 9b
		for(int l = 0; l<n; l++) {
			if(!copies.get(l).isEmpty()) {
				for(int i = l; i<n; i++) {
					GRBLinExpr b = new GRBLinExpr();
					for(int j = l; j<n; j++) {

						//lhs
						ImmutableTriple<Integer, Integer, Integer> key = new ImmutableTriple<>(l,j,i);
						if(x.get(key)!=null) {
							b.addTerm(1, x.get(key));
						}

						//rhs
						key = new ImmutableTriple<>(l,i,j);
						if(x.get(key)!=null) {
							b.addTerm(-1, x.get(key));
						}

					}
					model.addConstr(b, GRB.LESS_EQUAL, 0, "9b_"+l+"_"+i);
				}
			}
		}

		//restriction 9c
		for(int i = 0; i<n; i++) {
			GRBLinExpr constr = new GRBLinExpr();
			for(int l = 0; l<n; l++) {
				for(int j = l; j<n; j++) {
					ImmutableTriple<Integer, Integer, Integer> key = new ImmutableTriple<>(l,i,j);
					if(x.get(key)!=null) {
						constr.addTerm(1, x.get(key));
					}
				}
			}
			if(constr.size() != 0) {
				model.addConstr(constr, GRB.LESS_EQUAL, 1, "9c_"+i);
			}
		}

		//restriction 9d
		ArrayList<GRBLinExpr> constraints = new ArrayList<>();
		for(int l = 0; l<n; l++) {
			constraints.add(new GRBLinExpr());
		}
		for(Triple<Integer, Integer, Integer> key : x.keySet()) {
			constraints.get(key.getLeft()).addTerm(1, x.get(key));
		}
		for(GRBLinExpr expr : constraints) {
			if(expr.size() != 0) {
				model.addConstr(expr, GRB.LESS_EQUAL, k, "9d_"+constraints.indexOf(expr));
			}
		}


		//restriction 9e
		for(int l = 0; l<n; l++) {			
			for(int i = l; i<n; i++) {
				GRBLinExpr e = new GRBLinExpr();
				for(int j = l; j<n; j++) {
					ImmutableTriple<Integer, Integer, Integer> key = new ImmutableTriple<>(l,i,j);
					if(x.get(key) != null) {
						e.addTerm(1, x.get(key));
					}
					key = new ImmutableTriple<>(l,l,j);
					if(x.get(key) != null) {
						e.addTerm(-1, x.get(key));
					}

				}
				if(e.size() != 0) {
					model.addConstr(e, GRB.LESS_EQUAL, 0, "9e_"+l+"_"+i);
				}

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

		GRBModel relaxed = model.relax();
		relaxed.optimize();
		double gap = relaxed.get(GRB.DoubleAttr.ObjVal) - model.get(GRB.DoubleAttr.ObjVal);

		//uncomment to see which cycles are chosen
		
		GRBVar[] vars = model.getVars();
		for(GRBVar var : vars) {
			if(var.get(GRB.DoubleAttr.X) == 1) {
				System.out.println(var.get(GRB.StringAttr.VarName));
			}
		}
		

		System.out.println("EE -> Pairs matched: " + model.get(GRB.DoubleAttr.ObjVal) + " out of " + n + ". \n");
		env.dispose();
		Pair<Integer, Double> result = new Pair<Integer, Double>(T, gap);
		return result;
	}

	public static ArrayList<HashSet<Pair<Integer, Integer>>> edgesPerCopy(boolean[][] matches, int k) {
		int n = matches.length;
		ArrayList<HashSet<Pair<Integer, Integer>>> copies =  new ArrayList<>(n);
		for(int l = 0; l<n; l++) {

			HashSet<Pair<Integer, Integer>> copy = new HashSet<>();
			ArrayList<Integer> route = new ArrayList<>(k);
			route.add(l);
			recursiveFind(matches, k, route, copy);

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
