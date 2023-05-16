package org.matsim.project;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class AssignLinkToCounts {
	
	public static void main(String[] args) {
		//Config config = ConfigUtils.loadConfig( "/home/geompr/Documents/Leeds/RAIM/code/RAIM/matsim-raim/scenarios_v2/monday_2022_config_10pct.xml");
		Config config = ConfigUtils.loadConfig( "/home/geompr/Documents/Leeds/RAIM/code/RAIM/matsim-raim/scenarios_v1/winnipeg/2022/input/weekday/weekday_2022_config_25pct.xml");
		Scenario scenario = ScenarioUtils.loadScenario(config) ;
		
		try { 
			for (int i=1;i<=7;i++) {
				//File file = new File("/home/geompr/Documents/Leeds/RAIM/code/RAIM/data/westmidlands/TfWM/counts_day"+i+".xml");  
				File file = new File("/home/geompr/Documents/Leeds/RAIM/code/RAIM/data/winnipeg/DATA/Open_data_winnipeg/counts_day"+i+".xml");  
				DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();  
				DocumentBuilder db = dbf.newDocumentBuilder();  
				Document doc = db.parse(file);  
				doc.getDocumentElement().normalize();
				System.out.println("Root element: " + doc.getDocumentElement().getNodeName());
				
				NodeList nodeList = doc.getElementsByTagName("count");   
				ArrayList<String> li=new ArrayList<String>();
				for (int itr = 0; itr < nodeList.getLength(); itr++)   {  
					Node node = nodeList.item(itr);  
					if (node.getNodeType() == Node.ELEMENT_NODE)   {  
						Element eElement = (Element) node;  
						System.out.println("id: "+ eElement.getAttribute("cs_id"));  
						float x = Float.parseFloat(eElement.getAttribute("x"));
						float y = Float.parseFloat(eElement.getAttribute("y"));
						System.out.println(x+ "  "+y);
						
						Link l = NetworkUtils.getNearestLinkExactly(scenario.getNetwork(), new Coord(x, y));
						String link_id = l.getId().toString();
						
						// Deal with duplicated links manually
//						if(eElement.getAttribute("cs_id").equals("SAAN0029"))
//							link_id = "252320";
//						else if(eElement.getAttribute("cs_id").equals("BMAN0083"))
//							link_id = "5941";
//						else if(eElement.getAttribute("cs_id").equals("BMAN0070"))
//							link_id = "198323";
//						else if(eElement.getAttribute("cs_id").equals("BMAN0041"))
//							link_id = "264244";
//						else if(eElement.getAttribute("cs_id").equals("BMAN0035"))
//							link_id = "103843";
//						else if(eElement.getAttribute("cs_id").equals("BMAN0033"))
//							link_id = "195017";
	
						eElement.setAttribute("loc_id", link_id);
						li.add(link_id);
					}    
				}
				 HashSet<String> hCheckSet = new HashSet<>();
			      HashSet<String> hTargetSet = new HashSet<>();
			      for (String s : li) {
			         if(!hCheckSet.add(s)) {
			            hTargetSet.add(s);
			         }
			      }
			      System.out.println("Duplicate integers in given list is/are " + hTargetSet);
			   
			      
			      //try (FileOutputStream output = new FileOutputStream("/home/geompr/Documents/Leeds/RAIM/code/RAIM/data/westmidlands/counts_day"+i+"_.xml")) {
				  try (FileOutputStream output = new FileOutputStream("/home/geompr/Documents/Leeds/RAIM/code/RAIM/data/winnipeg/DATA/Open_data_winnipeg//__counts_day"+i+"_.xml")) {
	             writeXml(doc, output);
	         }
	
	      catch (Exception e) {
	         e.printStackTrace();
	     }
			}
		}
		catch (Exception e)   {  
			e.printStackTrace();  
		}  
	}   

	// write doc to output stream
	private static void writeXml(Document doc,OutputStream output)
	        throws TransformerException, UnsupportedEncodingException {
	
	    TransformerFactory transformerFactory = TransformerFactory.newInstance();
	    Transformer transformer = transformerFactory.newTransformer();
	
	    DOMSource source = new DOMSource(doc);
	    StreamResult result = new StreamResult(output);
	
	    transformer.transform(source, result);

	}
}
