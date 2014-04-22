package org.eder.owcs;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

//TODO tratar itens postUpdate são jsps tb
public class DependencyHelper {
	public static void main(String[] args) {
		runApp(args);
	}
	
	private static void runApp(String[] args){
		if(args.length != 2 && args.length != 4){
			System.out.println("Available commands:\n-f <jsp filePath> [-uses | -usedby <element name>]\n"
					+ "-d <jsp dirPath> [-uses | -usedby <element name>]");
		}
		else{
			DirectedGraph<String, DefaultEdge> g = new DefaultDirectedGraph<String, DefaultEdge>(DefaultEdge.class);
			if("-f".equals(args[0]))
				g = calculateDependenciesFromSingleElement(args[1]);
			else if ("-d".equals(args[0]))
				g = calculateDependenciesFromElementsFolder(args[1]);
			if(args.length == 2)
				showAllDependencies(g);
			else{
				if("-uses".equals(args[2]))
					showDependentsOf(args[3], g);
				else if ("-usedby".equals(args[2]))
					showElementsUsedBy(args[3], g);
			}
		}
	}
	
	public static List<String> getElementsUsedBy(String element, DirectedGraph<String, DefaultEdge> g){
		List<String> elements = new ArrayList<>();
		if(!g.vertexSet().isEmpty()){
			for(DefaultEdge edge : g.outgoingEdgesOf(element)){
				elements.add(g.getEdgeTarget(edge));
			}
		}
		return elements;
	}
	
	public static void showElementsUsedBy(String element, DirectedGraph<String, DefaultEdge> g){
		if(g.vertexSet().isEmpty())
			System.out.println("No references to owcs elements found.");
		else{
			System.out.print(element+" uses: ");
			if(g.outgoingEdgesOf(element).isEmpty())
				System.out.println("<no dependency found>");
			else{
				System.out.println();
				for(DefaultEdge edge : g.outgoingEdgesOf(element)){
					System.out.println("=> " + g.getEdgeTarget(edge));
				}
			}
		}
	}
	
	public static List<String> getDependentsOf(String element, DirectedGraph<String, DefaultEdge> g){
		List<String> elements = new ArrayList<>();
		if(!g.vertexSet().isEmpty()){
			for(DefaultEdge edge : g.incomingEdgesOf(element)){
				elements.add(g.getEdgeSource(edge));
			}
		}
		return elements;
	}
	
	public static void showDependentsOf(String element, DirectedGraph<String, DefaultEdge> g){
		if(g.vertexSet().isEmpty())
			System.out.println("No references to owcs elements found.");
		else{
			System.out.print(element+" used by: ");
			if(g.incomingEdgesOf(element).isEmpty())
				System.out.println("<no dependency found>");
			else{
				System.out.println();
				for(DefaultEdge edge : g.incomingEdgesOf(element)){
					System.out.println("=> " + g.getEdgeSource(edge));
				}
			}
		}
	}

	public static void showAllDependencies(DirectedGraph<String, DefaultEdge> g){
		if(g.vertexSet().isEmpty())
			System.out.println("No references to owcs elements found.");
		else{
			Iterator<String> vertexIt = g.vertexSet().iterator();
			while(vertexIt.hasNext()){
				String vertex = vertexIt.next();
				System.out.print(vertex+" uses: ");
				if(g.outgoingEdgesOf(vertex).isEmpty())
					System.out.println("<no dependency found>");
				else{
					System.out.println();
					for(DefaultEdge edge : g.outgoingEdgesOf(vertex)){
						System.out.println("=> " + g.getEdgeTarget(edge));
					}
				}
				System.out.println();
			}
		}
	}
	
	public static DirectedGraph<String,DefaultEdge> calculateDependenciesFromSingleElement(String filePath){
		String inputFile = getFile(filePath);
		if(inputFile != null){
			String parentNode = getElementName(filePath);
			Set<String> elements = findOWCSElements(removeComments(inputFile),true);
			return generateDependencyGraph(parentNode, elements);
		}
		return null;
	}
	
	public static DirectedGraph<String,DefaultEdge> calculateDependenciesFromElementsFolder(String directoryPath){
		DirectedGraph<String, DefaultEdge> g =
	            new DefaultDirectedGraph<String, DefaultEdge>(DefaultEdge.class);
		Set<String> jspFiles = getAllJspFiles(directoryPath);
		if(!jspFiles.isEmpty()){
			Iterator<String> it = jspFiles.iterator();
			while(it.hasNext()){
				String element = it.next();
				String parentNode = getElementName(element);
				Set<String> elements = findOWCSElements(removeComments(getFile(element)),true);
				g = generateDependencyGraph(parentNode, elements, g);
			}
			return g;
		}
		return null;
	}
	
	private static DirectedGraph<String, DefaultEdge> generateDependencyGraph(String parentNode, Set<String> childNodes){
		DirectedGraph<String, DefaultEdge> g =
	            new DefaultDirectedGraph<String, DefaultEdge>(DefaultEdge.class);
		return generateDependencyGraph(parentNode, childNodes, g);
	}
	
	private static DirectedGraph<String, DefaultEdge> generateDependencyGraph(String parentNode, Set<String> childNodes, DirectedGraph<String, DefaultEdge> g){
		g.addVertex(parentNode);
		Iterator<String> iterator = childNodes.iterator();
		while(iterator.hasNext()){
			String child = iterator.next();
			g.addVertex(child);
			g.addEdge(parentNode, child);
		}
		//System.out.println(g.toString());
		return g;
	}
	
	private static String getElementName(String jspFileName) {
		String regex = ".*[\\\\/](?<element>.+).jsp";
		
		Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(jspFileName);

		return matcher.find() ? matcher.group("element") : null;
	}
	
	private static Set<String> findOWCSElements(String src, boolean shortName) {
		//Old version: (?<element>[\\w\\\\/]+)
		String regex1 = "(<\\s*render:callelement(\\s|\\w|\"|'|-|\\(|\\)|\\.|=|<%|%>)*elementname\\s*=\\s*[\"'](?<fullelement>[\\\\/]{0,1}(\\w+[\\\\/])*(?<element>[\\w]+))[\"'])";
		String regex2 = "(<\\s*render:calltemplate(\\s|\\w|\"|'|-|\\(|\\)|\\.|=|<%|%>)*tname\\s*=\\s*[\"'](?<fullelement>[\\\\/]{0,1}(\\w+[\\\\/])*(?<element>[\\w]+))[\"'])";
		String regex3 = "(<\\s*render:getpageurl(\\s|\\w|\"|'|-|\\(|\\)|\\.|=|<%|%>)*pagename\\s*=\\s*[\"'](?<fullelement>[\\\\/]{0,1}(\\w+[\\\\/])*(?<element>[\\w]+))[\"'])";
		String regex4 = "(<\\s*render:gettemplateurl(\\s|\\w|\"|'|-|\\(|\\)|\\.|=|<%|%>)*tname\\s*=\\s*[\"'](?<fullelement>[\\\\/]{0,1}(\\w+[\\\\/])*(?<element>[\\w]+))[\"'])";
		String regex5 = "(<\\s*render:gettemplateurlparameters(\\s|\\w|\"|'|-|\\(|\\)|\\.|=|<%|%>)*tname\\s*=\\s*[\"'](?<fullelement>[\\\\/]{0,1}(\\w+[\\\\/])*(?<element>[\\w]+))[\"'])";
		String regex6 = "(<\\s*insite:calltemplate(\\s|\\w|\"|'|-|\\(|\\)|\\.|=|<%|%>)*tname\\s*=\\s*[\"'](?<fullelement>[\\\\/]{0,1}(\\w+[\\\\/])*(?<element>[\\w]+))[\"'])";
		String regex7 = "(<\\s*ics:callelement(\\s|\\w|\"|'|-|\\(|\\)|\\.|=|<%|%>)*element\\s*=\\s*[\"'](?<fullelement>[\\\\/]{0,1}(\\w+[\\\\/])*(?<element>[\\w]+))[\"'])";
		String regex8 = "(<\\s*satellite:page(\\s|\\w|\"|'|-|\\(|\\)|\\.|=|<%|%>)*pagename\\s*=\\s*[\"'](?<fullelement>[\\\\/]{0,1}(\\w+[\\\\/])*(?<element>[\\w]+))[\"'])";

		Set<String> elements = new HashSet<String>();
		Pattern pattern;
		Matcher matcher;

		pattern = Pattern.compile(regex1, Pattern.CASE_INSENSITIVE);
		matcher = pattern.matcher(src);
		while (matcher.find()) {
			//System.out.println("Captured sequence1: " + matcher.group(shortName?"element":"fullelement"));
			elements.add(matcher.group(shortName?"element":"fullelement"));
		}

		pattern = Pattern.compile(regex2, Pattern.CASE_INSENSITIVE);
		matcher = pattern.matcher(src);
		while (matcher.find()) {
			//System.out.println("Captured sequence2: " + matcher.group(shortName?"element":"fullelement"));
			elements.add(matcher.group(shortName?"element":"fullelement"));
		}

		pattern = Pattern.compile(regex3, Pattern.CASE_INSENSITIVE);
		matcher = pattern.matcher(src);
		while (matcher.find()) {
			//System.out.println("Captured sequence3: " + matcher.group(shortName?"element":"fullelement"));
			elements.add(matcher.group(shortName?"element":"fullelement"));
		}

		pattern = Pattern.compile(regex4, Pattern.CASE_INSENSITIVE);
		matcher = pattern.matcher(src);
		while (matcher.find()) {
			//System.out.println("Captured sequence4: " + matcher.group(shortName?"element":"fullelement"));
			elements.add(matcher.group(shortName?"element":"fullelement"));
		}

		pattern = Pattern.compile(regex5, Pattern.CASE_INSENSITIVE);
		matcher = pattern.matcher(src);
		while (matcher.find()) {
			//System.out.println("Captured sequence5: " + matcher.group(shortName?"element":"fullelement"));
			elements.add(matcher.group(shortName?"element":"fullelement"));
		}

		pattern = Pattern.compile(regex6, Pattern.CASE_INSENSITIVE);
		matcher = pattern.matcher(src);
		while (matcher.find()) {
			//System.out.println("Captured sequence6: " + matcher.group(shortName?"element":"fullelement"));
			elements.add(matcher.group(shortName?"element":"fullelement"));
		}

		pattern = Pattern.compile(regex7, Pattern.CASE_INSENSITIVE);
		matcher = pattern.matcher(src);
		while (matcher.find()) {
			//System.out.println("Captured sequence7: " + matcher.group(shortName?"element":"fullelement"));
			elements.add(matcher.group(shortName?"element":"fullelement"));
		}

		pattern = Pattern.compile(regex8, Pattern.CASE_INSENSITIVE);
		matcher = pattern.matcher(src);
		while (matcher.find()) {
			//System.out.println("Captured sequence8: " + matcher.group(shortName?"element":"fullelement"));
			elements.add(matcher.group(shortName?"element":"fullelement"));
		}
		//System.out.println(elements);
		return elements;
	}
	
	private static String removeComments(String src){
		String regex = "(<%--(.(?!<%--))*--%>)";
		Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(src);
		String replaced = matcher.replaceAll("");
		//System.out.println(replaced);
		return replaced;
	}
	
	private static Set<String> getAllJspFiles(String directoryPath) {
		Set<String> jspFiles = new HashSet<>();
		File directory = new File(directoryPath);
		// get all the files from a directory
		File[] fList = directory.listFiles();
		for (File file : fList) {
			if (file.isFile() && file.getAbsolutePath().endsWith(".jsp")) {
				//System.out.println(getElementName(file.getAbsolutePath()));
				jspFiles.add(file.getAbsolutePath());
			} else if (file.isDirectory()) {
				jspFiles.addAll(getAllJspFiles(file.getAbsolutePath()));
			}
		}
		return jspFiles;
	}
	
	private static String getFile(String filePath) {
		File f = new File(filePath);
		StringBuilder sb = new StringBuilder();
		try (RandomAccessFile raf = new RandomAccessFile(f, "r")) {
			long flen = raf.length();

			if (flen > 0) {
				String text;
				while ((text = raf.readLine()) != null) {
					sb.append(text);
				}
			}
			// System.out.println(sb.toString());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return sb.toString();
	}
	
	/*TESTES*/
	@SuppressWarnings("unused")
	private static void testregex(String src) {
		String regex = "(<%--(\\s|\\w|\"|'|-|\\(|\\)|\\.|=|<%|%>|<|>|:|/)*--%>)";
		//System.out.println(src);
		
		Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(src);
		while (matcher.find()) {
			// System.out.print("Start index: " + matcher.start());
			// System.out.println(" | End index: " + matcher.end() + " ");
			System.out.println("Captured sequence: " + matcher.group("fullelement"));
		}
	}
	
	/***
	 * Uma única regex contemplando as regras para todas as tags a serem analizadas  
	 * */
	@SuppressWarnings("unused")
	private static void findOWCSElementsBenchmark(String src) {
		long starttime;
		long endtime;
		starttime = System.currentTimeMillis();
		
		String regex1 = "(<\\s*render:callelement(\\s|\\w|\"|'|-|\\(|\\)|\\.|=|<%|%>)*elementname\\s*=\\s*[\"'](?<element>[\\w\\\\/]+)[\"'])";
		String regex2 = "(<\\s*render:calltemplate(\\s|\\w|\"|'|-|\\(|\\)|\\.|=|<%|%>)*tname\\s*=\\s*[\"'](?<template>[\\w\\\\/]+)[\"'])";
		String regex3 = "(<\\s*render:getpageurl(\\s|\\w|\"|'|-|\\(|\\)|\\.|=|<%|%>)*pagename\\s*=\\s*[\"'](?<pageurl>[\\w\\\\/]+)[\"'])";
		String regex4 = "(<\\s*render:gettemplateurl(\\s|\\w|\"|'|-|\\(|\\)|\\.|=|<%|%>)*tname\\s*=\\s*[\"'](?<templateurl>[\\w\\\\/]+)[\"'])";
		String regex5 = "(<\\s*render:gettemplateurlparameters(\\s|\\w|\"|'|-|\\(|\\)|\\.|=|<%|%>)*tname\\s*=\\s*[\"'](?<templateurlparams>[\\w\\\\/]+)[\"'])";
		String regex6 = "(<\\s*insite:calltemplate(\\s|\\w|\"|'|-|\\(|\\)|\\.|=|<%|%>)*tname\\s*=\\s*[\"'](?<insitetemplate>[\\w\\\\/]+)[\"'])";
		String regex7 = "(<\\s*ics:callelement(\\s|\\w|\"|'|-|\\(|\\)|\\.|=|<%|%>)*element\\s*=\\s*[\"'](?<icselement>[\\w\\\\/]+)[\"'])";
		String regex8 = "(<\\s*satellite:page(\\s|\\w|\"|'|-|\\(|\\)|\\.|=|<%|%>)*pagename\\s*=\\s*[\"'](?<satellitepage>[\\w\\\\/]+)[\"'])";
		String regexFinal = regex1+"|"+regex2+"|"+regex3+"|"+regex4+"|"+regex5+"|"+regex6+"|"+regex7+"|"+regex8;
		
		Pattern pattern = Pattern.compile(regexFinal,Pattern.CASE_INSENSITIVE);
	    Matcher matcher = pattern.matcher(src);

	    while (matcher.find()) {
	    	String capturedSeq = null;
	    	if(matcher.group("element")!=null){
	    		capturedSeq = matcher.group("element"); 
	    	}
	    	else if (matcher.group("template")!=null){
	    		capturedSeq = matcher.group("template");
		    }
	    	else if (matcher.group("pageurl")!=null){
	    		capturedSeq = matcher.group("pageurl");
	    	}
	    	else if (matcher.group("templateurl")!=null){
	    		capturedSeq = matcher.group("templateurl");
	    	}
	    	else if (matcher.group("templateurlparams")!=null){
	    		capturedSeq = matcher.group("templateurlparams");
	    	}
	    	else if (matcher.group("insitetemplate")!=null){
	    		capturedSeq = matcher.group("insitetemplate");
	    	}
	    	else if (matcher.group("icselement")!=null){
	    		capturedSeq = matcher.group("icselement");
	    	}
	    	else if (matcher.group("satellitepage")!=null){
	    		capturedSeq = matcher.group("satellitepage");
	    	}
	      
	      //System.out.println("Captured sequence: " + capturedSeq);
	    }
	    endtime = System.currentTimeMillis();
		System.out.println("ElapsedTime: "+(endtime-starttime));
	}
	
	/**
	 * Uma regex para cada tag a ser analizada 
	 * */
	@SuppressWarnings("unused")
	private static void findOWCSElementsBenchmarkV2(String src) {
		long starttime;
		long endtime;
		starttime = System.currentTimeMillis();
		
		String regex1 = "(<\\s*render:callelement(\\s|\\w|\"|'|-|\\(|\\)|\\.|=|<%|%>)*elementname\\s*=\\s*[\"'](?<element>[\\w\\\\/]+)[\"'])";
		String regex2 = "(<\\s*render:calltemplate(\\s|\\w|\"|'|-|\\(|\\)|\\.|=|<%|%>)*tname\\s*=\\s*[\"'](?<template>[\\w\\\\/]+)[\"'])";
		String regex3 = "(<\\s*render:getpageurl(\\s|\\w|\"|'|-|\\(|\\)|\\.|=|<%|%>)*pagename\\s*=\\s*[\"'](?<pageurl>[\\w\\\\/]+)[\"'])";
		String regex4 = "(<\\s*render:gettemplateurl(\\s|\\w|\"|'|-|\\(|\\)|\\.|=|<%|%>)*tname\\s*=\\s*[\"'](?<templateurl>[\\w\\\\/]+)[\"'])";
		String regex5 = "(<\\s*render:gettemplateurlparameters(\\s|\\w|\"|'|-|\\(|\\)|\\.|=|<%|%>)*tname\\s*=\\s*[\"'](?<templateurlparams>[\\w\\\\/]+)[\"'])";
		String regex6 = "(<\\s*insite:calltemplate(\\s|\\w|\"|'|-|\\(|\\)|\\.|=|<%|%>)*tname\\s*=\\s*[\"'](?<insitetemplate>[\\w\\\\/]+)[\"'])";
		String regex7 = "(<\\s*ics:callelement(\\s|\\w|\"|'|-|\\(|\\)|\\.|=|<%|%>)*element\\s*=\\s*[\"'](?<icselement>[\\w\\\\/]+)[\"'])";
		String regex8 = "(<\\s*satellite:page(\\s|\\w|\"|'|-|\\(|\\)|\\.|=|<%|%>)*pagename\\s*=\\s*[\"'](?<satellitepage>[\\w\\\\/]+)[\"'])";
		
		Pattern pattern; 
	    Matcher matcher; 
	    
	    pattern = Pattern.compile(regex1,Pattern.CASE_INSENSITIVE);
	    matcher = pattern.matcher(src);
	    while (matcher.find()) {
	      //System.out.println("Captured sequence1: " + matcher.group("element"));
	    }
	    
	    pattern = Pattern.compile(regex2,Pattern.CASE_INSENSITIVE);
	    matcher = pattern.matcher(src);
	    while (matcher.find()) {
	      //System.out.println("Captured sequence2: " + matcher.group("template"));
	    }
	    
	    pattern = Pattern.compile(regex3,Pattern.CASE_INSENSITIVE);
	    matcher = pattern.matcher(src);
	    while (matcher.find()) {
	      //System.out.println("Captured sequence3: " + matcher.group("pageurl"));
	    }
	    
	    pattern = Pattern.compile(regex4,Pattern.CASE_INSENSITIVE);
	    matcher = pattern.matcher(src);
	    while (matcher.find()) {
	      //System.out.println("Captured sequence4: " + matcher.group("templateurl"));
	    }
	    
	    pattern = Pattern.compile(regex5,Pattern.CASE_INSENSITIVE);
	    matcher = pattern.matcher(src);
	    while (matcher.find()) {
	      //System.out.println("Captured sequence5: " + matcher.group("templateurlparams"));
	    }
	    
	    pattern = Pattern.compile(regex6,Pattern.CASE_INSENSITIVE);
	    matcher = pattern.matcher(src);
	    while (matcher.find()) {
	      //System.out.println("Captured sequence6: " + matcher.group("insitetemplate"));
	    }
	    
	    pattern = Pattern.compile(regex7,Pattern.CASE_INSENSITIVE);
	    matcher = pattern.matcher(src);
	    while (matcher.find()) {
	      //System.out.println("Captured sequence7: " + matcher.group("icselement"));
	    }
	    
	    pattern = Pattern.compile(regex8,Pattern.CASE_INSENSITIVE);
	    matcher = pattern.matcher(src);
	    while (matcher.find()) {
	      //System.out.println("Captured sequence8: " + matcher.group("satellitepage"));
	    }
	    
	    endtime = System.currentTimeMillis();
		System.out.println("ElapsedTime: "+(endtime-starttime));
	}
	
}
