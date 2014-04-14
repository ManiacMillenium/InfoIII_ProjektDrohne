package mygame;

import com.jme3.app.SimpleApplication;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Dome;
import com.jme3.shadow.DirectionalLightShadowFilter;
import com.jme3.shadow.DirectionalLightShadowRenderer;

public class Main extends SimpleApplication {

    //Ziel Positionswerte
    float posX, posY, posZ, flughoehe, bodenhoehe, toleranz;
    //Status Abfragen
    boolean xErreicht, zErreicht, zielErreicht, autoWartet;
    Vector3f zielposition;
   
    // Le Drone
    Dome mesh = new Dome(Vector3f.ZERO, 2, 3, .4f,false);
    Geometry drone = new Geometry("Drone", mesh);
    // Zielposition Anzeige
    Box zielObj = new Box(Vector3f.ZERO, 0.1f, 0.1f, 0.1f);
    Geometry target = new Geometry("Box", zielObj);
    
    Waypoint wp1, wp2, wp3;
       
    public static void main(String[] args) {
                
        Main app = new Main();
        app.start();
    }

    public void fliegeZuWP (float xKoord, float zKoord, float hoehe){
        float wpX = xKoord;
        float wpZ = zKoord;
        float height = hoehe;
        float winkel, ziel;
        
        Vector3f pos = drone.getLocalTranslation();
        
        winkel = drone.getWorldRotation().getY();
        //System.out.println("winkel: "+winkel);
        ziel = drone.getWorldTranslation().angleBetween(pos);
        //System.out.println("zwischen: "+ziel);
        
        if (pos.x < wpX+toleranz && pos.x > wpX-toleranz){
            xErreicht = true;
            //System.out.println("X erreicht!");
        }
        else{
            if (xErreicht != false){
                xErreicht = false; 
                System.out.println("X_false");
            }
        }
        
        if (pos.z < wpZ+toleranz && pos.z > wpZ-toleranz){
            zErreicht = true;
            //System.out.println("Z erreicht!");
        }
        else{
            if (zErreicht != false){
                zErreicht = false;
                System.out.println("Z_false");                
            }
        }
        
        if (zErreicht && xErreicht && pos.y <= bodenhoehe){
            zielErreicht = true;
        }
        
        if (!xErreicht | !zErreicht){
            
            if(pos.y > height){
                drone.lookAt(zielposition, Vector3f.UNIT_Y);
                    
                if (pos.x < wpX-toleranz){
                    //System.out.println("X < X: "+pos.x);
                    drone.move(0.02f, 0, 0);
                }
                
                if (pos.z < posZ-toleranz){
                    //System.out.println("Z < Z: "+pos.z);
                    drone.move(0, 0, 0.02f);
                }
                
                if (pos.z > posZ+toleranz){
                    //System.out.println("Z > Z: "+pos.z);
                    drone.move(0, 0, -0.02f);
                }
                    
                if (pos.x > wpX+toleranz){
                    //System.out.println("X > X: "+pos.x);
                    drone.move(-0.02f, 0, 0);
                }
            }
            else{
                    if (pos.y < height){
                        //System.out.println("Flughöhe anpassen...");
                        drone.move(0, 0.02f, 0);   
                    }
            }
        }
        if (xErreicht && zErreicht && pos.y > bodenhoehe){
            drone.move(0, -0.02f, 0);
            //System.out.println("Lande...");
        }
        if (xErreicht && zErreicht && pos.y <=bodenhoehe){
            //System.out.println("Position erreicht!");
        }
    }
        
    @Override
    public void simpleInitApp() {        
        //Tastenbelegung laden
        initKeys();
        //Ziel Position 
        posX = 0;
        posY = 0.2f;
        posZ = 0;
        flughoehe = 2;
        bodenhoehe = 0.3f;
        toleranz = 0.2f;
        
        //Waypoints einrichten
        wp1 = new Waypoint(2,4,2);
        wp2 = new Waypoint(-3,2,2);
        wp3 = new Waypoint(0,1,2);
        
        xErreicht = false;
        zErreicht = false;
        zielErreicht = false;
        autoWartet = false;
        
        zielposition = new Vector3f(posX,posY,posZ);
        System.out.println("Ziel: "+posX+", "+posY+", "+posZ);
        
        // Brauner Boden
        Box b = new Box(Vector3f.ZERO, 14, 0.1f, 8);
        Geometry geom = new Geometry("Box", b);
        geom.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);

        // Aktuelles Ziel
        target.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
        //Positionierung des Zieles
        target.move(posX, posY, posZ);
        
        //Positionierung und Ausrichtung der Drone
        drone.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
        drone.move(4f, 0.2f, 0f);
        drone.rotate(0f,0f,1.5f);
        
        //Bodenmaterial
        Material mat_boden;
        mat_boden = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        mat_boden.setBoolean("UseMaterialColors",true);    
        mat_boden.setColor("Diffuse",ColorRGBA.White);
        mat_boden.setColor("Specular",ColorRGBA.White);
        mat_boden.setFloat("Shininess", 12f);  // [0,128]
        geom.setMaterial(mat_boden);

        //Zielobjekt Material
        Material mat_target;
        mat_target = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        mat_target.setBoolean("UseMaterialColors",true);    
        mat_target.setColor("Diffuse",ColorRGBA.Yellow);
        mat_target.setColor("Specular",ColorRGBA.White);
        mat_target.setFloat("Shininess", 12f);  // [0,128]
        target.setMaterial(mat_target);
        
        //Dronenmaterial
        Material mat_drone;
        mat_drone = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        mat_drone.setBoolean("UseMaterialColors",true);    
        mat_drone.setColor("Diffuse",ColorRGBA.Blue);
        mat_drone.setColor("Specular",ColorRGBA.White);
        mat_drone.setFloat("Shininess", 64f);  // [0,128]
        drone.setMaterial(mat_drone);

        rootNode.attachChild(geom);
        rootNode.attachChild(target);
        rootNode.attachChild(drone);
        
        /** Must add a light to make the lit object visible! */
        DirectionalLight sun = new DirectionalLight();
        sun.setDirection(new Vector3f(-1,-1,-1).normalizeLocal());
        sun.setColor(ColorRGBA.Gray);
        rootNode.addLight(sun);
    
        PointLight licht1 = new PointLight();
        licht1.setPosition(new Vector3f(0,2,0));
        licht1.setRadius(800f);
        licht1.setColor(ColorRGBA.White);
        rootNode.addLight(licht1);
    
        AmbientLight ambiLicht = new AmbientLight();
        ambiLicht.setColor(ColorRGBA.White.mult(1.8f));
        rootNode.addLight(ambiLicht);
    
        /* Drop shadows */
        final int SHADOWMAP_SIZE=1024;
        DirectionalLightShadowRenderer dlsr = new DirectionalLightShadowRenderer(assetManager, SHADOWMAP_SIZE, 3);
        dlsr.setLight(sun);
        viewPort.addProcessor(dlsr);
 
        DirectionalLightShadowFilter dlsf = new DirectionalLightShadowFilter(assetManager, SHADOWMAP_SIZE, 3);
        dlsf.setLight(sun);
        dlsf.setEnabled(true);
        FilterPostProcessor fpp = new FilterPostProcessor(assetManager);
        fpp.addFilter(dlsf);
        viewPort.addProcessor(fpp);
    
            
        //Kamera Position und Ausrichtung fest einstellen
        //flyCam.setEnabled(false);           // Kamera einfrieren
        final float ar = (float) this.settings.getWidth() / (float) this.settings.getHeight();
        cam.setFrustumPerspective(45, ar, 0.1f, 1000.0f);
        cam.setLocation(new Vector3f(-5, 10, 0));
        cam.lookAt(Vector3f.ZERO, Vector3f.UNIT_Y);
    }
    
        /** Tastenzuweisung. */
    private void initKeys() {
        // Drücken der Leertaste zeigt an, dass ein Fahrzeug angekommen ist
        inputManager.addMapping("Gast",  new KeyTrigger(KeyInput.KEY_SPACE));

        // Hinzufügen des Tastendrucks zum inputManager
        inputManager.addListener(actionListener, new String[]{"Gast"});
    }
    
    private ActionListener actionListener = new ActionListener() {
        public void onAction(String name, boolean keyPressed, float tpf) {
            if (name.equals("Gast") && !keyPressed) {
                if(!autoWartet){
                    setKoordinates(wp2);
                    target.move(posX, posY, posZ);
                    autoWartet = true;
                    System.out.println("Gast Wartet!");
                }
            }
        }
    };
    
    private void setKoordinates(Waypoint wp){
        //Ziel Position 
        posX = wp.getX();
        posZ = wp.getZ();
        flughoehe = wp.getHeight();
        bodenhoehe = 0.3f;
    }
    
    @Override
    public void simpleUpdate(float tpf) {
        if(autoWartet){
            fliegeZuWP(posX,posZ,flughoehe);
        }
        
        this.actionListener = new ActionListener(){
            public void onAction(String name, boolean pressed, float tpf){
                System.out.println(name + " = " + pressed);
            }
        };
    }

    @Override
    public void simpleRender(RenderManager rm) {

    }
}
