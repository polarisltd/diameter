 package pl.p4.diameter.scn;
 
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 public class Log extends Step
 {
   private static Logger log = LoggerFactory.getLogger(Log.class);
   private String comment;
 
   public Log(String comment)
   {
     super("LOG", 1, RepeatType.SEQUENCE);
 
     this.comment = comment;
   }
 
   public void run() {
     log.info(this.comment);
   }
 }

