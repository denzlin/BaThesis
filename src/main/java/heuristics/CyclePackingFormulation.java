package heuristics;

import java.util.concurrent.TimeUnit;

import org.jgrapht.alg.util.Pair;

import com.gurobi.gurobi.*;
import com.gurobi.gurobi.GRB.*;

public class CyclePackingFormulation {

	public static int solve(boolean[][] matches) throws GRBException {
		System.out.println("\nStarting no-k solve\n");

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
		System.out.println("Unlimited-k formulation solved in: "+Math.toIntExact(TimeUnit.NANOSECONDS.toSeconds(System.nanoTime()) - startTime)+" seconds");
		
		System.out.println("No-k -> Pairs matched: " + model.get(GRB.DoubleAttr.ObjVal) + " out of " + n + ". \n");
		env.dispose();
        return (int) model.get(GRB.DoubleAttr.ObjVal);
	}
}
