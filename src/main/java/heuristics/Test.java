package heuristics;

import java.io.File;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;

import org.jgrapht.alg.util.Pair;

import com.gurobi.gurobi.GRBException;

import data.XMLData;
import reproduction.CycleFormulation;
import reproduction.EEFormulation;
import util.CycleUtils;

public class Test {

	public static void main(String[] args) throws GRBException {
		
		int iter = 100;
		final int k = 4;
		double aggressiveness = 0.1;
		int randomSelection = 10;
		File folder = new File("src/main/resources/delorme");
		File[] listOfFiles = folder.listFiles();
		
		for(int u = 30; u<40; u++) {
			
			File data = listOfFiles[u];
			System.out.println("["+LocalTime.now().truncatedTo(ChronoUnit.MINUTES).toString()+"] "+"Running greedy heuristic on " + data.getName());
			
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
			double density = matchCount/(double) Math.pow(matches.length,2)*100/100;
			System.out.println("Data has " +matches.length+ " matchable pairs with an average density of " + density);

			ArrayList<ArrayList<Integer>> cycles = CycleUtils.getCycles(matches, k);
			GreedyCycles gc = new GreedyCycles(cycles, CycleUtils.calculateCycles(matches, cycles), aggressiveness, randomSelection, matches.length);
			
			int best = 0;
			for(int i = 0; i<iter; i++) {
				if((i+1)%5 == 0) {
					System.out.println(i+1);
				}
				
				ArrayList<Integer> result = gc.runNoFilter();
				int pairCount = 0;
				for(Integer c : result) {
					pairCount += cycles.get(c).size();
				}
				best = pairCount > best ? pairCount : best;
			}
			System.out.println("Average runtime: " + gc.getAverageRunTime());
			System.out.println("Best Solution: "+best+"\n");
			
		}

	}

}
