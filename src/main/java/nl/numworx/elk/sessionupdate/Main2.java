package nl.numworx.elk.sessionupdate;

import org.apache.http.HttpHost;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.IndicesClient;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;

public class Main2 {

	public static void main(String[] args) throws Exception {
		
		RestHighLevelClient client = new RestHighLevelClient(
		        RestClient.builder(
		                new HttpHost("localhost", 9200, "http")));

		DeleteIndexRequest delete = new DeleteIndexRequest("session-*");
		IndicesClient indices = client.indices();
		AcknowledgedResponse response = indices.delete(delete, RequestOptions.DEFAULT);
		System.out.println(response.isAcknowledged());
		
		client.close();
	}

}
