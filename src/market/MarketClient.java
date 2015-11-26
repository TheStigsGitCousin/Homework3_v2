/*
* To change this license header, choose License Headers in Project Properties.
* To change this template file, choose Tools | Templates
* and open the template in the editor.
*/
package market;

import views.ClientPanel;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;

/**
 *
 * @author David
 */
public class MarketClient {
    
    public MarketClient(String bankName) throws RemoteException{
//        Owner ownerObject=new OwnerImpl();
        // Register the newly created object at rmiregistry.
        try {
            LocateRegistry.getRegistry(1099).list();
        } catch (RemoteException e) {
            LocateRegistry.createRegistry(1099);
        }
//        try {
//            Naming.rebind(clientName, ownerObject);
//        } catch (MalformedURLException ex) {
//            Logger.getLogger(MarketClient.class.getName()).log(Level.SEVERE, null, ex);
//        }
        JFrame frame=new JFrame("ClientApplication");
        ClientPanel clientPanel;
        if(bankName==null)
            clientPanel=new ClientPanel();
        else
            clientPanel=new ClientPanel(bankName);
        
        frame.setContentPane(clientPanel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(700, 300);
        frame.setVisible(true);
    }
    
    public static void main(String[] args) throws RemoteException {
        if(args.length!=1)
            new MarketClient(null);
        else
            new MarketClient(args[0]);
    }
}

//http://stackoverflow.com/questions/774828/rmi-how-does-passing-a-remote-object-through-a-remote-method-work
//https://docs.oracle.com/javase/8/docs/platform/rmi/spec/rmi-objmodel7.html
//http://stackoverflow.com/questions/8330295/difference-between-java-rmi-naming-and-java-rmi-registry-locateregistry