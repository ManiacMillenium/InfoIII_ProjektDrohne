package mygame;

import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;

/**
 *
 * @author Maniac
 */
public class Waypoint {
    float x,z,flughoehe;
    boolean erreicht = false;
    int wpID;
    Box zielObj;
    Geometry target;
    
    public Waypoint(float koordX, float koordZ,float hoehe){
        x = koordX;
        z = koordZ;
        flughoehe = hoehe;
        wpID = 0;      
    }
    
    public int getID(){
        return wpID;
    }
    
    public void setID(int id){
        wpID = id;
    }
    
    public float getX(){
        return x;
    }
    public float getZ(){
        return z;
    }
    public float getHeight(){
        return flughoehe;
    }
    public boolean getErreicht(){
        return erreicht;
    }
    
    public void setErreicht(){
        erreicht = true;
    }
    public void setNichtErreicht(){
        erreicht = false;
    }
}
