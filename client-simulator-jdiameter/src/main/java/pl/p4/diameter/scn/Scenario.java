 package pl.p4.diameter.scn;
 
 import java.io.IOException;
 import java.util.AbstractList;
 import java.util.ListIterator;
 import java.util.Properties;
 import java.util.Vector;
 import org.jdiameter.api.Answer;
 import org.jdiameter.api.Avp;
 import org.jdiameter.api.Session;
 import org.jdiameter.api.SessionFactory;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 import pl.p4.config.ConfigParserProperties;
 import pl.p4.config.Configuration;
 import pl.p4.diameter.avp.AvpHelper;
 
 public class Scenario extends Step
 {
   private static Logger log = LoggerFactory.getLogger(Scenario.class);
   private static String errParsing = "Scenario file parsing failed";
 
   public static Object finished = new Object();
   private static int sessionCtr = 0;
 
   public Session session = null;
   public Answer lastAnswer = null;
   private AbstractList<Step> steps;
 
   public Scenario(String fname)
     throws IOException
   {
     super(fname, 1, RepeatType.SEQUENCE);
 
     load(fname);
   }
 
   public Scenario(String fname, int repeat, RepeatType type) throws IOException {
     super(fname, repeat, type);
 
     load(fname);
   }
 
   public void load(String fname) throws IOException {
     log.info("Loading scneario: " + this.name + " " + this.type + ":" + this.repeat);
 
     Configuration scnConfig = ConfigParserProperties.parseFile(fname);
 
     Properties props = scnConfig.getProperties();
 
     build(props);
   }
 
   public void run(SessionFactory factory) throws Exception {
     startScenario();
 
     log.info("Running scenario: " + this.name + " " + this.type + ":" + this.repeat);
 
     for (int i = 0; i < this.repeat; i++) {
       log.trace("Creating Diameter sesssion");
       this.session = factory.getNewSession();
 
       ListIterator it = this.steps.listIterator();
       while (it.hasNext()) {
         Step step = (Step)it.next();
 
         if ((step instanceof Scenario)) {
           Scenario scn = (Scenario)step;
           if (scn.type == RepeatType.SEQUENCE) {
             log.info("[SCN: " + scn.name + "] Running SEQ:" + (i + 1));
 
             scn.run(factory);
           }
           else {
             log.info("[SCN: " + scn.name + "] Running MULTI:" + (i + 1));
 
             new SessionThread(scn, factory).start();
           }
         }
         else if ((step instanceof Message)) {
           Message msg = (Message)step;
           log.info("Sending message: " + msg.name + " " + msg.type + ":" + msg.repeat);
 
           this.lastAnswer = ((Message)step).run(this.session, this.lastAnswer);
 

           if(this.lastAnswer==null){
              log.info("Answer received: NULL. Scenario execution stopped"); 
              break;
           }else{
              int resCode = this.lastAnswer.getResultCode().getInteger32();
 
              //log.info("Answer received: ResultCode=" + resCode);
              log.info("Answer received: ResultCode=" + 
              AvpHelper.getValueWithDesc(this.lastAnswer.getResultCode(), 0));
              if (resCode != 2001) {
                  log.error("Result code: " + resCode + ". Scenario execution stopped");
                  break;
              }
           }

         }
         else if ((step instanceof Wait)) {
           ((Wait)step).run();
         }
         else if ((step instanceof Log)) {
           ((Log)step).run();
         }
         else if ((step instanceof HttpGet)) {
             ((HttpGet)step).run();
           }
         else if ((step instanceof HttpC)) {
             ((HttpC)step).run();
           }
         else if ((step instanceof CBSet)) {
             ((CBSet)step).run();
           }

       }
 
     }
 
     endScenario();
   }
 
   public boolean shouldWait() {
     return sessionCtr != 0;
   }
 
   private void startScenario() {
     updateScenarioCntr(1);
     log.trace("[SCN:" + this.name + "]Session counter increased: " + sessionCtr);
   }
 
   private void endScenario() {
     updateScenarioCntr(-1);
     log.trace("[SCN:" + this.name + "] Session counter decreased: " + sessionCtr);
 
     if (sessionCtr == 0) {
       log.trace("Notyfying about end of sessions");
       synchronized (finished) {
         finished.notify();
       }
     }
   }
 
   private synchronized void updateScenarioCntr(int val) {
     sessionCtr += val;
   }
 
   private boolean build(Properties props) throws IOException {
     if (this.steps == null)
       this.steps = new Vector();
     else {
       this.steps.clear();
     }
     for (int i = 1; i <= props.size(); i++) {
	     String s = Integer.toString(i);
       if (!parseLine(props.getProperty(s))) {
         return false;
       }
     }
     return true;
   }
 
   private boolean parseLine(String line) throws IOException {
     log.trace("Parsing line: " + line);
     String[] args = null; String[] arg1 = null; String[] arg2 = null;
     if (line.startsWith("#")) {
    	log.info("Comment line, ignored!");
        return true;
     }else if (line.startsWith("LOG:")) {
       arg1 = new String[2];
       arg1[0] = "LOG";
       arg1[1] = line.split(":")[1];
     }else if (line.startsWith("HTTPGET:")) {
       arg1 = new String[2];
       arg1[0] = "HTTPGET";
       arg1[1] = line.substring(line.indexOf(":")+1);
     }else if (line.startsWith("HTTPC:")) {
       arg1 = new String[2];
       arg1[0] = "HTTPC";
       arg1[1] = line.substring(line.indexOf(":")+1);
     }else if (line.startsWith("CBSET:")) {
       arg1 = new String[2];
       arg1[0] = "CBSET";
       arg1[1] = line.substring(line.indexOf(":")+1);      
     }else {
       args = line.split(" ");
       if (args.length > 2) {
         log.error(errParsing + ": incorrect number of parameters");
         return false;
       }
       arg1 = args[0].split(":");
       if (arg1.length != 2) {
         log.error(errParsing + ": incorrect syntax of parameter 1");
         return false;
       }
 
       if (args.length == 2) {
         arg2 = args[1].split(":");
         if (arg2.length != 2) {
           log.error(errParsing + ": incorrect syntax of parameter 2");
           return false;
         }
 
       }
 
     }
 
     if (arg1[0].equals("SCN")) {
       if (args.length == 1) {
         this.steps.add(new Scenario(arg1[1]));
       } else {
         int rpt = Integer.parseInt(arg2[1]);
         if (arg2[0].equals("SEQ"))
           this.steps.add(new Scenario(arg1[1], rpt, RepeatType.SEQUENCE));
         else if (arg2[0].equals("MULTI"))
           this.steps.add(new Scenario(arg1[1], rpt, RepeatType.MULTIPROCESS));
         else
           log.error(errParsing + ": unknown repeat type");
       }
     }
     else if (arg1[0].equals("MSG")) {
       if (args.length == 1) {
         this.steps.add(new Message(arg1[1]));
       } else {
         int rpt = Integer.parseInt(arg2[1]);
         if (arg2[0].equals("SEQ"))
           this.steps.add(new Scenario(arg1[1], rpt, RepeatType.SEQUENCE));
         else if (arg2[0].equals("MULTI"))
           this.steps.add(new Scenario(arg1[1], rpt, RepeatType.MULTIPROCESS));
       }
     }
     else if (arg1[0].equals("WAIT")) {
       this.steps.add(new Wait(Integer.parseInt(arg1[1])));
     }
     else if (arg1[0].equals("LOG")) {
       this.steps.add(new Log(arg1[1]));
     }else if (arg1[0].equals("HTTPGET")) {
           this.steps.add(new HttpGet(arg1[1]));
     }else if (arg1[0].equals("HTTPC")) {
         this.steps.add(new HttpC(arg1[1]));      
     }else if (arg1[0].equals("CBSET")) {
         this.steps.add(new CBSet(arg1[1]));      
     } else {
       log.info(errParsing + ": unknown command [" + arg1[0] + "]");
       return false;
     }
 
     return true;
   }
 
   public String toString()
   {
     String s = "SCN:" + this.name + " " + this.type + ":" + this.repeat + "[";
     log.trace(s);
 
     ListIterator it = this.steps.listIterator();
     while (it.hasNext()) {
       Step step = (Step)it.next();
       if ((step instanceof Scenario))
         s = s + ((Scenario)step).toString() + ",";
       else if ((step instanceof Message))
         s = s + ((Message)step).toString() + ",";
       else if ((step instanceof Wait))
         s = s + ((Wait)step).toString() + ",";
     }
     return s.substring(0, s.length() - 1) + "]";
   }
   public class SessionThread extends Thread {
     Scenario scn;
     SessionFactory factory;
 
     public SessionThread(Scenario scn, SessionFactory factory) { this.scn = scn;
       this.factory = factory; }
 
     public void run()
     {
       try {
         this.scn.run(this.factory);
       }
       catch (Exception e) {
         e.printStackTrace();
       }
     }
   }
 }

