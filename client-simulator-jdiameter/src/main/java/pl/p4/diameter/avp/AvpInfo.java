 package pl.p4.diameter.avp;
 
 import java.io.IOException;
 import java.util.Enumeration;
 import java.util.Hashtable;
 import java.util.Properties;

 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;

 import pl.p4.config.ConfigParserProperties;
 import pl.p4.config.Configuration;
 import pl.p4.diameter.scn.Message;
 
 public class AvpInfo
 {
   private static Configuration config = null;
   private static Hashtable<String, String> names = new Hashtable();
   private static Logger log = LoggerFactory.getLogger(AvpInfo.class);
 
   public static int getCode(String name)
   {
     if (config == null) {
       init();
     }
     String code = (String)names.get(name);
 
     if (code == null) {
       return -1;
     }
     return Integer.parseInt(code);
   }
 
   public static String getName(int code)
   {
    String[] v = getValue(code);
    //log.info("getName() .. getValue("+code+")=>"+v);
    return (v != null) && (v.length > 0) ? v[0].trim() : null;
   }
 
   public static String getType(int code)
   {
     String[] v = getValue(code);
     return (v != null) && (v.length > 1) ? v[1].trim() : null;
   }
 
   public static int getVendorCode(int code)
   {
     String[] v = getValue(code);
     return (v != null) && (v.length > 2) ? Integer.parseInt(v[2].trim()) : -1;
   }
 
   public static String getValueDesc(int code, int value)
   {
     String[] p = getValue(code);
     if ((p != null) && (p.length > 3))
     {
       String[] d = p[3].split(",");
       for (int i = 0; i < d.length; i++) {
         if (d[i].trim().startsWith(String.valueOf(value))) { // robertsp
           String[] v = d[i].split("=");
           if ((v != null) && (v.length > 1)) {
             return v[1].trim();
           }
           return "???";
         }
       }
       return "???";
     }
     return null;
   }
 
   private static String[] getValue(int code)
   {
     if (config == null) {
	   log.info("getValue() config=null");
       init();
     }
     String v = (String)config.get(Integer.toString(code)); // robertsp
     //logger.info("getValue() config.get("+code+")=>"+v);
     return v != null ? v.split(":") : null;
   }
 
   private static void init()
   {
     try {
       config = ConfigParserProperties.parseFile("conf/avpinfo.properties");
     } catch (IOException ex) {
       log.error("IOException parseFile(conf/avpinfo.properties"+ ex.toString());
       return;
     }
 
     Properties props = config.getProperties();
 
     log.info("AvpInfo.init() avpinfo.properties[]="+props); // list AVP dictionary!
     if(names==null)log.info("names=NULL!!");
             
     for (Enumeration e = props.propertyNames(); e.hasMoreElements(); )
     {
       String key = (String)e.nextElement();
       String value = getName(Integer.parseInt(key));
       //log.info("key:value="+key+":"+value);
       names.put(value,key); // robertsp value, key -- to allow name to code lookups!!
     }
   }
 }

