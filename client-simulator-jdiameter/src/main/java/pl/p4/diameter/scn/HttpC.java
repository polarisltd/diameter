package pl.p4.diameter.scn;
 
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.auth.BasicScheme;
import org.apache.commons.httpclient.methods.GetMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import sun.net.www.http.HttpClient;
 


 public class HttpC extends Step
 {
   private static Logger log = LoggerFactory.getLogger(Log.class);
   private String arguments;

   private final String USER_AGENT = "Mozilla/5.0";
 
   public HttpC(String arguments)
   {
     super("HTTPC", 1, RepeatType.SEQUENCE);
 
     this.arguments = arguments; // user:pass url
   }
 
   public void run() {

   String url="";
   String user="";
   String pass="";
	   
   HttpGet httpGet=null;
   GetMethod get=null;
   try{

	 user="embla";
     pass="teligent";
     
     url = this.arguments; 
     
     HttpClient client = new HttpClient();
     // an arbitrary realm or host change the appropriate argument to null.
     client.getState().setCredentials(
         new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT),
         new UsernamePasswordCredentials(user, pass)
     );
     
     
     // create a GET method that reads a file over HTTPS, we're assuming
     // that this file requires basic authentication using the realm above.
     get = new GetMethod(url);

     // Tell the GET method to automatically handle authentication. The
     // method will use any appropriate credentials to handle basic
     // authentication requests.  Setting this value to false will cause
     // any request for authentication to return with a status of 401.
     // It will then be up to the client to handle the authentication.
     get.setDoAuthentication( true );

         // execute the GET
         int status = client.executeMethod( get );

         // print the status and response
         //System.out.println(status + "\n" + get.getResponseBodyAsString());
         log.info("HTTPC: ["+ url+ "] "+status+" => "+ get.getResponseBodyAsString());
     }catch(Exception e){
         log.error("HTTPC: ["+ url+ "] ("+Arrays.toString(e.getStackTrace()));
     } finally {
         // release any connection resources used by the method
         get.releaseConnection();
     }

    
   }//run()
 }

