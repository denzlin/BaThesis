package reproduction;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.jgrapht.Graph;
import org.jgrapht.alg.cycle.SzwarcfiterLauerSimpleCycles;
import org.jgrapht.graph.DefaultEdge;

/**
 * This class provides methods to find all cycles of a certain length in a directed graph
 */
public class CycleFinder {
	
	public static ArrayList<ArrayList<Integer>> getCycles(Graph<Integer, DefaultEdge> g, int k) {
		
		SzwarcfiterLauerSimpleCycles<Integer, DefaultEdge> finder = new SzwarcfiterLauerSimpleCycles<Integer, DefaultEdge>(g);
		ArrayList<ArrayList<Integer>> cycles = new ArrayList<ArrayList<Integer>>();
		finder.findSimpleCycles(new CycleConsumer(cycles, k));
		return cycles;
	}
	
	private static class CycleConsumer implements Consumer<List<Integer>> {

		ArrayList<ArrayList<Integer>> cycles;
		int k;
		public CycleConsumer(ArrayList<ArrayList<Integer>> cycles, int k) {
			this.cycles = cycles;
			this.k = k;
		}
		
		@Override
		public void accept(List<Integer> t) {
			if(t.size()<=k) {
				ArrayList<Integer> c = new ArrayList<>(t.size());
				c.addAll(t);
				c.sort(null); //natural order
				cycles.add(c);
			}
		}
	}
}
