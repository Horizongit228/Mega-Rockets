import dev.megarockets.OxygenRules;
public class OxygenCheck{
 public static void main(String[] args){
  int oxygen=OxygenRules.CAPACITY;
  for(int i=0;i<600;i++){if(OxygenRules.suffocating(true,true,oxygen))throw new AssertionError("Premature suffocation");oxygen=OxygenRules.next(oxygen,true,true,true);}
  if(oxygen!=0||!OxygenRules.suffocating(true,true,oxygen)||!OxygenRules.suffocating(true,false,600)||OxygenRules.suffocating(false,false,0))throw new AssertionError("Vacuum");
  for(int i=0;i<30;i++)oxygen=OxygenRules.next(oxygen,false,true,true);
  if(oxygen!=600||OxygenRules.next(600,false,true,true)!=600||OxygenRules.next(600,true,false,true)!=600||OxygenRules.next(600,true,true,false)!=600)throw new AssertionError("Tank");
  System.out.println("PASS oxygen 600 seconds, refill 30 seconds, suit seal and damage rules");
 }
}
