import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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

public class easySearch {
	static HashMap map1 = new HashMap();
	static HashMap map2 = new HashMap();
	
	public static void main(String[] args) throws ParseException, IOException {
		IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths
				.get("/Users/supreethks/Downloads/index")));
		int numberOfDocs = reader.maxDoc();
		IndexSearcher searcher = new IndexSearcher(reader);
		String queryString = "New York";
		float F = 0;
		// Get the preprocessed query terms
		Analyzer analyzer = new StandardAnalyzer();
		QueryParser parser = new QueryParser("TEXT", analyzer);
		Query query = parser.parse(queryString);
		Set<Term> queryTerms = new LinkedHashSet<Term>();
		searcher.createNormalizedWeight(query, false).extractTerms(queryTerms);
		System.out.println("Terms in the query: ");
		for (Term t : queryTerms) {
			int df=reader.docFreq(new Term("TEXT", t.text()));
			ClassicSimilarity dSimi = new ClassicSimilarity();
			// Get the segments of the index
			List<LeafReaderContext> leafContexts = reader.getContext().reader().leaves();
			// Processing each segment
			for (int i = 0; i < leafContexts.size(); i++) {
				// Get document length
				LeafReaderContext leafContext = leafContexts.get(i);
				calculateFreq(leafContext, t, dSimi, df, numberOfDocs);
			}	
		}
		calcFreqCompTerms(reader, queryTerms, numberOfDocs);
	}
	
	static void calculateFreq(LeafReaderContext leafContext, Term t, ClassicSimilarity dSimi, int docFreq, int numDocs) throws IOException{
		PostingsEnum de = MultiFields.getTermDocsEnum(leafContext.reader(),"TEXT", new BytesRef(t.text()));
		int doc;
		int startDocNo = leafContext.docBase;
		int numberOfDoc = leafContext.reader().maxDoc();
		if(de != null){
			while((doc = de.nextDoc()) != PostingsEnum.NO_MORE_DOCS){
				
				map1.put((de.docID() + startDocNo), de.freq());
			}
		}
		for (int docId = 0; docId < numberOfDoc; docId++) {
			double normalDocLength = dSimi.decodeNormValue(leafContext.reader().getNormValues("TEXT").get(docId));
			double docLeng = 1 / (normalDocLength * normalDocLength);
			
			int frequency =  map1.containsKey(docId + startDocNo) ? (int) map1.get(docId + startDocNo):0;
	
			double logVal = Math.log(1 + (numDocs/docFreq));
			double freq = (frequency/docLeng) * logVal;
				
			System.out.println("F("+t.text()+"," + (docId+startDocNo)+") = " + freq);
		}
	}
	
	static void calcFreqCompTerms(IndexReader reader, Set<Term> queryTerms, int numberOfDocs) throws IOException{
		String queryString = "New York";
		ClassicSimilarity dSimi = new ClassicSimilarity();
		List<LeafReaderContext> leafContexts = reader.getContext().reader().leaves();
		
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
					double logVal = Math.log(1 + (numberOfDocs / documentFreq));					
					freqForAllterms += (frequency/docLeng) * logVal;
				}
					
				System.out.println("F("+ queryString +","+(docId+startDocNo)+") = " + freqForAllterms);
				
			}
		}
	}
}
