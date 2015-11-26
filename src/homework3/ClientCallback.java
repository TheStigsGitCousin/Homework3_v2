/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package homework3;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 *
 * @author David
 */
public interface ClientCallback extends Remote {
    public void itemSold() throws RemoteException;
    public void wishAvaible() throws RemoteException;
}
