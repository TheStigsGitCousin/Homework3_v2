/*
* To change this license header, choose License Headers in Project Properties.
* To change this template file, choose Tools | Templates
* and open the template in the editor.
*/
package market;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 *
 * @author David
 */
public interface MarketRequest extends Remote {
    
    public Message SellItem(Item item) throws RemoteException;
    public Message ListItems() throws RemoteException;
    public Message BuyItem(long itemId, User owner) throws RemoteException;
    public Message AddWish(Item item) throws RemoteException;
    public Message Register(User owner) throws RemoteException;
    public Message Unregister(User owner) throws RemoteException;
    public Message LogIn(User loadedUser, String name, String password) throws RemoteException;
}
