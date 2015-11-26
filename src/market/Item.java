/*
* To change this license header, choose License Headers in Project Properties.
* To change this template file, choose Tools | Templates
* and open the template in the editor.
*/
package market;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author David
 */
public class Item implements Serializable {
    private String name;
    private float price;
    private long id;
    private String owner;
    
    public Item(String name, float price, String owner){
        this.name=name;
        this.price=price;
        this.owner=owner;
    }
    
    public void setName(String name){
        this.name=name;
    }
    
    public String getName(){
        return name;
    }
    public void setPrice(float price){
        this.price=price;
    }
    
    public float getPrice(){
        return price;
    }
    
    public void setId(long id){
        this.id=id;
    }
    
    public long getId(){
        return id;
    }
    
    public void setOwner(String owner){
        this.owner=owner;
    }
    
    public String getOwner(){
        return owner;
    }
    
    @Override
    public String toString(){
        return name+", "+Double.toString(price)+", "+owner;
//        return name+", "+Double.toString(price)+", UNKNOWN";
    }
}
