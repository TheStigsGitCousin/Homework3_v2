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
    
    public Message sellItem(long token, Item item) throws RemoteException;
    public Message ListItems() throws RemoteException;
    public Message buyItem(long token, long itemId, User owner) throws RemoteException;
    public Message addWish(long token, Item item) throws RemoteException;
    public Message getUserActivity(long token, User user) throws RemoteException;
    public Message register(User owner) throws RemoteException;
    public Message unregister(User owner) throws RemoteException;
    public Message logIn(User loadedUser, String name, String password) throws RemoteException;
    public Message logOut(long token, User user) throws RemoteException;
}
