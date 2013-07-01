package pl.p4.config;
 
import java.io.PrintStream;
import java.util.Enumeration;
import java.util.Properties;
import java.util.logging.Logger;

import pl.p4.diameter.avp.AvpInfo;
 
public class Configuration
{
   private Properties props;
 
   public Configuration(Properties props)
   {
     this.props = props;
   }
 
   public Enumeration<?> getPropertyName()
   {
     return this.props.propertyNames();
   }
 
   public Properties getProperties()
   {
     return this.props;
   }
 
   public String get(EnumProperties enumprop)
   {
     return this.props.getProperty(enumprop.value);
   }
 
   public String get(String key)
   {
	       Logger logger = Logger.getLogger(AvpInfo.class.getName());
	       String v = this.props.getProperty(key.trim()); // maybe trim() makes a trick?
	       //if(v==null){
	       //	   logger.info("Confuguration get("+key+")=>"+v);
	       //}
     return v;
   }
 
   public Object get(Object key)
   {
     return this.props.get(key);
   }
 
   public int getInt(EnumProperties enumprop)
   {
     String p = get(enumprop);
 
     if (p != null) {
       return Integer.parseInt(p);
     }
     return -1;
   }
 
   public boolean getBoolean(EnumProperties enumprop)
   {
     String p = get(enumprop);
 
     return Boolean.parseBoolean(p);
   }
 
   public String toString()
   {
     String list = "";
     for (Enumeration e = this.props.propertyNames(); e.hasMoreElements(); )
     {
       String key = (String)e.nextElement();
       list = list + key + " = " + this.props.getProperty(key) + "\n";
     }
 
     return list;
   }
 
   public void printAll(PrintStream out)
   {
     this.props.list(out);
   }
}

