package reproduction;

import java.io.IOException;
import java.util.ArrayList;
import java.util.stream.Collectors;


import com.gurobi.gurobi.GRB;
import com.gurobi.gurobi.GRBEnv;
import com.gurobi.gurobi.GRBException;
import com.gurobi.gurobi.GRBLinExpr;
import com.gurobi.gurobi.GRBModel;
import com.gurobi.gurobi.GRBVar;
import com.gurobi.gurobi.GRB.IntParam;

/**
 * Solves the cycle formulation of the problem
 */
public class CycleFormulation {

	/**
	 * initiates solve
	 * 
	 * @param data The name of the data file to be solved)
	 * @throws IOException 
	 * @throws GRBException 
	 */
	public static void solve(ArrayList<ArrayList<Integer>> cycles, int k, int n) throws IOException, GRBException {

		GRBEnv env = new GRBEnv(true);
		env.set(IntParam.OutputFlag, 0);
		//env.set("logFile", "mip1.log");
		env.start();

		GRBModel model = new GRBModel(env);

		// create list of cycle variables
		GRBVar[] z = new GRBVar[cycles.size()];

		for(int c = 0; c<cycles.size(); c++) {
			String listString = cycles.get(c).stream().map(Object::toString)
                    .collect(Collectors.joining(","));
			z[c] = model.addVar(0, 1, 0, GRB.BINARY, "z("+listString+")");
		}

		//create objective
		GRBLinExpr obj = new GRBLinExpr();
		for(int c = 0; c<cycles.size(); c++) {
			GRBVar var = z[c];
			obj.addTerm(cycles.get(c).size(), var);
		}
		model.setObjective(obj, GRB.MAXIMIZE);

		//assign vertices to cycles
		ArrayList<ArrayList<Integer>> cyclesPerVertex = new ArrayList<>(n);
		for(int i = 0; i<n; i++) {
			cyclesPerVertex.add(new ArrayList<Integer>());
		}
		for(int i = 0; i<cycles.size(); i++) {
			for(Integer v : cycles.get(i)) {
				cyclesPerVertex.get(v).add(i);
			}
		}

		int emptyCounter = 0;
		//create constraints
		for(ArrayList<Integer> vertex : cyclesPerVertex) {
			if(!vertex.isEmpty()) {
				GRBLinExpr expr = new GRBLinExpr();
				for(Integer c : vertex) {
					expr.addTerm(1, z[c]);
				}
				model.addConstr(expr, GRB.LESS_EQUAL, 1, "vertex_"+cyclesPerVertex.indexOf(vertex));
			}
			else {
				emptyCounter++;
			}
		}

		model.optimize();
		
		//uncomment to see which cycles are chosen
		/*
		GRBVar[] vars = model.getVars();
		for(GRBVar var : vars) {
			if(var.get(GRB.DoubleAttr.X) == 1) {
				System.out.println(var.get(GRB.StringAttr.VarName));
			}
		}
		*/
		System.out.println("Cycle -> Pairs matched: " + model.get(GRB.DoubleAttr.ObjVal) + " out of " + n + ". " +emptyCounter+ " pairs were unmatchable");
	}
}
