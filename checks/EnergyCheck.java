import dev.megarockets.EnergyRules;
public class EnergyCheck{
 public static void main(String[] args){
  if(EnergyRules.solar(6000,false,true,1,1)!=16||EnergyRules.solar(18000,false,true,1,1)!=0||EnergyRules.solar(6000,false,false,1,1)!=0||EnergyRules.solar(18000,true,true,1,1)!=16)throw new AssertionError("Light");
  if(EnergyRules.solar(6000,false,true,2,1)>=16||EnergyRules.solar(6000,false,true,1,.5)!=8)throw new AssertionError("Distance/weather");
  int charge=EnergyRules.CAPACITY;
  for(int s=0;s<18000;s++)charge=EnergyRules.stored(charge,0,1,EnergyRules.CAPACITY);
  if(charge!=42000||EnergyRules.stored(0,0,1,EnergyRules.CAPACITY)!=0||EnergyRules.stored(60000,16,1,60000)!=60000)throw new AssertionError("Storage");
  System.out.println("PASS solar output, shade, night, weather, charge limits");
 }
}
