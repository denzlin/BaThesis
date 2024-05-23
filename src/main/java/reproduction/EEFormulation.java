package reproduction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

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

	public static void solve(ArrayList<ArrayList<Integer>> cycles, int k, int n) throws GRBException {
		System.out.println("\nStarting EE solve\n");
		//create empty graph copies
		ArrayList<HashSet<Pair<Integer, Integer>>> copies = new ArrayList<>(n);
		for(int i = 0; i<n; i++) {
			copies.add(new HashSet<Pair<Integer, Integer>>());
		}

		//edge reduction: only add existing cycles to copies
		for(ArrayList<Integer> c : cycles) {
			int min = c.stream().mapToInt(Integer::valueOf).min().getAsInt();

			for(int v = 0; v<c.size()-1; v++) {
				copies.get(min).add(new Pair<>(c.get(v), c.get(v+1)));
			}

			copies.get(min).add(new Pair<>(c.get(c.size()-1), c.get(0)));
		}
		//construct gurobi model
		GRBEnv env = new GRBEnv(true);
		env.set(IntParam.OutputFlag, 0);
		env.set("logFile", "mip1.log");
		env.start();

		GRBModel model = new GRBModel(env);

		HashMap<Triple<Integer, Integer, Integer>, GRBVar> x = new HashMap<>();

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
		System.out.println("vars created");
		//create objective
		GRBLinExpr obj = new GRBLinExpr();

		for(GRBVar var : x.values()) {
			obj.addTerm(1, var);
		}
		model.setObjective(obj, GRB.MAXIMIZE);



		//restriction 9b
		for(int l = 0; l<n; l++) {
			System.out.println("on 9b copy "+l);
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
		System.out.println("constr 9b added");

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
		System.out.println("constr 9c added");

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
		System.out.println("constr 9d added");


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
		System.out.println("constr 9e added");

		model.optimize();

		//uncomment to see which edges are chosen
		/*
		 * double[] vals = model.get(GRB.DoubleAttr.X, model.getVars()); GRBVar[] vars =
		 * model.getVars(); for(GRBVar var : vars) { if(var.get(GRB.DoubleAttr.X) == 1)
		 * { System.out.println(var.get(GRB.StringAttr.VarName)+" = 1"); } }
		 */

		System.out.println("EE    -> Pairs matched: " + model.get(GRB.DoubleAttr.ObjVal));
	}
}
