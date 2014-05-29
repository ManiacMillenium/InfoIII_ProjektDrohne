package mygame;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;

/**
 *
 * @author Christoph Duda
 */
public class Flaeche {
    AssetManager assetManager;
    float x,y,z;
    boolean hover, aktiv;
    int typ;
    Box box;
    Geometry flaeche;
    Node rootNode;
    
    public Flaeche(AssetManager manager, Node root, float koordX, float koordY, float koordZ, int type){
        assetManager = manager;
        x = koordX;
        y = koordY;
        z = koordZ;
        typ = type;
        rootNode = root;
        
        box = new Box(Vector3f.ZERO, 0.96f, 0.05f, 0.48f);
        flaeche = new Geometry("Parkplatz", box);
        Material mat_flaeche = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat_flaeche.setTexture("ColorMap", assetManager.loadTexture("Textures/transparent.png"));
        mat_flaeche.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
        flaeche.setMaterial(mat_flaeche);
        flaeche.setLocalTranslation(x, y+0.025f, z);
        flaeche.setShadowMode(RenderQueue.ShadowMode.Off);
        flaeche.setQueueBucket(RenderQueue.Bucket.Transparent);
        rootNode.attachChild(flaeche);
    }
    
    public int getType(){
        return typ;
    }
    public boolean isHover(){
        return hover;
    }
    public boolean isActive(){
        return aktiv;
    }
}
