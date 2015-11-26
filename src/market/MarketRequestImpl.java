/*
* To change this license header, choose License Headers in Project Properties.
* To change this template file, choose Tools | Templates
* and open the template in the editor.
*/
package market;

import bank.Account;
import bank.RejectedException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author David
 */
public class MarketRequestImpl extends UnicastRemoteObject implements MarketRequest{
    
    private String datasource="mydb";
    private Connection conn;
    private Statement statement;
    private PreparedStatement insertItemStatement;
    private PreparedStatement findItemStatement;
    private PreparedStatement deleteItemStatement;
    private PreparedStatement insertUserStatement;
    private PreparedStatement findUserStatement;
    private PreparedStatement deleteUserStatement;
    
    // The key is the item-ID
    private final Map<Long, Item> uploadedItems=new HashMap<>();
    private final Map<String, List<Item>> wishedItems=new HashMap<>();
    private final Map<String, User> registeredUsers=new HashMap<>();
    
    public MarketRequestImpl() throws RemoteException
    {
        try {
            connect();
        } catch (SQLException ex) {
            Logger.getLogger(MarketRequestImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void connect() throws SQLException{
        conn=DriverManager.getConnection("jdbc:derby://localhost:1527/" + datasource + ";create=true");
        statement=conn.createStatement();
        createTable();
    }
    
    private void createTable() throws SQLException{
        statement.executeUpdate("CREATE TABLE ITEM (name VARCHAR(64) NOT NULL, price FLOAT NOT NULL, id BIGINT PRIMARY KEY, owner VARCHAR(64) NOT NULL)");
        statement.executeUpdate("CREATE TABLE USER (name VARCHAR(64) PRIMARY KEY, password VARCHAR(256) NOT NULL)");
        statement.executeUpdate("CREATE TABLE PURCHASE (buyer VARCHAR(64) NOT NULL, seller VARCHAR(64) NOT NULL, price FLOAT, time TIMESTAMP NOT NULL)");
        // Item statements
        insertItemStatement=conn.prepareStatement("INSERT INTO ITEM (name, price, id, owner) VALUES (?, ?, ?, ?)");
        findItemStatement=conn.prepareStatement("SELECT (name, price, id, owner) FROM ITEM WHERE id=?");
        deleteItemStatement=conn.prepareStatement("DELETE FROM ITEM WHERE id=?");
        // User statements
        insertUserStatement=conn.prepareStatement("INSERT INTO USER (name, password) VALUES (?, ?)");
        findUserStatement=conn.prepareStatement("SELECT (name, password) FROM USER WHERE name=?");
        deleteUserStatement=conn.prepareStatement("DELETE FROM USER WHERE name=?");
        // Sales statements
    }
    
    @Override
    public Message SellItem(Item item) throws RemoteException {
        System.out.println("sell item. "+item.toString());
        String message="error uploading item";
        synchronized(uploadedItems){
            long id;
            // Find a unique ID (i.e. an ID thats not present in the uploadedItems collection (Hashmap)
            do {
                id=Handler.getRandomLong();
            }
            while(uploadedItems.containsKey(id));
            // Set item ID
            item.setId(id);
            uploadedItems.put(item.getId(), item);
            System.out.println("Upload items length = "+uploadedItems.size());
            message="Upload of ("+item.toString()+") successful";
            
            synchronized(wishedItems){
                System.out.println(item.getName());
                // Get all wished items and iterate through them
                List<Item> li=wishedItems.get(item.getName());
                if(li!=null){
                    for(Item wishedItem:li){
                        // If a wished item has the same name and a equal or higher price than the currently uploaded item => send notification
                        // to the owner of the wish-item
                        if(item.getName().equals(wishedItem.getName()) && item.getPrice()<=wishedItem.getPrice()){
                            User owner=registeredUsers.get(wishedItem.getOwner());
                            if(owner!=null)
                                owner.wishAvaible(item);
                            
                        }
                    }
                }
            }
        }
        Message msg=new Message();
        msg.message=message;
        return msg;
    }
    
    @Override
    public Message ListItems() throws RemoteException {
        System.out.println("list items.");
        synchronized(uploadedItems){
            System.out.println("Upload items length = "+uploadedItems.size());
            Message msg=new Message();
            msg.obj=new ArrayList<>(uploadedItems.values());
            msg.message="Get items successful";
            return msg;
        }
    }
    
    @Override
    public Message BuyItem(long itemId, User buyer) throws RemoteException {
        String message="Unspecified error occured when buying item. No item bought.";
        synchronized(uploadedItems){
            Item item=uploadedItems.get(itemId);
            System.out.println("buy item. "+item.toString());
            if(item!=null){
                System.out.println("buy item. item = "+item.toString()+", buyer = "+buyer.toString());
                
                User owner=registeredUsers.get(item.getOwner());
                if(owner!=null){
                    Account sellerAccount=owner.getBankAccount();
                    Account buyerAccount=buyer.getBankAccount();
                    boolean successful=false;
                    try {
                        buyerAccount.withdraw(item.getPrice());
                        successful=true;
                    } catch (RejectedException ex) {
                        message="Could not withdraw money.";
                        successful=false;
                    }
                    if(successful){
                        try {
                            sellerAccount.deposit(item.getPrice());
                        } catch (RejectedException ex) {
                            message="Couldn't deposit money.";
                        }
                        message="Transaction successful.";
                        uploadedItems.remove(itemId);
                        owner.itemSold(item);
                    }
                }
            }else{
                message="Item not found.";
            }
        }
        Message msg=new Message();
        msg.message=message;
        return msg;
    }
    
    @Override
    public Message AddWish(Item item) throws RemoteException {
        System.out.println("add item. "+item.toString());
        String message="Error adding wish.";
        synchronized(registeredUsers){
            User owner=registeredUsers.get(item.getOwner());
            if(owner!=null){
                User user=registeredUsers.get(owner.getName());
                if(user!=null){
                    synchronized(wishedItems){
                        List<Item> list=wishedItems.get(item.getName());
                        if(list==null){
                            list=new ArrayList<Item>();
                            wishedItems.put(item.getName(), list);
                        }
                        list.add(item);
                        message="Wish added successfully";
                    }
                }else{
                    message="User not recognized";
                }
            }
        }
        Message msg=new Message();
        msg.message=message;
        return msg;
    }
    
    @Override
    public Message Register(User owner) throws RemoteException {
        System.out.println("register. "+owner.getName());
        String message="Error registering user";
        synchronized(registeredUsers){
            if(!registeredUsers.containsKey(owner.getName())){
                message="Registration successful";
            }else{
                message="Log in successful";
            }
            registeredUsers.put(owner.getName(), owner);
        }
        Message msg=new Message();
        msg.message=message;
        return msg;
    }
    
    @Override
    public Message Unregister(User owner) throws RemoteException {
        System.out.println("unregister. "+owner.getName());
        String message="Error unregistering user";
        synchronized(registeredUsers){
            registeredUsers.remove(owner.getName());
            List<Item> c=new ArrayList<>();
            // Remove items the owner has uploaded
            for(Item item:uploadedItems.values()){
                if(item.getOwner().equals(owner.getName()))
                    c.add(item);
            }
            uploadedItems.values().removeAll(c);
            message="Unregistration successful";
        }
        Message msg=new Message();
        msg.message=message;
        return msg;
    }
    
    @Override
    public Message LogIn(String name) throws RemoteException {
        Message msg=new Message();
        msg.obj=registeredUsers.get(name);
        return msg;
    }
    
}
