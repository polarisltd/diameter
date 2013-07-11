package pl.p4.diameter.scn;
 
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.couchbase.client.CouchbaseClient;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutionException;
import net.spy.memcached.internal.OperationFuture;

 
 public class CBSet extends Step
 {
   private static Logger log = LoggerFactory.getLogger(Log.class);
   private String       arguments;
   private String       cbUrl;
   private String       cbBucket;
   private String       cbBucketPasswd;
   private String       cbKey;
   private String       cbValue;



   private final String USER_AGENT = "Mozilla/5.0";
 
   public CBSet(String arguments)
   {
     super("CBSET", 1, RepeatType.SEQUENCE);
 
     this.arguments = arguments;
   }
 
   public void run() {
	   CouchbaseClient couchbaseClient=null;
	   boolean done=false;
	 try{      

        // CBSET:  url bucket password key value

        ArrayList<String>  lineOpts = splitWords(this.arguments," ");

        if(lineOpts.size()>=5){
          cbUrl = lineOpts.get(0); // url   ex. "http://172.16.133.26:8091/pools/"
          cbBucket = lineOpts.get(1); // bucket
          cbBucketPasswd = lineOpts.get(2); // password
          cbKey = lineOpts.get(3); // key   ex. "153:m:n:l"
          cbValue = lineOpts.get(4); // value ex. "400"
          
 
	  //log.debug("HTTPC user:"+user+ "  pass"+pass+ "  url:"+url); 
	}else{
	  log.info("CBSet must have >=5 arguments, exiting!"+this.arguments); 
	  return;
        }
        //
        // Couchbase API documentation:
        // http://www.couchbase.com/docs/couchbase-sdk-java-1.1/couchbase-sdk-ccfb.html
        //
        final List<URI> connectionUris = new LinkedList<>();
        connectionUris.add(URI.create(cbUrl));
        log.info("CBSET: "+cbUrl+" ... "+cbBucket+":"+ cbBucketPasswd);
        couchbaseClient = new CouchbaseClient(connectionUris, cbBucket, cbBucketPasswd);

        // increment.
        //couchbaseClient.incr("182:m:n:l", 100, 100);

        // SET a single key.
        OperationFuture<Boolean> set = couchbaseClient.set(cbKey, 0, cbValue);
        Boolean result = set.get();

//        Set<String> keys = new HashSet<>();
//        keys.add("182:m:n:l");
//        keys.add("182:m:n:c");
//        Map<String, Object> bulk = couchbaseClient.getBulk(keys);
//        System.out.println("Bulk: " + bulk.size());
//        for (Entry<String, Object> entry : bulk.entrySet()) {
//            System.out.println(entry.getKey().getClass());
//            System.out.println(entry.getValue().toString());
//        }

                log.info("CBSET: "+cbKey+" set ok? "+result);
                done=true;
	 }catch(Exception e){
		log.error("CBSET: "+Arrays.toString(e.getStackTrace()));
	 }finally{
		 if(!done)log.info("CBSET: finally!!");
         couchbaseClient.shutdown();
         }

   }

   ArrayList<String>   splitWords(String s,String regex){
	StringTokenizer st = new StringTokenizer(s,regex);
	ArrayList<String> words = new ArrayList<String>();
	while(st.hasMoreTokens()){
	  words.add(st.nextToken());
	}
        return words;
   }



 }






