package heuristics;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.Random;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.lang3.tuple.ImmutablePair;


/**
 * This class performs a greedy heuristic that iteratively includes cycles in a solution based on
 * their rankings as defined in the thesis. The algorithm is randomized by picking a cycle from
 * a fixed number of the best solutions every iteration.
 */
public class GreedyCycles {

	private double 	aggressiveness;
	private int 	n;
	private int 	v;
	private int 	randomSelection;
	private int 	runTime = 0;
	private int 	runs = 0;
	private TreeSet<ImmutablePair<ArrayList<Integer>, Double>> 	valueTreeBase;
	private HashMap<ArrayList<Integer>, Integer> 				cycleIndices;


	/**
	 * Creates the TreeMap and assign cycles indices.
	 * @param cycles
	 * @param cycleValues
	 * @param aggressiveness
	 * @param randomness
	 */
	public GreedyCycles(ArrayList<ArrayList<Integer>> cycles, ArrayList<Double> cycleValues,
			double aggressiveness, int randomSelection, int v) {

		n = cycles.size();
		this.aggressiveness = aggressiveness;
		this.randomSelection = randomSelection;
		
		//rank cycles and index
		this.valueTreeBase = new TreeSet<>(byValue);
		this.cycleIndices = new HashMap<>();
		for(int i = 0; i<n; i++) {
			valueTreeBase.add(new ImmutablePair<>(cycles.get(i), cycleValues.get(i)));
			cycleIndices.put(cycles.get(i), cycleIndices.size());

		}
		//reduce in size
		while(valueTreeBase.size()>(int) cycles.size()*aggressiveness){
			valueTreeBase.pollLast();
		}
	}

	/**
	 * Runs the algorithm.
	 * @param aggressiveness The fraction of cycles to include (will be rounded).
	 * @param cycles All possible cycles.
	 * @param cycleValues The corresponding values of all possible cycles
	 * @param randomness The number of cycles the next cycle is randomly selected from. 
	 * @return An ArrayList with the indexes of the chosen cycles in the input list of cycles.
	 */
	public ArrayList<Integer> runFilter() {
		runs++;
		long startTime = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime());
		ArrayList<Integer> result = new ArrayList<>((int) (aggressiveness*n));
		Random r = new Random();

		//copy the base tree
		TreeSet<ImmutablePair<ArrayList<Integer>, Double>> valueTree = new TreeSet<ImmutablePair<ArrayList<Integer>, Double>>(valueTreeBase);

		while(!valueTree.isEmpty()) {
			//System.out.println(valueTree.size());
			ArrayList<ImmutablePair<ArrayList<Integer>, Double>> selection = new ArrayList<>(randomSelection);

			int selectionSize = valueTree.size()>=randomSelection ? randomSelection : valueTree.size();
			for(int i = 0; i<selectionSize; i++) {
				selection.add(valueTree.pollFirst());
			}

			ImmutablePair<ArrayList<Integer>, Double> selected = selection.get(r.nextInt(selectionSize));
			result.add(cycleIndices.get(selected.getLeft()));
			//System.out.println(selected.getRight());
			ArrayList<ImmutablePair<ArrayList<Integer>, Double>> toRemove = new ArrayList<>();
			filterSelection:
				for(ImmutablePair<ArrayList<Integer>, Double> other : selection) {
					for(Integer vertex : selected.getLeft()) {
						if(other.getLeft().contains(vertex)) {
							toRemove.add(other);
							continue filterSelection;
						}
					}
				}
			selection.removeAll(toRemove);

			//dont know what the optimal list size is here but i prefer using more memory over cpu time
			toRemove = new ArrayList<>((int) 0.5*valueTree.size());
			filterTree:
				for(ImmutablePair<ArrayList<Integer>, Double> other : valueTree) {
					for(Integer vertex : selected.getLeft()) {
						if(other.getLeft().contains(vertex)) {
							toRemove.add(other);
							continue filterTree;
						}
					}
				}
			valueTree.removeAll(toRemove);

			// return unused and unfiltered cycles back to the tree
			for(ImmutablePair<ArrayList<Integer>, Double> leftOver : selection) {
				valueTree.add(leftOver);
			}
		}

		runTime += Math.toIntExact(TimeUnit.NANOSECONDS.toSeconds(System.nanoTime()) - startTime);

		//System.out.println("Heuristic solution with value "+result.size());
		return result;
	}

	public ArrayList<Integer> runNoFilter() {
		runs++;
		long startTime = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime());

		ArrayList<Integer> result = new ArrayList<>((int) (aggressiveness*n));
		Random r = new Random();

		//copy the base tree
		TreeSet<ImmutablePair<ArrayList<Integer>, Double>> valueTree = new TreeSet<ImmutablePair<ArrayList<Integer>, Double>>(valueTreeBase);
		HashSet<Integer> matchedNodes = new HashSet<>(v);
		while(!valueTree.isEmpty()) {
			//System.out.println(valueTree.size());
			ArrayList<ImmutablePair<ArrayList<Integer>, Double>> selection = new ArrayList<>(randomSelection);

			int selectionSize = valueTree.size()>=randomSelection ? randomSelection : valueTree.size();

			selectionLoop:
				while(selection.size() < selectionSize && valueTree.size() != 0) {
					ImmutablePair<ArrayList<Integer>, Double> polled = valueTree.pollFirst();
					for(Integer vertex : polled.getLeft()) {
						if(matchedNodes.contains(vertex)) {
							continue selectionLoop;
						}
					}
					selection.add(polled);
				}
			if(selection.isEmpty()) {
				break;
			}
			ImmutablePair<ArrayList<Integer>, Double> selected = selection.get(r.nextInt(selection.size()));
			result.add(cycleIndices.get(selected.getLeft()));
			matchedNodes.addAll(selected.getLeft());

			ArrayList<ImmutablePair<ArrayList<Integer>, Double>> toRemove = new ArrayList<>();
			filterSelection:
				for(ImmutablePair<ArrayList<Integer>, Double> other : selection) {
					for(Integer vertex : selected.getLeft()) {
						if(other.getLeft().contains(vertex)) {
							toRemove.add(other);
							continue filterSelection;
						}
					}
				}
			selection.removeAll(toRemove);

			// return unused and unfiltered cycles back to the tree
			for(ImmutablePair<ArrayList<Integer>, Double> leftOver : selection) {
				valueTree.add(leftOver);
			}
		}

		runTime += Math.toIntExact(TimeUnit.NANOSECONDS.toSeconds(System.nanoTime()) - startTime);

		//System.out.println("Tree FilterHeuristic solution with value "+result.size());
		return result;
	}

	public Comparator<ImmutablePair<ArrayList<Integer>, Double>> byValue = 
			(ImmutablePair<ArrayList<Integer>, Double> p1, ImmutablePair<ArrayList<Integer>, Double> p2)
			-> Double.compare(p1.getRight(), p2.getRight());

	public double getAverageRunTime() {
		return (double) runTime/(double) runs;
	}
}
