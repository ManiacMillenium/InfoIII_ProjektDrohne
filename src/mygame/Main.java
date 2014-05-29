package mygame;

import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.collision.CollisionResults;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Ray;
import com.jme3.math.Vector2f;
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
/**
 *
 * @author Christoph Duda, GUI: Stefan Hartwig
 */
public class Main extends SimpleApplication {

    public static void onAction() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    private MainMenuController myMainMenuController;
    //Umgebung bzw. Skymap, wird als groÃŸe Kugel fÃ¼r den Hintergrund verwendet.
    private Sphere sphereMesh = new Sphere(32, 32, 10, false, true);
    private Geometry sphere = new Geometry("Sky", sphereMesh);
    private Geometry oldTarget = new Geometry("Box", new Box(Vector3f.ZERO, 0.01f, 0.01f, 0.01f));
    private BulletAppState bulletAppState;
    private RigidBodyControl parkplatzcontrol;
    private RigidBodyControl autocontrol; 
    public Spatial Auto;
    
    //Strahl der Maus fÃ¼r die Interaktion
    Ray mouseRay;
    //Ergebnisse der Kollision des Strahls
    CollisionResults results;
    //Material fÃ¼r die aktuelle Auswahl
    Material mat_auswahl;
    
    /*Variablendeklaration*/
    //3D Objekte
    Dome mesh;
    Box zielObj;
    Geometry destination;
    Node drone = new Node();
    Material unsichtbar;
    
    //Ziel Positionswerte
    float posX, posY, posZ, flugGeschw, flughoehe, bodenhoehe, toleranz;
    
    //Status Abfragen
    boolean xErreicht, zErreicht, zielErreicht, aktZielErreicht, autoWartet, 
            abholen, droneBusy, zurBase, droneParking;
    //Modusabfragen
    boolean debug, simulationMode, editMode;
    int parkplatzModus;     //Parkplatzmodi: 0=Einzelplatz Auswahl, 1=PlÃ¤tze hinzufÃ¼gen, 2=Parkplatzreihe lÃ¶schen
            
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
        app.setShowSettings(true);
        //AppSettings settings = new AppSettings(true);
        //settings.put("Width", 1280);
        //settings.put("Height", 800);
        //settings.put("Title", "The Drone Model");
        //settings.put("VSync", true);
        //settings.put("Samples", 4);        
        app.setDisplayFps(false);
        app.setDisplayStatView(false);        
        //app.setSettings(settings);
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
                if(debug){
                    System.out.println("ID des WP "+flugroute[i]+": "+flugroute[i].getID());
                }                
            }
        }
        if (debug){
            System.out.println("Die Route besteht aus: "+routenlaenge+" Stationen");
            System.out.println("Station 1: "+wp1.x+", "+wp1.z);
            System.out.println("Station 2: "+wp2.x+", "+wp2.z);
            System.out.println("Station 3: "+wp3.x+", "+wp3.z);            
        }
    }
    //Flugspeicher leeren
    public void leereFlugroute(){
        routenlaenge = 0;

        for(int i=0; i < 8; i++){
            if(flugroute[i]!=null){
                flugroute[i].setNichtErreicht();
                flugroute[i].wpID = 0;
                resetVariables();
            }
        }
        if(debug){
            System.out.println("Flugroute geleert!");
        }
    }
    
    //Setzt einen Wegpunkt mit dem die Flugroute begonnen werden soll
    private void setzeStartWP(Waypoint wp){
        //Ziel Position 
        posX = wp.x;
        posZ = wp.z;
        flughoehe = wp.flughoehe;
        bodenhoehe = 0.3f;
    }
    //FlughÃ¶he dem aktuellen Wegpunkt anpassen
    public void hoeheAnpassen(Waypoint wp){
        float height = wp.flughoehe;
        Vector3f pos = drone.getLocalTranslation();
        
        droneParking = false;
        if (debug){
            System.out.println("Drone Parking: "+droneParking);
            System.out.println("Aktueller WP: "+aktZiel.x+", "+aktZiel.z);            
        }       
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
                
        /*ÃœberprÃ¼fung ob die X-Koordinate erreicht wurde.*/
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
        
        /*ÃœberprÃ¼fung ob die Z-Koordinate erreicht wurde.*/
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
         * Danach soll geprÃ¼ft werden, ob es einen weiteren Wegpunkt gibt*/
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
                    zielErreicht = true;
                    if (debug){
                        System.out.println("Drone busy: "+droneBusy);
                    }                    
                    if(!droneBusy && zielErreicht && !droneParking){
                        zurBase = true;
                        if (debug){
                            System.out.println("Zur Base: "+zurBase);
                            System.out.println("zielErreicht: "+zielErreicht);
                        }                        
                        fliegeZurBase();
                    }
                }
            }
        }
                
        /*Solange die X und Z Position nicht erreicht ist, soll die Drohne aufsteigen bis sie die angegebene FlughÃ¶he erreicht hat.
         Dann werden X und Z position Ã¼berprÃ¼ft und die Drohne fliegt in die jeweilige Richtung.*/
        if (!xErreicht | !zErreicht){ 
            if (debug){
                System.out.println("X-Pos: "+pos.x);
                System.out.println("Z-Pos: "+pos.z);
                System.out.println("Hoehe: "+pos.y);                
            }
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
        
        /*Wenn der Wegpunkt erreicht wurde und die Drohne Ã¼ber dem Boden steht soll diese Landen.*/
        if (xErreicht && zErreicht && pos.y > bodenhoehe && !droneParking){
            lande();
        }
    }    
    /*Die Routenliste wird auf ihre LÃ¤nge Ã¼berprÃ¼ft.
     Dann wird nacheinander ermittelt welcher Wegpunkt noch nicht erreicht wurde.
     Der nÃ¤chste unerreichte Punkt wird als aktuelles Ziel markiert.*/
    public void naechsterWP(){
        if(routenlaenge>0){
        for(int i =0; i < routenlaenge; i++){
            if (debug){
                System.out.println("RoutenlÃ¤nge: "+routenlaenge);
            }
            if (!flugroute[i].getErreicht()){
                aktZiel = flugroute[i];
                destination.setLocalTranslation(aktZiel.x,0.2f,aktZiel.z);
                //System.out.println("Aktueller WP: "+i+".   Erreicht: "+flugroute[i].getErreicht());
                break;
            }
        }
        }
    }
    //Landet die Drohne
    public void lande(){
        drone.move(0, -flugGeschw, 0);
        if (debug){
            System.out.println("Lande...");            
        }
    }
    //LÃ¤sst die Drohne zur Parkstation fliegen
    public void fliegeZurBase (){
        //System.out.println("zielErreicht: "+zielErreicht);
        if(zielErreicht){
            leereFlugroute();
            routenlaenge = 1;
            zielErreicht = false;
            zurBase = true;
        }
    }
    
    /** Tastenzuweisung. */
    private void initKeys() {
        // DrÃ¼cken der Leertaste zeigt an, dass ein Fahrzeug angekommen ist
        inputManager.addMapping("Gast",  new KeyTrigger(KeyInput.KEY_SPACE));
        //DrÃ¼cken von "F9" schaltet den Editier Modus zur Parkplatzverwaltung ein/aus. Dieser Modus schaltet die Simulation aus.
        inputManager.addMapping("Edit",  new KeyTrigger(KeyInput.KEY_F9));
        //DrÃ¼cken von "F1" schaltet den Debug Modus fÃ¼r volle Informationsdarstellung ein/aus.
        inputManager.addMapping("Debug",  new KeyTrigger(KeyInput.KEY_F1));
        //Mausklick fÃ¼r die Interaktion
        inputManager.addMapping("Auswahl",  new MouseButtonTrigger(MouseInput.BUTTON_LEFT));

        // HinzufÃ¼gen des Tastendrucks zum inputManager
        inputManager.addListener(actionListener, new String[]{"Gast"});
        inputManager.addListener(actionListener, new String[]{"Debug"});
        inputManager.addListener(actionListener, new String[]{"Edit"});
        inputManager.addListener(analogListener, new String[]{"Auswahl"});
    }
    //Hier werden die Tastatureingaben erfasst und umgesetzt.
    private ActionListener actionListener = new ActionListener() {
        public void onAction(String name, boolean keyPressed, float tpf) {
            //Wenn die Leertatste gedrÃ¼ckt wurde, wartet ein Gaste an der Einfachrt.
            if (name.equals("Gast") && !keyPressed) {
                if(!editMode){
                    if(!autoWartet){
                        rootNode.attachChild(Auto);
                        setzeStartWP(einfahrt);
                        destination.move(posX, posY, posZ);
                        autoWartet = true;
                        zielErreicht = false;
                        droneParking = false;
                        if (debug){
                            System.out.println("Gast Wartet!");
                        }
                    }
                    else{ 
                        rootNode.detachChild(Auto);
                    }
                }
            }
            //Umschalten in und aus dem Editier Modus (F9)
            if (name.equals("Edit") && !keyPressed) {
                int edit_mode = 0;
                if (editMode){
                    edit_mode = 1;
                }
                if(!editMode){
                    edit_mode = 0;
                }
                switch (edit_mode){
                    case 0:
                        editMode =true;
                        simulationMode = false;
                        camEdit();
                        rootNode.detachChild(drone);
                        if (debug){
                            System.out.println("Editier Modus aktiv...");
                        }                        
                        break;
                    case 1:
                        editMode =false;
                        simulationMode = true;
                        camSimulation();
                        rootNode.attachChild(drone);
                        if (debug){
                            System.out.println("Editier Modus ausgeschaltet.");
                        }                        
                        break;
                    default:
                        System.out.println("Editier Modus nicht vorhanden!"); 
                }             
            }
            //Umschalten in und aus dem Debug Modus (F1)
            if (name.equals("Debug") && !keyPressed) {
                int d_mode = 0;
                if (debug){
                    d_mode = 1;
                }
                if(!debug){
                    d_mode = 0;
                }
                switch (d_mode){
                    case 0:
                        debug =true;                        
                        System.out.println("Debug Modus aktiv...");
                        break;
                    case 1:
                        debug = false;
                        System.out.println("Debug Modus ausgeschaltet.");
                        break;
                    default:
                        System.out.println("Debug Modus nicht vorhanden!"); 
                }             
            }
        }
    };
    //Hier werden die Mausklicks erfasst und umgesetzt
    private AnalogListener analogListener = new AnalogListener() {
    public void onAnalog(String name, float intensity, float tpf) {
        //Wenn man im Editiermodus ist...
        if(editMode){
            //Wenn "Auswahl" per Mausklick aktiviert wurde...
            if (name.equals("Auswahl")) {
                switch(parkplatzModus){
                    case 0:
                        //mousePick() wird ausgefÃ¼hrt
                        mousePick();
                        //Der angewÃ¤hlte Parkplatz wird markiert.
                        selectParking();
                        break;
                    case 1:
                        getParkingStartpoint();
                        break;
                    case 2:
                        break;
                    default:
                        System.out.println("Woops! Da ist etwas schiefgelaufen.");
                }
            }// else if ...          
        }            
    }
  };
    //Diese Methode ist fÃ¼r die Objektauswahl mit der Maus zustÃ¤ndig.
    private void mousePick(){
        if (debug){
            System.out.println("Klick!");
        }
        // Liste mit den Resultaten zurÃ¼cksetzen.
        results = new CollisionResults();
        // Kovertiert die 2D-Position des Klicks in den 3D-Raum
        Vector2f click2d = inputManager.getCursorPosition();
        Vector3f click3d = cam.getWorldCoordinates(new Vector2f(click2d.x, click2d.y), 0f).clone();
        Vector3f dir = cam.getWorldCoordinates(new Vector2f(click2d.x, click2d.y), 1f).subtractLocal(click3d).normalizeLocal();
        // SchieÃŸt einen Strahl von der angeklickten Position ab.
        Ray ray = new Ray(click3d, dir);
        // Sammeln aller kollisionen zwischen dem Strahl und den Objekten in einer Liste.
        rootNode.collideWith(ray, results);
        for (int i = 0; i < results.size(); i++) {
            // FÃ¼r jeden â€œtrefferâ€�, bekommen wir die Distanz, den getroffenen Punkt sowie den Namen des Objekts
            float dist = results.getCollision(i).getDistance();
            Vector3f pt = results.getCollision(i).getContactPoint();
            String target = results.getCollision(i).getGeometry().getName();
            if (debug){
                // Ausgabe der vom Strahl getroffenen Objekte.
                System.out.println("Selection #" + i + ": " + target + " at " + pt + ", " + dist + " WU away.");
            }
        }
    }
    private void getParkingStartpoint(){
        //Ueberpruefung, ob nicht bereits ein Parkplatz auf dieser Stelle Liegt.
        //Der angeklickte Punkt wird ermittelt.
        //Ein Marker wird gesetzt und die interaktive Flaechenanzeige wird gestartet.
    }
    
    //Hier werden alle Variablen zurueckgesetzt auf ihre Initialisierungswerte
    private void resetVariables(){
        xErreicht = false;
        zErreicht = false;
        zielErreicht = false;
        droneParking = true;
        autoWartet = false;
        abholen = false;
        droneBusy = false;
        if (debug){
            System.out.println("Variablen zurÃ¼ckgesetzt.");
        }
    }
    private void selectParking (){
        // Wenn es Resultate gibt, dann...
        if (results.size() > 0) {
            // Es wird das erste Objekt ausgewÃ¤hlt, das den Strahl berÃ¼hrt hat.
            Geometry target = results.getClosestCollision().getGeometry();
            // Wenn es sich dabei um eine "Box" handelt, wird diese mit einem roten Material belegt und rotiert.
            if (target.getName().equals("Parkplatz")) {
              target.setMaterial(unsichtbar);
            }                   
        }
    }
    
    @Override
    public void simpleInitApp() {        
        //Tastenbelegung laden
        initKeys();
        
        //Grundeinstellung der Modi
        debug = false;
        simulationMode = true;
        editMode = false;
        parkplatzModus = 0;
        
        //Ziel Position 
        posX = 0;
        posY = 0.2f;
        posZ = 0;
        flughoehe = 2;
        bodenhoehe = 0.3f;
        flugGeschw = 0.05f;
        toleranz = 0.2f+flugGeschw;
        //pr = new TrianglePickResults();
        
        //Waypoints einrichten
        parkStation = new Waypoint (0,0,4);
        einfahrt = new Waypoint (1, -2, 4);
        wp1 = new Waypoint(-2,-12,2);
        wp2 = new Waypoint(-18,-12,2);
        wp3 = new Waypoint(-18,-2,2);
        
        xErreicht = false;
        zErreicht = false;
        zielErreicht = false;
        droneParking = true;
        autoWartet = false;
        abholen = false;
        droneBusy = false;
        
        //Background
        rootNode.attachChild(SkyFactory.createSky(assetManager, "Textures/umgebung.dds", false).scale(0.001f));
        
        //Das Grundobjekt: Parkplatz
        Spatial parkplatz = assetManager.loadModel("Models/parkplatzNeu_2.j3o");
        parkplatz.scale(0.04f);
        parkplatz.rotate(0.0f, -3.14f, 0.0f);
        parkplatz.setLocalTranslation(-17f, 0, -10f);
        parkplatz.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
        rootNode.attachChild(parkplatz);        
        Material mat_parkplatz = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        mat_parkplatz.setBoolean("UseMaterialColors",true);    
        mat_parkplatz.setColor("Diffuse",ColorRGBA.White);
        mat_parkplatz.setColor("Specular",ColorRGBA.White);
        mat_parkplatz.setFloat("Shininess", 100f); 
        parkplatz.setMaterial(mat_parkplatz); 

        //Auto
            Auto = assetManager.loadModel("Models/Auto-modelblend.obj");
            Auto.setLocalScale(0.3f);
            Auto.setLocalTranslation(15f,.0f ,-2.5f);
          
        
        //Auswahlflaechen
        for(int i=0; i>-18; i=i-2){
            for (int j=0; j>-11;j--){
                Flaeche auswahl = new Flaeche(assetManager, rootNode, i, 0, j, 1);
            }
        }
        //Material des AusgewÃ¤hlten Objekts
        mat_auswahl = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        mat_auswahl.setBoolean("UseMaterialColors",true);    
        mat_auswahl.setColor("Diffuse",ColorRGBA.Red);
        mat_auswahl.setColor("Specular",ColorRGBA.White);
        mat_auswahl.setFloat("Shininess", 100f); 
        
        // Aktuelles Ziel       
        zielObj = new Box(Vector3f.ZERO, 0.1f, 0.1f, 0.1f);
        destination = new Geometry("Box", zielObj);              
        Material mat_target = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        mat_target.setBoolean("UseMaterialColors",true);    
        mat_target.setColor("Diffuse",ColorRGBA.Yellow);
        mat_target.setColor("Specular",ColorRGBA.White);
        mat_target.setFloat("Shininess", 12f);  // [0,128]
        destination.setMaterial(mat_target);
        destination.setLocalTranslation(posX, posY, posZ);
        destination.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
        rootNode.attachChild(destination);
        
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
        rootNode.attachChild(drone);               
       
        //Unsichtbares Material
        unsichtbar = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        unsichtbar.setColor("Color", new ColorRGBA(0.7f,0.7f,0.7f,10f));
        unsichtbar.setTransparent(true);
        
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
        //Initiiert die Kamera im Simulations-Modus
        camSimulation();
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
    }
    //Diese Methode setzt die Kameraeinstellungen fÃ¼r den Simulationsmodus
    private void camSimulation(){
        // Kamera einstellungen
        inputManager.setCursorVisible(true);  //Macht den Mauszeiger sichtbar.
        flyCam.setDragToRotate(true);       //Ziehen mit gedrÃ¼ckter Maustaste um die Kamera zu drehen
        flyCam.setMoveSpeed(50);            //Geschwindigkeit der Kamerabewegung per Tastatur
        flyCam.setRotationSpeed(10);        //Geschwindigkeit der rotationsbewegung per Tastatur
        flyCam.setEnabled(false);           // Kamera einfrieren
        final float ar = (float) this.settings.getWidth() / (float) this.settings.getHeight();
        //diverse Positions und Ausrichtungseinstellungen
        cam.setFrustumPerspective(75, ar, 0.1f, 1000.0f);
        cam.setLocation(new Vector3f(-14, 14, -26));
        cam.lookAt(new Vector3f(-14, 0, -8), Vector3f.UNIT_Y);
    }
    //Diese Methode setzt die Kameraeinstellungen fÃ¼r den Editiermodus
    private void camEdit(){
        // Kamera einstellungen
        inputManager.setCursorVisible(true);    //Macht den Mauszeiger sichtbar.
        flyCam.setDragToRotate(true);          //Ziehen mit gedrÃ¼ckter Maustaste um die Kamera zu drehen
        flyCam.setMoveSpeed(50);                //Geschwindigkeit der Kamerabewegung per Tastatur
        flyCam.setRotationSpeed(10);            //Geschwindigkeit der rotationsbewegung per Tastatur
        flyCam.setEnabled(true);                // Kamera einfrieren
        final float ar = (float) this.settings.getWidth() / (float) this.settings.getHeight();
        //diverse Positions und Ausrichtungseinstellungen
        cam.setFrustumPerspective(85, ar, 0.1f, 1000.0f);
        cam.setLocation(new Vector3f(-14, 18, -12));
        cam.lookAt(new Vector3f(cam.getWorldCoordinates(Vector2f.ZERO, posZ).getX(), 0, cam.getWorldCoordinates(Vector2f.ZERO, posZ).getZ()), Vector3f.UNIT_Z);
    }
    
    @Override
    public void simpleUpdate(float tpf) {
        // Wenn man im simulations-Modus ist wir folgendes stÃ¤ndig aktualisiert:
        if (!editMode){
            //Wenn ein Auto an der Einfahrt wartet...
            if(autoWartet){
                //berechneFlugroute();
                //Wenn die Drohne noch kein Fahrzeug abholt...
                if(!abholen){
                    leereFlugroute();           //Flugroutenspeicher leeren
                    erstelleFlugroute();        //Die benÃ¶tigte Flugroute berechnen
                    naechsterWP();              //Zum nÃ¤chsten Waypoint fliegen
                    if (droneParking){
                        droneParking = false;   //Dem System mitteilen, dass die Drohne nicht mehr parkt
                    }
                    if (!abholen){
                        abholen = true;         //Dem System mitteilen, dass die Drohne ein Fahreug abholt
                    }                        
                }
            }
            //Wenn die Drohne gerade ein Fahrzeug abholt...
            if(abholen){
                zurBase = false;            //Dem System mitteilen, dass die Drohne nicht zur Base fliegt
                fliegeZuWP(aktZiel);        //Den aktuellen Wegpunkt anfliegen
            }
            //Wenn die Drohne im Parkmodus ist, soll nichts passieren
            if(droneParking){
            }
            //Wenn die Drohne zur Base fliegt...
            if (zurBase){
                //System.out.println("Fliege zur Base!");
                abholen = false;            //Dem System mitteilen, dass die Drohne kein Fahrzeug begleitet
                fliegeZuWP(parkStation);    //Zur Parkstation fliegen
            }
        }
        // Wenn man im Editier-Modus ist dann wrd folgendes stÃ¤ndig aktualisiert:
        else {
            
        }
        //Auf Benutzereingaben achten
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