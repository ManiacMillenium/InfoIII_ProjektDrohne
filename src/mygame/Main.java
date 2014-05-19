package mygame;

import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.niftygui.NiftyJmeDisplay;
import com.jme3.post.FilterPostProcessor;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Dome;
import com.jme3.scene.shape.Sphere;
import com.jme3.shadow.DirectionalLightShadowFilter;
import com.jme3.shadow.DirectionalLightShadowRenderer;
import com.jme3.system.AppSettings;
import com.jme3.util.SkyFactory;
import de.lessvoid.nifty.Nifty;
import mygame.GUI.MainMenuController;

public class Main extends SimpleApplication {

    public static void onAction() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    private MainMenuController myMainMenuController;
    private Sphere sphereMesh = new Sphere(32, 32, 10, false, true);
    private Geometry sphere = new Geometry("Sky", sphereMesh);
    private static boolean useHttp = false;
    private BulletAppState bulletAppState;
    private RigidBodyControl parkplatzcontrol;
    private RigidBodyControl autocontrol; 
    public Spatial Auto;
    
    

    /*Variablendeklaration*/
    //3D Objekte
    Dome mesh;
    Box zielObj;
    Geometry target;
    Node drone = new Node();
    
    //Ziel Positionswerte
    float posX, posY, posZ, flugGeschw, flughoehe, bodenhoehe, toleranz;
    
    //Status Abfragen
    boolean xErreicht, zErreicht, zielErreicht, aktZielErreicht, autoWartet, abholen, droneBusy, zurBase, droneParking;
    Vector3f ziel;
          
    //Wegepunkte
    Waypoint parkStation, einfahrt, wp1, wp2, wp3,aktZiel;
    Waypoint[] flugroute;
    int routenlaenge = 0;
    
    /*Konstruktor*/
    public Main (){
    // Wegpunkte Array
    flugroute = new Waypoint [8];
    }
       
    public static void main(String[] args) {              
        Main app = new Main();
        app.setShowSettings(false);
        AppSettings settings = new AppSettings(true);
        settings.put("Width", 1280);
        settings.put("Height", 800);
        settings.put("Title", "The Drone Model");
        settings.put("VSync", true);
        settings.put("Samples", 4);        
        app.setDisplayFps(false);
        app.setDisplayStatView(false);        
        app.setSettings(settings);
        app.start();
    }
    
    /*Hier wird die Flugroute erstellt.
     * Die errechneten Wegpunkte werden in ein eigenes Array gespeichert und einer Drohne zugewiesen*/
    public void erstelleFlugroute(){
        
        routenlaenge =0;
        flugroute[0]=einfahrt;
        flugroute[1]=wp1;
        flugroute[2]=wp2;
        flugroute[3]=wp3;
                
        //Durchsuchen des Arrays nach einem Wegpunkt, der noch nicht erreicht wurde
        for(int i=0; i < flugroute.length; i++){
            if(flugroute[i]!=null){
                routenlaenge++;
                flugroute[i].setID(i+1);
                System.out.println("ID des WP "+flugroute[i]+": "+flugroute[i].getID());
            }
        }
        
        System.out.println("Die Route besteht aus: "+routenlaenge+" Stationen");
        System.out.println("Station 1: "+wp1.x+", "+wp1.z);
        System.out.println("Station 2: "+wp2.x+", "+wp2.z);
        System.out.println("Station 3: "+wp3.x+", "+wp3.z);
    }
    
    //Flugspeicher leeren
    public void leereFlugroute(){
        routenlaenge = 0;

        for(int i=0; i < 8; i++){
            if(flugroute[i]!=null){
                flugroute[i].setNichtErreicht();
                flugroute[i].wpID = 0;
                //System.out.println("Flugroute "+flugroute[i]+" erreicht: "+flugroute[i].erreicht);
                //flugroute[i] = null;
                resetVariables();
            }
        }
        System.out.println("Flugroute geleert!");
    }
    
    //Flughöhe dem aktuellen Wegpunkt anpassen
    public void hoeheAnpassen(Waypoint wp){
        float height = wp.flughoehe;
        Vector3f pos = drone.getLocalTranslation();
        
        droneParking = false;
        //System.out.println("Drone Parking: "+droneParking);
        //System.out.println("Aktueller WP: "+aktZiel.x+", "+aktZiel.z);
        
        if (pos.y > height){
            lande();
        }
        if (pos.y < height && abholen || zurBase){
            drone.move(0, flugGeschw, 0);
        }
    }
    
    /*Abheben, fliegen und Landen zu einem Wegpunkt in einer vorgegebenen hoehe.*/
    public void fliegeZuWP (Waypoint wp){
        float wpX = wp.x;
        float wpZ = wp.z;
        float height = wp.flughoehe;
        ziel = new Vector3f(wpX,height,wpZ);
        
        if (!droneBusy){
            droneBusy = true;
            //System.out.println("Drone busy: "+droneBusy);    
        }

        //aktuelle Position der Drohne ermitteln
        Vector3f pos = drone.getLocalTranslation();
                
        /*Überprüfung ob die X-Koordinate erreicht wurde.*/
        if (pos.x < wpX+toleranz && pos.x > wpX-toleranz){
            xErreicht = true;
            //System.out.println("X erreicht!");
        }
        else{
            if (xErreicht != false){
                xErreicht = false; 
                //System.out.println("X_false");
            }
        }
        
        /*Überprüfung ob die Z-Koordinate erreicht wurde.*/
        if (pos.z < wpZ+toleranz && pos.z > wpZ-toleranz){
            zErreicht = true;
            //System.out.println("Z erreicht!");
        }
        else{
            if (zErreicht != false){
                zErreicht = false;
                //System.out.println("Z_false");                
            }
        }
        
        /*Sobald ein Wegpunkt erreicht wurde, wird dieser als "erreicht" markiert.
         * Danach soll geprüft werden, ob es einen weiteren Wegpunkt gibt*/
        if(zErreicht && xErreicht){
            wp.setErreicht();
            //System.out.println("ID des erreichten WP: "+wp.getID());
            if(routenlaenge > wp.getID()){
                naechsterWP();    
            }
            else{
                /*Wenn die Drohne am letzten Wegpunkt gelandet ist, ist das Ziel erreicht*/
                if (zErreicht && xErreicht && pos.y <= bodenhoehe){
                    abholen = false;
                    droneBusy = false;
                    //System.out.println("Drone busy: "+droneBusy);
                    zielErreicht = true;
                    //System.out.println("Drone Parking: "+droneParking);
                    if(!droneBusy && zielErreicht && !droneParking){
                        zurBase = true;
                        System.out.println("Zur Base: "+zurBase);
                        System.out.println("zielErreicht: "+zielErreicht);
                        fliegeZurBase();
                    }
                }
            }
        }
                
        /*Solange die X und Z Position nicht erreicht ist, soll die Drohne aufsteigen bis sie die angegebene Flughöhe erreicht hat.
         Dann werden X und Z position überprüft und die Drohne fliegt in die jeweilige Richtung.*/
        if (!xErreicht | !zErreicht){ 
                //System.out.println("X-Pos: "+pos.x);
                //System.out.println("Z-Pos: "+pos.z);
                //System.out.println("Hoehe: "+pos.y);
            if(pos.y < height+toleranz && pos.y > height-toleranz){
                drone.rotate(0, 0.1f, 0);
                //drone.lookAt(ziel, Vector3f.UNIT_Y);
                    
                if (pos.x < wp.x-toleranz){
                    //System.out.println("X < X: "+pos.x);
                    drone.move(flugGeschw, 0, 0);
                }
                
                if (pos.z < wp.z-toleranz){
                    //System.out.println("Z < Z: "+pos.z);
                    drone.move(0, 0, flugGeschw);
                }
                
                if (pos.z > wp.z+toleranz){
                    //System.out.println("Z > Z: "+pos.z);
                    drone.move(0, 0, -flugGeschw);
                }
                    
                if (pos.x > wp.x+toleranz){
                    //System.out.println("X > X: "+pos.x);
                    drone.move(-flugGeschw, 0, 0);
                }
            }
            else{
                hoeheAnpassen(wp);
            }
        }
        
                    //
            if (zurBase && pos.y <= bodenhoehe && !droneParking && !abholen){
                resetVariables();
                droneParking = true;
            }
        
        /*Wenn der Wegpunkt erreicht wurde und die Drohne über dem Boden steht soll diese Landen.*/
        if (xErreicht && zErreicht && pos.y > bodenhoehe && !droneParking){
            lande();
        }
    }
    
    //Landet die Drohne
    public void lande(){
        drone.move(0, -flugGeschw, 0);
        //System.out.println("Lande...");
    }
    
    //Lässt die Drohne zur Parkstation fliegen
    public void fliegeZurBase (){
        //System.out.println("zielErreicht: "+zielErreicht);
        if(zielErreicht){
            leereFlugroute();
            routenlaenge = 1;
            //aktZiel = parkStation;
            //setzeStartWP(parkStation);
            zielErreicht = false;
            zurBase = true;
        }
    }
    
    /*Die Routenliste wird auf ihre Länge überprüft.
     Dann wird nacheinander ermittelt welcher Wegpunkt noch nicht erreicht wurde.
     Der nächste unerreichte Punkt wird als aktuelles Ziel markiert.*/
    public void naechsterWP(){
        if(routenlaenge>0){
        for(int i =0; i < routenlaenge; i++){
            //System.out.println("Routenlänge: "+routenlaenge);
            if (!flugroute[i].getErreicht()){
                aktZiel = flugroute[i];
                target.setLocalTranslation(aktZiel.x,0.2f,aktZiel.z);
                //System.out.println("Aktueller WP: "+i+".   Erreicht: "+flugroute[i].getErreicht());
                break;
            }
        }
        }
    }
        
    /** Tastenzuweisung. */
    private void initKeys() {
        // Drücken der Leertaste zeigt an, dass ein Fahrzeug angekommen ist
        inputManager.addMapping("Gast",  new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addMapping("Route",  new KeyTrigger(KeyInput.KEY_R));
        inputManager.addMapping("Auto erscheint", new KeyTrigger(KeyInput.KEY_SPACE));

        // Hinzufügen des Tastendrucks zum inputManager
        inputManager.addListener(actionListener, new String[]{"Gast"});
        inputManager.addListener(actionListener, new String[]{"Route"});
        inputManager.addListener(actionListener, new String[]{"Auto erscheint"});
        
    }
    
    
    
    private ActionListener actionListener = new ActionListener() {
        public void onAction(String name, boolean keyPressed, float tpf) {
            if (name.equals("Gast") && !keyPressed) {
                if(!autoWartet){
                    setzeStartWP(einfahrt);
                    target.move(posX, posY, posZ);
                    autoWartet = true;
                    zielErreicht = false;
                    droneParking = false;
                    System.out.println("Gast Wartet!");
                }
            }
            if (name.equals("Route") && !keyPressed) {
                //Noch passiert nichts
            }
         if (name.equals("Auto erscheint")&&!keyPressed){
             if(!autoWartet){
                 rootNode.attachChild(Auto);
               }
             else 
                  rootNode.detachChild(Auto);
         }
        }
    };
    
    private void setzeStartWP(Waypoint wp){
        //Ziel Position 
        posX = wp.x;
        posZ = wp.z;
        flughoehe = wp.flughoehe;
        bodenhoehe = 0.3f;
    }
    
    private void resetVariables(){
        xErreicht = false;
        zErreicht = false;
        zielErreicht = false;
        droneParking = true;
        autoWartet = false;
        abholen = false;
        droneBusy = false;
        System.out.println("Variablen zurückgesetzt.");
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
        flugGeschw = 0.1f;
        toleranz = 0.2f+flugGeschw;
        
        //Waypoints einrichten
        parkStation = new Waypoint (0,0,5);
        einfahrt = new Waypoint (8, -5, 5);
        wp1 = new Waypoint(8,-18,2);
        wp2 = new Waypoint(-15,-18,2);
        wp3 = new Waypoint(-15,-2,2);
        
        xErreicht = false;
        zErreicht = false;
        zielErreicht = false;
        droneParking = true;
        autoWartet = false;
        abholen = false;
        droneBusy = false;
        
        //Das Grundobjekt
        Spatial parkplatz = assetManager.loadModel("Models/parkplatzNeu_2.j3o");
        parkplatz.scale(0.05f, 0.05f, 0.05f);
        parkplatz.rotate(0.0f, -3.14f, 0.0f);
        parkplatz.setLocalTranslation(-13, 0, -10.0f);
        parkplatz.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
        rootNode.attachChild(parkplatz);        
        Material mat_parkplatz = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        mat_parkplatz.setBoolean("UseMaterialColors",true);    
        mat_parkplatz.setColor("Diffuse",ColorRGBA.White);
        mat_parkplatz.setColor("Specular",ColorRGBA.White);
        mat_parkplatz.setFloat("Shininess", 100f); 
        parkplatz.setMaterial(mat_parkplatz); 
       
                                          
        // Aktuelles Ziel       
        zielObj = new Box(Vector3f.ZERO, 0.1f, 0.1f, 0.1f);
        target = new Geometry("Box", zielObj);              
        Material mat_target = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        mat_target.setBoolean("UseMaterialColors",true);    
        mat_target.setColor("Diffuse",ColorRGBA.Yellow);
        mat_target.setColor("Specular",ColorRGBA.White);
        mat_target.setFloat("Shininess", 12f);  // [0,128]
        target.setMaterial(mat_target);
        target.setLocalTranslation(posX, posY, posZ);
        target.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
        
        // Drohne
        Spatial droneDummy = assetManager.loadModel("Models/AR_Drone_Parrot_Dummy.j3o");
        droneDummy.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
        droneDummy.setLocalTranslation(0, 0.2f, 0);
        droneDummy.scale(0.01f);        
        Material mat_drone = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        mat_drone.setBoolean("UseMaterialColors",true);    
        mat_drone.setColor("Diffuse",ColorRGBA.Blue);
        mat_drone.setColor("Specular",ColorRGBA.White);
        mat_drone.setFloat("Shininess", 64f);  // [0,128]
        droneDummy.setMaterial(mat_drone);        
        drone.attachChild(droneDummy);
        rootNode.attachChild(target);
        rootNode.attachChild(drone);   
        
        //Auto
            Auto = assetManager.loadModel("Models/Auto-modelblend.obj");
            Auto.setLocalScale(0.3f);
            Auto.setLocalTranslation(15f,.0f ,-2.5f);
            
         
       
        // Licht
        DirectionalLight sun = new DirectionalLight();
        sun.setDirection(new Vector3f(-1,-1,-1).normalizeLocal());
        sun.setColor(ColorRGBA.White);
        rootNode.addLight(sun);    
        PointLight licht1 = new PointLight();
        licht1.setPosition(new Vector3f(0,2,0));
        licht1.setRadius(800f);
        licht1.setColor(ColorRGBA.White);
        rootNode.addLight(licht1);    
        AmbientLight ambiLicht = new AmbientLight();
        ambiLicht.setColor(ColorRGBA.White.mult(3.8f));
        rootNode.addLight(ambiLicht);
    
        // Schatten
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
        
        // Kamera
        flyCam.setDragToRotate(true);
        flyCam.setMoveSpeed(50);
        flyCam.setRotationSpeed(10);           
        //Vector3f dir = new Vector3f(-0.8404675f, -0.31721875f, -0.43930244f);
        //Vector3f up = new Vector3f(-0.28068435f, 0.948352f, -0.14780009f);
        //Vector3f left = new Vector3f(-0.46349823f, 9.1584027E-4f, 0.8860974f);        
        //cam.setAxes(left,up,dir); 
        final float ar = (float) this.settings.getWidth()/(float)this.settings.getHeight();
        cam.setFrustumPerspective(75, ar, 0.1f, 1000.0f);
        cam.setLocation(new Vector3f(-4, 14, -26));
        cam.lookAt(new Vector3f(-4, 0, -8), Vector3f.UNIT_Y);
        
        // GUI      
        myMainMenuController = new MainMenuController();
        stateManager.attach(myMainMenuController);       
        NiftyJmeDisplay niftyDisplay = new NiftyJmeDisplay(assetManager, inputManager, audioRenderer, guiViewPort);
        Nifty nifty = niftyDisplay.getNifty();
        guiViewPort.addProcessor(niftyDisplay);
        nifty.fromXml("Interface/MainMenuLayout.xml", "start", myMainMenuController);
        
        // Display
        guiNode.detachAllChildren();
        guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
        BitmapText helloText = new BitmapText(guiFont, false);
        helloText.setSize(10);
        helloText.setText("hier soll dann die Info sichtbar sein");
        helloText.setLocalTranslation(100, 100, 0);
        guiNode.attachChild(helloText);
        
        //Background
        rootNode.attachChild(SkyFactory.createSky(assetManager, "Textures/Chrome.dds", false));
    }
    
    @Override
    public void simpleUpdate(float tpf) {
        if(autoWartet){
            //berechneFlugroute();
            if(!abholen){
                leereFlugroute();
                erstelleFlugroute();
                naechsterWP();
                droneParking = false;
                abholen = true;
            }
        }
        
        //System.out.println("x - Erreicht: "+xErreicht);
        //System.out.println("z - Erreicht: "+zErreicht);
        //System.out.println("Ziel Erreicht: "+zielErreicht);
        //System.out.println("Drone Parkt: "+droneParking);
        //System.out.println("Auto wartet: "+autoWartet);
        //System.out.println("abholen: "+abholen);
        //System.out.println("Drone Busy: "+droneBusy);
        
        if(abholen){
            zurBase = false;
            fliegeZuWP(aktZiel);
        }
        
        if(droneParking){
        }
        
        if (zurBase){
            //System.out.println("Fliege zur Base!");
            abholen = false;
            fliegeZuWP(parkStation);
        }
        this.actionListener = new ActionListener(){
            public void onAction(String name, boolean pressed, float tpf){
                System.out.println(name + " = " + pressed);
            }
        };
        
        //System.out.println("Direction = " + cam.getDirection());
        //System.out.println("Up = " + cam.getUp());
        //System.out.println("Left = " +cam.getLeft());
        
        sphere.setLocalTranslation(cam.getLocation());
    }

    @Override
    public void simpleRender(RenderManager rm) {
        
    }
        public void onAction(String name, boolean isPressed, float tpf) {
        if(name.equals("Auto erscheint")){
            
            
            
          
        } else rootNode.detachChild(Auto);
        
    }
}
