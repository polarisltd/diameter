package org.mobicents.servers.diameter.charging;

import com.sun.net.httpserver.HttpContext;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.Map;

// httpserver
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.sql.Timestamp;
import java.net.InetSocketAddress;
//
import org.jdiameter.api.Answer;
import org.jdiameter.api.ApplicationId;
import org.jdiameter.api.Avp;
import org.jdiameter.api.AvpDataException;
import org.jdiameter.api.AvpSet;
import org.jdiameter.api.Configuration;
import org.jdiameter.api.EventListener;
import org.jdiameter.api.InternalException;
import org.jdiameter.api.Mode;
import org.jdiameter.api.Network;
import org.jdiameter.api.NetworkReqListener;
import org.jdiameter.api.Peer;
import org.jdiameter.api.Request;
import org.jdiameter.api.SessionFactory;
import org.jdiameter.api.Stack;
import org.jdiameter.api.app.AppAnswerEvent;
import org.jdiameter.api.app.AppRequestEvent;
import org.jdiameter.api.app.AppSession;
import org.jdiameter.api.auth.events.ReAuthAnswer;
import org.jdiameter.api.auth.events.ReAuthRequest;
import org.jdiameter.api.gx.ClientGxSession;
import org.jdiameter.api.gx.ServerGxSession;
import org.jdiameter.api.gx.events.GxReAuthRequest;
import org.jdiameter.api.gx.events.GxReAuthAnswer;
import org.jdiameter.common.impl.app.gx.GxReAuthRequestImpl;
import org.jdiameter.api.gx.events.GxCreditControlAnswer;
import org.jdiameter.api.gx.events.GxCreditControlRequest;
import org.jdiameter.api.gx.events.GxReAuthAnswer;
import org.jdiameter.api.gx.events.GxReAuthRequest;


import org.jdiameter.api.validation.AvpRepresentation;
import org.jdiameter.client.api.ISessionFactory;
import org.jdiameter.client.impl.helpers.XMLConfiguration;
import org.jdiameter.common.impl.app.gx.GxSessionFactoryImpl;
import org.jdiameter.common.impl.app.gx.GxCreditControlAnswerImpl;
import org.jdiameter.server.impl.app.gx.ServerGxSessionImpl;
import org.mobicents.diameter.dictionary.AvpDictionary;
import org.mobicents.servers.diameter.utils.DiameterUtilities;
import org.mobicents.servers.diameter.utils.StackCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import java.io.IOException;
import com.sun.net.httpserver.HttpHandler;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;


/**
 * Mobicents Diameter Charging Server Simulator.
 * 
 * @author <a href="mailto:brainslog@gmail.com"> Alexandre Mendonca </a>
 */
public class ChargingServerSimulator extends GxSessionFactoryImpl /*implements NetworkReqListener, EventListener<Request, Answer> */ implements HttpHandler{

final int  Result_Code=268;
final int  CC_Request_Number=415;
final int  CC_Request_Type=416;
final int  CC_Time=420;
final int  Granted_Service_Unit=431;
final int  Rating_Group=432;
      final int  Subscription_Id=443;
      final int  Subscription_Id_Data=444;
      final int  CC_Total_Octets = 421; // unsigned64
      final int  Multiple_Services_Credit_Control = 456;
      final int  Requested_Service_Unit=437;
      final int  CC_Input_Octets = 412; // unsigned64
      final int  CC_Output_Octets = 414;// unsigned64
      final int  Service_Context_Id=461;

//GxCreditControlRequest lastRequest; //  used for RAR
  private static final Logger logger = LoggerFactory.getLogger(ChargingServerSimulator.class);

  //private static final Object[] EMPTY_ARRAY = new Object[]{};

  private HashMap<String, Long> accounts = new HashMap<String, Long>();
  private HashMap<String, Long> reserved = new HashMap<String, Long>();

  //---------------
  
  private static Stack stack;
  private static SessionFactory factory;
  static StackSetup setup;
  String testSetup;
  ChargingServerSimulator sim;
  ServerGxSession ses;
  GxCreditControlRequest req;
  private String httpHost;
  private Integer httpPort=8088;
  /**
   * @param args
   * @throws Exception 
   */
  public static void main(String[] args) throws Exception{
    setup = new StackSetup();
    new ChargingServerSimulator();
  }

  
  // StackCreator stackCreator = null; refer to setup.stackCreator instead!
  private String testId;
  //ISessionFactory sessionFactory1;
  public ChargingServerSimulator() throws Exception {
      super(setup.getSessionFactory());
      //
      //stackCreator = setup.stackCreator;
//      this.setup = su;
        String path = "/test"; // path starting from slash (/)
        String path2 = "/set";
        InetSocketAddress addr = new InetSocketAddress(httpPort);
        httpHost = addr.toString();
        HttpServer server = HttpServer.create(addr, 0);
        //server.createContext(path, this);
        HttpContext context = server.createContext(path, this);
        context.getFilters().add(new ParameterFilter());
        //
        context = server.createContext(path2, this);
        context.getFilters().add(new ParameterFilter());


        logger.info("HttpServer host:"+httpHost+path);
        logger.info("HttpServer host:"+httpHost+path2);
        
        server.setExecutor(null); // creates a default executor
        server.start();
        //
        this.testId="";
        this.sim=this;
    AvpDictionary.INSTANCE.parseDictionary(this.getClass().getClassLoader().getResourceAsStream("dictionary.xml"));

    try {

      ((ISessionFactory) sessionFactory).registerAppFacory(ClientGxSession.class, this);
      ((ISessionFactory) sessionFactory).registerAppFacory(ServerGxSession.class, this);

      // Read users from properties file
      Properties properties = new Properties();
      try {
        InputStream is = this.getClass().getClassLoader().getResourceAsStream("accounts.properties");
        if(is == null) {
          throw new IOException("InputStream is null");
        }
        properties.load(is);
        for(Object property : properties.keySet()) {
          String accountName = (String) property;
          String balance = (String) properties.getProperty(accountName,  "0");
          if(logger.isInfoEnabled()) {
            logger.info("Provisioned user '" + accountName + "' with [" + balance + "] units.");
          }
          accounts.put(accountName, Long.valueOf(balance));
        }
      }
      catch (IOException e) {
        System.err.println("Failed to read 'accounts.properties' file. Aborting.");
        System.exit(-1);
      }
    }
    catch (Exception e) {
      logger.error("Failure initializing Mobicents Diameter Ro/Rf Server Simulator", e);
    }
  }


        public void handle(HttpExchange t) throws IOException {

        	
        String path = t.getHttpContext().getPath(); 

        if(path.equals("/test")){        	
        	        	
        Map<String, String> testOption = new HashMap<String, String>();    
        testOption.put("0", "Test reset");
        testOption.put("2", "FUI redirectAction=0 at CCA CC-Request-Type=2 and CC-Request-Number=2");
        testOption.put("3", "FUI with Redirect URL at CCA CC-Request-Type=2 and CC-Request-Number=2");
        testOption.put("4", "Test setup RAR, RAR has been sent after CC-Request-Type=2 and CC-Request-Number=2");
        testOption.put("5", "test setup CCSF=FAILOVER_SUPPORTED, CCFH=TERMINATE, DIAMETER_RESULT_CODE=DIAMETER_UNABLE_TO_DELIVER/3002, CCR-I");
        testOption.put("6", "test setup CCSF=FAILOVER_SUPPORTED, CCFH=TERMINATE, DIAMETER_RESULT_CODE=DIAMETER_UNABLE_TO_DELIVER/3002, CCR-U");
        testOption.put("10", "test setup - Dont respond (CCA-U) to CCR-U CC-Record-Number=2");
        

           
            
            java.util.Date d= new java.util.Date();
            //
            Map params = (Map)t.getAttribute("parameters");

            String title="";
            String response = "";
            logger.info("HTTP request  path:"+path+"   attrib:"+params.toString()+" "+new Timestamp(d.getTime()));
            if(!params.containsKey("id")){
                response = formatHelp(testOption);
            }else{
              testId =  (String)params.get("id"); // class instance variable
              logger.info("http test setup id="+testId);
              title = testOption.get(testId);
              if (title == null) title="N/A";  
              response = ""+new Timestamp(d.getTime())+" "+params.toString()+" id="+testId+" => "+title;
            }
            
            //
            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }else if(path.equals("/set")){  
        	Map params = (Map)t.getAttribute("parameters");
        	String accountName =  null; 
        	String balance =  null; 
        	String text = "usage: /set?u=75757575&b=200\n set balance 200 to sunscriber 75757575\n";
        	  if(params.containsKey("u")){
        		  accountName =  (String)params.get("u"); // class instance variable
        	  }
        	  if(params.containsKey("b")){
        		  balance =  (String)params.get("b"); // class instance variable
        	  }
        	  if(accountName!=null &&  balance!=null){
        		  accounts.put(accountName, Long.valueOf(balance)); 
        		  text = "Subsciber "+accountName+" balance:"+ balance;
        		  
        	  }
              String response = ""+new Timestamp(new java.util.Date().getTime())+" "+params.toString()+"\n"+text+" \n"+accounts.toString();
              //
              t.sendResponseHeaders(200, response.length());
              OutputStream os = t.getResponseBody();
              os.write(response.getBytes());
              os.close();       	
        	
        }
        }


    public String formatHelp(Map<String,String> testOption){
        StringBuffer out = new StringBuffer()
                .append("<html>")
                .append("<title>Test Setup</title>")
                .append("<body>")
                .append("<h1>Diameter simulator test setup page</h1>")
                .append("<p>Provide test setup via http://"+httpHost+"/test?id=<test_id></p>")
                .append("<p>Update test data via http://"+httpHost+"/set</p>")
                .append("<p>Following tests are available:</p><table>")
                ;
        for(String k : testOption.keySet()){
            out.append("<tr><td>").append(k).append(" = ").append(testOption.get(k)).append("</td></tr>");
        }        
        out.append("</table><p> To change test data please use http://host/set</p>");
        out.append("</body></html>"); 
        return out.toString();
         
     }   
        
        
        
        

  private void printLogo() {
    if(logger.isInfoEnabled()) {
      Properties sysProps = System.getProperties();

      String osLine = sysProps.getProperty("os.name") + "/" + sysProps.getProperty("os.arch");
      String javaLine = sysProps.getProperty("java.vm.vendor") + " " + sysProps.getProperty("java.vm.name") + " " + sysProps.getProperty("java.vm.version");

      Peer localPeer = setup.stackCreator.getMetaData().getLocalPeer();

      String diameterLine = localPeer.getProductName() + " (" +  localPeer.getUri() + " @ " + localPeer.getRealmName() + ")";
      //localPeer.
      logger.info("===============================================================================");
      logger.info("");
      logger.info("== Mobicents Diameter Ro/Rf Server Simulator (" + osLine + ")" );
      logger.info("");
      logger.info("== " + javaLine);
      logger.info("");
      logger.info("== " + diameterLine);
      logger.info("");
      logger.info("===============================================================================");
    }
  }

/*  
  public Answer processRequest(Request request) {
    if(logger.isInfoEnabled()) {
      logger.info("<< Received Request [" + request + "]");      
    }
    try {
    	ServerGxSessionImpl session = (sessionFactory).getNewAppSession(request.getSessionId(), ApplicationId.createByAuthAppId(0, 4), ServerGxSession.class, EMPTY_ARRAY);
      session.processRequest(request);
    }
    catch (InternalException e) {
      logger.error(">< Failure handling received request.", e);
    }

    return null;
  }
*/
/**
  public void receivedSuccessMessage(Request request, Answer answer) {
    if(logger.isInfoEnabled()) {
      logger.info("<< Received Success Message for Request [" + request + "] and Answer [" + answer + "]");
    }
  }

  @Override
  public void timeoutExpired(Request request) {
    if(logger.isInfoEnabled()) {
      logger.info("<< Received Timeout for Request [" + request + "]");
    }
  }
**/
  public void doCreditControlAnswer(ClientGxSession session, GxCreditControlRequest request, GxCreditControlAnswer answer) throws InternalException {
    // Do nothing.
  }

  public void doOtherEvent(AppSession session, AppRequestEvent request, AppAnswerEvent answer) throws InternalException {
    // Do nothing.
  }

  public void doReAuthRequest(ClientGxSession session, ReAuthRequest request) throws InternalException {
    // Do nothing.
  }

  public void doCreditControlRequest(ServerGxSession session, GxCreditControlRequest request) throws InternalException {

    String[] requestType = {"INITIAL","UPDATE","TERMINATE","EVENT"};  
	  
    DiameterUtilities.printMessage(request.getMessage());  // roberts
    
        
    AvpSet ccrAvps = request.getMessage().getAvps();

    GxCreditControlAnswer cca = null;

    long requestNumber=-1;
    try{
       requestNumber = ccrAvps.getAvp(Avp.CC_REQUEST_NUMBER).getUnsigned32();
    }catch(Exception e){              
    }
    
    
    
    switch (request.getRequestTypeAVPValue()) {
      // INITIAL_REQUEST                 1
    case 1:
      // UPDATE_REQUEST                  2
    case 2:
      if(logger.isInfoEnabled()) {
        logger.info("<< [doCreditControlRequest] Received Credit-Control-Request [" + requestType[request.getRequestTypeAVPValue()-1] + "]");
      }

      final int CCFH = 427;
      final int CCSF = 418;
      final int CCSF_FAILOVER_SUPPORTED=1;
      final int CCFH_TERMINATE=0;
      
      /*
       * <avp name="Credit-Control-Failure-Handling" code="427" mandatory="must" vendor-bit="mustnot" vendor-id="None" may-encrypt="yes" protected="may">
			<type type-name="Unsigned32" />
			<enum name="TERMINATE" code="0" />
         <avp name="CC-Session-Failover" code="418" mandatory="must" vendor-bit="mustnot" vendor-id="None" may-encrypt="yes" protected="may">
			<type type-name="Unsigned32" />
			<enum name="FAILOVER_NOT_SUPPORTED" code="0" />
			<enum name="FAILOVER_SUPPORTED" code="1" />

      */
      if(this.testId.equals("10") && request.getRequestTypeAVPValue()==2 && requestNumber==2){
           return; // dont send CCA.
      }
      if(this.testId.equals("5") && request.getRequestTypeAVPValue()==1 ||
         this.testId.equals("6") && request.getRequestTypeAVPValue()==2){  
        try{  
          logger.info("Triggering test05/test06"); 
          cca = createCCA(session, request, -1, 3002);
          //cca.getMessage().setError(true); IF setError() is executed no CCA has been delivered back to client e.g. being filtered!
 
          logger.info("Executing test 5 - CCSF=FAILOVER_SUPPORTED, CCFH=TERMINATE, DIAMETER_RESULT_CODE=DIAMETER_UNABLE_TO_DELIVER/3002");
  
          cca.getMessage().getAvps().addAvp(CCFH, CCFH_TERMINATE);
          cca.getMessage().getAvps().addAvp(CCSF, CCSF_FAILOVER_SUPPORTED);
          DiameterUtilities.printMessage(cca.getMessage());
          session.sendCreditControlAnswer(cca);
        }catch(Exception e){
           logger.info("Exception within test5:");
           e.printStackTrace();
        } 
        break;
      }else if(this.testId.equals("7") && request.getRequestTypeAVPValue()==1 ){
          // THIS IS RAR
      }
      
      if(this.testId.equals("4") && request.getRequestTypeAVPValue()==2 && requestNumber==2){
          //this.testId = "0"; // reset test condition
          //
          // We need this in place where CCA is returned from prepaid so we can verify FUI and send RAR
          //
          logger.info("Triggering RAR timer");
          SendRar rarCmd = new SendRar(session,request);
          new Timer().schedule(rarCmd, 2000);
          //new Thread(rar).start();     
                   
      } 
      
      
      
      
      try {

        long requestedUnits = 0;

        try{
          AvpSet av = ccrAvps.getAvp(Avp.MULTIPLE_SERVICES_CREDIT_CONTROL).getGrouped().getAvp(Avp.REQUESTED_SERVICE_UNIT).getGrouped(); // fails if no mscc.rsu is present    
          Avp a; 
          a = av.getAvp(Avp.CC_TOTAL_OCTETS);
          if(a!=null){
            requestedUnits+=a.getUnsigned64();
            //usedUnitAvpCode=CC_Total_Octets;
          }
          a = av.getAvp(Avp.CC_INPUT_OCTETS);
          if(a!=null){
            requestedUnits+=a.getUnsigned64();
            //usedUnitAvpCode=CC_Input_Octets;
          }
          a = av.getAvp(Avp.CC_OUTPUT_OCTETS);
          if(a!=null){
            requestedUnits+=a.getUnsigned64();
            //usedUnitAvpCode=CC_Output_Octets;
          }
        }catch(Exception e){
          logger.error("!!!!!Error accessing RSU => 0 "+e.getStackTrace());
        }

        logger.info("RSU = "+requestedUnits);


        String subscriptionId = "";
        try{
          subscriptionId = ccrAvps.getAvp(Avp.SUBSCRIPTION_ID).getGrouped().getAvp(Avp.SUBSCRIPTION_ID_DATA).getUTF8String();
        }catch(Exception e){
          logger.error("!!!!!Error accessing SubscriptionId => 0");
        }

        String serviceContextId = ccrAvps.getAvp(Avp.SERVICE_CONTEXT_ID).getUTF8String();

        if(logger.isInfoEnabled()) {
          logger.info(">> '" + subscriptionId + "' requested " + requestedUnits + " units for '" + serviceContextId + "'.");
        }

        Long balance = accounts.get(subscriptionId);
        if(balance != null) {
          if(balance <= 0) {
            //    DIAMETER_CREDIT_LIMIT_REACHED              4012
            // The credit-control server denies the service request because the
            // end user's account could not cover the requested service.  If the
            // CCR contained used-service-units they are deducted, if possible.
            cca = createCCA(session, request, -1, 4012);
            if(logger.isInfoEnabled()) {
              logger.info("<> '" + subscriptionId + "' has insufficient credit units. Rejecting.");
            }
          }
          else {
            // Check if not first request, should have Used-Service-Unit AVP
            if(ccrAvps.getAvp(Avp.CC_REQUEST_NUMBER) != null && ccrAvps.getAvp(Avp.CC_REQUEST_NUMBER).getUnsigned32() >= 1) {
              Avp usedServiceUnit = ccrAvps.getAvp(Avp.USED_SERVICE_UNIT);
              if(usedServiceUnit != null) {
                Long wereReserved = reserved.remove(subscriptionId + "_" + serviceContextId);
                wereReserved = wereReserved == null ? 0 : wereReserved; 
                long wereUsed = usedServiceUnit.getGrouped().getAvp(Avp.CC_TIME).getUnsigned32();
                long remaining = wereReserved - wereUsed;

                if(logger.isInfoEnabled()) {
                  logger.info(">> '" + subscriptionId + "' had " + wereReserved + " reserved units, " + wereUsed + " units were used. (rem: " + remaining + ").");
                }
                balance += remaining;
              }
            }

            long grantedUnits = Math.min(requestedUnits, balance);
            cca = createCCA(session, request, grantedUnits, 2001);

            reserved.put(subscriptionId + "_" + serviceContextId, grantedUnits);
            balance -= grantedUnits;
            if(logger.isInfoEnabled()) {
              logger.info(">> '" + subscriptionId + "' Balance: " + (balance + grantedUnits) + " // Available(" + balance + ")  Reserved(" + grantedUnits + ")");
            }
            accounts.put(subscriptionId, balance);

            // Check if the user has no more credit
            if(balance <= 0 ||  ((this.testId.equals("3")||this.testId.equals("2"))&& request.getRequestTypeAVPValue()==2 && requestNumber==2)) {
              logger.info("triggered test2 or test3 FUI or balance<=0");
            	// 8.34.  Final-Unit-Indication AVP
              // 
              // The Final-Unit-Indication AVP (AVP Code 430) is of type Grouped and
              // indicates that the Granted-Service-Unit AVP in the Credit-Control-
              // Answer, or in the AA answer, contains the final units for the
              // service.  After these units have expired, the Diameter credit-control
              // client is responsible for executing the action indicated in the
              // Final-Unit-Action AVP (see section 5.6).
              // 
              // If more than one unit type is received in the Credit-Control-Answer,
              // the unit type that first expired SHOULD cause the credit-control
              // client to execute the specified action.
              // 
              // In the first interrogation, the Final-Unit-Indication AVP with
              // Final-Unit-Action REDIRECT or RESTRICT_ACCESS can also be present
              // with no Granted-Service-Unit AVP in the Credit-Control-Answer or in
              // the AA answer.  This indicates to the Diameter credit-control client
              // to execute the specified action immediately.  If the home service
              // provider policy is to terminate the service, naturally, the server
              // SHOULD return the appropriate transient failure (see section 9.1) in
              // order to implement the policy-defined action.
              // 
              // The Final-Unit-Action AVP defines the behavior of the service element
              // when the user's account cannot cover the cost of the service and MUST
              // always be present if the Final-Unit-Indication AVP is included in a
              // command.
              // 
              // If the Final-Unit-Action AVP is set to TERMINATE, no other AVPs MUST
              // be present.
              // 
              // If the Final-Unit-Action AVP is set to REDIRECT at least the
              // Redirect-Server AVP MUST be present.  The Restriction-Filter-Rule AVP
              // or the Filter-Id AVP MAY be present in the Credit-Control-Answer
              // message if the user is also allowed to access other services that are
              // not accessible through the address given in the Redirect-Server AVP.
              // 
              // If the Final-Unit-Action AVP is set to RESTRICT_ACCESS, either the
              // Restriction-Filter-Rule AVP or the Filter-Id AVP SHOULD be present.
              // 
              // The Filter-Id AVP is defined in [NASREQ].  The Filter-Id AVP can be
              // used to reference an IP filter list installed in the access device by
              // means other than the Diameter credit-control application, e.g.,
              // locally configured or configured by another entity.
              // 
              // The Final-Unit-Indication AVP is defined as follows (per the
              // grouped-avp-def of RFC 3588 [DIAMBASE]):
              // 
              // Final-Unit-Indication ::= < AVP Header: 430 >
              //                           { Final-Unit-Action }
              //                          *[ Restriction-Filter-Rule ]
              //                          *[ Filter-Id ]
              //                           [ Redirect-Server ]
              AvpSet finalUnitIndicationAvp = cca.getMessage().getAvps().addGroupedAvp(Avp.FINAL_UNIT_INDICATION);

              // 8.35.  Final-Unit-Action AVP
              // 
              // The Final-Unit-Action AVP (AVP Code 449) is of type Enumerated and
              // indicates to the credit-control client the action to be taken when
              // the user's account cannot cover the service cost.
              // 
              // The Final-Unit-Action can be one of the following:
              // 
              // TERMINATE                       0
              //   The credit-control client MUST terminate the service session.
              //   This is the default handling, applicable whenever the credit-
              //   control client receives an unsupported Final-Unit-Action value,
              //   and it MUST be supported by all the Diameter credit-control client
              //   implementations conforming to this specification.
              // 
              // REDIRECT                        1
              //   The service element MUST redirect the user to the address
              //   specified in the Redirect-Server-Address AVP.  The redirect action
              //   is defined in section 5.6.2.
              // 
              // RESTRICT_ACCESS                 2
              //   The access device MUST restrict the user access according to the
              //   IP packet filters defined in the Restriction-Filter-Rule AVP or
              //   according to the IP packet filters identified by the Filter-Id
              //   AVP.  All the packets not matching the filters MUST be dropped
              //   (see section 5.6.3).
              short fuiAction=0;
              if( this.testId.equals("3"))fuiAction=1; 
              finalUnitIndicationAvp.addAvp(Avp.FINAL_UNIT_ACTION, fuiAction);
              if (fuiAction==1){ // 1=redirect
                  String url = "http://192.168.56.114:15417/scu/diameter/rar?SESSIONID=peer4.tecnomen.ie;1342615007;0&SITEID=1&RG=4&SERVICEID=1";
                  finalUnitIndicationAvp.addAvp(Avp.REDIRECT_ADDRESS,url,false,false,true);
              }
            }
          }
        }
        else {
          //    DIAMETER_USER_UNKNOWN                      5030
          // The specified end user is unknown in the credit-control server.
          cca = createCCA(session, request, -1, 5030);
          // cca.getMessage().setError(true); DONT USE THIS AS setError(true) makes CCA is being filtered out!  
          if(logger.isInfoEnabled()) {
            logger.info("<> '" + subscriptionId + "' is not provisioned in this server. Rejecting.");
          }
        }

        //cca.getMessage().getAvps().addAvp(461, serviceContextId, false);
        session.sendCreditControlAnswer(cca);
      }
      catch (Exception e) {
        logger.error(">< Failure processing Credit-Control-Request [" + (request.getRequestTypeAVPValue() == 1 ? "INITIAL" : "UPDATE") + "]", e);
      }
      break;
      // TERMINATION_REQUEST             3
    case 3:
      if(logger.isInfoEnabled()) {
        logger.info("<< Received Credit-Control-Request [TERMINATION]");
      }
      try {
        String subscriptionId = ccrAvps.getAvp(Avp.SUBSCRIPTION_ID).getGrouped().getAvp(Avp.SUBSCRIPTION_ID_DATA).getUTF8String();
        String serviceContextId = ccrAvps.getAvp(Avp.SERVICE_CONTEXT_ID).getUTF8String();

        if(logger.isInfoEnabled()) {
          logger.info(">> '" + subscriptionId + "' requested service termination for '" + serviceContextId + "'.");
        }

        Long balance = accounts.get(subscriptionId);

        if(ccrAvps.getAvp(Avp.CC_REQUEST_NUMBER) != null && ccrAvps.getAvp(Avp.CC_REQUEST_NUMBER).getUnsigned32() >= 1) {
          Avp usedServiceUnit = ccrAvps.getAvp(Avp.USED_SERVICE_UNIT);
          if(usedServiceUnit != null) {
            long wereReserved = reserved.remove(subscriptionId + "_" + serviceContextId);
            long wereUsed = usedServiceUnit.getGrouped().getAvp(Avp.CC_SERVICE_SPECIFIC_UNITS).getUnsigned32();
            long remaining = wereReserved - wereUsed;

            if(logger.isInfoEnabled()) {
              logger.info(">> '" + subscriptionId + "' had " + wereReserved + " reserved units, " + wereUsed + " units were used. (non-used: " + remaining + ").");
            }
            balance += remaining;
          }
        }

        if(logger.isInfoEnabled()) {
          logger.info(">> '" + subscriptionId + "' Balance: " + balance + " // Available(" + balance + ")  Reserved(0)");
        }
        accounts.put(subscriptionId, balance);

        cca = createCCA(session, request, -1, 2001);
        // 8.7.  Cost-Information AVP
        // 
        // The Cost-Information AVP (AVP Code 423) is of type Grouped, and it is
        // used to return the cost information of a service, which the credit-
        // control client can transfer transparently to the end user.  The
        // included Unit-Value AVP contains the cost estimate (always type of
        // money) of the service, in the case of price enquiry, or the
        // accumulated cost estimation, in the case of credit-control session.
        // 
        // The Currency-Code specifies in which currency the cost was given.
        // The Cost-Unit specifies the unit when the service cost is a cost per
        // unit (e.g., cost for the service is $1 per minute).
        // 
        // When the Requested-Action AVP with value PRICE_ENQUIRY is included in
        // the Credit-Control-Request command, the Cost-Information AVP sent in
        // the succeeding Credit-Control-Answer command contains the cost
        // estimation of the requested service, without any reservation being
        // made.
        // 
        // The Cost-Information AVP included in the Credit-Control-Answer
        // command with the CC-Request-Type set to UPDATE_REQUEST contains the
        // accumulated cost estimation for the session, without taking any
        // credit reservation into account.
        // 
        // The Cost-Information AVP included in the Credit-Control-Answer
        // command with the CC-Request-Type set to EVENT_REQUEST or
        // TERMINATION_REQUEST contains the estimated total cost for the
        // requested service.
        // 
        // It is defined as follows (per the grouped-avp-def of
        // RFC 3588 [DIAMBASE]):
        // 
        //           Cost-Information ::= < AVP Header: 423 >
        //                                { Unit-Value }
        //                                { Currency-Code }
        //                                [ Cost-Unit ]

        // 7.2.133 Remaining-Balance AVP
        //
        // The Remaining-Balance AVP (AVPcode 2021) is of type Grouped and 
        // provides information about the remaining account balance of the 
        // subscriber.
        //
        // It has the following ABNF grammar:
        //      Remaining-Balance :: =  < AVP Header: 2021 >
        //                              { Unit-Value }
        //                              { Currency-Code }

        // We use no money notion ... maybe later. 
        // AvpSet costInformation = ccaAvps.addGroupedAvp(423);

        session.sendCreditControlAnswer(cca);
      }
      catch (Exception e) {
        logger.error(">< Failure processing Credit-Control-Request [TERMINATION]", e);
      }
      break;
      // EVENT_REQUEST                   4
    case 4:
      if (logger.isInfoEnabled()) {
        logger.info("<< Received Credit-Control-Request [EVENT]");
      }
      /////////////////////////////////////////////////////
      cca = null;
      try {
    	  
    	Avp avp;
    	avp = DiameterUtilities.getAvpByCode(ccrAvps.getAvp(Avp.MULTIPLE_SERVICES_CREDIT_CONTROL),Avp.CC_SERVICE_SPECIFIC_UNITS);
    	long requestedUnits = (avp!=null)?avp.getUnsigned64():0;

    	avp = DiameterUtilities.getAvpByCode(ccrAvps.getAvp(Avp.SUBSCRIPTION_ID),Avp.SUBSCRIPTION_ID_DATA);
  		String subscriptionId = (avp!=null)?avp.getUTF8String():"";
        // 
        
        String serviceContextId = ccrAvps.getAvp(Avp.SERVICE_CONTEXT_ID).getUTF8String();

        if(logger.isInfoEnabled()) {
          logger.info(">> '" + subscriptionId + "' requested " + requestedUnits + " units for '" + serviceContextId + "'.");
        }

        Long balance = accounts.get(subscriptionId);
        if(balance == null) {
            //    DIAMETER_USER_UNKNOWN                      5030
            cca = createCCA(session, request, -1, 5030);
            // cca.getMessage().setError(true); DONT USE THIS AS setError(true) makes CCA is being filtered out!  

            if(logger.isInfoEnabled()) {
              logger.info("<> '" + subscriptionId + "' Unknown account. Rejecting 5030.");
            }
        
        }else if(balance <= 0) {
            //    DIAMETER_CREDIT_LIMIT_REACHED              4012
            // The credit-control server denies the service request because the
            // end user's account could not cover the requested service.  If the
            // CCR contained used-service-units they are deducted, if possible.
            cca = createCCA(session, request, -1, 4012);
            if(logger.isInfoEnabled()) {
              logger.info("<> '" + subscriptionId + "' has insufficient credit units. Rejecting 4012.");
            }
         } else {

        	 
             	long grantedUnits = 0;  
                //long wereRequested = requestedServiceUnits.getGrouped().getAvp(417).getUnsigned32();
                if(balance>requestedUnits) grantedUnits = requestedUnits;
                else grantedUnits = balance;
                balance -= grantedUnits;                
                accounts.put(subscriptionId, balance);

                cca = createCCA(session, request, grantedUnits, 2001);

                if(logger.isInfoEnabled()) {
                    logger.info(">> '" + subscriptionId + "' Requested " + requestedUnits + " Granted " + grantedUnits + " and remaining balance: " + balance + ").");
                }
              
          }

        if(cca==null){
        	  cca = createCCA(session, request, -1, 3002);  // unable to deliver
              if(logger.isInfoEnabled()) {
                  logger.info(">> '" + subscriptionId + "' Unable to deliver as CCA has not been created! 3002");
              }

        }
        
        //cca.getMessage().getAvps().addAvp(461, serviceContextId, false);
        session.sendCreditControlAnswer(cca);
      }
      catch (Exception e) {
        logger.error(">< Failure processing Credit-Control-Request [" + (request.getRequestTypeAVPValue() == 1 ? "INITIAL" : "UPDATE") + "]", e);
      }
      break;
    default:
      break;
    }
  }

  public void doReAuthAnswer(ServerGxSession session, ReAuthRequest request, ReAuthAnswer answer) throws InternalException {
    // Do Nothing.
  }

  public void sessionSupervisionTimerExpired(ServerGxSession session) {
    // Do Nothing.
  }

  public void denyAccessOnTxExpire(ServerGxSession clientCCASessionImpl) {
    // Do Nothing.
  }

  public void txTimerExpired(ServerGxSession session) {
    // Do Nothing.
  }

  private GxCreditControlAnswer createCCA(ServerGxSession session, GxCreditControlRequest request, long grantedUnits, long resultCode) throws InternalException, AvpDataException {
	  GxCreditControlAnswerImpl answer = new GxCreditControlAnswerImpl((Request) request.getMessage(), resultCode);

    AvpSet ccrAvps = request.getMessage().getAvps();
    AvpSet ccaAvps = answer.getMessage().getAvps();


//final int  CC_Request_Number=415;
//final int  CC_Request=Type=416;
//final int  CC_Time=420;
//final int  Granted_Service_Unit=431;





    // <Credit-Control-Answer> ::= < Diameter Header: 272, PXY >
    //  < Session-Id >
    //  { Result-Code }
    //  { Origin-Host }
    //  { Origin-Realm }
    Peer localPeer = setup.stackCreator.getMetaData().getLocalPeer();
    ccaAvps.addAvp(Avp.ORIGIN_HOST,localPeer.getUri());
    ccaAvps.addAvp(Avp.ORIGIN_REALM,localPeer.getRealmName(),false);		//ccaAvps.
    //  { Auth-Application-Id }

    //  { CC-Request-Type }
    // Using the same as the one present in request
    ccaAvps.addAvp(ccrAvps.getAvp(CC_Request_Type));

    //  { CC-Request-Number }
    // Using the same as the one present in request
    ccaAvps.addAvp(ccrAvps.getAvp(CC_Request_Number));

    //  [ User-Name ]
    //  [ CC-Session-Failover ]
    //  [ CC-Sub-Session-Id ]
    //  [ Acct-Multi-Session-Id ]
    //  [ Origin-State-Id ]
    //  [ Event-Timestamp ]

    //  [ Granted-Service-Unit ]
    // 8.17.  Granted-Service-Unit AVP
    //
    // Granted-Service-Unit AVP (AVP Code 431) is of type Grouped and
    // contains the amount of units that the Diameter credit-control client
    // can provide to the end user until the service must be released or the
    // new Credit-Control-Request must be sent.  A client is not required to
    // implement all the unit types, and it must treat unknown or
    // unsupported unit types in the answer message as an incorrect CCA
    // answer.  In this case, the client MUST terminate the credit-control
    // session and indicate in the Termination-Cause AVP reason
    // DIAMETER_BAD_ANSWER.
    //
    // The Granted-Service-Unit AVP is defined as follows (per the grouped-
    // avp-def of RFC 3588 [DIAMBASE]):
    //
    // Granted-Service-Unit ::= < AVP Header: 431 >
    //                          [ Tariff-Time-Change ]
    //                          [ CC-Time ]
    //                          [ CC-Money ]
    //                          [ CC-Total-Octets ]
    //                          [ CC-Input-Octets ]
    //                          [ CC-Output-Octets ]
    //                          [ CC-Service-Specific-Units ]
    //                         *[ AVP ]
    
    AvpSet mscc = ccaAvps.addGroupedAvp(Multiple_Services_Credit_Control);
    mscc.addAvp(Rating_Group,1);

    if(grantedUnits >= 0) {
      AvpSet gsuAvp = mscc.addGroupedAvp(Granted_Service_Unit); //431
      mscc.addAvp(Result_Code,2001);
      // Fetch AVP/Value from Request
      // gsuAvp.addAvp(ccrAvps.getAvp(437).getGrouped().getAvp(420));
     // Try to return same units as requested.
     int grantTypeAvpCode = CC_Total_Octets;
     try{
       AvpSet av = ccrAvps.getAvp(Multiple_Services_Credit_Control).getGrouped().getAvp(Requested_Service_Unit).getGrouped(); // fails if no mscc.rsu is present    
       Avp a = av.getAvpByIndex(0);
       grantTypeAvpCode = a.getCode();
     }catch(Exception e){
       logger.error("!!!! did not access request.mscc[0].rsu.avp[0]");      
     }
      gsuAvp.addAvp(grantTypeAvpCode, grantedUnits); // append as Unsigned64 as second argument is long 
    }

    // *[ Multiple-Services-Credit-Control ]
    //  [ Cost-Information]
    //  [ Final-Unit-Indication ]
    //  [ Check-Balance-Result ]
    //  [ Credit-Control-Failure-Handling ]
    //  [ Direct-Debiting-Failure-Handling ]
    //  [ Validity-Time]
    // *[ Redirect-Host]
    //  [ Redirect-Host-Usage ]
    //  [ Redirect-Max-Cache-Time ]
    // *[ Proxy-Info ]
    // *[ Route-Record ]
    // *[ Failed-AVP ]
    // *[ AVP ]

    if(logger.isInfoEnabled()) {
      logger.info(">> Created Credit-Control-Answer.");
      DiameterUtilities.printMessage(answer.getMessage());
    }

    return answer;
  }
/**********************/
  //org.mobicents.diameter.stack.functional.gx.AbstractServer
  // org.mobicents.diameter.stack.functional.gx.AbstractClient.java
  // org.mobicents.diameter.stack.functional.gx.base.Client
  // ... Server.java
  // ... GxSessionBasicFlowTest.java
  GxReAuthRequest createRAR(int reAuthRequestType, ServerGxSession gxSession, GxCreditControlRequest request) throws Exception {
    //  <RA-Request> ::= < Diameter Header: 258, REQ, PXY >
    //String serverUri = "aaa://192.168.1.7:3868"; // serverURINode1 = "aaa://" + serverHost + ":" + serverPortNode1; 

    //ApplicationId applicationId=gxSession.getSessionAppId(); -- this creates 0,4,0
    ApplicationId applicationId = ApplicationId.createByAuthAppId(0L, 4L);
    // public static ApplicationId createByAuthAppId(long vendorId, long authAppId)

    
    String clientReamName = request.getOriginRealm();
    String clientHostName = request.getOriginHost();
    logger.info("about to new GxReAuthRequestImpl() appid:"+applicationId+" realm:"+clientReamName+" host:"+clientHostName);
    GxReAuthRequest rar = new GxReAuthRequestImpl(gxSession.getSessions().get(0)
        .createRequest(ReAuthRequest.code, /*getApplicationId()*/applicationId, /*getClientRealmName()*/clientReamName,clientHostName));
    // gxSession = org.jdiameter.api.gx.ServerGxSession
// gxSession.getSessions() =  interface java.util.List<org.jdiameter.api.Session>
// gxSession.getSessions().get(0)= interface org.jdiameter.api.Session
    
    logger.info("ok, rar created!");

    //get server session activity

//GxServerSessionActivity.sendReAuthRequest(RAR);
    
    
    
    // AVPs present by default: Origin-Host, Origin-Realm, Session-Id,
    // Vendor-Specific-Application-Id, Destination-Realm
    AvpSet rarAvps = rar.getMessage().getAvps();

    //  < Session-Id >
    //  { Auth-Application-Id }
    //  { Origin-Host }
    //rarAvps.removeAvp(Avp.ORIGIN_HOST);
    //rarAvps.addAvp(Avp.ORIGIN_HOST, serverUri/*getServerURI()*/, true);

    //  { Origin-Realm }
    //  { Destination-Realm }
    //  { Destination-Host }
    //rarAvps.addAvp(Avp.DESTINATION_HOST, clientHostName);
    //  { Re-Auth-Request-Type }
    rarAvps.addAvp(Avp.RE_AUTH_REQUEST_TYPE, reAuthRequestType);
    //  [ Session-Release-Cause ]
    //  [ Origin-State-Id ]
    // *[ Event-Trigger ]
    //  [ Event-Report-Indication ]
    // *[ Charging-Rule-Remove ]
    // *[ Charging-Rule-Install ]
    //  [ Default-EPS-Bearer-QoS ]
    // *[ QoS-Information ]
    //  [ Revalidation-Time ]
    // *[ Usage-Monitoring-Information ]
    // *[ Proxy-Info ]
    // *[ Route-Record ]
    // *[ AVP]
    
    return rar;
  }
  
class SendRar extends TimerTask  {
     ServerGxSession ses;
     GxCreditControlRequest req;

     public SendRar(ServerGxSession _ses, GxCreditControlRequest _req) {
         this.ses = _ses;
         this.req = _req;
     }

     @Override
     public void run() {
          logger.info("ActivatedvSendRar run(");                
            try{
                GxReAuthRequest rar=sim.createRAR(0,this.ses,this.req);
                logger.info("Rar created!"); 
                DiameterUtilities.printMessage(rar.getMessage());
                logger.info("about to send Rar!"); 
                ses.sendGxReAuthRequest(rar);
            }catch(Exception e){
                StringWriter errors = new StringWriter();
                e.printStackTrace(new PrintWriter(errors));
                logger.info("Exception within RAR send: "+errors.toString());
            }
     }
}  
  
  

} // class





