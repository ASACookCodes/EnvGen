package generator;


import java.io.*;
import java.util.*;

import org.dom4j.*;
import org.dom4j.io.*;

public class XMLFactory {
	
	private String path;
	private File uppaalTemplate;
	private Document document;
	private final String attackerTemplatePath
						= "/nta/template[name='Attacker']";
	
	
	public XMLFactory(File xml) throws IOException, DocumentException {
		//Open UPPAAL file
		SAXReader reader = new SAXReader();
		
		//Store the path
		uppaalTemplate = xml;
				
		//Parse the UPPAAL file.
		document = reader.read(uppaalTemplate);
		path = uppaalTemplate.getAbsolutePath();
	}
	
	
	public ArrayList<String> getNodeNames() {
		ArrayList<String> nodeNames = new ArrayList<String>();
		
		//Iterate through nodes and check if "Node" is in the name.
		 List<Node> templates = document.selectNodes("/nta/*");
		 for (Node n : templates) {
			 if (n.selectSingleNode("./name") != null) {
				 String nodeName = n.selectSingleNode("./name").getText();
				 //TODO regex for case insensitivity.
				 if ("Environment".equals(nodeName)) {
					 List<Node> environmentAttributes = n.selectNodes("./location");
					 for (Node m : environmentAttributes) {
						 if (m.selectSingleNode("./name").getText().startsWith("Node")) {
							 nodeNames.add(m.selectSingleNode("./name").getText());
						 }
					 }
				} 
			 }
		 }
		return nodeNames;
	}
	
	public void removeAttackerTemplate() throws IOException {
		//Get the UPPAAL content.
		Node node = document.selectSingleNode(attackerTemplatePath);
		
		//Remove the node
		node.detach();
		
	}
	
	
	public int getStartingNodeIndex() {
		//Get the attacker template from the XML
		Node attacker = document.selectSingleNode(attackerTemplatePath
				+"/location[1]"); //The first location would be the starting point!
		
		//Get the ID attribute as a string
		String idString = attacker.valueOf("@id");
		
		//Return the ID number (first two characters are always "id") then the number
		return Integer.valueOf(idString.substring(2, idString.length()));
	}
	
	public void amendLocationSet(String name) {
		//Get all NTA child node into a list
		Element dec = (Element) document.selectSingleNode("/nta/declaration");
		Element nta = (Element) document.selectSingleNode("/nta");
		dec.setText(dec.getText()+"\n bool in"+name+";");
		try {
			this.refresh();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	public List<Node> getTemplates() {
		return document.selectNodes("/nta/template");
	}
	
	//Getters and Setters
	public Document getDocument() {return document;}
	
	public void refresh() throws IOException {
		Writer writer = new OutputStreamWriter(new FileOutputStream(path), "UTF-8");//Convert file output stream to writer object
	 	document.write(writer);//Write DOM model built in memory to XML file
	 	writer.close();//Close output stream
	}
	
}
