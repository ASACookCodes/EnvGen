package generator;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import org.dom4j.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

public class Main implements ActionListener{
	
	private static JFrame f;
	private static JButton load;
	private static JButton generate;
	private static JButton queryLoad;
	private static JButton analyse;
	private static JButton addNode;
	private static JButton delNode;
	private static JButton instantiate;
	private static JProgressBar progress;
	private static JTextArea fauxConsole;
	private static JScrollPane scroller;
	private static JScrollBar vertical;
	private static JCheckBox attackTraceChoice;
	
	private static File uppaalTemplate;
	private static File queryFile;
	private static File envFolder;
	private static File traceFolder;
	private static String templateName;
	private static final String envPath = "envs";
	private static final String tracesPath = envPath+"/traces";
	private static Object[] queryFolders;
	
	private static FileWriter writer;
	private static String[] possibleEnvs;
	private static String systemDeclarations = "";
	private static int environments;
	private static int overlaps;
	private static int nodes;
	private static ArrayList<String> locationList;
	private static ArrayList<String> queryNames;
	private static String[] locations;
	private static XMLFactory xmlWorker;
	private static boolean loaded;
	private static boolean attackTraces;
	
	
	//User Interface
	Main(){
		JPanel buttonPane = new JPanel();
		f = new JFrame();
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		load = new JButton("Select UPPAAL Template");
		load.setBounds(50,100,100,40); //x,y,width,height
		load.addActionListener(this);
		buttonPane.add(load);
		
		instantiate = new JButton("Insantiate Scenario");
		instantiate.setEnabled(false);
		instantiate.setBounds(50,100,100,40);
		instantiate.addActionListener(this);
		buttonPane.add(instantiate);
//		addNode = new JButton("Add Node");
//		addNode.setEnabled(false);
//		addNode.setBounds(50,100,100,40);
//		addNode.addActionListener(this);
//		buttonPane.add(addNode);
//		
//		delNode = new JButton("Remove Node");
//		delNode.setEnabled(false);
//		delNode.setBounds(50,100,100,40);
//		delNode.addActionListener(this);
//		buttonPane.add(delNode);
		
		queryLoad = new JButton("Select Query File");
		queryLoad.setBounds(200, 100, 100, 40);
		queryLoad.addActionListener(this);
		queryLoad.setEnabled(false);
		buttonPane.add(queryLoad);
		
		generate = new JButton("Generate");
		generate.setEnabled(false);
		generate.setBounds(50,200,100,40);
		generate.addActionListener(this);
		buttonPane.add(generate);
		
		analyse = new JButton("Analyse");
		analyse.setEnabled(false);
		analyse.setBounds(200, 200, 100, 40);
		analyse.addActionListener(this);
		buttonPane.add(analyse);
		
		attackTraceChoice = new JCheckBox("Prune Duplicate Attack Traces");
		attackTraceChoice.addActionListener(this);
		buttonPane.add(attackTraceChoice);
		
		progress = new JProgressBar();
		progress.setValue(0);
		
		fauxConsole = new JTextArea();
		fauxConsole.setLineWrap(true);
		fauxConsole.setWrapStyleWord(true);
		fauxConsole.setBorder(new LineBorder(Color.BLACK));
		
		scroller = new JScrollPane();
		scroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		scroller.getViewport().setBackground(Color.WHITE);
		scroller.getViewport().add(fauxConsole,BorderLayout.EAST);
		vertical = scroller.getVerticalScrollBar();
		
		f.add(buttonPane, BorderLayout.NORTH);
		f.add(progress, BorderLayout.SOUTH);
		f.add(scroller, BorderLayout.CENTER);
		
		f.setSize(800,500);
		f.repaint();
		f.setExtendedState(JFrame.MAXIMIZED_BOTH); 
		f.setVisible(true);
		
	}
	
	/**
	 * Main Method
	 * @param args command line inputs
	 * @throws IOException
	 * @throws DocumentException
	 * @throws InterruptedException
	 */
	public static void main(String[] args) throws IOException, DocumentException, InterruptedException {	
		
		//Folder for the environments
		envFolder = new File(envPath);
		envFolder.mkdirs();
		
		//Put traces in same named folder
		traceFolder = new File(tracesPath);
		traceFolder.mkdirs();
		
		//Wipe the folders clean
		wipeFolder(envFolder);
		
		//Show UI
		new Main();
	}
	
	
	
	/**
	 * Cleans a directory of files, simple solution using no external libs
	 * @param folderName the folder to clean
	 */
	private static void wipeFolder(File folderName) {
		for(File file: folderName.listFiles()) {
			if (!file.isDirectory()) {
				file.delete();
			}
		}
	}
	
	
	
	
	/**
	 * Display the nodes in the UI console.
	 */
	private static void displayNodes() {
		//Display nodes on console.
		fauxConsole.append("Nodes: ");
		for (String location : locationList) {
			fauxConsole.append(location+" ");
		}
		fauxConsole.append("\n");
	}
	
	
	
	
	/**
	 * Loads an UPPAAL template.
	 * @throws DocumentException 
	 * @throws IOException 
	 */
	private static void loadTemplate() throws IOException, DocumentException {
		FileNameExtensionFilter f = new FileNameExtensionFilter("xml", "xml"); //Only allow XML to be loaded
		JFileChooser choose = new JFileChooser();
		choose.setFileFilter(f);
		
		int returnVal = choose.showOpenDialog(null);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			uppaalTemplate = choose.getSelectedFile();
			fauxConsole.append("UPPAAL Template: "+uppaalTemplate.getName()+"\n");
			queryLoad.setEnabled(true);
		
			//Get the user provide names of the locations
			XMLFactory xmlf = new XMLFactory(uppaalTemplate);
			locationList = xmlf.getNodeNames();
			
			displayNodes();
			
		}
		//Ready for the user to add nodes.
//		addNode.setEnabled(true);
//		delNode.setEnabled(true);
		instantiate.setEnabled(true);
	}
	
	
	
	
	
	/**
	 * Add nodes
	 * @throws DocumentException 
	 * @throws IOException 
	 */
	private static void addNode() throws IOException, DocumentException {
		
		//User input
		String newNodes = JOptionPane.showInputDialog(f, "Enter the names of nodes (separated by commas)");
		if (newNodes != null) { //If the user entered something
			//Sanitise the commas out and throw into an array
			newNodes = newNodes.replaceAll("\\s", "");
			String[] newNodesArray = newNodes.split(",");
			
			XMLFactory t = new XMLFactory(uppaalTemplate);
			
			//Add each entered node to the list of nodes.
			for (String n : newNodesArray) {
				locationList.add(n);
				//Update the global declarations
				t.amendLocationSet(n);
			}
			displayNodes();
		}
	}
	
	
	/**
	 * Removes a node
	 * TODO remove the global declaration of the form "in[Nodename]"
	 */
	private static void removeNode() {
		String removeNode = JOptionPane.showInputDialog(f, "Enter the name of node.");
		if (removeNode != null) { //If user entered something
			if (locationList.contains(removeNode)) { //If node exists
				locationList.remove(removeNode);
				fauxConsole.append("Node removed. \n");
			} else {
				fauxConsole.append("Node not found! \n");
			}
		}
		
	}
	
	
	
	
	/**
	 * Loads a query file
	 * @throws FileNotFoundException 
	 */
	private static void loadQuery() throws FileNotFoundException {
		FileNameExtensionFilter f = new FileNameExtensionFilter("UPPAAL Query Files", "q");
		JFileChooser choose = new JFileChooser();
		choose.setFileFilter(f);
		
		//File Selection
		int returnVal = choose.showOpenDialog(null);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			queryFile = choose.getSelectedFile();
			fauxConsole.append("Query File: "+queryFile.getName()+"\n");
			
			
			//Gather the names of the queries
			//TODO Cases where the query has no name!
			queryNames = new ArrayList<String>();
			Scanner scanner = new Scanner(new File(queryFile.getAbsolutePath()));
			while (scanner.hasNextLine()) {
				   String line = scanner.nextLine();
				   //If the does not contain query quantifiers, then it's the name of the query.
				   if (!line.contains("E<>") &&
					  !line.contains("E[]") &&
					  !line.contains("A[]") &&
					  !line.contains("A<>") &&
					  !line.contains("-->") &&
					  !line.contains("//") &&
					  !line.contains("/*") &&
					  !line.contains("*/") &&
					  !line.equals("")
					  ) 
				   {
					   queryNames.add(line);
				   }
			}
			
			//Display queries
			fauxConsole.append("Queries: \n");
			for (String s : queryNames) {
				fauxConsole.append(s+"\n");
			}
		}
	}
	
	
	
	/**
	 * Kuratowski's theorem states that there can be no complete planar graphs with > 4 nodes.
	 * Complete graphs of 4 nodes requires 6 edges. For us, edges represent overlaps.
	 * Therefore, we exclude environments that have more than SIX overlaps. 
	 * @param possibleEnvironment
	 * @return
	 */
	private static boolean kuratowskiCheck(String possibleEnvironment) {
		char overlapOccurence = '1';
		int count = 0;
		int kuratowskiEdgeMax = 6;
		//Imperative method for searching for 1's in a string.
		for (int i = 0; i < possibleEnvironment.length(); i++) {
		    if (possibleEnvironment.charAt(i) == overlapOccurence) {
		        count++;
		    }
		}
		if (count>kuratowskiEdgeMax) {return false;}
		return true;
	}
	
	
	
	/**
	 * Method for generating environments. 
	 * @throws IOException
	 * @throws DocumentException
	 * @throws InterruptedException
	 */
	private static void generateEnvironments() throws IOException, DocumentException, InterruptedException {
		
		//Add all locations to the official static array field
		locations = locationList.toArray(new String[locationList.size()]);
		
		//Number of possible overlaps given by the handshake lemma
		nodes = locations.length;
		overlaps = nodes*(nodes-1)/2;
		
		//Number of possible environments
		environments = (int) Math.pow(2, overlaps); 
		progress.setMaximum(environments);
		possibleEnvs = new String[environments];
		
		templateName = uppaalTemplate.getName().substring(0, uppaalTemplate.getName().indexOf('.'));	
		writer = new FileWriter("verify"+templateName+".bat");
		
		//Create each environment and write it to an XML file
		for (int i=0; i<environments; i++) {
		
			xmlWorker = new XMLFactory(uppaalTemplate);
				
			//Get the starting position
			int startingPos = xmlWorker.getStartingNodeIndex();
					
			//Removing the already existing attacker.
			//xmlWorker.removeAttackerTemplate();
			Document doc = xmlWorker.getDocument();
					
			//0-padded binary string 
			String possibleEnvironment = String.format("%"+overlaps+"s", Integer.toBinaryString(i)).replace(' ', '0');
			possibleEnvs[i] = possibleEnvironment;
			if (kuratowskiCheck(possibleEnvironment)) {

				//Create the environment
				EnvironmentCreator environment = new EnvironmentCreator(doc, startingPos, 100, nodes, locations, systemDeclarations);
				
				//Write the environment to a numbered XML file
				environment.WriteDoc("envs/environment"+i+".xml",possibleEnvironment);
				
				//Write the verification execution command to the batch file
				writer.write("verifyta -t 1 -f envs/traces/environment"+i+" envs/environment"+i+".xml "+"\""+queryFile.getAbsolutePath()+"\" \n");
				progress.setValue(progress.getValue()+1); // Update progress bar
			}
		}
		Thread.sleep(100); //Short pause
		progress.setValue(0); //Return to 0
		writer.close(); //Close
	}
	
	
	
	
	
	/**
	 * Method for analysing the generated environments. Calls the batch file which in turn calls
	 * the verifyta.exe UPPAAL verifier.
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private static void analyse() throws IOException, InterruptedException {
		//Clean the folder of old traces
		wipeFolder(traceFolder);
		
		//Get location of analysis batch file
		String batchFileName = "verify"+templateName+".bat";
		String batchLocation = System.getProperty("user.dir")+"/"+batchFileName;
		
		//Run the analysis through CMD
		//TODO cross-platform!
		Runtime runtime = Runtime.getRuntime();
		Process p = runtime.exec("cmd /c "+batchLocation);
		
		//For fauxConsole, read the CMD output to fauxConsole through InputStream
		InputStream is = p.getInputStream();
		int i = 0;
		while( (i = is.read() ) != -1) { //While p is running (-1 is returned at end of stream)
			//TODO update progress bar (somehow)
			fauxConsole.append(String.valueOf( (char) i )); //Update console.
			vertical.setValue(vertical.getMaximum()); //Autoscroll to bottom.	
		}
		
		//Output completion message when process terminates.
		if (p.waitFor()==0) {
			fauxConsole.append("Search Complete! \n"); //Update console.
			vertical.setValue(vertical.getMaximum()); //Autoscroll to bottom.
			//Remove the batch file
			File batchFile = new File(batchFileName);
			batchFile.delete();
		}
		
		if (attackTraces) { //If user checked box pruning attack traces.
			fauxConsole.append("Removing duplicate attack traces...");
			vertical.setValue(vertical.getMaximum());
			
			//Prune attack traces if checkbox checked
			pruneDuplicateTraces();
			
			fauxConsole.append("Finished removing duplicate attack traces.");
			vertical.setValue(vertical.getMaximum());
		}
		
		//Collect query numbers off of the filenames of the resulting trace files
		File[] results = new File(tracesPath).listFiles();
		
		//Prepare Folders for query names
		queryFolders = queryNames.toArray();
		for (int j=0; j<queryFolders.length; j++) {
			queryFolders[j] = tracesPath+"/"+queryFolders[j];
			File queryFolder = new File(queryFolders[j].toString());
			queryFolder.mkdirs();
		}
		
		//Get the query number from the trace file and move to the correct folder
		for (File f : results) {
			Integer queryNumber = Integer.valueOf(f.getName().substring(f.getName().indexOf("-")+1, f.getName().indexOf("-")+2));
			f.renameTo(new File(queryFolders[queryNumber-1].toString()+"/"+f.getName()));
		}
		
		
	}
	
	
	
	
	/**
	 * Remove attack traces that are the same
	 * O(n^2), not scalable for a very in depth analysis with large numbers of attack traces
	 */
	private static void pruneDuplicateTraces() {
		File[] attackTraces = new File(tracesPath).listFiles();
		for (int i = 0; i<attackTraces.length; i++) {
			for (int j = 1; j<attackTraces.length-1; j++) {
				File file1 = attackTraces[i];
				File file2 = attackTraces[j];
				if (i!=j) {
					if (checkBinaryEquality(file1, file2)) {
						file2.delete();
					}	
				}
				
			}
		}
	}
	
	
	/**
	 * Compares two files by checking their bytes
	 * @param file1
	 * @param file2
	 * @return true or false
	 */
	private static boolean checkBinaryEquality(File file1, File file2) {
		if (file1.length() != file2.length()) return false; //different length
		try(FileInputStream f1 = new FileInputStream(file1); FileInputStream f2 = new FileInputStream(file2)){
            byte bus1[] = new byte[1024],
                 bus2[] = new byte[1024];
            // comparing files bytes one by one if we found unmatched results that means they are not equal
            while((f1.read(bus1)) >= 0) {
                f2.read(bus2);
                for(int i = 0; i < 1024;i++)
                    if(bus1[i] != bus2[i]) 
                        return false;
            }
            // passed
            return true;
    } catch (IOException exp) {
        // problems occurred so let's consider them not equal
        return false;
    }
	}
	
	
	
	private static void instantiate() {
		
		final JPanel initiatorPanel = new JPanel();
		final JPanel responderPanel = new JPanel();
		final JPanel attackerPanel = new JPanel();
		final JPanel scPanel = new JPanel();
        final JRadioButton initiatorHubButton = new JRadioButton("Hub");
        final JRadioButton initiatorNodeButton = new JRadioButton("Node");
        final JRadioButton responderHubButton = new JRadioButton("Hub");
        final JRadioButton responderNodeButton = new JRadioButton("Node");
        final JRadioButton attackerHubButton = new JRadioButton("Hub");
        final JRadioButton attackerNodeButton = new JRadioButton("Node");
        final JRadioButton yesButton = new JRadioButton("Yes");
        final JRadioButton noButton = new JRadioButton("No");
        final JTextField initiatorName = new JTextField("", 20);
        final JTextField responderName = new JTextField("", 20);
        final JTextField attackerName = new JTextField("", 20);
        final JLabel initprompt = new JLabel("Initiator Name?");
        final JLabel resprompt = new JLabel("Responder Name?");
        final JLabel attackerprompt = new JLabel("Attacker Name?");
        final JLabel scprompt = new JLabel("Scenario Choice?");

        initiatorPanel.add(initprompt);
        initiatorPanel.add(initiatorHubButton);
        initiatorPanel.add(initiatorNodeButton);
        initiatorPanel.add(initiatorName);
        
        responderPanel.add(resprompt);
        responderPanel.add(responderHubButton);
        responderPanel.add(responderNodeButton);
        responderPanel.add(responderName);
        
        attackerPanel.add(attackerprompt);
        attackerPanel.add(attackerHubButton);
        attackerPanel.add(attackerNodeButton);
        attackerPanel.add(attackerName);

        scPanel.add(scprompt);
        scPanel.add(yesButton);
        scPanel.add(noButton);
        
        locations = locationList.toArray(new String[locationList.size()]);
        
        String initiatorAgent = "";
        String responderAgent = "";
        String attackerAgent = "";
        
        //Initiator instantiation
        JOptionPane.showMessageDialog(null, initiatorPanel);
        String initiator = initiatorName.getText();
        if (initiatorHubButton.isSelected()) {
        	initiateHubOrNode(initiator,"hub","Initiator");
        	initiatorAgent = initiator+"Agent";
        	initiator+="Brain";
        } else if (initiatorNodeButton.isSelected()) {
        	initiateHubOrNode(initiator,"node","Initiator");
        	initiator+="Brain";
        } else {
        	fauxConsole.append("No principal type selected!");
        }
        
        //Responder instantiation
        JOptionPane.showMessageDialog(null, responderPanel);
        String responder = responderName.getText();
        if (responderHubButton.isSelected()) {
        	initiateHubOrNode(responder,"hub","Responder");
        	responderAgent = responder+"Agent";
        	responder+="Brain";
        } else if (responderNodeButton.isSelected()) {
        	initiateHubOrNode(responder,"node","Responder");
        	responder+="Brain";
        } else {
        	fauxConsole.append("No principal type selected!");
        }
        
        //Attacker instantiation
        JOptionPane.showMessageDialog(null, attackerPanel);
        String attacker = attackerName.getText();
        if (attackerHubButton.isSelected()) {
        	initiateHubOrNode(attacker,"hub","Attacker");
        	attackerAgent = attacker+"Agent";
        	attacker+="Brain";
        } else if (attackerNodeButton.isSelected()) {
        	initiateHubOrNode(attacker,"node","Attacker");
        	attacker+="Brain";
        } else {
        	fauxConsole.append("No principal type selected!");
        }
        
        JOptionPane.showMessageDialog(null, scPanel);
        if (yesButton.isSelected()) {
        	systemDeclarations += "sc = ScenarioChoice();";
        }
        
        systemDeclarations += 
        		"system "+attacker+","+attackerAgent+","+
        					initiator+","+initiatorAgent+","+
        					responder+","+responderAgent+"sc;";
        
		generate.setEnabled(true);
	}
	
	
	/**
	 * Writes a line(s) which instantiates a principal as a node or hub in the system declarations
	 * @param name
	 * @param type
	 * @param principal
	 */
	private static void initiateHubOrNode(String name, String type, String principal) {
		if (type == "hub") {
			systemDeclarations += name+"Agent = Environment(";
	    	for (int i=0; i<locations.length; i++) {
	    		if (i == locations.length-1) {
	    			systemDeclarations += name+"In"+locations[i];
	    		} else {
	    			systemDeclarations += name+"In"+locations[i]+",";
	    		}
	    	}
	    	systemDeclarations += ");"+name+"Brain = "+principal+"(";
        	for (int i=0; i<locations.length; i++) {
        		if (i == locations.length-1) {
        			systemDeclarations += name+"In"+locations[i];
        		} else {
        			systemDeclarations += name+"In"+locations[i]+",";
        		}
        	}
        	systemDeclarations += ");";
	    	
		}
		
		if (type == "node") {
			systemDeclarations += name+="Brain = "+principal+"(";
			for (int i=0; i<locations.length; i++) {
        		if (i == locations.length-1) {
        			systemDeclarations += "isNode";
        		} else {
        			systemDeclarations += "isNode,";
        		}
        	}
        	systemDeclarations += ");";
		}
		
	}
	

	@Override
	/**
	 * UI action listener.
	 */
	public void actionPerformed(ActionEvent e) { //Event handler
		Object src = e.getSource();
		
		if (src == (Object) attackTraceChoice) {
			if (attackTraceChoice.isSelected()) {
				attackTraces=true;
			}
			if (!attackTraceChoice.isSelected()) {
				attackTraces=false;
			}
		}
		
		//If Select UPPAAL Template Button pressed.
		if (src == (Object) load) {
			try {loadTemplate();} catch (IOException | DocumentException e1) 
			{
				e1.printStackTrace();
			}	
		}
		
		if (src == instantiate) {
			instantiate();
		}
		
		//If Add Node Button Pressed
		if (src == addNode) {
			try {addNode();} catch (IOException | DocumentException e1) 
			{
				e1.printStackTrace();
			}
		}
		
		if (src == delNode) {
			removeNode();
		}
		
		
		//If Select Query Button Pressed
		if (src == queryLoad) {
			try {loadQuery();} catch (FileNotFoundException e1) 
			{
				e1.printStackTrace();
			}
		}
		
		
		if (src == generate) {
			//Used seperate thread so that progress bar can be updated on each generation.
			Runnable runner = new Runnable() {
				public void run() {
					try {generateEnvironments();} catch (IOException | DocumentException | InterruptedException e) 
					{
						e.printStackTrace();
					}
					//Success message
					fauxConsole.append("Environments created successfully. \n");
					analyse.setEnabled(true);
				}
			};
			//Create new "generation" thread and start it.
			Thread t = new Thread(runner, "Environment Generator");
			t.start();
		}
		
		
		//If Analyse Button Pressed
		if (src == analyse) {
				//Runnable object for multithreading (for ease of user interface)
				Runnable runner = new Runnable() {
				public void run() {
						try {analyse();} catch (IOException | InterruptedException e)
						{
							e.printStackTrace();
						}
				}
			};
			//Create new "analysis" thread and start it.
			Thread t = new Thread(runner, "Analysis");
			t.start();
		}	
	}
}
	
	
	

	

