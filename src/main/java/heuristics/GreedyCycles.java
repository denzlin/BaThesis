package heuristics;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;
import java.util.Random;
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
	private ArrayList<ArrayList<Integer>> 	knownSolutions;

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

		knownSolutions = new ArrayList<>(50000);
	}

	public ArrayList<Integer> runNoFilter() {
		runs++;
		long startTime = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime());

		ArrayList<Integer> result = new ArrayList<>((int) (aggressiveness*n));
		Random r = new Random();

		//copy the base tree
		TreeSet<ImmutablePair<ArrayList<Integer>, Double>> valueTree = new TreeSet<ImmutablePair<ArrayList<Integer>, Double>>(valueTreeBase);
		HashSet<Integer> matchedNodes = new HashSet<>(v);
		//if there are pairs to pick left
		while(!valueTree.isEmpty()) {
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
				knownSolutions.add(result);
				break;
			}
			

			ArrayList<ImmutablePair<ArrayList<Integer>, Double>> selectionCopy = new ArrayList<>(selection);
			ImmutablePair<ArrayList<Integer>, Double> selected = null;

			while(!selectionCopy.isEmpty()) {

				selected = selectionCopy.get(r.nextInt(selectionCopy.size()));
				selectionCopy.remove(selected);
				result.add(cycleIndices.get(selected.getLeft()));
				if(knownSolutions.contains(result)) {
					//System.out.println("known solution encountered");
					result.remove(cycleIndices.get(selected.getLeft()));
					selected = null;
				}
				else {
					break;
				}
			}
			
			if(selected == null) {
				//System.out.println("branch cut, "+knownSolutions.size()+" solutions added, tree has"+ valueTree.size()+" cycles left");
				knownSolutions.add(result);
				continue;
			}
			

			//result.add(cycleIndices.get(selected.getLeft()));
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

		knownSolutions.add(result);
		return result;
	}

	public Comparator<ImmutablePair<ArrayList<Integer>, Double>> byValue = 
			((ImmutablePair<ArrayList<Integer>, Double> p1, ImmutablePair<ArrayList<Integer>, Double> p2)
					-> Double.compare(p1.getRight(), p2.getRight()));

	public double getAverageRunTime() {
		return (double) runTime/(double) runs;
	}


}
