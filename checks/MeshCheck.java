import dev.megarockets.client.TitanMesh;

public class MeshCheck {
    public static void main(String[] args) {
        float minY=Float.POSITIVE_INFINITY,maxY=Float.NEGATIVE_INFINITY,maxX=0,maxZ=0;
        for (var quad:TitanMesh.QUADS) {
            if (quad.points().length!=4) throw new AssertionError("Invalid quad");
            float n=quad.nx()*quad.nx()+quad.ny()*quad.ny()+quad.nz()*quad.nz();
            if (!Float.isFinite(n) || Math.abs(n-1)>1e-4) throw new AssertionError("Invalid normal");
            for(var p:quad.points()) {
                if(!Float.isFinite(p.x())||!Float.isFinite(p.y())||!Float.isFinite(p.z())) throw new AssertionError("Invalid point");
                minY=Math.min(minY,p.y());maxY=Math.max(maxY,p.y());
                maxX=Math.max(maxX,Math.abs(p.x()));maxZ=Math.max(maxZ,Math.abs(p.z()));
            }
        }
        if(TitanMesh.QUADS.size()<2500) throw new AssertionError("Expected detailed model");
        if(minY!=0||maxY!=64||maxX>7||maxZ>7) throw new AssertionError("Model does not fit entity bounds");
        if(TitanMesh.GLASS.size()!=56||TitanMesh.CABIN.size()<250)throw new AssertionError("Cabin mesh missing");
        for(var list:java.util.List.of(TitanMesh.GLASS,TitanMesh.CABIN))for(var q:list){
            float n=q.nx()*q.nx()+q.ny()*q.ny()+q.nz()*q.nz();
            if(!Float.isFinite(n)||Math.abs(n-1)>1e-4)throw new AssertionError("Cabin normal");
            for(var point:q.points())if(point.y()<50.9||point.y()>55.5||Math.hypot(point.x(),point.z())>3.81)throw new AssertionError("Cabin outside upper stage");
        }
        System.out.println("PASS cabin: "+TitanMesh.CABIN.size()+" interior quads, "+TitanMesh.GLASS.size()+" glass panes");
        System.out.printf("PASS: %d quads; height %.1f; extents X=±%.1f, Z=±%.1f; all normals finite and normalized.%n",TitanMesh.QUADS.size(),maxY-minY,maxX,maxZ);
    }
}
