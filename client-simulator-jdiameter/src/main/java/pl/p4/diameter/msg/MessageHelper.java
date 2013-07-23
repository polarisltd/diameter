 package pl.p4.diameter.msg;
 
 import java.io.File;
 import java.io.IOException;
 import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
 import java.net.InetAddress;
 import java.net.UnknownHostException;
 import java.util.ArrayList;
import java.util.Arrays;
 import java.util.Date;
 import java.util.Iterator;
 import javax.xml.parsers.DocumentBuilder;
 import javax.xml.parsers.DocumentBuilderFactory;
 import javax.xml.parsers.ParserConfigurationException;
 import org.jdiameter.api.Answer;
 import org.jdiameter.api.ApplicationId;
 import org.jdiameter.api.Avp;
 import org.jdiameter.api.AvpDataException;
 import org.jdiameter.api.AvpSet;
 import org.jdiameter.api.EventListener;
 import org.jdiameter.api.Message;
 import org.jdiameter.api.Request;
 import org.jdiameter.api.Session;
 import org.jdiameter.api.annotation.AvpType;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 import org.w3c.dom.Document;
 import org.w3c.dom.NamedNodeMap;
 import org.w3c.dom.Node;
 import org.w3c.dom.NodeList;
 import org.xml.sax.SAXException;
 import pl.p4.diameter.avp.AvpHelper;
 import pl.p4.diameter.avp.AvpInfo;
import pl.p4.diameter.client.jDiamClient;
 
 public class MessageHelper
 {
   private static final int TIME_OUT = 15000;
   private static Logger log = LoggerFactory.getLogger(MessageHelper.class);
   private static Node root;
   private static int requestNumber = 0;
 
   public static Answer sendMessage(Session session, String fname, Answer prevAnswer) throws Exception
   {
     Request request = (Request)createMessageFromXML(session, fname, prevAnswer);
 
     log.info("Message to be sent={\n" + getMessageAsString(request) + "}");
 
     MySessionEventListener listener = new MySessionEventListener(); // robertsp
 
     session.send(request, listener);
 
     long startTime = System.currentTimeMillis();
     try {
       synchronized (MessageHelper.class) {
         MessageHelper.class.wait(15000L); // waiting extra 15sec.
       }
     } catch (Exception e) {
	   log.debug("getting into catch at message send!");
	   log.error(printStackTrace(e));
     }  
     long stopTime = System.currentTimeMillis();
     if (listener.hasAnswer()) {
         Answer answer = listener.getAnswer();
         log.info("Answer received after " + (stopTime - startTime) + "ms");
         log.info("Received message={\n" + getMessageAsString(answer) + "}");
         return answer;
     }else
         log.error("Can not find answer!");
     stopTime = System.currentTimeMillis();
     log.debug("Normal return from message sent, probably no answer " + (stopTime - startTime) + "ms");
     return null;
   }
 
   public static String getMessageAsString(Message msg)
   {
     return 
       "Credit Control " + (msg.isRequest() ? "Request" : "Answer") + 
       "(" + msg.getCommandCode() + ") f=" + (
       msg.isRequest() ? "R" : "-") + (
       msg.isProxiable() ? "P" : "-") + (
       msg.isError() ? "E" : "-") + (
       msg.isReTransmitted() ? "T" : "-") + "\n" + 
       //AvpHelper.getAvpSetAsString(msg.getAvps(), 1);
                AvpHelper.printMyAvps(msg.getAvps());
   }
 
   private static Message createMessageFromXML(Session session, String fName, Answer prevAnswer)
   {
     
              log.info("Creating document via parsefile: "+fName);
    
	          Document doc = parseFile(fName);
 
              
              if(doc==null){
            	  log.error("Null document reference, exitting! ");
                  return null;
              }    

     root = doc.getDocumentElement();
 
     int req = getAttrInt(root, "request");
 
     if (req == 1) {
       return createRequestFromXML(session, prevAnswer);
     }
     return createAnswerFromXML(session);
   }
 
   private static Request createRequestFromXML(Session session, Answer prevAnswer)
   {
     int cmdCode = getAttrInt(root, "CommandCode");
     String sessionId = getElementValue(root, "Session-Id");
     String destRealm = getElementValueAlt(root, "Destination-Realm", 
       prevAnswer, "Origin-Realm");
     String destHost = getElementValueAlt(root, "Destination-Host", 
       prevAnswer, "Origin-Host");
 
     log.trace("Creating Request from file. CommandCode=" + cmdCode + 
       ", Session-Id=" + sessionId + 
       ", Destination-Realm=" + destRealm + 
       ", Destination-Host=" + destHost);
 
     Request request = session.createRequest(
       cmdCode, 
       ApplicationId.createByAuthAppId(4L), 
       destRealm);
 
     AvpSet avpSet = request.getAvps();
 
     if (destHost != null) {
       addAVP(avpSet, Avp.DESTINATION_HOST, 0L, true, false, destHost);
     }
 
     avpSet.removeAvp(Avp.SESSION_ID);
     avpSet.insertAvp(0, Avp.SESSION_ID, sessionId == null ? session.getSessionId() : sessionId, 
       0L, true, false, true);
 
     addAvps(root, avpSet);
 
     overwriteAVPS(avpSet);
 
     return request;
   }
 
   private static void overwriteAVPS(AvpSet avpSet)
   {
     String msisdn = jDiamClient.getMSISDN(); // not using now!
 
     if (jDiamClient.getAutomaticRN()) {
       avpSet.removeAvp(Avp.CC_REQUEST_NUMBER); //415
       avpSet.insertAvp(7, Avp.CC_REQUEST_NUMBER, requestNumber++, true, false); //415
     }
 
     ArrayList ovrAvp = jDiamClient.getOvrAvps();
     for (Iterator i = ovrAvp.iterator(); i.hasNext(); ) {
       String[] a = ((String)i.next()).split("=");

 
       if (a[0].toLowerCase().equals("destination-realm")) {
           try {
             avpSet.removeAvp(Avp.DESTINATION_REALM);
             avpSet.addAvp(Avp.DESTINATION_REALM, a[1],  true, false, false);
           } catch (Exception e) {
          	 log.error("Destination-Realm: "+Arrays.toString(e.getStackTrace()));
           }
       }else if (a[0].toLowerCase().equals("destination-host")) {
               try {
                 avpSet.removeAvp(Avp.DESTINATION_HOST);
                 avpSet.addAvp(Avp.DESTINATION_HOST, a[1],  true, false, false);
               } catch (Exception e) {
              	 log.error("Destination-Host: "+Arrays.toString(e.getStackTrace()));
               }
       }else if (a[0].toLowerCase().equals("anum")) {
    	   log.debug("Updating ANUM");
           try {
             AvpSet sub = avpSet.getAvp(Avp.SUBSCRIPTION_ID).getGrouped(); //443
             sub.removeAvp(Avp.SUBSCRIPTION_ID_DATA); //444
             sub.addAvp(Avp.SUBSCRIPTION_ID_DATA, a[1], true, false, false); //444
           } catch (AvpDataException e) {
        	   log.error("Subscription-Id-Data: \n"+Arrays.toString(e.getStackTrace()));
           }
           //avp.
           // 
           try{
               AvpSet sub = avpSet.getAvp(Avp.SERVICE_INFORMATION).getGrouped().getAvp(Avp.MMS_INFORMATION).getGrouped().getAvp(Avp.ORIGINATOR_ADDRESS).getGrouped();
               sub.removeAvp(Avp.ADDRESS_DATA,10415L); //444
               //sub.
               sub.addAvp(Avp.ADDRESS_DATA, a[1], 10415L, true, false, false); 
             } catch (Exception e) { 
          	   log.error("Originator-Address: "+Arrays.toString(e.getStackTrace()));         	   
             }
           log.debug("ANUM: "+a[1]);
       }else if (a[0].toLowerCase().equals("rating-group")) {
         try {
           AvpSet sub = avpSet.getAvp(Avp.MULTIPLE_SERVICES_CREDIT_CONTROL).getGrouped(); //456
           sub.removeAvp(Avp.RATING_GROUP); //432
           sub.insertAvp(0, Avp.RATING_GROUP, Integer.parseInt(a[1]), true, false);
         } catch (Exception e) {
             log.error("Rating-Group: "+Arrays.toString(e.getStackTrace()));
         }
       } else if (a[0].toLowerCase().equals("called-station-id")) {
         try {
           AvpSet sub = avpSet.getAvp(Avp.SERVICE_INFORMATION).getGrouped().getAvp(Avp.PS_INFORMATION).getGrouped(); 
           sub.removeAvp(30);//CALLED_STATION_ID
           sub.addAvp(30, a[1], 10415L, true, false, false);
         } catch (Exception e) {
        	 log.error("Called-Station-Id: "+Arrays.toString(e.getStackTrace()));
         }
       } else if (a[0].toLowerCase().equals("sgsn-address")){
         try {
           AvpSet sub = avpSet.getAvp(Avp.SERVICE_INFORMATION).getGrouped().getAvp(Avp.PS_INFORMATION).getGrouped(); 
           sub.removeAvp(Avp.SGSN_ADDRESS);//1228 SGSN_ADDRESS 
           sub.addAvp(Avp.SGSN_ADDRESS, InetAddress.getByName(a[1]), 10415L, true, false);//1228
         } catch (AvpDataException e) {
           log.error("SGSN-Address: "+Arrays.toString(e.getStackTrace()));
         } catch (UnknownHostException e) {
           log.error("SGSN-Address: "+Arrays.toString(e.getStackTrace()));
         }
         
       }else if (a[0].toLowerCase().equals("bnum")) {
    	   try{
                  AvpSet sub = avpSet.getAvp(Avp.SERVICE_INFORMATION).getGrouped().getAvp(Avp.MMS_INFORMATION).getGrouped().getAvp(Avp.RECIPIENT_ADDRESS).getGrouped();
                  sub.removeAvp(Avp.ADDRESS_DATA,10415L); //444
                  sub.addAvp(Avp.ADDRESS_DATA, a[1], 10415L, true, false, false); 
                } catch (Exception e) {
             	   log.error("BNUM: "+Arrays.toString(e.getStackTrace()));
                }
    	        log.debug("BNUM: "+a[1]);

       }
     }
   }
 
   private static void addAvps(Node node, AvpSet avpSet)
   {
     NodeList children = node.getChildNodes();
 
     for (int i = 0; i < children.getLength(); i++) {
       Node child = children.item(i);
       if (child.getNodeType() == 1) {
         String name = child.getNodeName();
         int code = AvpInfo.getCode(name);
         if (code == -1) {
           log.error("Unable to find AVP code for name=" + name);
           return;
         }
         long vendorId = getAttrLong(child, "vendor");
         vendorId = vendorId == -1L ? AvpInfo.getVendorCode(code) : vendorId;
         boolean mflag = 
           getAttrInt(child, "mandatory") == 1;
         boolean pflag = 
           getAttrInt(child, "protected") == 1;
         String val = getElementValue(child);
 
         if (AvpHelper.isGrouped(code)) {
           log.trace("Node name=" + name + ", code=" + code);
           log.trace("Adding grouped avp [code=" + code + ", f=" + (
             vendorId != 0L ? "V" : "-") + (
             mflag ? "M" : "-") + (
             pflag ? "P" : "-") + (
             vendorId != 0L ? ", vnd=" + vendorId : "") + "]");
           AvpSet avpSetBranch = avpSet.addGroupedAvp(code, vendorId, mflag, pflag);
           addAvps(child, avpSetBranch);
         } else {
           log.trace("Node name=" + name + ", code=" + code + ", val=" + val);
           addAVP(avpSet, code, vendorId, mflag, pflag, val);
         }
       }
     }
   }
 
   private static void addAVP(AvpSet avpSet, int code, long vendorId, boolean mflag, boolean pflag, String value)
   {
     if (avpSet.getAvp(code) != null) {
       return;
     }
     String avpType = AvpInfo.getType(code);
     if (avpType == null)
     {
       log.error(" *** No avp type found [code=" + code + "] ***");
       return;
     }
 
     AvpType type = AvpType.valueOf(avpType);
     log.trace("Adding avp [code=" + code + ", type=" + type.toString() + ", f=" + (
       vendorId != 0L ? "V" : "-") + (
       mflag ? "M" : "-") + (
       pflag ? "P" : "-") + (
       vendorId != 0L ? ", vnd=" + vendorId : "") + 
       ", val=" + value + "]");
 
     if (type == AvpType.DiameterIdentity) {
       avpSet.addAvp(code, value, vendorId, mflag, pflag, true);
     } else if (type == AvpType.DiameterURI) {
       avpSet.addAvp(code, value, vendorId, mflag, pflag, true);
     } else if (type == AvpType.UTF8String) {
       avpSet.addAvp(code, value, vendorId, mflag, pflag, false);
     } else if (type == AvpType.Float32) {
       avpSet.addAvp(code, Float.parseFloat(value), vendorId, mflag, pflag);
     } else if (type == AvpType.Float64) {
       avpSet.addAvp(code, Double.parseDouble(value), vendorId, mflag, pflag);
     } else if (type == AvpType.Integer32) {
       avpSet.addAvp(code, Integer.parseInt(value), vendorId, mflag, pflag);
     } else if (type == AvpType.Integer64) {
       avpSet.addAvp(code, Long.parseLong(value), vendorId, mflag, pflag);
     } else if (type == AvpType.Unsigned32)
     {
       avpSet.addAvp(code, Integer.parseInt(value), vendorId, mflag, pflag);
     } else if (type == AvpType.Unsigned64) {
       avpSet.addAvp(code, Long.parseLong(value), vendorId, mflag, pflag);
     }
     else if (type == AvpType.Enumerated) {
       avpSet.addAvp(code, Integer.parseInt(value), vendorId, mflag, pflag);
     } else if (type == AvpType.OctetString) {
       if (value.startsWith("0x")) {
         value = new String(new char[] { '\001' });
       }
       avpSet.addAvp(code, value, vendorId, mflag, pflag, true);
     } else if (type == AvpType.Time) {
       avpSet.addAvp(code, new Date(), vendorId, mflag, pflag);
     } else if (type == AvpType.Address) {
       try {
         avpSet.addAvp(code, InetAddress.getByName(value), vendorId, mflag, pflag);
       } catch (UnknownHostException ex) {
         log.error("Unable to parse Address", ex);
       }
     }
   }
 
   public static String hexToBytes(String hex)
   {
     if (!hex.startsWith("0x")) {
       return null;
     }
     hex = hex.substring(2);
 
     int i = Integer.parseInt(hex, 16);
 
     System.out.println(i);
 
     return "s";
   }
 
   private static Answer createAnswerFromXML(Session session)
   {
     return null;
   }
 
   private static String getElementValueAlt(Node node, String nodeName, Answer answer, String ansName)
   {
     String elemVal = getElementValue(root, nodeName);
     if ((elemVal == null) && (answer != null))
     {
       elemVal = (String)AvpHelper.getValue(answer.getAvps(), 
         AvpInfo.getCode(ansName), 0);
     }
     return elemVal;
   }
 
   private static String getElementValue(Node node, String nodeName)
   {
     NodeList children = node.getChildNodes();
 
     for (int i = 0; i < children.getLength(); i++) {
       Node child = children.item(i);
       if ((child.getNodeType() == 1) && 
         (child.getNodeName().equals(nodeName))) {
         return getElementValue(child);
       }
     }
 
     return null;
   }
 
   private static String getElementValue(Node elem)
   {
     if ((elem != null) && 
       (elem.hasChildNodes())) {
       for (Node kid = elem.getFirstChild(); kid != null; kid = kid.getNextSibling()) {
         if (kid.getNodeType() == 3) {
           return kid.getNodeValue();
         }
       }
     }
 
     return "";
   }
 
   private static String getIndentSpaces(int indent) {
     StringBuffer buffer = new StringBuffer();
     for (int i = 0; i < indent; i++) {
       buffer.append(" ");
     }
     return buffer.toString();
   }
 
   private static String getAttribute(Node node, String attrName)
   {
     NamedNodeMap attr = node.getAttributes();
     if (attr != null) {
       Node nodeAttr = attr.getNamedItem(attrName);
       if (nodeAttr != null) {
         return nodeAttr.getNodeValue();
       }
     }
     return null;
   }
 
   private static int getAttrInt(Node node, String attrName)
   {
     String s = getAttribute(node, attrName);
     if (s != null) {
       return Integer.parseInt(s);
     }
     return -1;
   }
 
   private static long getAttrLong(Node node, String attrName)
   {
     String s = getAttribute(node, attrName);
     if (s != null) {
       return Long.parseLong(s);
     }
     return -1L;
   }
 
   private static void writeDocumentToOutput(Node node, int indent)
   {
     String nodeName = node.getNodeName();
 
     String nodeValue = getElementValue(node);
 
     NamedNodeMap attributes = node.getAttributes();
     String attr = "";
     for (int i = 0; i < attributes.getLength(); i++) {
       Node attribute = attributes.item(i);
       attr = attr + " " + attribute.getNodeName() + "=" + attribute.getNodeValue();
     }
     log.debug(getIndentSpaces(indent) + "<" + nodeName + attr + ">" + nodeValue);
 
     NodeList children = node.getChildNodes();
     for (int i = 0; i < children.getLength(); i++) {
       Node child = children.item(i);
       if (child.getNodeType() == 1) {
         writeDocumentToOutput(child, indent + 2);
       }
     }
     if (children.getLength() == 0) {
       log.debug(getIndentSpaces(indent));
     }
     log.debug("</" + nodeName + ">");
   }
 
   private static Document parseFile(String fileName)
   {
	          log.info("Parsing: "+fileName);
     Document doc = null;
     DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
     docBuilderFactory.setIgnoringElementContentWhitespace(true);
              DocumentBuilder docBuilder=null;

     try {
       docBuilder = docBuilderFactory.newDocumentBuilder();
     }
     catch (ParserConfigurationException e)
     {
       //DocumentBuilder docBuilder;
       log.error("Wrong parser configuration: " + e.getMessage());
       return null;
     }
     File sourceFile = new File(fileName);
     try {
       doc = docBuilder.parse(sourceFile);
     }
     catch (SAXException e) {
       log.error("Wrong XML file structure: " + e.getMessage());
       return null;
     }
     catch (IOException e) {
       log.error("Could not read source file: " + e.getMessage());
     }
 
     return doc;
   }

   private static   String printStackTrace(Exception ex){
     StringWriter errors = new StringWriter();
     ex.printStackTrace(new PrintWriter(errors)); 
     return ex.getMessage()+" -> "+errors.toString();   
   }
   
   
   private static class MySessionEventListener implements EventListener<Request, Answer>
   {
     private Answer answer = null;
 
     public void receivedSuccessMessage(Request request, Answer answer) {
       this.answer = answer;
       continueProcessiong();
     }
 
     public void timeoutExpired(Request request) {
       continueProcessiong();
     }
 
     public boolean hasAnswer() {
       return this.answer != null;
     }
 
     public Answer getAnswer() {
       return this.answer;
     }
 
     private void continueProcessiong() {
       synchronized (MessageHelper.class) {
         MessageHelper.class.notifyAll();
       }
     }
   }
 }

