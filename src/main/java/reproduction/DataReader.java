package reproduction;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

/**
 * Reads a csv file of compatibility data
 */
public class DataReader {

	private int[] ids;
	private boolean[][] matches;
	
	/**
	 * initializes values from the file specified 
	 * @param fileName file name without .csv
	 * @throws IOException
	 */
	public DataReader(String fileName) throws IOException {
		
		InputStream is = this.getClass().getResourceAsStream("/ "+fileName+".csv" );
		String filePath;
		try(BufferedReader br = new BufferedReader(new InputStreamReader(is))){
			filePath = br.lines().collect(Collectors.joining("\n"));
		}
				   	
        String line;
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            while ((line = br.readLine()) != null) {
                // Split the line by commas
                String[] values = line.split(",");

                //TODO make lists when data
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
	}
	
	public int[] getIds() {
		return ids;
	}
	
	public boolean[][] getMatches(){
		return matches;
	}
}
