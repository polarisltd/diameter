 package pl.p4.diameter.avp;
 
import java.net.InetAddress;
import java.util.Iterator;
import org.jdiameter.api.Avp;
import org.jdiameter.api.AvpDataException;
import org.jdiameter.api.AvpSet;
import org.jdiameter.api.annotation.AvpType;
import org.jdiameter.common.impl.validation.DictionaryImpl;
import org.jdiameter.api.validation.AvpRepresentation;
import org.jdiameter.api.validation.Dictionary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.p4.config.Configuration;
import pl.p4.config.EnumProperties;
import pl.p4.diameter.client.jDiamClient;

 
 public class AvpHelper
 {
   private static Logger log = LoggerFactory.getLogger(AvpHelper.class);
   private static boolean showVnd = false;
   private static Configuration config = null;
   public static Dictionary AVP_DICTIONARY = DictionaryImpl.INSTANCE;

   
   
   public static String getAvpSetAsString(AvpSet avpSet, int lvl)
   {
     if (config == null) {
       config = jDiamClient.getConfig();
       showVnd = config.getBoolean(EnumProperties.SHOW_VENDOR_ID);
     }
 
     String s = "";
     Iterator iterator = avpSet.iterator();
     while (iterator.hasNext()) {
       Avp avp = (Avp)iterator.next();
       int code = avp.getCode();
 
       s = s + getTab(lvl) + (avp.isMandatory() ? "M" : " ") + (
         avp.isVendorId() ? "V" : " ") + (
         avp.isEncrypted() ? "P  " : "   ") + 
         AvpInfo.getName(code) + "(" + code + ")" + (
         (showVnd) && (avp.isVendorId()) ? " vnd=" + avp.getVendorId() + ": " : ": ") + 
         getValueWithDesc(avp, lvl) + "\n";
     }
     return s;
   }
 
   
   public static String printMyAvps(AvpSet avps) {
	   if (config == null) {
	         config = jDiamClient.getConfig();
	         showVnd = config.getBoolean(EnumProperties.SHOW_VENDOR_ID);
	   }
       String avpLine = "{ "; // always start with { to have any grouped included in brackets
	    for(Avp avp : avps) {
	      int code = avp.getCode();	
	      String avpName = AvpInfo.getName(code);	
	      String avpType = AvpInfo.getType(code);
	      
	      if (avpType == null) {
	        return null;
	      }
	      AvpType type = AvpType.valueOf(avpType); // AvpType.Grouped 
	      // if ((type == AvpType.Unsigned32) || (type == AvpType.Enumerated))
	      Object avpValue = null;
	      boolean isGrouped = false;
	      
	      // extract current AVP value
	      try {

	        if(type == AvpType.Integer32 || type == AvpType.Enumerated ) {
	          avpValue = avp.getInteger32();
	        }
	        else if(type == AvpType.Address) {
		          avpValue = avp.getAddress();
		    }

	        else if(type == AvpType.DiameterIdentity) {
		          avpValue = avp.getDiameterIdentity();
		    }
	        else if(type == AvpType.Unsigned32) {
	          avpValue = avp.getUnsigned32();
	        }
	        else if(type == AvpType.Float64) {
	          avpValue = avp.getFloat64();
	        }
	        else if(type == AvpType.Integer64) {
	          avpValue = avp.getInteger64();
	        }
	        else if(type == AvpType.Time) {
	          avpValue = avp.getTime();
	        }
	        //AvpType.
	        else if(type == AvpType.Unsigned64) {
	          avpValue = avp.getUnsigned64();
	        }
	        else if(type == AvpType.UTF8String) {
		          avpValue = avp.getUTF8String();
		    }	        
	        else if(type == AvpType.OctetString) {
		          avpValue = avp.getUTF8String();
		    }	        
	        else if(type == AvpType.Grouped) {
	          avpValue = "<Grouped>";
	          isGrouped = true;
	        }
	        else {
	          avpValue = avp.getOctetString(); //.replaceAll("\r", "").replaceAll("\n", "");
	        }
	      }
	      catch (Exception ignore) {
	    	  avpValue = avp.toString();  
	        //try {
	          //avpValue = avp.getOctetString().replaceAll("\r", "").replaceAll("\n", "");
	        //}
	        //catch (AvpDataException e) {
	        //  avpValue = avp.toString();
	        //}
	      }
         //
	      
	      
	      if(isGrouped) {
	    	  avpLine +=  avpName+": "; 
	          try {
	        	  avpLine +=  printMyAvps(avp.getGrouped());          
	          }catch (AvpDataException e) {
	        	  log.info("Failed to ungroup... ignore then... \n"); 
	          }
	      }else{
	          avpLine +=  avpName+":"+avpValue+"; ";
	      }    

	      
	    }// avps loop
	    
       return avpLine+"}";
	  }
   
   
   
   private static String getTab(int lvl) {
     String s = "";
     for (int i = 0; i < lvl; i++) {
       s = s + "   ";
     }
     return s;
   }


/**
 * This function takes argument code and translates into description
 * like: getValueWithDesc(lastAnswer.getResultCode(),0)
 * @param avp
 * @param lvl
 * @return
 */


   public static String getValueWithDesc(Avp avp, int lvl)
   {
     String avpType = AvpInfo.getType(avp.getCode());
 
     if (avpType == null) {
       return null;
     }
     AvpType type = AvpType.valueOf(avpType);
     Object val = getValue(avp, lvl);
 
     //if ((type == AvpType.Unsigned32) || (type == AvpType.Enumerated))
     //{
       //if ((val != null) && (!val.equals("???")) && (!val.startsWith("{")))
       //{
       //  String d = AvpInfo.getValueDesc(avp.getCode(), Integer.parseInt(val));
 
       //  return d + "(" + val + ")";
       //}
     //}
 
     return "[getValueWithDesc]";
   }
 
   public static Object getValue(AvpSet avpSet, int avpCode, int lvl)  // robertsp, changed return type to Object
   {
     if (avpSet != null)
     {
       Avp avp = avpSet.getAvp(avpCode);
       if (avp != null) {
         return getValue(avp, lvl);
       }
     }
 
     return null;
   }
 
   private static Object getValue(Avp avp, int lvl) // robertsp, changed return type to Object
   {
     String avpType = AvpInfo.getType(avp.getCode());
     if (avpType == null)
       return null;
     try
     {
       AvpType type = AvpType.valueOf(avpType);
       if (type == AvpType.DiameterIdentity)
         return avp.getDiameterIdentity();
       if (type == AvpType.DiameterURI)
         return avp.getDiameterURI();
       if (type == AvpType.UTF8String)
         return avp.getUTF8String();
       if (type == AvpType.Float32)
         return avp.getFloat32();
       if (type == AvpType.Float64)
         return avp.getFloat64();
       if (type == AvpType.Integer32)
         return avp.getInteger32();
       if (type == AvpType.Integer64)
         return avp.getInteger64();
       if (type == AvpType.Unsigned32)
         return avp.getUnsigned32();
       if (type == AvpType.Unsigned64)
         return avp.getUnsigned64();
       if (type == AvpType.Grouped)
         return "{\n" + getAvpSetAsString(avp.getGrouped(), lvl + 1) + getTab(lvl) + "}";
       if (type == AvpType.Enumerated)
         return avp.getInteger32();
       if (type == AvpType.OctetString)
         return avp.getOctetString();
       if (type == AvpType.Time)
         return avp.getTime();
       if (type == AvpType.Address)
         return avp.getAddress().getHostAddress();
     }
     catch (AvpDataException ex) {
       log.error("error", ex);
     }
     return "val=???";
   }
 
   public static boolean isGrouped(Avp avp)
   {
     return isGrouped(avp.getCode());
   }
 
   public static boolean isGrouped(int code)
   {
     String avpType = AvpInfo.getType(code);
 
     return (avpType != null) && 
       (AvpType.valueOf(avpType) == AvpType.Grouped);
   }
 }

