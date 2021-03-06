package org.aksw.iguana.utils.comparator;

import java.util.Comparator;

import org.aksw.iguana.generation.CoherenceMetrics;


/**
 * Compares a String build as follows: subject predicate object
 * First it compares subject 
 * if those are equal it compares the predicates 
 * if those are equal it compares the object 
 * 
 * @author Felix Conrads
 */
public class TripleComparator implements Comparator<String>{

	/* (non-Javadoc)
	 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
	 */
	@Override
	public int compare(String s1, String s2) {
		s1 = s1.trim();
		s2 = s2.trim();
		s1 = s1.replace("\t", " ").replace("><", "> <").trim();
		s2 = s2.replace("\t", " ").replace("><", "> <").trim();
		String[] cmp1 = s1.split(" ");
		if(cmp1.length<2){
			System.out.println("Comparison is: "+cmp1[0]);
		}
		for(int k=3;k<cmp1.length;k++){
			cmp1[2]+=" "+cmp1[k];
		}
		String[] cmp2 = s2.split(" ");
		for(int k=3;k<cmp2.length;k++){
			cmp2[2]+=" "+cmp2[k];
		}
		int ret =  cmp1[0].compareTo(cmp2[0]);
		if(ret ==0){
			int t1=0, t2=0;
			if(cmp1[1].equals(CoherenceMetrics.TYPE_STRING)){
				t1 = -1;
			}
			if(cmp2[1].equals(CoherenceMetrics.TYPE_STRING)){
				t2 = 1;
			}
			if(t1==-1 && t2==1){
				ret =0;
			}
			else if(t1==-1 || t2==1){
				ret = t1+t2; //-1+0 or 0+1;
			}
			else{
				ret =  cmp1[1].compareTo(cmp2[1]);
			}
		}
		if(ret == 0){
			ret =  cmp1[2].compareTo(cmp2[2]);
		}
		return ret;
	}

}
