package data;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Scanner;

/**
 * This class reads .wmd (weighted matching data, not Windows Media Download Package) files.
 * Kidney Matching Data Files in this format are made freely available on https://preflib.simonrey.fr/ by:
 * Nicholas Mattei and Toby Walsh. PrefLib: A Library of Preference Data. Proceedings of Third International Conference on Algorithmic Decision Theory (ADT 2013)
 * 
 */
public class WMDReader {

	public static boolean[][] read(File file) throws FileNotFoundException {

		try (Scanner sc = new Scanner(new FileReader(file))) {
			
			//skip info lines
			for(int i = 0; i<9; i++) {sc.nextLine();};
			int n = Integer.parseInt(sc.nextLine().split(" ")[3]);
			System.out.println(n+" pairs");
			//skip more
			for(int i = 0; i<n+1; i++) {sc.nextLine();};
			
			boolean[][] matches = new boolean[n][n];
			while(sc.hasNextLine()) {
				String[] line = sc.nextLine().split(",");
				matches[Integer.parseInt(line[0])-1][Integer.parseInt(line[1])-1] = true;
			}
			return matches;
		}
	}
}
