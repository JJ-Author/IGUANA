package org.aksw.iguana.testcases.workers;

import java.io.File;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import org.aksw.iguana.connection.Connection;
import org.aksw.iguana.connection.ConnectionFactory;
import org.aksw.iguana.utils.logging.LogHandler;


public class UpdateWorker extends Worker implements Runnable{
	
	public static void main(String[] argc){
		ConnectionFactory.setDriver("org.apache.jena.jdbc.remote.RemoteEndpointDriver");
		ConnectionFactory.setJDBCPrefix("jdbc:jena:remote:query=http://");
		org.apache.log4j.Logger.getLogger("log4j.logger.org.apache.jena.arq.info").setLevel(org.apache.log4j.Level.OFF);
		org.apache.log4j.Logger.getLogger("log4j.logger.org.apache.jena.arq.exec").setLevel(org.apache.log4j.Level.OFF);
		org.apache.log4j.Logger.getLogger("log4j.logger.org.apache.jena").setLevel(org.apache.log4j.Level.OFF);
		org.apache.log4j.Logger.getRootLogger().setLevel(org.apache.log4j.Level.OFF);
		
		
		Connection con = ConnectionFactory.createImplConnection("localhost:9999/bigdata/sparql", "localhost:9999/bigdata/sparql",  -1);
		
		
		con.uploadFile("changeset/000000.added(1).nt", null);
	}
	
	protected static final String DELETE_STRING="removed";
	protected static final String ADD_STRING="added";
	
	private int index=0;

	

	
	private Logger log = Logger.getLogger(UpdateWorker.class.getSimpleName());

	private boolean sparqlLoad;

	private String graphURI=null;
	
	private Properties props;
	private List<Integer[]> latencyAmount = new LinkedList<Integer[]>();
	private List<LatencyStrategy> latencyStrategy  = new LinkedList<LatencyStrategy>();

	private UpdateStrategy updateStrategy;
	private Random rand;
	private int[] intervall = new int[2];
	private boolean first = true;
	private WorkerStrategy workerStrategy = WorkerStrategy.NONE;
	
	private UpdateFileHandler ufh;
	private List<File> liveDataList = new LinkedList<File>();
	private long lastTime;
	private boolean updateQueries;
	private Long noCount;
	
	public enum UpdateStrategy{
		VARIABLE, FIXED, NONE
	};
	
	
	/**
	 * ADDED: only updates Files to ADD
	 * REMOVED: only updates Files to remove
	 * NONE: just go through the files and update the next file in order
	 * NEXT: will take the next file which isn't uploaded by another worker yet
	 * 
	 * @author Felix Conrads
	 *
	 */
	public enum WorkerStrategy{
		ADDED, REMOVED, NONE, NEXT
	};
	
	
	public UpdateWorker(){
		super(UpdateWorker.class.getSimpleName());
		workerType="UPDATE";
	}
	
	
	public void init(){
		init(getWorkerNr());
	}
	
	public void init(int workerNr){
		//INIT LOGGER
		initLogger();
		this.workerNr=workerNr;
		rand = new Random(this.workerNr);

//		//timeLImit
//		timeLimit = (Long)props.get("TIMELIMIT");
//		//UpdateStrategy
//		updateStrategy = (UpdateStrategy) props.get("UPDATESTRATEGY");
//		//workerStrategy
//		workerStrategy = (WorkerStrategy) props.get("WORKERSTRATEGY");
//		if(!workerStrategy.equals(WorkerStrategy.NONE)){
//			workerStrategy=WorkerStrategy.NEXT;
//		}
		//latencyAmount
//		int i=0;
//		while(props.containsKey("LATENCYAMOUNT"+i)){
//			//latencyAmount
//			Integer[] intervall = new Integer[2];
//			intervall = (Integer[])props.get("LATENCYAMOUNT"+i);
//			LatencyStrategy latStrat = (LatencyStrategy)props.get("LATENCYSTRATEGY"+i);
//			latencyAmount.add(getIntervallLatency(intervall, latStrat, rand));
//			
//			//LatencyStrategy
//			latencyStrategy.add(latStrat);
//			i++;
//		}
		for(int i=0;i<latencyAmount.size();i++){
			Integer[] intervall = new Integer[2];
			intervall = latencyAmount.get(i);
			LatencyStrategy latStrat =latencyStrategy.get(i);
			latencyAmount.set(i, getIntervallLatency(intervall, latStrat, rand));
		}
		
		//File init
//		liveDataList = (List<File>)props.get("FILES");
		initMaps();
		//Connection
//		this.con = ConnectionFactory.createConnection(props.getProperty("CONNECTION"));
//		this.con = (Connection) props.get("CONNECTION");
		//sparqlLoad
//		sparqlLoad = Boolean.valueOf(props.getProperty("SPARQLLOAD"));
		//GraphURI
//		graphURI = props.getProperty("GRAPHURI");
			
		setIntervallUpdate();
	}
	
	public void setLatencyStrategy(List<LatencyStrategy> latencyStrategy){
		this.latencyStrategy = latencyStrategy;
	}
	
	public void setLatencyAmount(List<Integer[]> latencyAmount){
		this.latencyAmount = latencyAmount;
	}
	
	
	public void setGraphURI(String graphURI){
		this.graphURI = graphURI;
	}
	
	public void setSparqlLoad(Boolean sparqlLoad){
		this.sparqlLoad = sparqlLoad;
	}
	
	
	public void setConnection(Connection con){
		this.con = con;
	}
	
	public void setTimeLimit(long timeLimit){
		this.timeLimit = timeLimit;
	}
	
	public void setWorkerStrategy(WorkerStrategy ws){
		this.workerStrategy = ws;
		if(!workerStrategy.equals(WorkerStrategy.NONE)){
			workerStrategy=WorkerStrategy.NEXT;
		}
	}
	
	public void setUpdateStrategy(UpdateStrategy us){
		this.updateStrategy = us;
	}
	
	private void initLogger(){
		LogHandler.initLogFileHandler(log, UpdateWorker.class.getSimpleName());
	}
	
	@Override
	protected String[] getNextQuery(){
		waitTime();
		String[] ret = new String[2];
		//ret[0] = QueryFilePath
		//ret[1] = QueryNr.
		if (queryMixFile !=null && !queryMixFile.isEmpty()) {
			if (!queryMix.hasNext())
				queryMix = queryMixList.iterator();
			index = Integer.valueOf(queryMix.next());

		} 
		if(index>= liveDataList.size()){
			// no more files, exit thread
			this.sendEndSignal();
			return ret;
		}
		
		File current;
		if(workerStrategy.equals(WorkerStrategy.NEXT)){
			current= liveDataList.remove(index);
			while(!ufh.getLiveDataListAll().contains(current)){
				if(index>= liveDataList.size()){
					// no more files, exit thread
					this.sendEndSignal();
					return ret;
				}
				current= liveDataList.remove(index);
			}
			ufh.getLiveDataListAll().remove(current);
		}
		else{
			current = liveDataList.get(index);
		}
		ret[0] = current.getAbsolutePath();
		ret[1] = current.getName();
		if(!workerStrategy.equals(WorkerStrategy.NEXT)){
			index+=1;
		}
		return ret;
	}
	
	@Override
	protected Integer testQuery(String query){
		System.out.println(query);
		try {
			if(this.con.isClosed()){
				lastTime =0;
				return -2;
			}
		} catch (SQLException e) {
			lastTime =0;
			return -2;
		}
		if(query==null){
			lastTime =0;
			return 0;
		}
//		waitTime();
		//executeQuery
		//TODO change ret in ImplConnection
		this.con.setTriplesToUpload(noCount);
		if(this.updateQueries){
			return this.con.update(query).intValue();
		}
		
		if(sparqlLoad){
			int ret =  Long.valueOf(this.con.loadUpdate(query, graphURI)).intValue();
			lastTime = ret;
			return ret;
		}else{
			if(query.contains(UpdateWorker.DELETE_STRING)){
				int ret =  Long.valueOf(this.con.deleteFile(query, graphURI)).intValue();
				lastTime = ret;
				return ret;
			}
			else{
				int ret = Long.valueOf(this.con.uploadFile(query, graphURI)).intValue();
				lastTime = ret;
				return ret;
			}
		}
	}

	private void waitTime(){
		if(first ){
			first = false;
			return;
		}
		int latency=0;
		for(int i=0;i<latencyAmount.size();i++){
			latency+=getLatency(latencyAmount.get(i), latencyStrategy.get(i), rand);
		}
		int wait=getAmount();
		long totalWait = Math.max(0, wait+latency-lastTime);
		
		log.finest("Waiting "+(totalWait)+"ms before next UPDATE Query");
		Calendar start = Calendar.getInstance();
		while((Calendar.getInstance().getTimeInMillis()-start.getTimeInMillis())<=totalWait){
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
	}
	

	
	private void setIntervallUpdate(){
		switch(this.updateStrategy ){
		case VARIABLE:
			int amount = Long.valueOf(Math.round(Double.valueOf((timeLimit*1.0)/liveDataList.size()))).intValue();
			Double sig = Math.sqrt(amount);
			intervall[0] = Math.round(Double.valueOf(amount-sig).floatValue());
			intervall[1] = Math.round(Double.valueOf(amount+sig).floatValue());
			log.fine("Update Time Intervall is set to: [ "+intervall[0]+"ms ; "+intervall[1]+"ms ]");
			break;
		case FIXED:
			intervall[0] = Long.valueOf(Math.round(Double.valueOf((timeLimit*1.0)/liveDataList.size()))).intValue();
			log.fine("Update Time is set to "+intervall[0]+"ms");
			break;
		default:
			break;
		}
	}
	
	private int getAmount(){
		switch(this.updateStrategy ){
		case VARIABLE:
			int n= (intervall[1]-intervall[0]);
			double nextGaussian = (rand.nextGaussian()+1)/2;
			int ret = (int)(n * nextGaussian )+intervall[0];
			return ret;
		case FIXED:
			return intervall[0];
		case NONE:
			return 0;
		}
		return 0;
	}
	
	
	@Override
	public void run() {
		start();
	}


	public Properties getProps() {
		return props;
	}


	public void setProps(Properties props) {
		this.props = props;
	}

	private void initMaps(){
		for(File f : liveDataList){
			resultMap.put(f.getName(), 0);
			failMap.put(f.getName(), 0);
			succMap.put(f.getName(), 0);
		}
	}



	public UpdateFileHandler getUfh() {
		return ufh;
	}


	public void setUfh(UpdateFileHandler ufh) {
		this.ufh = ufh;
	}


	public void setLiveDataList(List<File> list) {
		this.liveDataList  = list;
	}


	public void setNoOfTriples(Long count) {
		this.noCount = count;
	}


	
}
