package reproduction;

import java.io.IOException;

import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;


/**
 * Reads a csv file of compatibility data
 */
public class DataReader {

	private int[] ids;
	private boolean[][] matches;
	
	public static void main(String[] args) throws IOException {
		DataReader dr = new DataReader("Instance_S_1.xlsx");
		dr.getIds();
	}
	/**
	 * initializes values from the file specified 
	 * @param fileName file name without .csv
	 * @throws IOException
	 */
	public DataReader(String fileName) throws IOException {
		
		try {
			XSSFWorkbook wb = new XSSFWorkbook("src/main/resources/"+fileName);

		     XSSFSheet sheet = wb.getSheetAt(0);
		        XSSFRow row; 
		        XSSFCell cell;
		        
		    int rows; // No of rows
		    rows = sheet.getPhysicalNumberOfRows();
		    int cols = sheet.getRow(0).getLastCellNum();
		    System.out.println(cols);
		    this.ids = new int[rows-1];
		    this.matches = new boolean[rows-1][cols-1];
		    
		    for(int r = 1; r < rows; r++) {
		        row = sheet.getRow(r);
		        if(row != null) {
		            for(int c = 0; c < cols; c++) {
		                cell = row.getCell((short)c);
		                if(cell != null) {
		                    if(c == 0) {
		                    	ids[r-1] = (int) cell.getNumericCellValue();
		                    }
		                    else {
		                    	if(cell.getNumericCellValue() == 1) {
		                    		matches[r-1][c-1] = true;
		                    	}
		                    }
		                }
		            }
		        }
		    }
		    wb.close();
		} catch(Exception ioe) {
		    ioe.printStackTrace();
		}
		System.out.println(ids[34]);
	}
	
	public int[] getIds() {
		return ids;
	}
	
	public boolean[][] getMatches(){
		return matches;
	}
}
