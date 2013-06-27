/*    */ package pl.p4.diameter.scn;
/*    */ 
/*    */ import java.io.IOException;
/*    */ import org.jdiameter.api.Answer;
/*    */ import org.jdiameter.api.Request;
/*    */ import org.jdiameter.api.Session;
/*    */ import org.jdiameter.api.SessionFactory;
/*    */ import org.slf4j.Logger;
/*    */ import org.slf4j.LoggerFactory;
/*    */ import pl.p4.diameter.msg.MessageHelper;
/*    */ 
/*    */ public class Message extends Step
/*    */ {
/* 15 */   private static Logger log = LoggerFactory.getLogger(Message.class);
/* 16 */   Request req = null;
/*    */ 
/*    */   public Message(String fname) {
/* 19 */     super(fname, 1, RepeatType.SEQUENCE);
/*    */   }
/*    */ 
/*    */   public Message(String fname, int repeat, RepeatType type) throws IOException {
/* 23 */     super(fname, repeat, type);
/*    */   }
/*    */ 
/*    */   public String toString()
/*    */   {
/* 29 */     return "MSG:" + this.name + " " + this.type + ":" + this.repeat;
/*    */   }
/*    */ 
/*    */   public void run(SessionFactory factory)
/*    */   {
/*    */   }
/*    */ 
/*    */   public Answer run(Session session, Answer lastAnswer)
/*    */     throws Exception
/*    */   {
/* 42 */     log.info("Creating and sending message [" + this.name + "]");
/*    */ 
/* 44 */     return MessageHelper.sendMessage(session, this.name, lastAnswer);
/*    */   }
/*    */ }

/* Location:           C:\Java\JDiameter\p4.versions\1.6.0_b82\jdiam.jar
 * Qualified Name:     pl.p4.diameter.scn.Message
 * JD-Core Version:    0.6.0
 */