package heuristics;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.jgrapht.alg.interfaces.MatchingAlgorithm.Matching;
import org.jgrapht.alg.matching.SparseEdmondsMaximumCardinalityMatching;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

import com.gurobi.gurobi.GRB;
import com.gurobi.gurobi.GRBEnv;
import com.gurobi.gurobi.GRBException;
import com.gurobi.gurobi.GRBLinExpr;
import com.gurobi.gurobi.GRBModel;
import com.gurobi.gurobi.GRBVar;

import util.CycleUtils;

import com.gurobi.gurobi.GRB.IntParam;

public class PairingFormulation {

	/**
	 * Return a set of 2-length cyles. This set is an optimal matching with the lowest average
	 * score per cycle as described in the thesis.
	 * 
	 * @param matches
	 * @return
	 * @throws GRBException
	 */
	public static HashSet<ArrayList<Integer>> solve(boolean[][] matches) throws GRBException {
		int n = matches.length;

		ArrayList<Integer[]> cycles = new ArrayList<>();
		//add edges
		for(int i = 0; i<n; i++) {
			for(int j = i; j<n; j++) {
				if(matches[i][j] && matches[j][i]) {
					cycles.add(new Integer[] {i, j});
				}
			}
		}

		Random r = new Random();
		GRBEnv env = new GRBEnv(true);
		env.set(IntParam.OutputFlag, 0);
		env.start();

		GRBModel model = new GRBModel(env);
		model.set(GRB.DoubleParam.TimeLimit, 1800.0);
		model.set(GRB.IntParam.Seed, r.nextInt(100));

		double[][] pairValues = CycleUtils.calculatePairs(matches);
		GRBVar[] z = new GRBVar[cycles.size()];
		GRBVar[] y = new GRBVar[cycles.size()];

		Matching<Integer, DefaultEdge> matching = findMaxPairing(cycles, n);

		GRBLinExpr sizeConstr = new GRBLinExpr();
		for(int c = 0; c<cycles.size(); c++) {
			Integer[] cycle = cycles.get(c);
			int i = cycle[0];
			int j = cycle[1];
			double cycleValue = pairValues[i][0] + pairValues[i][1] + pairValues[j][0] + pairValues[j][1];
			z[c] = model.addVar(0, 1, cycleValue, GRB.BINARY, "z["+i+"]["+j+"]");
			y[c] = model.addVar(0, 1, 0, GRB.BINARY, "y["+i+"]["+j+"]");
			GRBLinExpr constr = new GRBLinExpr();

			constr.addTerm(1, z[c]);
			constr.addTerm(-1, y[c]);
			model.addConstr(constr, GRB.EQUAL, 0, "equal["+i+"]["+j+"]");

			sizeConstr.addTerm(1, y[c]);
		}
		model.addConstr(sizeConstr, GRB.EQUAL, matching.getEdges().size(), "sizeConstraint");

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

		//create constraints
		for(int v = 0; v<n; v++) {
			ArrayList<Integer> vertex = cyclesPerVertex.get(v);
			if(!vertex.isEmpty()) {
				GRBLinExpr expr = new GRBLinExpr();
				for(Integer c : vertex) {
					expr.addTerm(1, y[c]);
				}
				model.addConstr(expr, GRB.LESS_EQUAL, 1, "vertex_"+cyclesPerVertex.indexOf(vertex));
			}
		}

		long startTime = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime());
		model.optimize();
		Integer T = Math.toIntExact(TimeUnit.NANOSECONDS.toSeconds(System.nanoTime()) - startTime);
		System.out.println("2-cycle matching paired "+matching.getEdges().size()*2+" vertices in "+T+" seconds");
		
		HashSet<ArrayList<Integer>> result = new HashSet<>(matching.getEdges().size());
		for(int c = 0; c<cycles.size(); c++) {
			if(y[c].get(GRB.DoubleAttr.X) == 1.0) {
				Integer[] cycle = cycles.get(c);
				ArrayList<Integer> list = new ArrayList<>(2);
				list.add(cycle[0]);
				list.add(cycle[1]);
				if(!matches[cycle[0]][cycle[1]]) {
					throw new IllegalArgumentException();
				}
				result.add(list);
			}
		}


		return result;
	}
	
	public static Matching<Integer, DefaultEdge> findMaxPairing(ArrayList<Integer[]> cycles, int n) throws GRBException {

		//first find maximum amount of matches
		SimpleGraph<Integer, DefaultEdge> g = new SimpleGraph<>(DefaultEdge.class);


		//add vertices
		ArrayList<Integer> vertices = new ArrayList<>(n);
		for(int i = 0; i<n; i++) {
			vertices.add(i);
			g.addVertex(vertices.get(i));

		}

		for(Integer[] cycle : cycles) {
			g.addEdge(cycle[0], cycle[1]);
		}

		SparseEdmondsMaximumCardinalityMatching<Integer, DefaultEdge> matcher = new SparseEdmondsMaximumCardinalityMatching<>(g);
		return matcher.getMatching();
	}
}
