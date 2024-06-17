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
		
		int method = 1;
		final int k = 4;
		double T_average = 0;
		double gapAverage = 0;
		double cycleT_average = 0;

		File folder = new File("src/main/resources/delorme");
		File[] listOfFiles = folder.listFiles();

		int testSetSize = 10;
		for(int u = 60; u<70; u++) {
			File data = listOfFiles[u];

			ArrayList<Double> cycleValuesAll = new ArrayList<>();

			ArrayList<Double> cycleValuesSol = new ArrayList<>();
			Pair<Integer, Double> result;
			if(method == 0) {
				result = CycleFormulation.run(data, k, cycleValuesAll, cycleValuesSol);
				
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

			}
			if(method == 1) {
				result = EEFormulation.run(data, k);
			}
			
			
		}
		
		
		
		
		//display statistics for all runs
		System.out.println("\navg cycle T: "+((double) cycleT_average)/testSetSize);
		System.out.println("avg T: "+((double) T_average)/testSetSize);
		System.out.println("avg gap: "+ ((double) gapAverage)/testSetSize);

	}

	public void connectivity(SimpleDirectedGraph<Integer, DefaultEdge> g) {
		GabowStrongConnectivityInspector<Integer, DefaultEdge> insp = new GabowStrongConnectivityInspector<>(g);
		if(!insp.isStronglyConnected()) {
			System.out.println("Graph not strongly connected, analysing...");
			for(Set<Integer> set : insp.stronglyConnectedSets()) {
				System.out.println("set size: "+ set.size());
			}
		} else {
			System.out.println("graph is strongly connected");
		}
	}
}






