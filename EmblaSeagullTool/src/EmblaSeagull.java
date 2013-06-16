import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import jargs.gnu.*;



public class EmblaSeagull {

	
	
	final String COUNTERDEF_PATH = "seagullTool.properties";
	final int COUNTERDEF_DIGITS = 4;
	TimestampGen tsGen = null;  // instance variable
	String counterdefInitValue;
	
	
	public static void main(String[] args) {
		System.out.println("Version: 130613 14:32");

		new EmblaSeagull(args);
        
	}

/**
 * Constructor call
 */
	
	EmblaSeagull(String args[]){
		
		
		setupCounterdef();
	    doMain(args);
				
	}
	
/**
 * wrapped main method within instance context. Called from contructor 
 * 	
 * @param args
 */
	void doMain(String[] args){   // here we are at nonstatic context
		
		//System.out.println("args: "+args);
		  CmdLineParser parser = new CmdLineParser();
		  CmdLineParser.Option textOpt = parser.addStringOption('K',"Key");
		  CmdLineParser.Option countersOpt = parser.addStringOption('C',"Counterdef");		  
		  CmdLineParser.Option timestampOpt = parser.addStringOption('T',"Timestamp");
		  CmdLineParser.Option ipAddressOpt = parser.addStringOption('I',"IPAddress");
		  // Text replacements
		  Map<String, String> textReplacements = new HashMap<String, String>();
					  		 
		  try{
		     parser.parse(args);
		  }catch(Exception e){
			  e.printStackTrace();
		  }
		  Vector textOpts = parser.getOptionValues(textOpt);
		  for(Object s: textOpts){
			  String[] pair = ((String)s).split("=");
			  if(pair.length==2){ // protect against wrong param
			    System.out.println("Text replacement option: "+pair[0]+":"+pair[1]);
			    textReplacements.put(pair[0],pair[1]);
			  }
		  }
		  //
		  // Address replacements
		  //
		  Map<String, String> ipAddressRepl = new HashMap<String, String>();
	  		 
		  try{
		     parser.parse(args);
		  }catch(Exception e){
			  e.printStackTrace();
		  }
		  Vector ipOpts = parser.getOptionValues(ipAddressOpt);
		  for(Object s: ipOpts){
			  String[] pair = ((String)s).split("=");
			  String ipv4bin = convertIPtoBin(pair[1]);
			  System.out.println("ipaddr: "+pair[0]+":"+pair[1]+" => "+ipv4bin);
			  ipAddressRepl.put(pair[0],ipv4bin);
		  }
          //
		  //
		  //
		  String argTimestamp = (String)parser.getOptionValue(timestampOpt);
		  if(argTimestamp!=null){
			  System.out.println("Timestamp: "+argTimestamp);			  
		  }

		  tsGen = new TimestampGen(argTimestamp); // should be instantiated anyway to avoid nullpointers.

		  String[] fileArgs = parser.getRemainingArgs();
		  
		  if(fileArgs.length<2){
				usage();
				return;
		  }

		  for(String s: fileArgs){
			  System.out.println("Other: "+s);
		  }
		  
		  String argSeagullScenInputFile=fileArgs[0];
		  String argSeagullScenOutputFile=fileArgs[1];
		
		System.out.println("seagull scenario files '"+argSeagullScenInputFile+"' => '"+argSeagullScenOutputFile+"'");
        //String str_date="2013/02/19 22:57:05 +0000";
      
      try{
      	  // Open the file that is the first 
      	  // command line parameter
      	
      	  FileInputStream inStream = new FileInputStream(argSeagullScenInputFile);
      	  DataInputStream in = new DataInputStream(inStream);
      	  BufferedReader br = new BufferedReader(new InputStreamReader(in));
      	  
      	  FileWriter outStream = new FileWriter(argSeagullScenOutputFile);
            BufferedWriter out = new BufferedWriter(outStream);
    	          	  
      	  String strLine;
      	  //Read File Line By Line
            // <avp name="Event-Timestamp" value="xxxxxxx"> </avp>		  
      	  
      	  System.out.println("About to read lines");
      	  while ((strLine = br.readLine()) != null)   {
      		 // replace Event-Timestamp value 
      		 strLine = tsGen.replaceTimestamp(strLine); 
      		 // replace text  textReplacements
      		 strLine = replaceTokens(strLine,textReplacements);
      		 strLine = replaceTokens(strLine,ipAddressRepl);
      		 strLine = replaceCounterdef(strLine);
             out.write(strLine+"\n");
               //        		  
      		 //System.out.println (strLine);
      	  }
      	  //Close the input stream
      	  in.close();
            out.close();
      }catch (Exception e){//Catch exception if any
      	  System.err.println("Error: " + e.getMessage());
      }
      /////////////////////////////////////////////
		
				
	}

	static int nextRandomInRange(int Min,int Max){
		int result = Min + (int)(Math.random() * ((Max - Min) + 1));
		return result;
	}	
	
	
void setupCounterdef(){
    /*
	Properties properties = new Properties();
	String value = "";
	Integer i;
	try{
	  // read properties file
	  File fi = new File(COUNTERDEF_PATH);	
	  System.out.println("Seagulltool prop file: "+fi.getAbsolutePath());	
	  properties.load(new FileInputStream(fi));
	  value = (String) properties.get("counter");
      if(value == null)value="0";	  
	  i = Integer.parseInt(value);
	  i+=1;
	  counterdefInitValue=StringUtils.leftPad(i.toString(), COUNTERDEF_DIGITS, '0');		  
	}catch(Exception e){
  	  System.out.println("error rewading prop file");
	  e.printStackTrace();
	  counterdefInitValue=StringUtils.leftPad("1", COUNTERDEF_DIGITS, '0');; 
	}
	//
	//
	//
	try{
	    properties.put("counter",counterdefInitValue);
	    properties.store(new FileOutputStream(COUNTERDEF_PATH), null);
	    System.out.println("Value incremented <counterdef init=\""+counterdefInitValue+"\">");
  	}catch(Exception e){
  		  System.out.println("error writing prop file");
  		  e.printStackTrace();	
  	}
*/
  Integer genNumber = nextRandomInRange(1,9999);
  counterdefInitValue=StringUtils.leftPad(genNumber.toString(), COUNTERDEF_DIGITS, '0');
  System.out.println("New value for <counterdef init=\""+counterdefInitValue+"\">");
	
}
	
	
	
    private static void usage()
    {
        StringBuffer sb = new StringBuffer();
        sb.append("\n");      
        sb.append("EmblaSeagull Arguments: [options] seagull_scen_template seagull_scen_output file\n");
        sb.append("options:\n");
        sb.append("-K or --Key key=value // text replacement \'[key]\' to \'value\' \n");
        sb.append("-T or --Timestamp \"2013/02/19 22:57:05 +0000\"   //  \n");
        sb.append("   //used for \'Event-Timestamp\' AVP,  each next timestamp is +50 sec, updates directly into AVP\n");       
        sb.append("-I or --IPAddress \"10.0.10.1\" // converted into bin format for Seagull use\n");        
        sb.append("<counterdef \'init\' attribute is increased by 1 and padded up to 4 digits.(extra functionality)\n");       
        sb.append("example: java EmblaSeagull -T \"2013/02/19 22:57:05 +0000\" -K Called=6555544423 -K Calee=234672 seagull_scenario01 seagull_scenario01T\n");       
        System.out.print(sb.toString());
    }
	

/*
 * Example call:
		Map<String, String> replacements = new HashMap<String, String>();
		replacements.put("Name","Roberts");
		replacements.put("Invoice Number","987654321");
		replacements.put("Due Date","11/11/11");
		System.out.println(replaceTokens("Hello [Name] Please find attached [Invoice Number] which is due on [Due Date]",replacements));		
 	
 */

/**
 *     
 * @param ipV4Text - ip v4 address e.g. 10.0.10.1
 * @return
 */
String convertIPtoBin(String ipV4Text){
	String[] addrArray = ipV4Text.split("\\."); 
	long num = 0; 
	for (int i = 0; i < addrArray.length; i++) { 
		System.out.println("("+i+") "+addrArray[i]);
	    int power = 3 - i; 
	    num += ((Integer.parseInt(addrArray[i]) % 256 * Math.pow(256, power))); 
	}
	System.out.println("ip: "+ipV4Text+" = "+num+";  0x"+Long.toHexString(num));   
    return Long.toHexString(num); // do not prefix "0x", leave it at Seagull script as extra syntax will be required e.g. 0x0001[ip]
	
		
}
    
    
    
    
    
    
    
	
	public String replaceTokens(String text,
			Map<String, String> replacements) {
		boolean hasReplaced = false;
		Pattern pattern = Pattern.compile("\\[(.+?)\\]");
		Matcher matcher = pattern.matcher(text);
		StringBuffer buffer = new StringBuffer();
		while (matcher.find()) {
			String replacement = replacements.get(matcher.group(1));
			if (replacement != null) {
				matcher.appendReplacement(buffer, "");
				buffer.append(replacement);
       		    hasReplaced = true;
			}
		}
		matcher.appendTail(buffer);
		String strLine2 = buffer.toString();
		if(hasReplaced)System.out.println(">>> replaced text : "+text+" => "+strLine2);
		return strLine2;
	}	

	
String	replaceCounterdef(String strLine){
		 final String COUNTERDEF_TAG = "<counterdef";
      	 if(strLine.contains(COUNTERDEF_TAG)){ // ts is timestamp array, each next timestamp gets next value
     		StringBuffer sb = new StringBuffer(strLine); 
    		int p1 = sb.indexOf("init=\"")+6;
    		int p2 = sb.indexOf("\">",p1);
    		if (p1>0 && p2>0){    
    		    String strLine2 = sb.replace(p1,p2,counterdefInitValue).toString();
    		    System.out.println(">>> replaced counterdef init :  "+strLine+" => "+strLine2);
    		    //strLine = strLine2;
    		    return strLine2;
    	    }
      	 }	
    return strLine;  	 
	}
	
	
	/**
	 * 
	 * @author Roberts
	 *
	 */
	class TimestampGen{
		final CharSequence TIMESTAMP_TAG = "name=\"Event-Timestamp\"";
		private int tsIndex = 0; // contains current index of used timestamp
		private List<String> ts; // contains array of calculated  timestamps
		private String timestamp; // argument timestamp
		
        TimestampGen(String argTimestamp){	
		  //ts = new ArrayList<String>(); // initialize
          timestamp = argTimestamp;	
		  if(timestamp!=null){ // ts is initialized only if timestamp is present		   
            ts = calc(argTimestamp);
            System.out.println(ts);
		  }
        }

        
        String replaceTimestamp(String strLine){	
          if(timestamp==null)return strLine; // no timestamp was specified at constructor. 	
       	 
       	  if(strLine.contains(TIMESTAMP_TAG) && tsIndex<ts.size()){ // ts is timestamp array, each next timestamp gets next value
        		StringBuffer sb = new StringBuffer(strLine); 
       			String tsStr = ts.get(tsIndex++);
       		    int p1 = sb.indexOf("value=\"")+7;
       		    int p2 = sb.indexOf("\"",p1);
       		    String strLine2 = sb.replace(p1,p2,"0x"+tsStr).toString();
       		    System.out.println(">>> replaced Event-Timestamp :  "+strLine+" => "+strLine2);
       		    return strLine2;
       	  }
       	  return strLine; // default
       }	
        
    	List<String> calc(String str_date){	
            long myDateMillis=0;
            long d1900=0;
            Date myDate=null;
            List<String> ls=new ArrayList<String>();
    		try {
            
              myDate = parseDate(str_date);
      		  System.out.println(printDate(myDate)+" // orig date");
              //
              Calendar cal=Calendar.getInstance();
              
              cal.setTime(myDate);
              cal.add(Calendar.HOUR, -5);
              cal.add(Calendar.MINUTE, -30);   
              myDate = cal.getTime(); // Update date to -5:30
              //
              Calendar c = Calendar.getInstance();
              c.clear();
              c.setTimeZone(TimeZone.getTimeZone("GMT"));
              c.set(1900,0,1,0,0,0);
              d1900 = c.getTimeInMillis();
              System.out.println(printDate(c.getTime())+" // 1900");
    		
      		  for(int i=0;i<10;i++){
      	    	long durInSec = (cal.getTimeInMillis() - d1900)/1000;   
      	    	String durInSecHex = Long.toHexString(durInSec);
      			System.out.println(printDate(cal.getTime())+" "+durInSec+" "+durInSecHex);
                ls.add(durInSecHex);
      			cal.add(Calendar.SECOND, +50);   
      			
      		  }
    		
    		
    		
    		} catch (ParseException e) {
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    		} 
    		return ls;
            
    	}// DateCalc constructor     

     
    	public String printDate(Date d){
    		SimpleDateFormat sdf =
    		    new SimpleDateFormat("yyyy/MM/dd HH:mm:ss Z");
            sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
    		
    		return 	sdf.format(d);	
    		
    	}
    	public Date parseDate(String s) throws ParseException{				
            DateFormat sdf ; 
            sdf = new SimpleDateFormat("yyyyyy/MM/dd HH:mm:ss Z");  
            sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
    	    return (Date)sdf.parse(s);				
    	}
 
        
        
		
	}
	
	


	
	
	
}

