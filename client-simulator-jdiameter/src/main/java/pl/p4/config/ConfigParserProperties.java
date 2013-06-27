 package pl.p4.config;
 
 import java.io.FileInputStream;
 import java.io.IOException;
 import java.util.Properties;
 import java.util.logging.Logger;
 
 public class ConfigParserProperties
 {
   public static Configuration parseFile(String configFile)
     throws IOException
   {
     Logger.getLogger(ConfigParserProperties.class.getName())
       .finest("Reading config file: " + configFile);
 
     Properties props = new Properties();
 
     props.load(new FileInputStream(configFile));
 
     Configuration config = new Configuration(props);
 
     Logger.getLogger(ConfigParserProperties.class.getName())
       .finest("Config loaded. ");
 
     return config;
   }
 }

