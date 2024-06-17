package heuristics;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Random;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.jgrapht.alg.cycle.HawickJamesSimpleCycles;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;

import com.gurobi.gurobi.GRB;
import com.gurobi.gurobi.GRBEnv;
import com.gurobi.gurobi.GRBException;
import com.gurobi.gurobi.GRBLinExpr;
import com.gurobi.gurobi.GRBModel;
import com.gurobi.gurobi.GRBVar;
import com.gurobi.gurobi.GRB.IntParam;

import util.CycleUtils;
import util.CycleUtils.CycleConsumer;

/*  This class does a tabu search starting from the initial solution provided. It takes a full
	solution, removes a certain amount of cycles, lists all possible new cycles, and restarts
	from the best solution found. Chosen solutions are saved in a tabu list to prevent cycling.

 */
public class TabuLocalSearch {

	private boolean[][] matches;
	private HashSet<ArrayList<Integer>> initialSolution;
	private int k;
	private HashSet<Integer> vertices;
	private TreeSet<ImmutablePair<ArrayList<Integer>, Double>> bestSolution;
	private int bestObj;
	private double[][] pairValues;

	public TabuLocalSearch(boolean[][] matches, HashSet<ArrayList<Integer>> initialSolution, int k) {
		this.matches = matches;
		this.initialSolution = initialSolution;
		this.k = k;

		int initialObj = 0;
		for(ArrayList<Integer> c : initialSolution) {
			initialObj += c.size();
		}
		this.bestObj = initialObj;
		
		vertices = new HashSet<>(matches.length);

		for(int i = 0; i<matches.length; i++) {
			boolean give = false;
			boolean receive = false;
			for(int j = 0; j<matches.length; j++) {
				if(matches[i][j]) {
					give = true;
				}
				if(matches[j][i]) {
					receive = true;
				}
			}
			if(give && receive) {
				vertices.add(i);
			}
		}
		pairValues = CycleUtils.calculatePairs(matches);
	}

	public void run(int runTime, int UB) throws Exception {
		System.out.println("Running Local Search matheuristic for at most "+runTime+" seconds");
		int TABU_SIZE = 1;
		int SAMPLE = 0;
		int solNumILP = 1;
		Random r = new Random();
		
		HashSet<TreeSet<ImmutablePair<ArrayList<Integer>, Double>>> tabuList = new HashSet<>(50);
		//this is a monstrous variable and i apologize
		LinkedList<ImmutablePair<TreeSet<ImmutablePair<ArrayList<Integer>, Double>>, Integer>> tabuAges = new LinkedList<>();

		GRBEnv env = new GRBEnv(true);
		env.set(IntParam.OutputFlag, 0);
		env.start();
		
		CycleComparator cc = new CycleComparator();
		TreeSet<ImmutablePair<ArrayList<Integer>, Double>> previousSolution = new TreeSet<>(cc);

		for(ArrayList<Integer> c: initialSolution) {
			previousSolution.add(new ImmutablePair<ArrayList<Integer>, Double>(c, CycleUtils.calculateCycle(matches, c, pairValues)));
		}

		//to allow modifications of the tree map copy it
		bestSolution = new TreeSet<ImmutablePair<ArrayList<Integer>, Double>>(cc);
		tabuList.add(previousSolution);

		int improveTime = 0;
		int iter = 0;
		long startSecond = Instant.now().getEpochSecond();
		ArrayList<Integer> improveMoments = new ArrayList<>(matches.length/2);
		ArrayList<Integer> improveValues = new ArrayList<>(matches.length/2);
		improveMoments.add(0);
		improveValues.add(bestObj);
		
		Comparator<TreeSet<ImmutablePair<ArrayList<Integer>, Double>>> solComp = new SolutionComparator();

		//loop
		while(Instant.now().getEpochSecond() < startSecond + runTime) {
			if(iter==1) {
				SAMPLE = 5;
			}
			//System.out.println("current solution: ");
			//for(ImmutablePair<ArrayList<Integer>, Double> pair : previousSolution) {
			//System.out.print("("+Arrays.toString(pair.getLeft().toArray())+", "+pair.getRight()+"] ");
			//}
			if(tabuList.size()>TABU_SIZE) {
				tabuList.remove(tabuAges.pollFirst().getLeft());
			}
			//System.out.println("(" +(Instant.now().getEpochSecond()-startSecond)+ "s) new iteration");
			TreeSet<TreeSet<ImmutablePair<ArrayList<Integer>, Double>>> neighbours = new TreeSet<>(solComp);

			ArrayList<ImmutablePair<ArrayList<Integer>, Double>> cycleList = new ArrayList<>(previousSolution.size());
			HashSet<Integer> currentVertices = new HashSet<>(matches.length);
			HashSet<Integer> freeVertices = new HashSet<>(k*SAMPLE);

			for(ImmutablePair<ArrayList<Integer>,Double> c : previousSolution) {
				for(Integer i : c.getLeft()) {
					currentVertices.add(i);
				}
				cycleList.add(c);
			}
			for(int v : vertices) {
				if(!currentVertices.contains(v)) {
					freeVertices.add(v);
				}
			}

			for(ImmutablePair<ArrayList<Integer>, Double> pair : previousSolution) {
				ArrayList<ImmutablePair<ArrayList<Integer>, Double>> pairsToRemove = new ArrayList<>(SAMPLE);
				pairsToRemove.add(pair);
				while(pairsToRemove.size()<SAMPLE) {
					ImmutablePair<ArrayList<Integer>, Double> newPair = cycleList.get(r.nextInt(cycleList.size()));
					if(!pairsToRemove.contains(newPair)){
						pairsToRemove.add(newPair);
					}
				}
				TreeSet<ImmutablePair<ArrayList<Integer>, Double>> currentSolution = new TreeSet<>(cc);
				currentSolution.addAll(previousSolution);
				currentSolution.removeAll(pairsToRemove);

				HashSet<Integer> sampledVertices = new HashSet<>(SAMPLE*k);

				for(ImmutablePair<ArrayList<Integer>, Double> toRemove : pairsToRemove) {
					sampledVertices.addAll(toRemove.getLeft());
				}

				sampledVertices.addAll(freeVertices);

				//get sub-solutions
				HashSet<HashSet<ArrayList<Integer>>> subSolutions = findSubSolutions(sampledVertices, solNumILP, env);
				//System.out.println(subSolutions.size()+ " subsolutions found with "+ sampledVertices.size()+" free vertices");

				//add subsolutions 
				for(HashSet<ArrayList<Integer>> sub : subSolutions) {
					//print hier subsolution uit om te zien wat er gebuert

					TreeSet<ImmutablePair<ArrayList<Integer>, Double>> tentative = new TreeSet<>(currentSolution);

					//add cycles from subsolution i
					ArrayList<ImmutablePair<ArrayList<Integer>, Double>> toAdd = new ArrayList<>();
					for(ArrayList<Integer> c : sub) {
						toAdd.add(new ImmutablePair<ArrayList<Integer>, Double>(c, CycleUtils.calculateCycle(matches, c, pairValues)));
					}
					tentative.addAll(toAdd);
					neighbours.add(tentative);
				}
			}

			while(true) {
				TreeSet<ImmutablePair<ArrayList<Integer>, Double>> next = neighbours.pollLast();

				if(!tabuList.contains(next)) {
					previousSolution = next;
					int objVal = 0;
					for(ImmutablePair<ArrayList<Integer>, Double> p : next) {
						objVal += p.getLeft().size();
					}
					//System.out.println("best found: " + objVal + " in "+neighbours.size()+ " neighbours");
					if(objVal>bestObj) {
						bestObj = objVal;
						bestSolution = new TreeSet<>(next);
						if(bestObj>= UB-5) {
							System.out.println("("+(Instant.now().getEpochSecond()-startSecond)+ "s) Nearing upper bound: best obj improved to: "+bestObj+" and UB is "+ UB);
						}
						//System.out.println("(" +(Instant.now().getEpochSecond()-startSecond)+ "s) best obj improved to: "+bestObj);
						improveMoments.add((int) (Instant.now().getEpochSecond()-startSecond));
						improveValues.add(bestObj);
						if(bestObj == UB) {
							System.out.println("Upper bound reached!");
							System.out.println("Improvements:");
							System.out.println(Arrays.toString(improveMoments.toArray()));
							System.out.println(Arrays.toString(improveValues.toArray()));
							return;
						}
						improveTime = 0;
					}
					else {
						improveTime++;
						if(improveTime>3) {
							//System.out.println();
							//System.out.println("Increasing num of sampled cycles");
							SAMPLE += 5; //dit afbouwen?
							improveTime = 0;
							//solNumILP++;
							//TABU_SIZE += 5;
						}
					}
					tabuList.add(next);
					tabuAges.add(new ImmutablePair<>(next, iter));
					break;
				}
				else {
					System.out.println("Tabu list collision");
				}
				if(neighbours.isEmpty()) {
					throw new IllegalStateException("no neighbours left");
				}


			}
		}
		
		System.out.println("Time limit reached");
		System.out.println("Improvements:");
		System.out.println(Arrays.toString(improveMoments.toArray()));
		System.out.println(Arrays.toString(improveValues.toArray()));
	}

	private HashSet<HashSet<ArrayList<Integer>>> findSubSolutions(HashSet<Integer> freeVertices, int solNumILP, GRBEnv env) throws GRBException {

		SimpleDirectedGraph<Integer, DefaultEdge> g = new SimpleDirectedGraph<>(DefaultEdge.class);

		//add vertices
		ArrayList<Integer> vertices = new ArrayList<>(freeVertices.size());
		for(Integer v : freeVertices) {
			vertices.add(v);
			g.addVertex(v);
		}

		for(Integer d : vertices) {
			for(Integer r : vertices) {
				if(matches[d][r]) {
					g.addEdge(d, r);
				}
			}
		}

		//find cycles
		HawickJamesSimpleCycles<Integer, DefaultEdge> finder = new HawickJamesSimpleCycles<>(g);
		finder.setPathLimit(k);
		ArrayList<ArrayList<Integer>> cycles = new ArrayList<>();
		finder.findSimpleCycles(new CycleConsumer(cycles));
		
		//find combinations
		return findCombinationsILP(cycles, matches, matches.length, solNumILP, env);

	}
	private HashSet<HashSet<ArrayList<Integer>>> findCombinationsILP(ArrayList<ArrayList<Integer>> cycles, boolean[][] matches, int n, int solNum, GRBEnv env) throws GRBException{

		Random r = new Random();

		GRBModel model = new GRBModel(env);
		model.set(GRB.DoubleParam.TimeLimit, 1800.0);
		model.set(GRB.DoubleParam.PoolGap, 1.0);
		model.set(GRB.IntParam.PoolSearchMode, 2);
		
		model.set(GRB.IntParam.PoolSolutions, solNum);
		model.set(GRB.IntParam.Seed, r.nextInt(100));

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
		
		//create constraints
		for(ArrayList<Integer> vertex : cyclesPerVertex) {
			if(!vertex.isEmpty()) {
				GRBLinExpr expr = new GRBLinExpr();
				for(Integer c : vertex) {
					expr.addTerm(1, z[c]);
				}
				model.addConstr(expr, GRB.LESS_EQUAL, 1, "vertex_"+cyclesPerVertex.indexOf(vertex));

			}
		}
		
		model.optimize();
		HashSet<HashSet<ArrayList<Integer>>> result = new HashSet<>(solNum);
		for(int s = 0; s<solNum; s++) {
			HashSet<ArrayList<Integer>> solution = new HashSet<>();
			model.set(GRB.IntParam.SolutionNumber, s);
			for(int u = 0; u<z.length; u++) {
				GRBVar var = z[u];
				if(var.get(GRB.DoubleAttr.Xn) == 1) {
					solution.add(cycles.get(u));
				}
			}
			result.add(solution);
		}
		env.dispose();
		return result;
	}

	public int getBestObj() {
		return bestObj;
	}

	private class SolutionComparator implements Comparator<TreeSet<ImmutablePair<ArrayList<Integer>, Double>>>{

		@Override
		public int compare(TreeSet<ImmutablePair<ArrayList<Integer>, Double>> o1,
				TreeSet<ImmutablePair<ArrayList<Integer>, Double>> o2) {
			Integer count1 = 0;
			Integer count2 = 0;
			Double sum1 = 0.0;
			Double sum2 = 0.0;
			for(ImmutablePair<ArrayList<Integer>, Double> p : o1) {
				count1 += p.getLeft().size();
				sum1 += p.getRight();
			}

			for(ImmutablePair<ArrayList<Integer>, Double> p : o2) {
				count2 += p.getLeft().size();
				sum2 += p.getRight();
			}
			if(count1-count2 == 0) {

				return sum2.compareTo(sum1);
			}
			else {
				return count1-count2;
			}
		}

	}

	private class CycleComparator implements Comparator<ImmutablePair<ArrayList<Integer>, Double>> {

		@Override
		public int compare(ImmutablePair<ArrayList<Integer>, Double> o1, ImmutablePair<ArrayList<Integer>, Double> o2) {
			if(o1.getLeft().size()-o2.getLeft().size() == 0) {

				return o2.getRight().compareTo(o1.getRight());
			}
			else {
				return o1.getLeft().size()-o2.getLeft().size();
			}
		}
	}
	
	public ArrayList<ArrayList<Integer>> getSolutionCycles(){
		ArrayList<ArrayList<Integer>> toReturn = new ArrayList<>(bestSolution.size());
		
		for(ImmutablePair<ArrayList<Integer>, Double> pair : bestSolution) {
			
			toReturn.add(pair.getLeft());
		}
		return toReturn;
	}
	
	public boolean[][] getSolutionMatrix(){
		
		boolean[][] toReturn = new boolean[matches.length][matches.length];
		for(ImmutablePair<ArrayList<Integer>, Double> pair : bestSolution) {
			ArrayList<Integer> cycle = pair.getLeft();
			for(int i = 0; i<cycle.size()-1; i++) {

				toReturn[cycle.get(i)][cycle.get(i+1)] = true;
			}
			toReturn[cycle.get(cycle.size()-1)][cycle.get(0)] = true;
		}
		
		
		return toReturn;
	}
}
