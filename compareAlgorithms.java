import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;
import org.apache.lucene.store.FSDirectory;

public class compareAlgorithms {
	
	static Pattern topPattern = Pattern.compile("<top>(.+?)</top>", Pattern.DOTALL);
	static Pattern descPattern = Pattern.compile("<desc>(.+?)<smry>", Pattern.DOTALL);
	static Pattern titlePattern = Pattern.compile("<title>(.+?)<desc>", Pattern.DOTALL);
	static Pattern queryNoPattern = Pattern.compile("<num>(.+?)<dom>", Pattern.DOTALL);
	static HashMap<String,Integer> titleHashMap = new HashMap<String,Integer>();
	static HashMap<String,Integer> descHashMap = new HashMap<String,Integer>();
	
	public static void main(String[] args) throws IOException, ParseException {
		
		IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get("/Users/supreethks/Downloads/index")));
		int N = reader.maxDoc();
		IndexSearcher searcher = new IndexSearcher(reader);
		float F = 0;
		Path dir = Paths.get("/Users/supreethks/Desktop/topics.51-100");
		String content = new String(Files.readAllBytes(dir));
		
		Analyzer analyzer = new StandardAnalyzer();
		
		QueryParser parser = new QueryParser("TEXT", analyzer);
		
    	ArrayList<ArrayList<String>> dataContents = getTagContents(content);
    	ArrayList<String> description = dataContents.get(0);
    	ArrayList<String> titleList = dataContents.get(1);
    	ArrayList<String> queryNumber = dataContents.get(2);
    	
		Map<String, Double> h1 = new HashMap<String, Double>();
		Map<String, Double> h2 = new HashMap<String, Double>();
    	
		searcher.setSimilarity(new BM25Similarity());
    	PrintWriter bm25shortQuery = new PrintWriter(new FileWriter("BM25ShortQuery.txt"));
    	PrintWriter bm25longQuery = new PrintWriter(new FileWriter("BM25Longquery.txt"));
    	setQueries(bm25shortQuery, titleList, searcher, parser, 1);
    	setQueries(bm25longQuery, description, searcher, parser, 2);
    	
    	searcher.setSimilarity(new ClassicSimilarity());
    	PrintWriter VectorSpaceShortQuery = new PrintWriter(new FileWriter("VectorSpaceShortQuery.txt"));
    	PrintWriter VectorSpaceLongquery = new PrintWriter(new FileWriter("VectorSpaceLongquery.txt"));
    	setQueries(VectorSpaceShortQuery, titleList, searcher, parser, 1);
    	setQueries(VectorSpaceLongquery, description, searcher, parser, 2);
    	
    	searcher.setSimilarity(new LMDirichletSimilarity());
    	PrintWriter LMDirichletShortQuery = new PrintWriter(new FileWriter("LMDirichletShortQuery.txt"));
    	PrintWriter LMDirichletLongquery = new PrintWriter(new FileWriter("LMDirichletLongquery.txt"));
    	setQueries(LMDirichletShortQuery, titleList, searcher, parser, 1);
    	setQueries(LMDirichletLongquery, description, searcher, parser, 2);
    	
    	searcher.setSimilarity(new LMJelinekMercerSimilarity((float)0.7));
    	PrintWriter LMJelinekMercerSimilarityShortQuery = new PrintWriter(new FileWriter("LMJelinekMercerSimilarityShortQuery.txt"));
    	PrintWriter LMJelinekMercerSimilarityLongQuery = new PrintWriter(new FileWriter("LMJelinekMercerSimilarityLongquery.txt"));
    	setQueries(LMJelinekMercerSimilarityShortQuery, titleList, searcher, parser, 1);
    	setQueries(LMJelinekMercerSimilarityLongQuery, description, searcher, parser, 2);
	}
	
	private static void setQueries(PrintWriter fileName, ArrayList<String> listName, IndexSearcher searcher, QueryParser parser, int selection) throws ParseException, IOException{
	 	for(String val: listName){
    		String queryString = val;
    		Query query = parser.parse(QueryParser.escape(queryString));
    		TopDocs results = searcher.search(query, 1000);
    		
    		ScoreDoc[] score = results.scoreDocs;
    		for(int i=0;i<score.length;i++) {	
    			Integer qID = selection == 1 ? titleHashMap.get(val):descHashMap.get(val);
    			Document doc=searcher.doc(score[i].doc);
    			String dId = doc.get("DOCNO");
    			fileName.write(qID+"\t"+0+"\t"+dId+"        "+"\t"+(i+1)+"\t"+score[i].score+"             "+"\t"+"run-1");
    			fileName.println();
    		}
    	} 
	 	fileName.close();
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

