package reproduction;

import java.io.File;
import java.io.IOException;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.jgrapht.alg.util.Pair;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;

import com.gurobi.gurobi.GRB;
import com.gurobi.gurobi.GRBEnv;
import com.gurobi.gurobi.GRBException;
import com.gurobi.gurobi.GRBLinExpr;
import com.gurobi.gurobi.GRBModel;
import com.gurobi.gurobi.GRBVar;
import com.gurobi.gurobi.GRB.IntParam;

import data.XMLData;
import heuristics.CyclePackingFormulation;
import util.CycleUtils;


/**
 * Solves the cycle formulation of the problem
 */
public class CycleFormulation {

	public static Pair<Integer, Double> run(File data, int k, ArrayList<Double> cycleValuesAll, ArrayList<Double> cycleValuesSol) throws GRBException, IOException {
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

		ArrayList<ArrayList<Integer>> cycles = CycleUtils.getCycles(matches, k);

		double density = matchCount/(double) Math.pow(matches.length,2)*100/100;
		System.out.println("Data has " +matches.length+ " matchable pairs with an average density of " + density);


		Pair<Integer, Double> result = solve(cycles, matches, n, cycleValuesAll, cycleValuesSol);


		return result;
	}
	/**
	 * initiates solve
	 * 
	 * @param data The name of the data file to be solved)
	 * @throws IOException 
	 * @throws GRBException 
	 */
	public static Pair<Integer, Double> solve(ArrayList<ArrayList<Integer>> cycles, boolean[][] matches, int n, ArrayList<Double> cycleValuesAll, ArrayList<Double> cycleValuesSol) throws IOException, GRBException {


		Random r = new Random();

		GRBEnv env = new GRBEnv(true);
		env.set(IntParam.OutputFlag, 1);
		env.start();

		GRBModel model = new GRBModel(env);
		model.set(GRB.DoubleParam.TimeLimit, 1800.0);
		model.set(GRB.DoubleParam.PoolGap, 0.0);
		model.set(GRB.IntParam.PoolSearchMode, 0);
		int solNum = 1;
		model.set(GRB.IntParam.PoolSolutions, solNum);
		model.set(GRB.IntParam.Seed, r.nextInt(100));

		
		model.set(GRB.DoubleParam.Heuristics, 0.2);
		
        model.set(GRB.IntParam.MIPFocus, 1);
        model.set(GRB.IntParam.SolutionLimit, Integer.MAX_VALUE);
        model.set(GRB.DoubleParam.ImproveStartGap, Double.POSITIVE_INFINITY);
        model.set(GRB.DoubleParam.ImproveStartTime, Double.POSITIVE_INFINITY);
        model.set(GRB.DoubleParam.NoRelHeurTime, Double.POSITIVE_INFINITY);
        model.set(GRB.DoubleParam.NoRelHeurWork, Double.POSITIVE_INFINITY);
        model.set(GRB.IntParam.RINS, 1);
		 

		// create list of cycle variables
		GRBVar[] z = new GRBVar[cycles.size()];
		
		//if(cycles.size()>24000000) {
		//	return null;
		//}
		
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

		long startTime = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime());
		model.optimize();
		Integer T = Math.toIntExact(TimeUnit.NANOSECONDS.toSeconds(System.nanoTime()) - startTime);
		//System.out.println("status code: " +model.get(GRB.IntAttr.Status));
		GRBModel relaxed = model.relax();
		relaxed.optimize();
		double gap = relaxed.get(GRB.DoubleAttr.ObjVal) - model.get(GRB.DoubleAttr.ObjVal);

		//for multiple solutions

		int equiCounter = 1;
		for(int i = 1; i<solNum; i++) {
			model.set(GRB.IntParam.SolutionNumber, i);
			if(model.get(GRB.DoubleAttr.PoolObjVal) == model.get(GRB.DoubleAttr.ObjVal)) {
				equiCounter ++;
			}
		}


		System.out.println("Number of equivalent solutions: "+ equiCounter);
		
		ArrayList<Double> cycleScores = CycleUtils.calculateCycles(matches, cycles);
		cycleValuesAll.addAll(cycleScores);
		ArrayList<Integer> solutionCycles = new ArrayList<>();
		for(int s = 0; s<solNum; s++) {
			model.set(GRB.IntParam.SolutionNumber, s);
			for(int u = 0; u<z.length; u++) {
				GRBVar var = z[u];
				if(var.get(GRB.DoubleAttr.Xn) == 1) {
					solutionCycles.add(u);
					cycleValuesSol.add(cycleScores.get(u));
				}
			}
		}


		System.out.println("Solved in "+T+" seconds");
		System.out.println("Cycle -> Pairs matched: " + model.get(GRB.DoubleAttr.ObjVal) + " out of " + n + ". " +emptyCounter+ " pairs were not in any cycle");
		env.dispose();
		Pair<Integer, Double> result = new Pair<Integer, Double>(T, gap);
		return result;
	}
}
