package pl.p4.diameter.scn;
 
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
 
 public class HttpGet extends Step
 {
   private static Logger log = LoggerFactory.getLogger(Log.class);
   private String url;
   private final String USER_AGENT = "Mozilla/5.0";
 
   public HttpGet(String url)
   {
     super("HTTPGET", 1, RepeatType.SEQUENCE);
 
     this.url = url;
   }
 
   public void run() {

	 try{      
                URL obj = new URL(url);
                HttpURLConnection con = (HttpURLConnection) obj.openConnection();
                con.setRequestMethod("GET");  // set method
                con.setRequestProperty("User-Agent", USER_AGENT); // add request header
                int responseCode = con.getResponseCode();
                //
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuffer response = new StringBuffer();
                //
                while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                }
                in.close();
                log.info("HTTPGET: ["+ url+ "] ("+ responseCode+ ") => "+response.toString());
	 }catch(Exception e){
		 log.error("HTTPGET: ["+ url+ "] ("+Arrays.toString(e.getStackTrace()));
	 }

   }
 }

