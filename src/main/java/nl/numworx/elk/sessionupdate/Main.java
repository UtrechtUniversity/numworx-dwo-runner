package nl.numworx.elk.sessionupdate;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;

import org.apache.http.HttpHost;
import org.elasticsearch.action.DocWriteResponse.Result;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteRequestBuilder;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.ClearScrollRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.action.support.replication.ReplicationResponse;
import org.elasticsearch.client.IndicesClient;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.ExistsQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;

public class Main {

	static String basicDateTime = "yyyy-MM-dd'T'HH:mm:ss.SSS";
	static SimpleDateFormat formatter = new SimpleDateFormat(basicDateTime);
	
	public static class UserRecord { 
		public Date timestamp;
		public String user_id;
		public long duration;
		
		UserRecord(String u) { user_id = u; }

		@Override
		public String toString() {
			return "UserRecord [timestamp=" + timestamp + ", user_id=" + user_id + ", duration=" + duration + "]";
		}
	}
	
	public static void main(String[] args) throws Exception {
		
		formatter.setTimeZone(TimeZone.getTimeZone("GMT"));

		RestHighLevelClient client = new RestHighLevelClient(
		        RestClient.builder(
		                new HttpHost("localhost", 9200, "http")));
		
		/// SEARCH API

		Map<String,UserRecord> lastTime = new TreeMap<>();
for(int m = 0; m < 12; m++ ) {
	
		int from = 0;
		long totalHits;
		SearchRequest searchRequest = new SearchRequest("logstash-*"); 
		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder(); 
		QueryBuilder query = new ExistsQueryBuilder("user_id");
		Date fromdate = new Date(2022-1900,m  ,1,0,0,0);
		Date todate   = new Date(2022-1900,m+1,1,0,0,0);
		QueryBuilder start = new RangeQueryBuilder("@timestamp").from(fromdate, true).to(todate, false);
		searchSourceBuilder.query(new BoolQueryBuilder().must(query)
				.must(start)
				); 
		searchSourceBuilder.sort("@timestamp", SortOrder.ASC);
		searchSourceBuilder.from(from);
		searchSourceBuilder.size(1000);
		searchRequest.source(searchSourceBuilder); 
		searchRequest.scroll(TimeValue.timeValueMinutes(2L)); 
		List<String> scrollIds = new ArrayList<>(20);
		SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
	do {
		RestStatus status = searchResponse.status();
//		TimeValue took = searchResponse.getTook();
//		Boolean terminatedEarly = searchResponse.isTerminatedEarly();
//		boolean timedOut = searchResponse.isTimedOut();
		SearchHits hits = searchResponse.getHits();
		totalHits = hits.getTotalHits();
		float maxScore = hits.getMaxScore();
		SearchHit[] searchHits = hits.getHits();
		from += searchHits.length;
		for (SearchHit hit : searchHits) {
		    // do something with the SearchHit
//			String index = hit.getIndex();
//			String type = hit.getType();
//			String id = hit.getId();
//			float score = hit.getScore();
			Map<String, Object> sourceAsMap = hit.getSourceAsMap();
			String userid = (String) sourceAsMap.get("user_id");
			String timestamp = (String) sourceAsMap.get("@timestamp");
			timestamp = timestamp.substring(0,23);
			Date t = formatter.parse(timestamp);
			long time = t.getTime();
			UserRecord record = lastTime.computeIfAbsent(userid, UserRecord::new);
			if (record.timestamp == null) {
				record.timestamp = t;
				record.duration = 0;
			} else {
				long diff = time - record.timestamp.getTime();
				if (diff > 30*60*1000L) {
					put(client, record);
					record.timestamp = t;
					record.duration = 0;
				} else {
					record.duration += diff;
					record.timestamp = t;
				}
			}
		}
		String scrollId = searchResponse.getScrollId();
		if (scrollId != null) scrollIds.add(scrollId);
		if (scrollId == null || from >= totalHits) 			
			break;
		SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId); 
		scrollRequest.scroll(TimeValue.timeValueSeconds(120));
		searchResponse = client.scroll(scrollRequest, RequestOptions.DEFAULT);
		scrollId = searchResponse.getScrollId();
		if (scrollId != null) scrollIds.add(scrollId);
		} while(from < totalHits);
	
		ClearScrollRequest clear = new ClearScrollRequest();
		clear.setScrollIds(scrollIds);
		client.clearScroll(clear, RequestOptions.DEFAULT);
}
		lastTime.values().forEach(item -> put(client, item));
		
		client.close();
	}

	
	
	private static void put(RestHighLevelClient client, UserRecord record) {
		
		Map<String, Object> json = new TreeMap<>();
		json.put("user_id", record.user_id);
		int len = record.user_id.length();
		String id = record.user_id.substring(len-2);
		json.put("timestamp", record.timestamp);
		json.put("duration", record.duration / 1000.0); // double in seconds
		IndexRequest request = new IndexRequest("session-"+id, "_doc", record.user_id + Long.toString(record.timestamp.getTime()))
		        .source(json); 		
		try {
			IndexResponse indexResponse = client.index(request, RequestOptions.DEFAULT);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}








	private static void putObject(RestHighLevelClient client) throws IOException {
		Map<String,Object> jsonMap = new TreeMap<String, Object>();
		jsonMap.put("user", "kimchy");
		jsonMap.put("postDate", new Date());
		jsonMap.put("message", "trying out Elasticsearch");
		IndexRequest request = new IndexRequest("posts", "doc", "1")
		        .source(jsonMap); 		
		IndexResponse indexResponse = client.index(request, RequestOptions.DEFAULT);

		String index = indexResponse.getIndex();
		String type = indexResponse.getType();
		String id = indexResponse.getId();
		long version = indexResponse.getVersion();

		if (indexResponse.getResult() == Result.CREATED) {
		    
		} else if (indexResponse.getResult() == Result.UPDATED) {
		    
		}
		ReplicationResponse.ShardInfo shardInfo = indexResponse.getShardInfo();
		if (shardInfo.getTotal() != shardInfo.getSuccessful()) {
		    
		}
		if (shardInfo.getFailed() > 0) {
		    for (ReplicationResponse.ShardInfo.Failure failure :
		            shardInfo.getFailures()) {
		        String reason = failure.reason(); 
		    }
		}
	}

}
