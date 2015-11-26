/*
* To change this license header, choose License Headers in Project Properties.
* To change this template file, choose Tools | Templates
* and open the template in the editor.
*/
package homework3;

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
public class OwnerImpl extends UnicastRemoteObject implements Owner {
    
    private String name;
    private String password;
    private Account account;
    
    public OwnerImpl(String name, String password, Account account) throws RemoteException, Exception{
        this.name=name;
        try {
            this.password=generateStorngPasswordHash(password);
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(OwnerImpl.class.getName()).log(Level.SEVERE, null, ex);
            throw new Exception();
        } catch (InvalidKeySpecException ex) {
            Logger.getLogger(OwnerImpl.class.getName()).log(Level.SEVERE, null, ex);
            throw new Exception();
        }
        this.account=account;
    }
    
    private static String generateStorngPasswordHash(String password) throws NoSuchAlgorithmException, InvalidKeySpecException
    {
        int iterations = 1000;
        char[] chars = password.toCharArray();
        byte[] salt = getSalt().getBytes();
         
        PBEKeySpec spec = new PBEKeySpec(chars, salt, iterations, 64 * 8);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        byte[] hash = skf.generateSecret(spec).getEncoded();
        return iterations + ":" + toHex(salt) + ":" + toHex(hash);
    }
     
    private static String getSalt() throws NoSuchAlgorithmException
    {
        SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
        byte[] salt = new byte[16];
        sr.nextBytes(salt);
        return salt.toString();
    }
     
    private static String toHex(byte[] array) throws NoSuchAlgorithmException
    {
        BigInteger bi = new BigInteger(1, array);
        String hex = bi.toString(16);
        int paddingLength = (array.length * 2) - hex.length();
        if(paddingLength > 0)
        {
            return String.format("%0"  +paddingLength + "d", 0) + hex;
        }else{
            return hex;
        }
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
        return name;
    }
    
    @Override
    public Account getBankAccount() throws RemoteException {
        return account;
    }
    
}
