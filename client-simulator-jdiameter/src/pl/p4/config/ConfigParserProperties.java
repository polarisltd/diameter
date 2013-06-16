/*    */ package pl.p4.config;
/*    */ 
/*    */ import java.io.FileInputStream;
/*    */ import java.io.IOException;
/*    */ import java.util.Properties;
/*    */ import java.util.logging.Logger;
/*    */ 
/*    */ public class ConfigParserProperties
/*    */ {
/*    */   public static Configuration parseFile(String configFile)
/*    */     throws IOException
/*    */   {
/* 22 */     Logger.getLogger(ConfigParserProperties.class.getName())
/* 23 */       .finest("Reading config file: " + configFile);
/*    */ 
/* 26 */     Properties props = new Properties();
/*    */ 
/* 29 */     props.load(new FileInputStream(configFile));
/*    */ 
/* 31 */     Configuration config = new Configuration(props);
/*    */ 
/* 33 */     Logger.getLogger(ConfigParserProperties.class.getName())
/* 34 */       .finest("Config loaded. ");
/*    */ 
/* 36 */     return config;
/*    */   }
/*    */ }

/* Location:           C:\Java\JDiameter\p4.versions\1.6.0_b82\jdiam.jar
 * Qualified Name:     pl.p4.config.ConfigParserProperties
 * JD-Core Version:    0.6.0
 */