 package pl.p4.diameter.scn;
 
 import java.io.IOException;
 import org.jdiameter.api.Answer;
 import org.jdiameter.api.Request;
 import org.jdiameter.api.Session;
 import org.jdiameter.api.SessionFactory;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 import pl.p4.diameter.msg.MessageHelper;
 
 public class Message extends Step
 {
   private static Logger log = LoggerFactory.getLogger(Message.class);
   Request req = null;
 
   public Message(String fname) {
     super(fname, 1, RepeatType.SEQUENCE);
   }
 
   public Message(String fname, int repeat, RepeatType type) throws IOException {
     super(fname, repeat, type);
   }
 
   public String toString()
   {
     return "MSG:" + this.name + " " + this.type + ":" + this.repeat;
   }
 
   public void run(SessionFactory factory)
   {
   }
 
   public Answer run(Session session, Answer lastAnswer)
     throws Exception
   {
     log.info("Creating and sending message [" + this.name + "]");
 
     return MessageHelper.sendMessage(session, this.name, lastAnswer);
   }
 }

