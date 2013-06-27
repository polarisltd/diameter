/*    */ package pl.p4.diameter.avp;
/*    */ 
/*    */ import java.io.IOException;
/*    */ import java.util.Enumeration;
/*    */ import java.util.Hashtable;
/*    */ import java.util.Properties;
/*    */ import java.util.logging.Level;
/*    */ import java.util.logging.Logger;

import org.slf4j.LoggerFactory;

/*    */ import pl.p4.config.ConfigParserProperties;
/*    */ import pl.p4.config.Configuration;
/*    */ 
/*    */ public class AvpInfo
/*    */ {
/* 22 */   private static Configuration config = null;
/* 23 */   private static Hashtable<String, String> names = new Hashtable();
/*    */ 
/*    */   public static int getCode(String name)
/*    */   {
/* 27 */     if (config == null) {
/* 28 */       init();
/*    */     }
/* 30 */     String code = (String)names.get(name);
/*    */ 
/* 32 */     if (code == null) {
/* 33 */       return -1;
/*    */     }
/* 35 */     return Integer.parseInt(code);
/*    */   }
/*    */ 
/*    */   public static String getName(int code)
/*    */   {
	        Logger logger = Logger.getLogger(AvpInfo.class.getName());
/* 40 */    String[] v = getValue(code);
             logger.info("getName() .. getValue("+code+")=>"+v);
/* 41 */     return (v != null) && (v.length > 0) ? v[0].trim() : null;
/*    */   }
/*    */ 
/*    */   public static String getType(int code)
/*    */   {
/* 46 */     String[] v = getValue(code);
/* 47 */     return (v != null) && (v.length > 1) ? v[1].trim() : null;
/*    */   }
/*    */ 
/*    */   public static int getVendorCode(int code)
/*    */   {
/* 52 */     String[] v = getValue(code);
/* 53 */     return (v != null) && (v.length > 2) ? Integer.parseInt(v[2].trim()) : -1;
/*    */   }
/*    */ 
/*    */   public static String getValueDesc(int code, int value)
/*    */   {
/* 58 */     String[] p = getValue(code);
/* 59 */     if ((p != null) && (p.length > 3))
/*    */     {
/* 61 */       String[] d = p[3].split(",");
/* 62 */       for (int i = 0; i < d.length; i++) {
/* 63 */         if (d[i].trim().startsWith(String.valueOf(value))) { // robertsp
/* 64 */           String[] v = d[i].split("=");
/* 65 */           if ((v != null) && (v.length > 1)) {
/* 66 */             return v[1].trim();
/*    */           }
/* 68 */           return "???";
/*    */         }
/*    */       }
/* 71 */       return "???";
/*    */     }
/* 73 */     return null;
/*    */   }
/*    */ 
/*    */   private static String[] getValue(int code)
/*    */   {
	         Logger logger = Logger.getLogger(AvpInfo.class.getName());
/* 78 */     if (config == null) {
	           logger.info("getValue() config=null");
/* 79 */       init();
/*    */     }
/* 81 */     String v = (String)config.get(Integer.toString(code)); // robertsp
             logger.info("getValue() config.get("+code+")=>"+v);
/* 83 */     return v != null ? v.split(":") : null;
/*    */   }
/*    */ 
/*    */   private static void init()
/*    */   {
	         Logger myLogger = Logger.getLogger(AvpInfo.class.getName());
/*    */     try {
/* 89 */       config = ConfigParserProperties.parseFile("conf/avpinfo.properties");
/*    */     } catch (IOException ex) {
/* 91 */       Logger.getLogger(AvpInfo.class.getName()).log(Level.SEVERE, null, ex);
/* 92 */       return;
/*    */     }
/*    */ 
/* 95 */     Properties props = config.getProperties();
/*    */ 
             myLogger.info("AvpInfo.init() avpinfo.properties[]="+props);
             if(names==null)myLogger.info("names=NULL!!");
/* 97 */     for (Enumeration e = props.propertyNames(); e.hasMoreElements(); )
/*    */     {
/* 99 */       String key = (String)e.nextElement();
                String value = getName(Integer.parseInt(key));
                myLogger.info("key:value="+key+":"+value);
                names.put(value,key); // robertsp value, key -- to allow name to code lookups!!
/*    */     }
/*    */   }
/*    */ }

/* Location:           C:\Java\JDiameter\p4.versions\1.6.0_b82\jdiam.jar
 * Qualified Name:     pl.p4.diameter.avp.AvpInfo
 * JD-Core Version:    0.6.0
 */