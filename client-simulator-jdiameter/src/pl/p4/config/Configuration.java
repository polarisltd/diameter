/*    */ package pl.p4.config;
/*    */ 
/*    */ import java.io.PrintStream;
/*    */ import java.util.Enumeration;
/*    */ import java.util.Properties;
import java.util.logging.Logger;

import pl.p4.diameter.avp.AvpInfo;
/*    */ 
/*    */ public class Configuration
/*    */ {
/*    */   private Properties props;
/*    */ 
/*    */   public Configuration(Properties props)
/*    */   {
/* 22 */     this.props = props;
/*    */   }
/*    */ 
/*    */   public Enumeration<?> getPropertyName()
/*    */   {
/* 27 */     return this.props.propertyNames();
/*    */   }
/*    */ 
/*    */   public Properties getProperties()
/*    */   {
/* 32 */     return this.props;
/*    */   }
/*    */ 
/*    */   public String get(EnumProperties enumprop)
/*    */   {
/* 41 */     return this.props.getProperty(enumprop.value);
/*    */   }
/*    */ 
/*    */   public String get(String key)
/*    */   {
	       Logger logger = Logger.getLogger(AvpInfo.class.getName());
	       String v = this.props.getProperty(key.trim()); // maybe trim() makes a trick?
	       //if(v==null){
	    	   logger.info("Confuguration get("+key+")=>"+v);
	       //}
/* 46 */     return v;
/*    */   }
/*    */ 
/*    */   public Object get(Object key)
/*    */   {
/* 51 */     return this.props.get(key);
/*    */   }
/*    */ 
/*    */   public int getInt(EnumProperties enumprop)
/*    */   {
/* 59 */     String p = get(enumprop);
/*    */ 
/* 61 */     if (p != null) {
/* 62 */       return Integer.parseInt(p);
/*    */     }
/* 64 */     return -1;
/*    */   }
/*    */ 
/*    */   public boolean getBoolean(EnumProperties enumprop)
/*    */   {
/* 73 */     String p = get(enumprop);
/*    */ 
/* 75 */     return Boolean.parseBoolean(p);
/*    */   }
/*    */ 
/*    */   public String toString()
/*    */   {
/* 84 */     String list = "";
/* 85 */     for (Enumeration e = this.props.propertyNames(); e.hasMoreElements(); )
/*    */     {
/* 87 */       String key = (String)e.nextElement();
/* 88 */       list = list + key + " = " + this.props.getProperty(key) + "\n";
/*    */     }
/*    */ 
/* 91 */     return list;
/*    */   }
/*    */ 
/*    */   public void printAll(PrintStream out)
/*    */   {
/* 99 */     this.props.list(out);
/*    */   }
/*    */ }

/* Location:           C:\Java\JDiameter\p4.versions\1.6.0_b82\jdiam.jar
 * Qualified Name:     pl.p4.config.Configuration
 * JD-Core Version:    0.6.0
 */