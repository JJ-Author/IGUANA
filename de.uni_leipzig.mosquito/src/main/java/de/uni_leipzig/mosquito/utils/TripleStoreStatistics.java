package de.uni_leipzig.mosquito.utils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bio_gene.wookie.connection.Connection;
import org.bio_gene.wookie.utils.LogHandler;

import com.hp.hpl.jena.rdf.model.impl.LiteralImpl;



/**
 * Erst ab SPARQL 1.1 möglich
 * Problem: Möglicherweise könnten die Queries nicht schnell genug ausführbar sein, dann muss das über denn RAM laufen
 * 
 * @author Felix Conrads
 *
 */
public class TripleStoreStatistics {

		private static Long query(Connection con, String query){
			Logger log = Logger.getLogger("TripleStoreStatistics");
			try {
				ResultSet result = con.select(query);
				result.next();
				LiteralImpl ret = (LiteralImpl) result.getObject("no");
				Long count = ret.getLong();
				return count;
			} catch ( SQLException | NullPointerException e) {
				LogHandler.writeStackTrace(log, e, Level.WARNING);
			}
			return -1L;
		}
	
		public static Long tripleCount(Connection con, String graphURI){
			//validated with 4store
			String query="SELECT (COUNT(*) AS ?no) ";
			query+=(graphURI!=null?"FROM <"+graphURI+">":"");
			query+=" { ?s ?p ?o  }";
			return query(con, query);
			
		}
		
		public static Long objectNodeCount(Connection con, String graphURI){
			//validated with 4store 
			String query="SELECT (COUNT(DISTINCT ?o) AS ?no) ";
			query+=(graphURI!=null?"FROM <"+graphURI+">":"");
			query+=" WHERE{ ?s ?p ?o  filter (!isLiteral(?o)) }";
			return query(con, query);
		}
		
		public static Long subjectNodeCount(Connection con, String graphURI){
			//validated with 4store 
			String query="SELECT (COUNT(DISTINCT ?s) AS ?no) ";
			query+=(graphURI!=null?"FROM <"+graphURI+">":"");
			query+=" WHERE { ?s ?p ?o  }";
			return query(con, query);
		}
		
		public static Long entityCount(Connection con, String graphURI){
			//validated with 4store 
			String query="SELECT (COUNT(DISTINCT ?s) AS ?no) ";
			query+=(graphURI!=null?"FROM <"+graphURI+">":"");
			query+=" WHERE{ ?s a []  }";
			return query(con, query);
		}
		
		public static Long avgOutDegree(Connection con, Boolean literals, String graphURI){
			String query="SELECT (AVG(?co) AS ?no)  ";
			query+=(graphURI!=null?"FROM <"+graphURI+"> ":"");
			if(literals){
				
				query+="WHERE { SELECT (SAMPLE(?s) AS ?NAME) (COUNT(?t) AS ?co) WHERE  { ?s ?p ?t } GROUP BY ?s}";
			}else{
				
				query+= " WHERE { SELECT (SAMPLE(?s) AS ?NAME) (COUNT(?t) AS ?co) "+
						"WHERE  { ?s ?p ?t filter(!isLiteral(?t))} GROUP BY ?s}";
			}
			return query(con, query);
		}
		
		public static Long avgInDegree(Connection con, Boolean literals, String graphURI){
			String query="SELECT (AVG(?co) AS ?no)  ";
			query+=(graphURI!=null?"FROM <"+graphURI+"> ":"");
			if(literals){
				query+=" WHERE { SELECT (SAMPLE(?s) AS ?NAME) (COUNT(?t) AS ?co) WHERE  { ?t ?p ?o } GROUP BY ?o}";
			}else{
				query+="WHERE { SELECT (SAMPLE(?s) AS ?NAME) (COUNT(?t) AS ?co) "
						+ "WHERE  { ?t ?p ?o filter(!isLiteral(?o)) } GROUP BY ?o}";
			}
			return query(con, query);
		}
		
	
}
