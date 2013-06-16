/*     */ package pl.p4.diameter.msg;
/*     */ 
/*     */ import java.io.File;
/*     */ import java.io.IOException;
/*     */ import java.io.PrintStream;
/*     */ import java.net.InetAddress;
/*     */ import java.net.UnknownHostException;
/*     */ import java.util.ArrayList;
/*     */ import java.util.Date;
/*     */ import java.util.Iterator;
/*     */ import javax.xml.parsers.DocumentBuilder;
/*     */ import javax.xml.parsers.DocumentBuilderFactory;
/*     */ import javax.xml.parsers.ParserConfigurationException;
/*     */ import org.jdiameter.api.Answer;
/*     */ import org.jdiameter.api.ApplicationId;
/*     */ import org.jdiameter.api.Avp;
/*     */ import org.jdiameter.api.AvpDataException;
/*     */ import org.jdiameter.api.AvpSet;
/*     */ import org.jdiameter.api.EventListener;
/*     */ import org.jdiameter.api.Message;
/*     */ import org.jdiameter.api.Request;
/*     */ import org.jdiameter.api.Session;
/*     */ import org.jdiameter.api.annotation.AvpType;
/*     */ import org.slf4j.Logger;
/*     */ import org.slf4j.LoggerFactory;
/*     */ import org.w3c.dom.Document;
/*     */ import org.w3c.dom.NamedNodeMap;
/*     */ import org.w3c.dom.Node;
/*     */ import org.w3c.dom.NodeList;
/*     */ import org.xml.sax.SAXException;
/*     */ import pl.p4.diameter.avp.AvpHelper;
/*     */ import pl.p4.diameter.avp.AvpInfo;
/*     */ import pl.p4.diameter.client.jDiamClient;
/*     */ 
/*     */ public class MessageHelper
/*     */ {
/*     */   private static final int TIME_OUT = 15000;
/*  47 */   private static Logger log = LoggerFactory.getLogger(MessageHelper.class);
/*     */   private static Node root;
/*  49 */   private static int requestNumber = 0;
/*     */ 
/*     */   public static Answer sendMessage(Session session, String fname, Answer prevAnswer) throws Exception
/*     */   {
/*  53 */     Request request = (Request)createMessageFromXML(session, fname, prevAnswer);
/*     */ 
/*  55 */     log.debug("Message to be sent={\n" + getMessageAsString(request) + "}");
/*     */ 
/*  57 */     MySessionEventListener listener = new MySessionEventListener(); // robertsp
/*     */ 
/*  59 */     session.send(request, listener);
/*     */ 
/*  61 */     long startTime = System.currentTimeMillis();
/*     */     try {
/*  63 */       synchronized (MessageHelper.class) {
/*  64 */         MessageHelper.class.wait(16000L);
/*     */       }
/*     */     } catch (Exception e) {
/*  67 */       e.printStackTrace();
/*     */ 
/*  69 */       long stopTime = System.currentTimeMillis();
/*  70 */       if (listener.hasAnswer()) {
/*  71 */         Answer answer = listener.getAnswer();
/*  72 */         log.debug("Answer received after " + (stopTime - startTime) + "ms");
/*  73 */         log.debug("Received message={\n" + getMessageAsString(answer) + "}");
/*  74 */         return answer;
/*     */       }
/*  76 */       log.error("Can not find answer!");
/*  77 */     }return null;
/*     */   }
/*     */ 
/*     */   public static String getMessageAsString(Message msg)
/*     */   {
/*  82 */     return 
/*  83 */       "Credit Control " + (msg.isRequest() ? "Request" : "Answer") + 
/*  84 */       "(" + msg.getCommandCode() + ") f=" + (
/*  85 */       msg.isRequest() ? "R" : "-") + (
/*  86 */       msg.isProxiable() ? "P" : "-") + (
/*  87 */       msg.isError() ? "E" : "-") + (
/*  88 */       msg.isReTransmitted() ? "T" : "-") + "\n" + 
/*  89 */       AvpHelper.getAvpSetAsString(msg.getAvps(), 1);
/*     */   }
/*     */ 
/*     */   private static Message createMessageFromXML(Session session, String fName, Answer prevAnswer)
/*     */   {
/*  98 */     
              log.info("Creating document via parsefile: "+fName);
    
	          Document doc = parseFile(fName);
/*     */ 
              
              if(doc==null){
            	  log.error("Null document reference, exitting! ");
                  return null;
              }    

/* 100 */     root = doc.getDocumentElement();
/*     */ 
/* 102 */     int req = getAttrInt(root, "request");
/*     */ 
/* 104 */     if (req == 1) {
/* 105 */       return createRequestFromXML(session, prevAnswer);
/*     */     }
/* 107 */     return createAnswerFromXML(session);
/*     */   }
/*     */ 
/*     */   private static Request createRequestFromXML(Session session, Answer prevAnswer)
/*     */   {
/* 114 */     int cmdCode = getAttrInt(root, "CommandCode");
/* 115 */     String sessionId = getElementValue(root, "Session-Id");
/* 116 */     String destRealm = getElementValueAlt(root, "Destination-Realm", 
/* 117 */       prevAnswer, "Origin-Realm");
/* 118 */     String destHost = getElementValueAlt(root, "Destination-Host", 
/* 119 */       prevAnswer, "Origin-Host");
/*     */ 
/* 121 */     log.trace("Creating Request from file. CommandCode=" + cmdCode + 
/* 122 */       ", Session-Id=" + sessionId + 
/* 123 */       ", Destination-Realm=" + destRealm + 
/* 124 */       ", Destination-Host=" + destHost);
/*     */ 
/* 126 */     Request request = session.createRequest(
/* 127 */       cmdCode, 
/* 128 */       ApplicationId.createByAuthAppId(4L), 
/* 129 */       destRealm);
/*     */ 
/* 132 */     AvpSet avpSet = request.getAvps();
/*     */ 
/* 134 */     if (destHost != null) {
/* 135 */       addAVP(avpSet, Avp.DESTINATION_HOST, 0L, true, false, destHost);
/*     */     }
/*     */ 
/* 138 */     avpSet.removeAvp(Avp.SESSION_ID);
/* 139 */     avpSet.insertAvp(0, Avp.SESSION_ID, sessionId == null ? session.getSessionId() : sessionId, 
/* 140 */       0L, true, false, true);
/*     */ 
/* 142 */     addAvps(root, avpSet);
/*     */ 
/* 146 */     overwriteAVPS(avpSet);
/*     */ 
/* 148 */     return request;
/*     */   }
/*     */ 
/*     */   private static void overwriteAVPS(AvpSet avpSet)
/*     */   {
/* 154 */     String msisdn = jDiamClient.getMSISDN();
/* 155 */     if (msisdn != null) {
/*     */       try {
/* 157 */         AvpSet sub = avpSet.getAvp(Avp.SUBSCRIPTION_ID).getGrouped(); //443
/* 158 */         sub.removeAvp(Avp.SUBSCRIPTION_ID_DATA); //444
/* 159 */         sub.addAvp(Avp.SUBSCRIPTION_ID_DATA, msisdn, true, false, false); //444
/*     */       } catch (AvpDataException e) {
/* 161 */         e.printStackTrace();
/*     */       }
/*     */ 
/*     */     }
/*     */ 
/* 166 */     if (jDiamClient.getAutomaticRN()) {
/* 167 */       avpSet.removeAvp(Avp.CC_REQUEST_NUMBER); //415
/* 168 */       avpSet.insertAvp(7, Avp.CC_REQUEST_NUMBER, requestNumber++, true, false); //415
/*     */     }
/*     */ 
/* 172 */     ArrayList ovrAvp = jDiamClient.getOvrAvps();
/* 173 */     for (Iterator i = ovrAvp.iterator(); i.hasNext(); ) {
/* 174 */       String[] a = ((String)i.next()).split("=");
/*     */ 
/* 176 */       if (a[0].toLowerCase().equals("rating-group")) {
/*     */         try {
/* 178 */           AvpSet sub = avpSet.getAvp(Avp.MULTIPLE_SERVICES_CREDIT_CONTROL).getGrouped(); //456
/* 179 */           sub.removeAvp(Avp.RATING_GROUP); //432
/* 180 */           sub.insertAvp(0, Avp.RATING_GROUP, Integer.parseInt(a[1]), true, false);
/*     */         } catch (AvpDataException e) {
/* 182 */           e.printStackTrace();
/*     */         }
/* 184 */       } else if (a[0].toLowerCase().equals("called-station-id")) {
/*     */         try {
/* 186 */           AvpSet sub = avpSet.getAvp(Avp.SERVICE_INFORMATION).getGrouped().getAvp(Avp.PS_INFORMATION).getGrouped(); //873,874
/* 187 */           sub.removeAvp(30);//CALLED_STATION_ID
/* 188 */           sub.addAvp(30, a[1], 10415L, true, false, false);
/*     */         } catch (AvpDataException e) {
/* 190 */           e.printStackTrace();
/*     */         }
/*     */       } else {
/* 192 */         if (!a[0].toLowerCase().equals("sgsn-address")) continue;
/*     */         try {
/* 194 */           AvpSet sub = avpSet.getAvp(Avp.SERVICE_INFORMATION).getGrouped().getAvp(Avp.PS_INFORMATION).getGrouped(); //873,874
/* 195 */           sub.removeAvp(Avp.SGSN_ADDRESS);//1228 SGSN_ADDRESS 
/* 196 */           sub.addAvp(Avp.SGSN_ADDRESS, InetAddress.getByName(a[1]), 10415L, true, false);//1228
/*     */         } catch (AvpDataException e) {
/* 198 */           e.printStackTrace();
/*     */         } catch (UnknownHostException e) {
/* 200 */           e.printStackTrace();
/*     */         }
/*     */       }
/*     */     }
/*     */   }
/*     */ 
/*     */   private static void addAvps(Node node, AvpSet avpSet)
/*     */   {
/* 209 */     NodeList children = node.getChildNodes();
/*     */ 
/* 211 */     for (int i = 0; i < children.getLength(); i++) {
/* 212 */       Node child = children.item(i);
/* 213 */       if (child.getNodeType() == 1) {
/* 214 */         String name = child.getNodeName();
/* 215 */         int code = AvpInfo.getCode(name);
/* 216 */         if (code == -1) {
/* 217 */           log.error("Unable to find AVP code for name=" + name);
/* 218 */           return;
/*     */         }
/* 220 */         long vendorId = getAttrLong(child, "vendor");
/* 221 */         vendorId = vendorId == -1L ? AvpInfo.getVendorCode(code) : vendorId;
/* 222 */         boolean mflag = 
/* 223 */           getAttrInt(child, "mandatory") == 1;
/* 224 */         boolean pflag = 
/* 225 */           getAttrInt(child, "protected") == 1;
/* 226 */         String val = getElementValue(child);
/*     */ 
/* 228 */         if (AvpHelper.isGrouped(code)) {
/* 229 */           log.trace("Node name=" + name + ", code=" + code);
/* 230 */           log.trace("Adding grouped avp [code=" + code + ", f=" + (
/* 231 */             vendorId != 0L ? "V" : "-") + (
/* 232 */             mflag ? "M" : "-") + (
/* 233 */             pflag ? "P" : "-") + (
/* 234 */             vendorId != 0L ? ", vnd=" + vendorId : "") + "]");
/* 235 */           AvpSet avpSetBranch = avpSet.addGroupedAvp(code, vendorId, mflag, pflag);
/* 236 */           addAvps(child, avpSetBranch);
/*     */         } else {
/* 238 */           log.trace("Node name=" + name + ", code=" + code + ", val=" + val);
/* 239 */           addAVP(avpSet, code, vendorId, mflag, pflag, val);
/*     */         }
/*     */       }
/*     */     }
/*     */   }
/*     */ 
/*     */   private static void addAVP(AvpSet avpSet, int code, long vendorId, boolean mflag, boolean pflag, String value)
/*     */   {
/* 258 */     if (avpSet.getAvp(code) != null) {
/* 259 */       return;
/*     */     }
/* 261 */     String avpType = AvpInfo.getType(code);
/* 262 */     if (avpType == null)
/*     */     {
/* 264 */       log.error(" *** No avp type found [code=" + code + "] ***");
/* 265 */       return;
/*     */     }
/*     */ 
/* 268 */     AvpType type = AvpType.valueOf(avpType);
/* 269 */     log.trace("Adding avp [code=" + code + ", type=" + type.toString() + ", f=" + (
/* 270 */       vendorId != 0L ? "V" : "-") + (
/* 271 */       mflag ? "M" : "-") + (
/* 272 */       pflag ? "P" : "-") + (
/* 273 */       vendorId != 0L ? ", vnd=" + vendorId : "") + 
/* 274 */       ", val=" + value + "]");
/*     */ 
/* 276 */     if (type == AvpType.DiameterIdentity) {
/* 277 */       avpSet.addAvp(code, value, vendorId, mflag, pflag, true);
/* 278 */     } else if (type == AvpType.DiameterURI) {
/* 279 */       avpSet.addAvp(code, value, vendorId, mflag, pflag, true);
/* 280 */     } else if (type == AvpType.UTF8String) {
/* 281 */       avpSet.addAvp(code, value, vendorId, mflag, pflag, false);
/* 282 */     } else if (type == AvpType.Float32) {
/* 283 */       avpSet.addAvp(code, Float.parseFloat(value), vendorId, mflag, pflag);
/* 284 */     } else if (type == AvpType.Float64) {
/* 285 */       avpSet.addAvp(code, Double.parseDouble(value), vendorId, mflag, pflag);
/* 286 */     } else if (type == AvpType.Integer32) {
/* 287 */       avpSet.addAvp(code, Integer.parseInt(value), vendorId, mflag, pflag);
/* 288 */     } else if (type == AvpType.Integer64) {
/* 289 */       avpSet.addAvp(code, Long.parseLong(value), vendorId, mflag, pflag);
/* 290 */     } else if (type == AvpType.Unsigned32)
/*     */     {
/* 292 */       avpSet.addAvp(code, Integer.parseInt(value), vendorId, mflag, pflag);
/* 293 */     } else if (type == AvpType.Unsigned64) {
/* 294 */       avpSet.addAvp(code, Long.parseLong(value), vendorId, mflag, pflag);
/*     */     }
/* 297 */     else if (type == AvpType.Enumerated) {
/* 298 */       avpSet.addAvp(code, Integer.parseInt(value), vendorId, mflag, pflag);
/* 299 */     } else if (type == AvpType.OctetString) {
/* 300 */       if (value.startsWith("0x")) {
/* 301 */         value = new String(new char[] { '\001' });
/*     */       }
/* 303 */       avpSet.addAvp(code, value, vendorId, mflag, pflag, true);
/* 304 */     } else if (type == AvpType.Time) {
/* 305 */       avpSet.addAvp(code, new Date(), vendorId, mflag, pflag);
/* 306 */     } else if (type == AvpType.Address) {
/*     */       try {
/* 308 */         avpSet.addAvp(code, InetAddress.getByName(value), vendorId, mflag, pflag);
/*     */       } catch (UnknownHostException ex) {
/* 310 */         log.error("Unable to parse Address", ex);
/*     */       }
/*     */     }
/*     */   }
/*     */ 
/*     */   public static String hexToBytes(String hex)
/*     */   {
/* 317 */     if (!hex.startsWith("0x")) {
/* 318 */       return null;
/*     */     }
/* 320 */     hex = hex.substring(2);
/*     */ 
/* 322 */     int i = Integer.parseInt(hex, 16);
/*     */ 
/* 324 */     System.out.println(i);
/*     */ 
/* 326 */     return "s";
/*     */   }
/*     */ 
/*     */   private static Answer createAnswerFromXML(Session session)
/*     */   {
/* 331 */     return null;
/*     */   }
/*     */ 
/*     */   private static String getElementValueAlt(Node node, String nodeName, Answer answer, String ansName)
/*     */   {
/* 338 */     String elemVal = getElementValue(root, nodeName);
/* 339 */     if ((elemVal == null) && (answer != null))
/*     */     {
/* 341 */       elemVal = (String)AvpHelper.getValue(answer.getAvps(), 
/* 342 */         AvpInfo.getCode(ansName), 0);
/*     */     }
/* 344 */     return elemVal;
/*     */   }
/*     */ 
/*     */   private static String getElementValue(Node node, String nodeName)
/*     */   {
/* 349 */     NodeList children = node.getChildNodes();
/*     */ 
/* 351 */     for (int i = 0; i < children.getLength(); i++) {
/* 352 */       Node child = children.item(i);
/* 353 */       if ((child.getNodeType() == 1) && 
/* 354 */         (child.getNodeName().equals(nodeName))) {
/* 355 */         return getElementValue(child);
/*     */       }
/*     */     }
/*     */ 
/* 359 */     return null;
/*     */   }
/*     */ 
/*     */   private static String getElementValue(Node elem)
/*     */   {
/* 368 */     if ((elem != null) && 
/* 369 */       (elem.hasChildNodes())) {
/* 370 */       for (Node kid = elem.getFirstChild(); kid != null; kid = kid.getNextSibling()) {
/* 371 */         if (kid.getNodeType() == 3) {
/* 372 */           return kid.getNodeValue();
/*     */         }
/*     */       }
/*     */     }
/*     */ 
/* 377 */     return "";
/*     */   }
/*     */ 
/*     */   private static String getIndentSpaces(int indent) {
/* 381 */     StringBuffer buffer = new StringBuffer();
/* 382 */     for (int i = 0; i < indent; i++) {
/* 383 */       buffer.append(" ");
/*     */     }
/* 385 */     return buffer.toString();
/*     */   }
/*     */ 
/*     */   private static String getAttribute(Node node, String attrName)
/*     */   {
/* 390 */     NamedNodeMap attr = node.getAttributes();
/* 391 */     if (attr != null) {
/* 392 */       Node nodeAttr = attr.getNamedItem(attrName);
/* 393 */       if (nodeAttr != null) {
/* 394 */         return nodeAttr.getNodeValue();
/*     */       }
/*     */     }
/* 397 */     return null;
/*     */   }
/*     */ 
/*     */   private static int getAttrInt(Node node, String attrName)
/*     */   {
/* 402 */     String s = getAttribute(node, attrName);
/* 403 */     if (s != null) {
/* 404 */       return Integer.parseInt(s);
/*     */     }
/* 406 */     return -1;
/*     */   }
/*     */ 
/*     */   private static long getAttrLong(Node node, String attrName)
/*     */   {
/* 411 */     String s = getAttribute(node, attrName);
/* 412 */     if (s != null) {
/* 413 */       return Long.parseLong(s);
/*     */     }
/* 415 */     return -1L;
/*     */   }
/*     */ 
/*     */   private static void writeDocumentToOutput(Node node, int indent)
/*     */   {
/* 421 */     String nodeName = node.getNodeName();
/*     */ 
/* 423 */     String nodeValue = getElementValue(node);
/*     */ 
/* 425 */     NamedNodeMap attributes = node.getAttributes();
/* 426 */     String attr = "";
/* 427 */     for (int i = 0; i < attributes.getLength(); i++) {
/* 428 */       Node attribute = attributes.item(i);
/* 429 */       attr = attr + " " + attribute.getNodeName() + "=" + attribute.getNodeValue();
/*     */     }
/* 431 */     log.debug(getIndentSpaces(indent) + "<" + nodeName + attr + ">" + nodeValue);
/*     */ 
/* 433 */     NodeList children = node.getChildNodes();
/* 434 */     for (int i = 0; i < children.getLength(); i++) {
/* 435 */       Node child = children.item(i);
/* 436 */       if (child.getNodeType() == 1) {
/* 437 */         writeDocumentToOutput(child, indent + 2);
/*     */       }
/*     */     }
/* 440 */     if (children.getLength() == 0) {
/* 441 */       log.debug(getIndentSpaces(indent));
/*     */     }
/* 443 */     log.debug("</" + nodeName + ">");
/*     */   }
/*     */ 
/*     */   private static Document parseFile(String fileName)
/*     */   {
	          log.info("Parsing: "+fileName);
/* 453 */     Document doc = null;
/* 454 */     DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
/* 455 */     docBuilderFactory.setIgnoringElementContentWhitespace(true);
              DocumentBuilder docBuilder=null;

/*     */     try {
/* 457 */       docBuilder = docBuilderFactory.newDocumentBuilder();
/*     */     }
/*     */     catch (ParserConfigurationException e)
/*     */     {
/*     */       //DocumentBuilder docBuilder;
/* 460 */       log.error("Wrong parser configuration: " + e.getMessage());
/* 461 */       return null;
/*     */     }
/* 463 */     File sourceFile = new File(fileName);
/*     */     try {
/* 465 */       doc = docBuilder.parse(sourceFile);
/*     */     }
/*     */     catch (SAXException e) {
/* 468 */       log.error("Wrong XML file structure: " + e.getMessage());
/* 469 */       return null;
/*     */     }
/*     */     catch (IOException e) {
/* 472 */       log.error("Could not read source file: " + e.getMessage());
/*     */     }
/*     */ 
/* 475 */     return doc;
/*     */   }
/*     */ 
/*     */   private static class MySessionEventListener implements EventListener<Request, Answer>
/*     */   {
/* 480 */     private Answer answer = null;
/*     */ 
/*     */     public void receivedSuccessMessage(Request request, Answer answer) {
/* 483 */       this.answer = answer;
/* 484 */       continueProcessiong();
/*     */     }
/*     */ 
/*     */     public void timeoutExpired(Request request) {
/* 488 */       continueProcessiong();
/*     */     }
/*     */ 
/*     */     public boolean hasAnswer() {
/* 492 */       return this.answer != null;
/*     */     }
/*     */ 
/*     */     public Answer getAnswer() {
/* 496 */       return this.answer;
/*     */     }
/*     */ 
/*     */     private void continueProcessiong() {
/* 500 */       synchronized (MessageHelper.class) {
/* 501 */         MessageHelper.class.notifyAll();
/*     */       }
/*     */     }
/*     */   }
/*     */ }

/* Location:           C:\Java\JDiameter\p4.versions\1.6.0_b82\jdiam.jar
 * Qualified Name:     pl.p4.diameter.msg.MessageHelper
 * JD-Core Version:    0.6.0
 */