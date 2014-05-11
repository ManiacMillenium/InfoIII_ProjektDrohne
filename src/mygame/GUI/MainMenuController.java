package mygame.GUI;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.effects.EffectEventId;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.screen.ScreenController;
import de.lessvoid.nifty.tools.SizeValue;

public class MainMenuController extends AbstractAppState implements ScreenController {
    
    private Application app;
    private AppStateManager stateManager;
    private Nifty nifty;
    private Screen screen;
    private Element popUpPanel;
    private Element pfeilPanel;
    private Element display;
    
    public MainMenuController() {
  
    }
  
    public void bind(Nifty nifty, Screen screen) {
        this.nifty = nifty;
        this.screen = screen;
        popUpPanel = screen.findElementByName("popUp");
        pfeilPanel = screen.findElementByName("pfeilPanel");
        display = screen.findElementByName("display");
    }
  
    public void onStartScreen() {
        
    }

    public void onEndScreen() {
        
    }
    
    public void onPfeilClicked() {  
        int y = popUpPanel.getY();
        int yOffset = 580;
        if (y == 661) {       
            //Popup
            popUpPanel.setConstraintY(new SizeValue(""+ (int)(popUpPanel.getY()-yOffset))); 
            popUpPanel.startEffect(EffectEventId.onCustom);
            popUpPanel.getParent().layoutElements();
            
            //Pfeil 
            pfeilPanel.setConstraintY(new SizeValue(""+ (int)(pfeilPanel.getY()-yOffset))); 
            pfeilPanel.startEffect(EffectEventId.onCustom);
            pfeilPanel.getParent().layoutElements();          
        }        
        if (y == 81) {          
           //Popup
            popUpPanel.setConstraintY(new SizeValue(""+ (int)(popUpPanel.getY()+yOffset))); 
            popUpPanel.startEffect(EffectEventId.onCustom);
            popUpPanel.getParent().layoutElements();
            
            //Pfeil 
            pfeilPanel.setConstraintY(new SizeValue(""+ (int)(pfeilPanel.getY()+yOffset))); 
            pfeilPanel.startEffect(EffectEventId.onCustom);
            pfeilPanel.getParent().layoutElements();            
        }
    }
    
    public void onNeuesFahrzeug() {
        System.out.println("Chopex");       
    }
      
  @Override
    public void initialize(AppStateManager stateManager, Application app) {
        this.app = app;
        this.stateManager = stateManager;
    } 
}