/*
* To change this license header, choose License Headers in Project Properties.
* To change this template file, choose Tools | Templates
* and open the template in the editor.
*/
package market;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

/**
 *
 * @author David
 */
public class ClientCallbackImpl extends UnicastRemoteObject implements ClientCallback {
    
    public ClientCallbackImpl() throws RemoteException{}
    
    @Override
    public void itemSold() throws RemoteException {
        System.out.println("Sold!");
    }
    
    @Override
    public void wishAvaible() throws RemoteException {
        System.out.println("Avaible!");
    }
    
}
