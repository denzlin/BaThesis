package data;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import util.CycleUtils;

import java.io.File;
import java.io.FileInputStream;
public class XMLData {
	
	private boolean[][] matches;
	
	// keys are ID's from data, values are index in matrix
	private HashMap<Integer, Integer> ids;
	
	public XMLData(File file) {
		
		ArrayList<Pair<Integer, Integer>> edges = new ArrayList<>();
		
		int currentID = -1;
		int max = 0;
		SortedSet<Integer> donors = new TreeSet<>();
		SortedSet<Integer> recipients = new TreeSet<>();
		
		XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
		try {
			XMLEventReader reader = xmlInputFactory.createXMLEventReader(new FileInputStream(file));
			
			//skip first line
			//reader.nextEvent();
			
			while (reader.hasNext()) {
			    XMLEvent nextEvent = reader.nextEvent();
			    if (nextEvent.isStartElement()) {
			        StartElement startElement = nextEvent.asStartElement();
			        
			        if(startElement.getName().getLocalPart() == "entry") {
			        	currentID = Integer.parseInt(startElement.getAttributeByName(new QName("donor_id")).getValue());
			        	if(currentID>max) {
			        		max = currentID;
			        	}
			        	donors.add(currentID);
			        }
			        if(startElement.getName().getLocalPart() == "recipient") {

			        	nextEvent = reader.nextEvent();
			        	
			        	int recipient = Integer.parseInt(nextEvent.asCharacters().getData());
			        	//for the way the donors are numbered
			        	if(recipient>max) {
			        		max = recipient;
			        	}
			        	recipients.add(recipient);

			        	edges.add(new ImmutablePair<Integer, Integer>(currentID, recipient));
			        }
			    }
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (XMLStreamException e) {
			e.printStackTrace();
		}
		
		ids = new HashMap<>();
		int pointer = 0;
		for(Integer d : donors) {
			if(recipients.contains(d)) {
				ids.put(d, pointer);
				pointer++;
			}
		}
		
		matches = new boolean[ids.size()][ids.size()];
		for(Pair<Integer, Integer> p : edges) {
			int left = p.getLeft();
			int right = p.getRight();
			if(left != right && ids.containsKey(left) && ids.containsKey(right)) {
				matches[ids.get(left)][ids.get(right)] = true;
			}
		}
		
		matches = CycleUtils.reduceMatchMatrix(matches);
		matches = CycleUtils.orderMatrixByDegree(matches, 0);
	}

	public boolean[][] getMatches() {
		return matches;
	}
	
	//untested
	public ArrayList<Integer> getIds(){
		ArrayList<Integer> result = new ArrayList<>(this.ids.size());
		for(Integer key : ids.keySet()) {
			result.add(ids.get(key), key);
		}
		return result;
	}
}
