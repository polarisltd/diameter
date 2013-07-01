 package pl.p4.diameter.scn;
 
 public class Step
 {
   public String name;
   public int repeat;
   public RepeatType type;
 
   public Step(String name, int repeat, RepeatType type)
   {
     this.name = name;
     this.repeat = repeat;
     this.type = type;
   }
 }

