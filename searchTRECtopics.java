import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.text.Document;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

import com.ibm.icu.impl.Pair;

public class searchTRECtopics {
	
	static HashMap<String,Integer> titleHashMap = new HashMap<String,Integer>();
	static HashMap<String,Integer> descHashMap = new HashMap<String,Integer>();
	static HashMap map2 = new HashMap();
	static Pattern topPattern = Pattern.compile("<top>(.+?)</top>", Pattern.DOTALL);
	static Pattern descPattern = Pattern.compile("<desc>(.+?)<smry>", Pattern.DOTALL);
	static Pattern titlePattern = Pattern.compile("<title>(.+?)<desc>", Pattern.DOTALL);
	static Pattern queryNoPattern = Pattern.compile("<num>(.+?)<dom>", Pattern.DOTALL);
	static Map<String, Double> newHashMap = new HashMap<String, Double>();
	public static void main(String[] args) throws IOException, ParseException {
		// TODO Auto-generated method stub
		IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get("/Users/supreethks/Downloads/index")));
		int numberOfDocs = reader.maxDoc();
		IndexSearcher searcher = new IndexSearcher(reader);
		Analyzer analyzer = new StandardAnalyzer();
		QueryParser parser = new QueryParser("TEXT", analyzer);
		PrintWriter printWriterFile = new PrintWriter(new FileWriter("TFIDFshortQuery.txt"));
		PrintWriter printWriterFile2 = new PrintWriter(new FileWriter("TFIDFlongQuery.txt"));
		float F = 0;
		 Path trecData = Paths.get("/Users/supreethks/Downloads", "topics.51-100");
		    try {
		    	 byte[ ] trecBytes = Files.readAllBytes(trecData);
		      	 String fullTrecData = new String(trecBytes, "ISO-8859-1");
		      	 ArrayList<ArrayList<String>> content = getTagContents(fullTrecData);
		    	 ArrayList<String> descList = content.get(0);
		    	 ArrayList<String> titleList = content.get(1);
		    	 ArrayList<String> queryNumList = content.get(2);
		    	 for(String val: titleList){
		    		String queryString = val;
		     		Query query = parser.parse(QueryParser.escape(queryString));
		     		Set<Term> queryTerms = new LinkedHashSet<Term>();
		     		searcher.createNormalizedWeight(query, false).extractTerms(queryTerms);
		     		calcFreqCompTerms(reader, queryTerms, numberOfDocs, val, 1);
		     		Map<String, Double> sortedMap = sortByValue(newHashMap);
		        	int count = 1;
		        	Iterator iter = sortedMap.entrySet().iterator();
		        	while(iter.hasNext() && count <= 1000){
		        		Map.Entry pair = (Map.Entry) iter.next();
		        		iter.remove();
		        		printWriterFile.println();
		        		printWriterFile.write(pair.getKey() + "\t" + count + "\t" + pair.getValue() + "\t" + "run-1");
		        		count++;
		        	}
		    	 }
		    	 for(String val: descList) {
		    		 String queryString = val;
			     		Query query = parser.parse(QueryParser.escape(queryString));
			     		Set<Term> queryTerms = new LinkedHashSet<Term>();
			     		searcher.createNormalizedWeight(query, false).extractTerms(queryTerms);
			     		calcFreqCompTerms(reader, queryTerms, numberOfDocs, val, 2);
			     		Map<String, Double> sortedMap = sortByValue(newHashMap);
			        	int count = 1;
			        	Iterator iter = sortedMap.entrySet().iterator();
			        	while(iter.hasNext() && count <= 1000){
			        		Map.Entry pair = (Map.Entry) iter.next();
			        		iter.remove();
			        		printWriterFile2.println();
			        		printWriterFile2.write(pair.getKey() + "\t" + count + "\t" + pair.getValue() + "\t" + "run-1");
			        		count++;
			        	}
		    	 }
		    	 printWriterFile.close();
		    	 printWriterFile2.close();
		    } catch (IOException e) {
		    	System.out.println(e);
		    }
	}
	
	//This piece of code has been taken from https://www.mkyong.com/java/how-to-sort-a-map-in-java/
	
	private static Map<String, Double> sortByValue(Map<String, Double> unsortMap) {
        List<Map.Entry<String, Double>> list =
                new LinkedList<Map.Entry<String, Double>>(unsortMap.entrySet());

        Collections.sort(list, new Comparator<Map.Entry<String, Double>>() {
            public int compare(Map.Entry<String, Double> o1,
                               Map.Entry<String, Double> o2) {
                return (o1.getValue()).compareTo(o2.getValue());
            }
        });

        Map<String, Double> sortedMap = new LinkedHashMap<String, Double>();
        for (Map.Entry<String, Double> entry : list) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }
        return sortedMap;
    }
	
	static void calcFreqCompTerms(IndexReader reader, Set<Term> queryTerms, int numberOfDocs, String val, int selection) throws IOException{
		ClassicSimilarity dSimi = new ClassicSimilarity();
		List<LeafReaderContext> leafContexts = reader.getContext().reader().leaves();
		Integer qID;
		for (int i = 0; i < leafContexts.size(); i++) {
			LeafReaderContext leafContext = leafContexts.get(i);
			int startDocNo = leafContext.docBase;
			int numberOfDoc = leafContext.reader().maxDoc();
			//System.out.println(numberOfDoc);
			
			for (int docId = 0; docId < numberOfDoc; docId++) {
			
				double normalDocLength = dSimi.decodeNormValue(leafContext.reader().getNormValues("TEXT").get(docId));
				double docLeng = 1 / (normalDocLength * normalDocLength);
				int documentFreq = 0;
				double freqForAllterms = 0;
				
				for (Term t : queryTerms) {
					int doc;
					documentFreq = reader.docFreq(new Term("TEXT", t.text()));
					PostingsEnum de = MultiFields.getTermDocsEnum(leafContext.reader(),"TEXT", new BytesRef(t.text()));
					
					if(de != null){
						while((doc = de.nextDoc()) != PostingsEnum.NO_MORE_DOCS){
							map2.put((de.docID() + startDocNo), de.freq());
						}
					}
	
					int frequency = map2.containsKey(docId + startDocNo) ? (int) map2.get(docId + startDocNo) : 0;
					double logVal = documentFreq > 0 ?  Math.log(1 + (numberOfDocs / documentFreq)) : 0;
									
					freqForAllterms += (frequency/docLeng) * logVal;
				}
				
				if(freqForAllterms != 0) {
					Set docNumberString = new TreeSet<String>();
					docNumberString.add("DOCNO");
					org.apache.lucene.document.Document docNumber = leafContext.reader().document(docId, docNumberString);
					qID = selection == 1? titleHashMap.get(val) : descHashMap.get(val); 
					int docNo = docId + startDocNo;
					String docString = docNumber.get("DOCNO");
					String combined = qID+"\t"+"0"+"\t"+docString;
					newHashMap.put(combined, freqForAllterms);
				}
			}
		}
	}

	
	static String matchText(Matcher matcherName) {
		String finalMatchedArg = new String();
		while (matcherName.find()) {	
			finalMatchedArg = matcherName.group(1);
	    }
    	
	    return finalMatchedArg;
	}
	
	private static ArrayList<ArrayList<String>> getTagContents(String fullData) {
		 ArrayList<ArrayList<String>> mainList = new ArrayList<ArrayList<String>>();
		    ArrayList<String> allDocuments = new ArrayList<String>();
		    ArrayList<String> titleList = new ArrayList<String>();
		    ArrayList<String> descriptionList = new ArrayList<String>();
		    ArrayList<String> queryNumberList = new ArrayList<String>();

		    final Matcher top = topPattern.matcher(fullData);
		    
		    while(top.find()){
		    	allDocuments.add(top.group(1));
		    }
		    
		    for(String eachDoc: allDocuments){
			    Matcher descriptionMatcher = descPattern.matcher(eachDoc);
			    Matcher titleMatcher = titlePattern.matcher(eachDoc);
			    Matcher queryNoMatcher = queryNoPattern.matcher(eachDoc);

			    String description = matchText(descriptionMatcher).replaceAll("\\n", "").replaceAll("\\z", "").replaceAll("Description:", "").trim();
			    descriptionList.add(description);
			    
			    String title = matchText(titleMatcher).replaceAll("\\n", "").replaceAll("\\z","").trim();
			    titleList.add(title);
			    
			    String queryNo = matchText(queryNoMatcher);
			    queryNo = queryNo.replaceAll("Number:","").replaceAll("\\s", "").replaceAll("^[\\d]","");
			    
			    int queryNumberInt = Integer.parseInt(queryNo);
			    queryNumberList.add(queryNo); 

			    titleHashMap.put(title, queryNumberInt);
			    descHashMap.put(description, queryNumberInt);
		    }
		    
		    mainList.add(descriptionList);
		    mainList.add(titleList);
		    mainList.add(queryNumberList);
		    
		    return mainList;
	}
}

