import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.net.URI;
import java.net.HttpURLConnection;
import java.nio.file.Files;
import java.nio.file.Path;

/** Builds the two shaded planet disks from NASA satellite mosaics. No network access in the game. */
public class PlanetTextures {
    private static final int SIZE=1024;
    private static final String EARTH="https://upload.wikimedia.org/wikipedia/commons/archive/9/91/20170416020821%21Land_shallow_topo_2048.jpg";
    private static final String MOON="https://svs.gsfc.nasa.gov/vis/a000000/a004700/a004720/lroc_color_2k.jpg";
    public static void main(String[] args) throws Exception {
        Path root=Path.of(args[0],"assets","megarockets","textures","sky");Files.createDirectories(root);
        create(root.resolve("earth.png"),EARTH,true,12,8);
        create(root.resolve("moon.png"),MOON,false,0,0);
        for(String name:new String[]{"mercury","venus","mars","jupiter","saturn","uranus","neptune","europa","titan"})procedural(root.resolve(name+"_map.png"),name);
    }
    private static void procedural(Path out,String name)throws Exception{
        BufferedImage img=new BufferedImage(512,256,BufferedImage.TYPE_INT_RGB);
        int[] rgb=switch(name){case "mercury"->new int[]{151,145,137};case "venus"->new int[]{218,182,116};case "mars"->new int[]{179,101,65};case "jupiter"->new int[]{201,161,125};case "saturn"->new int[]{221,190,138};case "uranus"->new int[]{146,208,216};case "neptune"->new int[]{68,112,203};case "europa"->new int[]{194,205,196};default->new int[]{185,132,64};};
        for(int y=0;y<256;y++)for(int x=0;x<512;x++){
            double lon=x*Math.PI/256,lat=(.5-y/255.)*Math.PI;
            double n=Math.sin(lon*7+2*Math.cos(lat*9))*Math.cos(lat*11-lon*4)+.25*Math.sin(lon*31+lat*19);
            boolean gas=name.equals("jupiter")||name.equals("saturn")||name.equals("uranus")||name.equals("neptune");
            double band=Math.sin(lat*26+.15*Math.sin(lon*6))+.4*Math.sin(lat*63+.2*Math.cos(lon*12));
            double amp=name.equals("jupiter")?24:name.equals("saturn")?10:4;
            double d=gas?band*amp+4*n: n*(name.equals("titan")?7:26);
            int red=rgb[0],green=rgb[1],blue=rgb[2];
            if(name.equals("mars")&&Math.abs(lat)>1.3){red=220;green=209;blue=193;}
            if(name.equals("europa")&&Math.abs(Math.sin(lon*13+lat*17+2*Math.sin(lat*8)))<.10){red=134;green=106;blue=81;}
            if(name.equals("jupiter")&&Math.hypot((Math.IEEEremainder(lon-2.2,2*Math.PI))/.22,(lat+.35)/.09)<1){red=185;green=99;blue=67;}
            img.setRGB(x,y,((int)clamp(red+d,0,255)<<16)|((int)clamp(green+d,0,255)<<8)|(int)clamp(blue+d,0,255));
        }ImageIO.write(img,"PNG",out.toFile());
    }
    private static BufferedImage read(String url) throws Exception {
        var con=(HttpURLConnection)URI.create(url).toURL().openConnection();
        con.setRequestProperty("User-Agent","MegaRockets/0.6.1 (https://github.com/Horizongit228/Mega-Rockets; build-time NASA texture attribution)");
        con.setConnectTimeout(25000);con.setReadTimeout(30000);con.setInstanceFollowRedirects(true);
        try(var in=con.getInputStream()){
            BufferedImage img=ImageIO.read(in);
            if(img==null||img.getWidth()<1000||img.getWidth()<img.getHeight()*1.9)throw new IllegalStateException("Invalid map: "+url);
            return img;
        } finally {con.disconnect();}
    }
    private static double clamp(double x,double lo,double hi){return Math.max(lo,Math.min(hi,x));}
    private static int sample(BufferedImage img,double longitude,double latitude) {
        double u=(longitude/(2*Math.PI)+.5)*img.getWidth()-.5;
        double v=(.5-latitude/Math.PI)*img.getHeight()-.5;
        int x=(int)Math.floor(u),y=(int)Math.floor(v);double a=u-x,b=v-y;
        int x0=Math.floorMod(x,img.getWidth()),x1=Math.floorMod(x+1,img.getWidth());
        int y0=(int)clamp(y,0,img.getHeight()-1),y1=(int)clamp(y+1,0,img.getHeight()-1);
        int[] c={img.getRGB(x0,y0),img.getRGB(x1,y0),img.getRGB(x0,y1),img.getRGB(x1,y1)};
        int out=0;
        for(int shift:new int[]{16,8,0}){
            double p=((c[0]>>shift)&255)*(1-a)+((c[1]>>shift)&255)*a;
            double q=((c[2]>>shift)&255)*(1-a)+((c[3]>>shift)&255)*a;
            out|=(int)Math.round(p*(1-b)+q*b)<<shift;
        }return out;
    }
    private static void create(Path out,String url,boolean earth,double lonDeg,double latDeg)throws Exception{
        BufferedImage src=read(url),dst=new BufferedImage(SIZE,SIZE,BufferedImage.TYPE_INT_ARGB);
        ImageIO.write(src,"PNG",out.resolveSibling((earth?"earth":"moon")+"_map.png").toFile());
        double lat=Math.toRadians(latDeg),lon=Math.toRadians(lonDeg);
        for(int j=0;j<SIZE;j++)for(int i=0;i<SIZE;i++){
            double x=(i+.5-SIZE/2.0)/(SIZE/2.0-2),y=(SIZE/2.0-j-.5)/(SIZE/2.0-2),r=Math.hypot(x,y);
            if(r>1.002)continue;
            double z=Math.sqrt(Math.max(0,1-r*r));
            double latitude=Math.asin(clamp(y*Math.cos(lat)+z*Math.sin(lat),-1,1));
            double longitude=lon+Math.atan2(x,z*Math.cos(lat)-y*Math.sin(lat));
            int c=sample(src,longitude,latitude);
            double light=(earth?.27:.14)+(earth?.73:.86)*Math.max(0,-.43*x+.43*y+.79*z);
            double rim=earth?Math.pow(clamp(r,0,1),9)*.45:0;
            int red=(int)clamp(((c>>16)&255)*light+20*rim,0,255);
            int green=(int)clamp(((c>>8)&255)*light+85*rim,0,255);
            int blue=(int)clamp((c&255)*light+150*rim,0,255);
            int alpha=(int)(255*clamp((1.002-r)*SIZE/2,0,1));
            dst.setRGB(i,j,(alpha<<24)|(red<<16)|(green<<8)|blue);
        }
        if(!ImageIO.write(dst,"PNG",out.toFile()))throw new IllegalStateException("PNG encoder unavailable");
        System.out.println("Generated "+out+" from "+src.getWidth()+"x"+src.getHeight()+" satellite map");
    }
}
