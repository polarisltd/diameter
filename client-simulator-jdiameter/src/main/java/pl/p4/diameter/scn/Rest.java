package pl.p4.diameter.scn;
 
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.StringTokenizer;


import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
 
 public class Rest extends Step
 {
   private static Logger log = LoggerFactory.getLogger(Log.class);
   private String arguments;
   private final String USER_AGENT = "Mozilla/5.0";
 
   public Rest(String arguments)
   {
     super("HTTPGET", 1, RepeatType.SEQUENCE);
 
     this.arguments = arguments; // user:pass url json_str
     // example:
     // scott:tiger http://localhost:8080/RESTfulExample/json/product/get {"qty":100,"name":"iPad 4"}
     // if no authentification is required use n:n
   }
 
   public void run() {

	   String url="";
	   String user="";
	   String pass="";
	   String userpass="";
	   String jsonStr="";
	   
	   
	 try{   
		 log.info("Rest args: "+this.arguments);
		 ArrayList<String>	lineOpts = splitWords(this.arguments," ");
	     if(lineOpts.size()>=1){
	    	 userpass=lineOpts.get(0);
	         //ArrayList<String> userpass = splitWords(lineOpts.get(0),":");
	         //if (userpass.size()==2 && userpass.get(0)!="n"){
	         //       user=userpass.get(0);	
	         //       pass=userpass.get(1);	            	
	         //}
	     }
	     if(lineOpts.size()>=2){
	         url = lineOpts.get(1);
	     }  
	     if(lineOpts.size()>=3){
	         jsonStr = lineOpts.get(2);
	     } 
		 log.debug("REST user:pass "+user+":"+pass+ "  url:"+url+" json "+jsonStr); 		 
		 
		 
		 
         URL obj = new URL(url);
         HttpURLConnection con = (HttpURLConnection) obj.openConnection();
         int responseCode = -1;
         if(jsonStr.equals("")){  // this is GET
    		        log.info("Rest going GET user:pass="+userpass);
        			con.setRequestMethod("GET");
        			String basicAuth = "Basic " + new String(Base64.encodeBase64(userpass.getBytes())); // userpass=user:pass
        			con.setRequestProperty ("Authorization", basicAuth);
        			//con.setRequestProperty("Accept", "application/json");
        			con.setRequestProperty("Accept", "application/json");
        			responseCode = con.getResponseCode();
        			if (responseCode != 200) {
        				throw new RuntimeException("Failed : HTTP error code : " + con.getResponseCode());
        			}
                    // follows reading
         }else{
		        log.info("Rest going POST user:pass="+userpass+" writing :"+jsonStr);

        			con.setDoOutput(true);
        			con.setRequestMethod("POST");
        			String basicAuth = "Basic " + new String(Base64.encodeBase64(userpass.getBytes())); // userpass=user:pass
        			con.setRequestProperty ("Authorization", basicAuth);
        			con.setRequestProperty("Content-Type", "application/json");

        			OutputStream os = con.getOutputStream();
        			os.write(jsonStr.getBytes());
        			os.flush();
        			responseCode = con.getResponseCode();
        			if (responseCode !=200 && responseCode != HttpURLConnection.HTTP_CREATED) { 
        				throw new RuntimeException("Failed : HTTP error code : "
        						+ con.getResponseCode());
        			}
        			// follows reading
                	
                	
         }
                
                

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
         log.info("Rest: ["+ url+ "] ("+ responseCode+ ") => "+response.toString());
	 }catch(Exception e){
		 log.error("Rest: ["+ url+ "] ("+printStackTrace(e));
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
   private static   String printStackTrace(Exception ex){
	     StringWriter errors = new StringWriter();
	     ex.printStackTrace(new PrintWriter(errors)); 
	     return ex.getMessage()+" -> "+errors.toString();   
	   }

 }

