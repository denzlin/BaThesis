package reproduction;
import com.gurobi.gurobi.GRB;
import com.gurobi.gurobi.GRB.IntParam;
import com.gurobi.gurobi.GRBEnv;
import com.gurobi.gurobi.GRBException;
import com.gurobi.gurobi.GRBModel;
import com.gurobi.gurobi.GRBVar;

import java.util.ArrayList;


public class CycleGurobi {
	
	
	public static void solve(ArrayList<ArrayList<Integer>> cycles) throws GRBException {
		GRBEnv env = new GRBEnv(true);
	    env.set(IntParam.OutputFlag, 0);
	    //env.set("logFile", "mip1.log");
	    env.start();
	    
	    GRBModel model = new GRBModel(env);
	    
	    GRBVar[] z = new GRBVar[cycles.size()];
	    
	    for(int c = 0; c<cycles.size(); c++) {
	    	
	    	z[c] = model.addVar(0, 1, cycles.get(c).size(), GRB.BINARY, "z("+c+")");
	    }
	}
}
