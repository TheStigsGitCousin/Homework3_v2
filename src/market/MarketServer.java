/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package market;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;

/**
 *
 * @author David
 */
public class MarketServer {
    
    private static final String USAGE = "java bankrmi.Server <bank_rmi_url>";
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {
        // TODO code application logic here
        MarketRequest marketRequest=new MarketRequestImpl();
        try {
            LocateRegistry.getRegistry(1099).list();
        } catch (RemoteException e) {
            LocateRegistry.createRegistry(1099);
        }
        Naming.rebind("MarketRequest", marketRequest);
        System.out.println("Binding complete…\n");
    }
    
}
