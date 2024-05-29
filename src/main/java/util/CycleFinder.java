package util;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.jgrapht.Graph;
import org.jgrapht.alg.cycle.*;

import org.jgrapht.graph.DefaultEdge;

/**
 * This class provides methods to find all cycles of a certain length in a directed graph
 */
public class CycleFinder {

	public static ArrayList<ArrayList<Integer>> getCycles(Graph<Integer, DefaultEdge> g, int k) {

		HawickJamesSimpleCycles<Integer, DefaultEdge> finder = new HawickJamesSimpleCycles<>(g);
		finder.setPathLimit(k);
		ArrayList<ArrayList<Integer>> cycles = new ArrayList<>();
		finder.findSimpleCycles(new CycleConsumer(cycles));
		
		
		return cycles;
	}

	private static class CycleConsumer implements Consumer<List<Integer>> {

		ArrayList<ArrayList<Integer>> cycles;

		public CycleConsumer(ArrayList<ArrayList<Integer>> cycles) {
			this.cycles = cycles;
		}

		@Override
		public void accept(List<Integer> t) {
			ArrayList<Integer> c = new ArrayList<>(t.size());
			c.addAll(t);
			cycles.add(c);
		}
	}
}
