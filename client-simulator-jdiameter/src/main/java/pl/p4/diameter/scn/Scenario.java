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
/*  29 */   private static Logger log = LoggerFactory.getLogger(Scenario.class);
/*  30 */   private static String errParsing = "Scenario file parsing failed";
 
/*  32 */   public static Object finished = new Object();
/*  33 */   private static int sessionCtr = 0;
 
/*  35 */   public Session session = null;
/*  36 */   public Answer lastAnswer = null;
   private AbstractList<Step> steps;
 
   public Scenario(String fname)
     throws IOException
   {
/*  41 */     super(fname, 1, RepeatType.SEQUENCE);
 
/*  43 */     load(fname);
   }
 
   public Scenario(String fname, int repeat, RepeatType type) throws IOException {
/*  47 */     super(fname, repeat, type);
 
/*  49 */     load(fname);
   }
 
   public void load(String fname) throws IOException {
/*  53 */     log.info("Loading scneario: " + this.name + " " + this.type + ":" + this.repeat);
 
/*  55 */     Configuration scnConfig = ConfigParserProperties.parseFile(fname);
 
/*  57 */     Properties props = scnConfig.getProperties();
 
/*  59 */     build(props);
   }
 
   public void run(SessionFactory factory) throws Exception {
/*  63 */     startScenario();
 
/*  65 */     log.info("Running scenario: " + this.name + " " + this.type + ":" + this.repeat);
 
/*  67 */     for (int i = 0; i < this.repeat; i++) {
/*  68 */       log.trace("Creating Diameter sesssion");
/*  69 */       this.session = factory.getNewSession();
 
/*  72 */       ListIterator it = this.steps.listIterator();
/*  73 */       while (it.hasNext()) {
/*  74 */         Step step = (Step)it.next();
 
/*  76 */         if ((step instanceof Scenario)) {
/*  77 */           Scenario scn = (Scenario)step;
/*  78 */           if (scn.type == RepeatType.SEQUENCE) {
/*  79 */             log.info("[SCN: " + scn.name + "] Running SEQ:" + (i + 1));
 
/*  81 */             scn.run(factory);
           }
           else {
/*  84 */             log.info("[SCN: " + scn.name + "] Running MULTI:" + (i + 1));
 
/*  86 */             new SessionThread(scn, factory).start();
           }
         }
/*  89 */         else if ((step instanceof Message)) {
/*  90 */           Message msg = (Message)step;
/*  91 */           log.info("Sending message: " + msg.name + " " + msg.type + ":" + msg.repeat);
 
/*  93 */           this.lastAnswer = ((Message)step).run(this.session, this.lastAnswer);
 

                  if(this.lastAnswer==null){
                	  log.info("Answer received: NULL"); 
                  }else{
                  

/*  95 */           int resCode = this.lastAnswer.getResultCode().getInteger32();
 
/*  97 */           log.info("Answer received: ResultCode=" + 
/*  98 */             AvpHelper.getValueWithDesc(this.lastAnswer.getResultCode(), 0));
 
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
     if (line.startsWith("LOG:")) {
       arg1 = new String[2];
       arg1[0] = "LOG";
       arg1[1] = line.split(":")[1];
     }
     else {
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
     } else {
       log.error(errParsing + ": unknown command [" + arg1[0] + "]");
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

