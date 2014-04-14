/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mygame;

/**
 *
 * @author Maniac
 */
public class Waypoint {
    float x,z,flughoehe;
    boolean erreicht = false;
    
    public Waypoint(float koordX, float koordZ,float hoehe){
        x = koordX;
        z = koordZ;
        flughoehe = hoehe;
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
