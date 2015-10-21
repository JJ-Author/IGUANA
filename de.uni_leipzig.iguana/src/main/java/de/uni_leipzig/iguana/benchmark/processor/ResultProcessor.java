package de.uni_leipzig.iguana.benchmark.processor;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.logging.Logger;

import org.bio_gene.wookie.utils.LogHandler;

import de.uni_leipzig.iguana.utils.FileHandler;
import de.uni_leipzig.iguana.utils.ResultSet;

public class ResultProcessor {

	public static final String RESULT_FOLDER = "results";
	public static final String TEMP_RESULT_FOLDER = "tempResults";
	
	private static HashMap<String, Collection<ResultSet>> results;
	
	
	private static Logger log = Logger.getLogger(ResultProcessor.class
			.getSimpleName());

	static {
		LogHandler.initLogFileHandler(log,
				ResultProcessor.class.getSimpleName());
	}
	
	public static void init(){
		//mkdirs
		FileHandler.removeRecursive(RESULT_FOLDER);
		FileHandler.removeRecursive(TEMP_RESULT_FOLDER);
		new File(RESULT_FOLDER).mkdir();
		new File(TEMP_RESULT_FOLDER).mkdir();
		
		results = new HashMap<String, Collection<ResultSet>>();
	}
	
	
	public static Collection<ResultSet> getResultsForTestcase(String id){
		return results.get(id);
	}
	
	public static void putResultsForTestcase(String id, Collection<ResultSet> testcaseResults){
		results.put(id, testcaseResults);
	}
	
	public static void saveResults(){
		for (String key : results.keySet()) {
			for (ResultSet res : results.get(key)) {
				log.info("Saving Results for "+key+"...");
				
				String testCase = key.split("&")[0]+key.split("&")[2];
				testCase.replaceAll("[^A-Za-z0-9.]", "");
				String fileSep =File.separator;
				if(fileSep.equals("\\")){
					fileSep=File.separator+File.separator;
				}
				String[] fileName = res.getFileName().split(fileSep);
				String[] prefixes = res.getPrefixes();
				String suffix="";
				for(String prefix : prefixes){
					suffix+=prefix+File.separator;
				}
				new File("."+File.separator+
						RESULT_FOLDER+
						File.separator+testCase+
						File.separator+suffix).mkdirs();
				res.setFileName("."+File.separator+
						RESULT_FOLDER+
						File.separator+testCase+
						File.separator+suffix+fileName[fileName.length-1]);
				try {
					res.save();
				} catch (IOException e) {
					log.severe("Couldn't save Results for "+key);
				}
			}
		}
		log.info("Finished saving results");
	}

	
	
}
