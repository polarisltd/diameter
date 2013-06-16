package org.mobicents.servers.diameter.utils;

import org.apache.log4j.Logger;
import org.jdiameter.api.Avp;
import org.jdiameter.api.AvpDataException;
import org.jdiameter.api.AvpSet;
import org.jdiameter.api.Message;
import org.jdiameter.api.validation.AvpRepresentation;
import org.jdiameter.api.validation.Dictionary;
import org.jdiameter.common.impl.validation.DictionaryImpl;

public class DiameterUtilities {

  private static Logger logger = Logger.getLogger(DiameterUtilities.class);

  public static Dictionary AVP_DICTIONARY = DictionaryImpl.INSTANCE;

  public static void printMessage(Message message) {
    String reqType="";    
    switch (message.getCommandCode()) {
            case 258:  reqType = "RA";
                     break;
            case 272:  reqType = "CC";
                     break;
            default: reqType = "XX";
                     break;
        }
    
    
    String reqFlag = message.isRequest() ? "R" : "A";
    String flags = reqFlag += message.isError() ? " | E" : "";

    if(logger.isInfoEnabled()) {
      logger.info("Message [" + flags + "] Command-Code: " + message.getCommandCode() + " / E2E(" 
          + message.getEndToEndIdentifier() + ") / HbH(" + message.getHopByHopIdentifier() + ")");
      logger.info("- - - - - - - - - - - - - - - - AVPs - - - - - - - - - - - - - - - -");      
      String msg = printMyAvps(message.getAvps());
      logger.info(reqType+reqFlag+" ... "+msg);
    }
  }

  public static void printAvps(AvpSet avps) {
    printAvps(avps, "");
  }

  public static void printAvps(AvpSet avps, String indentation) {
    for(Avp avp : avps) {
      AvpRepresentation avpRep = AVP_DICTIONARY.getAvp(avp.getCode(), avp.getVendorId());
      Object avpValue = null;
      boolean isGrouped = false;

      try {
        String avpType = AVP_DICTIONARY.getAvp(avp.getCode(), avp.getVendorId()).getType();

        if("Integer32".equals(avpType) || "AppId".equals(avpType)) {
          avpValue = avp.getInteger32();
        }
        else if("Unsigned32".equals(avpType) || "VendorId".equals(avpType)) {
          avpValue = avp.getUnsigned32();
        }
        else if("Float64".equals(avpType)) {
          avpValue = avp.getFloat64();
        }
        else if("Integer64".equals(avpType)) {
          avpValue = avp.getInteger64();
        }
        else if("Time".equals(avpType)) {
          avpValue = avp.getTime();
        }
        else if("Unsigned64".equals(avpType)) {
          avpValue = avp.getUnsigned64();
        }
        else if("Grouped".equals(avpType)) {
          avpValue = "<Grouped>";
          isGrouped = true;
        }
        else {
          avpValue = avp.getOctetString().replaceAll("\r", "").replaceAll("\n", "");
        }
      }
      catch (Exception ignore) {
        try {
          avpValue = avp.getOctetString().replaceAll("\r", "").replaceAll("\n", "");
        }
        catch (AvpDataException e) {
          avpValue = avp.toString();
        }
      }

      String avpLine = indentation + avp.getCode() + ": " + avpRep.getName();
      while(avpLine.length() < 50) {
        avpLine += avpLine.length() % 2 == 0 ? "." : " ";
      }
      avpLine += avpValue;

      logger.info(avpLine);

      if(isGrouped) {
        try {
          printAvps(avp.getGrouped(), indentation + "  ");          
        }
        catch (AvpDataException e) {
          // Failed to ungroup... ignore then...
        }
      }
    }
  }
  
  
  
  
  public static String printMyAvps(AvpSet avps) {
        String avpLine = "{ "; // always start with { to have any grouped included in brackets
	    for(Avp avp : avps) {
	      AvpRepresentation avpRep = AVP_DICTIONARY.getAvp(avp.getCode(), avp.getVendorId());
	      Object avpValue = null;
	      boolean isGrouped = false;
	      
	      // extract current AVP value
	      try {
	        String avpType = AVP_DICTIONARY.getAvp(avp.getCode(), avp.getVendorId()).getType();

	        if("Integer32".equals(avpType) || "AppId".equals(avpType)) {
	          avpValue = avp.getInteger32();
	        }
	        else if("Unsigned32".equals(avpType) || "VendorId".equals(avpType)) {
	          avpValue = avp.getUnsigned32();
	        }
	        else if("Float64".equals(avpType)) {
	          avpValue = avp.getFloat64();
	        }
	        else if("Integer64".equals(avpType)) {
	          avpValue = avp.getInteger64();
	        }
	        else if("Time".equals(avpType)) {
	          avpValue = avp.getTime();
	        }
	        else if("Unsigned64".equals(avpType)) {
	          avpValue = avp.getUnsigned64();
	        }
	        else if("Grouped".equals(avpType)) {
	          avpValue = "<Grouped>";
	          isGrouped = true;
	        }
	        else {
	          avpValue = avp.getOctetString().replaceAll("\r", "").replaceAll("\n", "");
	        }
	      }
	      catch (Exception ignore) {
	        try {
	          avpValue = avp.getOctetString().replaceAll("\r", "").replaceAll("\n", "");
	        }
	        catch (AvpDataException e) {
	          avpValue = avp.toString();
	        }
	      }
          //
	      
	      
	      if(isGrouped) {
	    	  avpLine +=  avpRep.getName()+": "; 
	          try {
	        	  avpLine +=  printMyAvps(avp.getGrouped());          
	          }catch (AvpDataException e) {
	        	  logger.info("Failed to ungroup... ignore then... \n"); 
	          }
	      }else{
	          avpLine +=  avpRep.getName()+":"+avpValue+"; ";
	      }    

	      
	    }// avps loop
	    
        return avpLine+"}";
	  }
  
  ////////////////////////////////////////////////////////
/**
 * Funtion which allows extracting any subAVP from given grouped AVP construct.
 * 
 * Example: Multiple-Services-Credit-Control[456].Used-Service-Unit[446].CC-Service-Specific-Units[417]
 *
 * mscc = ccrAvps.getAvp(456);
 * avp =  getFirstSubAvp(mscc,417)
 * 
 *     
 * @param startAvp Avp from which start access 
 * @param avpCode  Code to identify Avp we want to extract
 * @return         Avp found having required code.
 * 
 * Summary of Avp API:
 * 
 * org.jdiameter.api.Avp
 *    getGrouped() : avpSet
 *    getCode():     int
 *    getFloat32/Float64/Integer32/Integer64/Unsigned32/Unsigned64/OctetString/UTF8String: float/double/int/long/long/long/String/String
 *    
 * org.jdiameter.api.AvpSet   
 *    getAvpByIndex(int index):Avp  returns Avp by position 
 *    getAvp(int avpCode): Avp
 *    getAvp(getAvp(int avpCode, long vendorId): Avp 
 *    asArray():Avp[]
 *    size():int
 *    
 * org.jdiameter.api.validation.AvpRepresentation
 *    isGrouped():boolean 
 *    getName():String 
 *    getType():String

 * 
 * 
 * 
 */

  public static Avp getAvpByCode(Avp avp,int avpCode) {
	  
	        AvpRepresentation avpRep = AVP_DICTIONARY.getAvp(avp.getCode(), avp.getVendorId());
	        if (avpRep.isGrouped()){
	          try{	
	    	  for(Avp a: avp.getGrouped()){
	    		Avp res =  getAvpByCode(a,avpCode); 
	    	    if(res!=null)return res;
	          }//for
	          }catch(Exception e){
	    		  
	    	  }
	        }else if(avp.getCode()==avpCode){ 
	            return avp;	
	        }
	        return null;	      
  }//fn	      
	      

  
  
  
}
