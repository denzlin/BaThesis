package reproduction;

import java.io.File;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.jgrapht.alg.connectivity.GabowStrongConnectivityInspector;
import org.jgrapht.alg.util.Pair;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;

import data.XMLData;
import heuristics.CyclePackingFormulation;
import util.CycleUtils;
import util.TimedPrintStream;

public class Main {

	public static void main(String[] args) throws Exception {

		System.setOut(new TimedPrintStream(System.out));
		
		int method = 0;
		final int k = 5;
		double T_average = 0;
		double cycleT_average = 0;

		File folder = new File("src/main/resources/preflib");
		File[] listOfFiles = folder.listFiles();

		int testSetSize = 5;
		for(int u = 50; u<60; u++) {
			File data = listOfFiles[u];

			ArrayList<Double> cycleValuesAll = new ArrayList<>();

			ArrayList<Double> cycleValuesSol = new ArrayList<>();
			Pair<Integer, Double> result = null;
			if(method == 0) {
				result = CycleFormulation.run(data, k, cycleValuesAll, cycleValuesSol);
				
				//for distributions
				/*
				double max = Collections.max(cycleValuesAll);
				double[] histogramAll = new double[20];
				double[] histogramSol = new double[20];
				
				for(Double value : cycleValuesAll) {
					int index = (int) Math.ceil((value/max)*20) -1;
					histogramAll[index]++;;
				}
				for(Double value : cycleValuesSol) {
					int index = (int) Math.floor((value/max)*20);
					histogramSol[index]++;
				}
				
				double[] fractions = new double[20];
				double total = 0.0;
				for(int i = 0; i<20; i++) {
					if(histogramAll[i]>0) {
						fractions[i] = (histogramSol[i]/1.0)/histogramAll[i];
						total += fractions[i];
					}
					
				}
				
				for(int i = 0; i<20; i++) {
					fractions[i] = Math.round((fractions[i])*1000)/10.0;
				}
				System.out.println(Arrays.toString(histogramAll));
				System.out.println(Arrays.toString(histogramSol));
				System.out.println(Arrays.toString(fractions)+ "\n");
				*/

			}
			if(method == 1) {
				boolean[][] matches = (new XMLData(data)).getMatches();
				result = EEFormulation.solve(matches, k, new ArrayList<>(), 10000, 1800);
				
			}
			T_average += result.getFirst();
			cycleT_average += result.getSecond();
		}
		
		//display statistics for all runs
		System.out.println("avg cycle T: "+((double) cycleT_average)/testSetSize);
		System.out.println("avg T: "+((double) T_average)/testSetSize);

	}
}






