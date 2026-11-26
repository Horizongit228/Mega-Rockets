import dev.megarockets.StationLayout;
public class StationCheck{
 public static void main(String[] args){
  int z=StationLayout.Z;
  if(StationLayout.height(StationLayout.PAD_X,z)!=101||StationLayout.material(StationLayout.PAD_X,101,z)!=0)throw new AssertionError("Pad");
  if(StationLayout.material(8,101,z)!=StationLayout.DOOR_LOWER||StationLayout.material(14,102,z)!=StationLayout.DOOR_UPPER)throw new AssertionError("Airlock");
  if(StationLayout.material(-5,101,z)!=StationLayout.OXYGEN||StationLayout.material(11,101,z+1)!=StationLayout.OXYGEN)throw new AssertionError("Oxygen");
  if(!StationLayout.gravity(32,101,z)||StationLayout.gravity(100,101,z)||StationLayout.gravity(32,200,z))throw new AssertionError("Gravity");
  for(int x=-7;x<=7;x++)for(int k=-5;k<=5;k++)if(StationLayout.material(x,100,z+k)==0||StationLayout.material(x,107,z+k)==0)throw new AssertionError("Seal");
  System.out.println("PASS station layout");
 }
}
