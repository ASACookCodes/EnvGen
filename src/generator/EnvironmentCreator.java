package generator;
import java.awt.Point;
import java.io.*;
import org.dom4j.*;
import java.lang.*;
import java.util.*;

public class EnvironmentCreator {	
	
	private int NODE_GAP;
	private int LABEL_LOC;
	private int STARTING_ID;
	private String[] MOVEMENTS;
	private String[] LOCATIONS;
	private String systemDeclarations;
	private int noOfNodes;
	private int[][] overlapNames;
	private String[][] locationNames;
	private HashSet<Integer> nodeNames;
	
	private Document document;
	private Element root;
	private Element globalDeclarations;
	private Element envTemplate;
	private Element movTemplate;
	private Element sysTemplate;
	
	
	/**
	 * Creator constructor
	 * @param gap the user-defined spacing between the RSUs
	 * @param number user-defined number of RSUs
	 * @param movements the list of user-defined movements
	 * @param locations the user-defined names of the locations 
	 */
	public EnvironmentCreator(Document doc, int startingID, int gap, int number, String[] locations, String sysDec) {
		
		//Positioning of the UPPAAL elements
		NODE_GAP = gap;
		LABEL_LOC = NODE_GAP/2;
		STARTING_ID = startingID; 
		LOCATIONS = locations;
		systemDeclarations = sysDec;
		noOfNodes = number;
		document = doc;
		
	}
	
	
	/**
	  * Writes the XML document directly to the user-specified file
	  * @param document
	  * @param path
	  * @throws IOException
	  */
	 public void WriteDoc(String path, String overlaps) throws IOException {
		 
		overlaps = new StringBuilder(overlaps).reverse().toString(); //reverse the binary string (for more logical iteration)
		overlapNames = new int[overlaps.length()][2]; //stores the IDs of the nodes that overlap based on the overlaps binary string
		locationNames = new String[overlaps.length()][2]; //stores the names of locations that overlap
		nodeNames = new HashSet<Integer>(); //stores the nodes involved in overlappings
		
		//Gather the names of nodes involved in overlaps that will be used for naming the overlap nodes, and store / add them to the relevant data structures.
		int index1 = 1;
		int index2 = 2;
		for (int i=0; i < overlaps.length(); i++) {
			if (overlaps.charAt(i)=='1') {
				overlapNames[i][0] = index1; nodeNames.add(index1); locationNames[i][0] = LOCATIONS[index1-1];
				overlapNames[i][1] = index2; nodeNames.add(index2); locationNames[i][1] = LOCATIONS[index2-1];
			}
			if (index2 == noOfNodes) {index1++;index2=index1+1;} else {index2++;}
		}
		 
		//Create the environment.
		document = this.CreateDoc();
		
		//Write the environment to the XML document.
		Writer writer = new OutputStreamWriter(new FileOutputStream(path), "UTF-8");//Convert file output stream to writer object
	 	document.write(writer);//Write DOM model built in memory to XML file
	 	writer.close();//Close output stream
	 	//System.out.println("Success!");
	 }
	 
	

	 /**
	  * Creates the XML document, consisting of two templates, the environment and the movement.
	  * Adds the RSUs and the synchronisations
	  * @return the completed XML document
	  */
	 public Document CreateDoc() {

		 //Detach all nodes from loaded document
		 List<Node> templates = document.selectNodes("/nta/*");
		 for (Node n : templates) {
			 n.detach();
		 }
		 
		 //Dom4j is so horrible that it does not let you build XML nodes if they don't "exist" in an XML document.
		 //Elements are a static class, so here, I use a "ghost document" to build my environment template and system declarations, then will just copy it to the real XML and remove the ghost document
		 Document ghost = DocumentHelper.createDocument();
		 envTemplate = ghost.addElement("template");
		 Document sysghost = DocumentHelper.createDocument();
		 sysTemplate = sysghost.addElement("system");
		 
		 Element name = envTemplate.addElement("name");
		 sysTemplate.setText(systemDeclarations);
		 name.addText("Environment");
		 
		 //Add parameters for the location tracking booleans
		 String parameters = "";
		 for (int i=0; i<LOCATIONS.length; i++) {
			 parameters += "bool &in"+LOCATIONS[i]+",";
			 System.out.println(LOCATIONS[i]);
		 }
		 parameters = parameters.substring(0, parameters.length()-1); //Remove final comma
		 envTemplate.addElement("parameter").addText(parameters);
		 
		 addOverlaps(); //Add the overlaps
		 addLocations(); //Add the nodes
	     
	     //Set initial location, no man's land (NML) created in addLocations()
		 envTemplate.addElement("init").addAttribute("ref", "id0");
		 
		
		 addTransitions(); //Add the transitions from singleton nodes to NML
		 addOverlapTransitions(); //Add the transitions from overlapping regions to NML
	     //addControlTransitions(); //Add the transitions for the movement system.
		 
		 //Add the template before the initiator
		 Document newDocument = DocumentHelper.createDocument();
		 newDocument.addDocType("nta", null, "uppaal.dtd");
		 Element nta = newDocument.addElement("nta");
		 
		//Add the environment node specifically before the Initiator node in the document
		 for (Node n : templates) {
			 if (n.selectSingleNode("./name") != null) {
				 if ("Initiator".equals(n.selectSingleNode("./name").getText())) {
					 nta.add(envTemplate);
				 }
				 if (!"Environment".equals(n.selectSingleNode("./name").getText())) {
					 nta.add(n);
				 }
			} else {
				if ("system".equals(n.getName())) {
					nta.add(sysTemplate);
				} else {
					nta.add(n);
				}
			}
		 }
	     return newDocument;
	 }
	 
	 /**
	  * Algorithm to add the transitions from the overlap nodes to NML.
	  */
	 private void addOverlapTransitions() {
		int labelLocX = (int) 1.5*NODE_GAP;
		int labelLocY = 0-labelLocX;
		for (int i=0; i<overlapNames.length; i++) {
			if (overlapNames[i][0]!=0 && overlapNames[i][1]!=0) {
					 
					//Add transitions one for moving in to overlap
					 Element association= envTemplate.addElement("transition");
					 Element disassociation= envTemplate.addElement("transition");
					 
					 //Tell the transition arrows where to go
					 association.addElement("source").addAttribute("ref", "id0");
					 association.addElement("target").addAttribute("ref", "o"+overlapNames[i][0]+overlapNames[i][1]);
					 disassociation.addElement("source").addAttribute("ref", "o"+overlapNames[i][0]+overlapNames[i][1]);
					 disassociation.addElement("target").addAttribute("ref", "id0");
					 
					 //Add the updates
					 association.addElement("label").addAttribute("kind", "assignment").addAttribute("x", String.valueOf(labelLocX+10)).addAttribute("y", String.valueOf(labelLocY+10)).addText("in"+locationNames[i][0]+"=true, \r\n in"+locationNames[i][1]+"=true");
					 disassociation.addElement("label").addAttribute("kind", "assignment").addAttribute("x", String.valueOf(labelLocX)).addAttribute("y", String.valueOf(labelLocY+20)).addText("in"+locationNames[i][0]+"=false, \r\n in"+locationNames[i][1]+"=false");
					 					 
					 //Add a nail to space them out
					 association.addElement("nail").addAttribute("x", String.valueOf(labelLocX)).addAttribute("y", "0");
					 disassociation.addElement("nail").addAttribute("x", "0").addAttribute("y", String.valueOf(labelLocY));
					 
				 }
				 labelLocX += NODE_GAP;
				 labelLocY -= NODE_GAP;
			}
			
		}
		
	
	
	 /**
	  * Adds the possible locations of the agent into the environment, first the NML state at (0,0)
	  *  then the RSUs, naming them RSU1, RSU2, etc, and the coordinates at (50,50), (100,100), (150,150) etc.
	  * @param template the UPPAAL template
	  * @param noOfLocations the number of nodes.
	  */
	 private void addLocations() {
		 
		 //Add no man's land location
		 Element NML = envTemplate.addElement("location");
		 NML.addAttribute("id", "id0").addAttribute("x", "0").addAttribute("y", "0");
		 NML.addElement("name").addAttribute("x", "0").addAttribute("y", "0").addText("NML");
		 
		 //Add nodes as long as they aren't part of overlaps
		 for (int i=1; i<noOfNodes+1; i++) {
			 if (!nodeNames.contains(i)) {
				 addLocation("loc"+i, i*NODE_GAP, i*NODE_GAP, LOCATIONS[i-1]);
			 }
			 STARTING_ID++;
		 }
		 
	 }
	 
	 
	 
	 /**
	  * Adds the transitions, both associations and disassociations between the RSUs.
	  * @param template the UPPAAL template
	  * @param noOfLocations the number of RSUs
	  */
	 private void addTransitions() {
		 int labelLoc = LABEL_LOC;
		 for (int i = 0; i<noOfNodes; i++) {
			 if (!nodeNames.contains(i+1)) { //Dont add transitions for nodes that aren't there!
				 //Add the transition arrows, one for an moving to a location and one for moving out. 
				 Element association= envTemplate.addElement("transition");
				 Element disassociation= envTemplate.addElement("transition");
				 //Tell it which two nodes to connect
				 association.addElement("source").addAttribute("ref", "id0");
				 disassociation.addElement("source").addAttribute("ref", "loc"+(i+1));
				 association.addElement("target").addAttribute("ref", "loc"+(i+1));
				 disassociation.addElement("target").addAttribute("ref", "id0");
				 
				 //Add the location set updates
				 association.addElement("label").addAttribute("kind", "assignment").addAttribute("x", String.valueOf(labelLoc+10)).addAttribute("y", String.valueOf(labelLoc+10)).addText("in"+LOCATIONS[i]+"=true");
				 disassociation.addElement("label").addAttribute("kind", "assignment").addAttribute("x", String.valueOf(labelLoc)).addAttribute("y", String.valueOf(labelLoc+20)).addText("in"+LOCATIONS[i]+"=false");
				 //Add nails to space the arrows out
				 association.addElement("nail").addAttribute("x", String.valueOf(labelLoc)).addAttribute("y", "0");
				 disassociation.addElement("nail").addAttribute("x", "0").addAttribute("y", String.valueOf(labelLoc));
				 labelLoc += NODE_GAP;
			 }
		 }
	 }
	 
	 /**
	  * 
	  * @param template
	  * @param noOfOverlaps
	  * @return
	  */
	 private void addOverlaps() {
		 
		//Controls spacing of the overlap states, starts half way between the first two nodes
		 int x = NODE_GAP / 2;
		 int y = 0-x;
		 
		 //Add the overlaps
		 for (int i=0; i<overlapNames.length; i++) {
			 x += NODE_GAP; //the coordinates of the overlap state
			 y -= NODE_GAP;
			 if (overlapNames[i][0]!=0 && overlapNames[i][1]!=0) { //if there is an overlap in this position 
					 this.addLocation("o"+overlapNames[i][0]+overlapNames[i][1], x, y, locationNames[i][0]+locationNames[i][1]);
			 }
		 }

	 }
	
	  
	
	 /**
	  * puts a node in the environment
	  * @param template the template
	  * @param id the id of the location
	  * @param x x coordinate of the location
	  * @param y y coordinate of the location
	  * @param name the name of the location
	  */
	 private void addLocation(String id, int x, int y, String name) {
		 Element newLocation = envTemplate.addElement("location");
		 newLocation.addAttribute("id", id).addAttribute("x", String.valueOf(x)).addAttribute("y", String.valueOf(y));
		 newLocation.addElement("name").addAttribute("x", String.valueOf(x)).addAttribute("y", String.valueOf(y)).addText(name);
	 }	
	 
	 //Getters
	 public Element getRoot() {return this.root;}
	 public Element getGlobalDecs() {return this.globalDeclarations;}
	 public Element getEnvTemplate() {return this.envTemplate;}
	 public Element getMovTemplate() {return this.movTemplate;}
	 public Element getSysTemplate() {return this.sysTemplate;}
	 
	 
	 
}
