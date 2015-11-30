/*
* To change this license header, choose License Headers in Project Properties.
* To change this template file, choose Tools | Templates
* and open the template in the editor.
*/
package market;

import bank.Account;
import java.math.BigInteger;
import views.ClientPanel;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 *
 * @author David
 */
public class UserImpl extends UnicastRemoteObject implements User {
    
    private String name;
    private String password;
    private Account account;
    private String bankName;
    
    public UserImpl(String name, String password, Account account, String bankName) throws RemoteException, Exception{
        this(name, password, bankName);
        this.account=account;
    }
    
    public UserImpl(String name, String password, String bankName) throws RemoteException, Exception{
        this.name=name;
        try {
            this.password=Handler.generateStorngPasswordHash(password);
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(UserImpl.class.getName()).log(Level.SEVERE, null, ex);
            throw new Exception();
        } catch (InvalidKeySpecException ex) {
            Logger.getLogger(UserImpl.class.getName()).log(Level.SEVERE, null, ex);
            throw new Exception();
        }
        this.bankName=bankName;
    }
    
    @Override
    public String toString(){
        return name;
    }
    
    @Override
    public void itemSold(Item item) throws RemoteException {
        String s="Your item ("+item.toString()+") has been sold!";
        System.out.println(s);
        ClientPanel.clientPanel.statusChanged(s);
    }
    
    @Override
    public void wishAvaible(Item item) throws RemoteException {
        String s="Your wish-item ("+item.toString()+") is now avaible!";
        System.out.println(s);
        ClientPanel.clientPanel.statusChanged(s);
    }
    
    @Override
    public String getName() throws RemoteException {
        return name;
    }
    
    @Override
    public String getPassword() throws RemoteException {
        return password;
    }
    
    @Override
    public Account getBankAccount() throws RemoteException {
        return account;
    }
    
    @Override
    public void setBankAccount(Account account){
        this.account=account;
    }
    
    @Override
    public String getBankName() throws RemoteException {
        return bankName;
    }
    
    @Override
    public void createFromUser(User user){
        try {
            this.name=user.getName();
            this.password=user.getPassword();
            this.account=user.getBankAccount();
            this.bankName=user.getBankName();
        } catch (RemoteException ex) {
            Logger.getLogger(UserImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
