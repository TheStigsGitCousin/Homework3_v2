/*
* To change this license header, choose License Headers in Project Properties.
* To change this template file, choose Tools | Templates
* and open the template in the editor.
*/
package market;

import bank.Account;
import bank.AccountImpl;
import bank.Bank;
import bank.RejectedException;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import views.ClientPanel;

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
    private PreparedStatement findAllItemStatement;
    private PreparedStatement deleteItemStatement;
    
    private PreparedStatement insertUserStatement;
    private PreparedStatement findUserStatement;
    private PreparedStatement deleteUserStatement;
    
    private PreparedStatement insertPurchaseStatement;
    private PreparedStatement findBoughtPurchaseStatement;
    private PreparedStatement findSoldPurchaseStatement;
    private PreparedStatement findTotalBoughtAndSoldStatement;
//    private PreparedStatement deletePurchaseStatement;
    
    private Bank bankobj;
    
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
//        try {
//            bankobj = (Bank) Naming.lookup("");
//        } catch (NotBoundException ex) {
//            Logger.getLogger(ClientPanel.class.getName()).log(Level.SEVERE, null, ex);
//            if(bankobj==null){
////                statusChanged("The bank wasn't found");
//            }
//        } catch (MalformedURLException ex) {
//            Logger.getLogger(MarketRequestImpl.class.getName()).log(Level.SEVERE, null, ex);
//        }
    }
    
    private void restoreState(){
        try {
            ResultSet result=findAllItemStatement.executeQuery();
            while(result.next()){
                Item item=new Item(result.getString(1), result.getFloat(2), result.getLong(3), result.getString(4));
                uploadedItems.put(item.getId(),item);
            }
            
            System.out.println("DB state restored!");
        } catch (SQLException ex) {
            Logger.getLogger(MarketRequestImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    
    private void connect() throws SQLException{
        conn=DriverManager.getConnection("jdbc:derby://localhost:1527/" + datasource + ";create=true");
        statement=conn.createStatement();
        createTable();
        
        restoreState();
    }
    
    private void createTable() {
        int ro;
        try {
            ro=statement.executeUpdate("CREATE TABLE ITEM (name VARCHAR(64) NOT NULL, price FLOAT NOT NULL, id BIGINT PRIMARY KEY, owner VARCHAR(64) NOT NULL)");
            System.out.println("ITEM CREATION changed "+ro+" rows");
        } catch (SQLException ex) {
            if(!ex.getSQLState().equals("X0Y32"))
                Logger.getLogger(MarketRequestImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            ro=statement.executeUpdate("CREATE TABLE OWNER (name VARCHAR(64) PRIMARY KEY, password VARCHAR(256) NOT NULL)");
            System.out.println("OWNER CREATION changed "+ro+" rows");
        } catch (SQLException ex) {
            if(!ex.getSQLState().equals("X0Y32"))
                Logger.getLogger(MarketRequestImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            ro=statement.executeUpdate("CREATE TABLE PURCHASE (buyer VARCHAR(64) NOT NULL, seller VARCHAR(64) NOT NULL, price FLOAT)");
            System.out.println("PURCHASE CREATION changed "+ro+" rows");
        } catch (SQLException ex) {
            if(!ex.getSQLState().equals("X0Y32"))
                Logger.getLogger(MarketRequestImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        try {
            ResultSet rs=statement.executeQuery("select name, price, id, owner from ITEM");
            while(rs.next()){
                System.out.println("Item: "+rs.getString(1)+", "+rs.getFloat(2)+", "+rs.getLong(3)+", "+rs.getString(4));
            }
            rs=statement.executeQuery("select buyer, seller, price from PURCHASE");
            while(rs.next()){
                System.out.println("Purchase: "+rs.getString(1)+", "+rs.getString(2)+", "+rs.getFloat(3));
            }
            // Item statements
            insertItemStatement=conn.prepareStatement("INSERT INTO ITEM (name, price, id, owner) VALUES (?, ?, ?, ?)");
            findItemStatement=conn.prepareStatement("SELECT name, price, id, owner FROM ITEM WHERE id=?");
            findAllItemStatement=conn.prepareStatement("SELECT name, price, id, owner FROM ITEM");
            deleteItemStatement=conn.prepareStatement("DELETE FROM ITEM WHERE id=?");
            // User statements
            insertUserStatement=conn.prepareStatement("INSERT INTO OWNER (name, password) VALUES (?, ?)");
            findUserStatement=conn.prepareStatement("SELECT name, password FROM OWNER WHERE name=?");
            deleteUserStatement=conn.prepareStatement("DELETE FROM OWNER WHERE name=?");
            // Purchase statements
            insertPurchaseStatement=conn.prepareStatement("INSERT INTO PURCHASE (buyer, seller, price) VALUES (?, ?, ?)");
            findBoughtPurchaseStatement=conn.prepareStatement("SELECT buyer, seller, price FROM PURCHASE WHERE buyer=?");
            findSoldPurchaseStatement=conn.prepareStatement("SELECT buyer, seller, price FROM PURCHASE WHERE seller=?");
//            findTotalBoughtAndSoldStatement=conn.prepareStatement("SELECT SUM(buyer = ?) As bought, SUM(seller = ?) AS sold FROM PURCHASE");
//        deletePurchaseStatement=conn.prepareStatement("DELETE FROM PURCHASE WHERE name=?");
        } catch (SQLException ex) {
            Logger.getLogger(MarketRequestImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println("DB created and statements initialized");
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
            
            try {
                // Add to database
                insertItemStatement.setString(1, item.getName());
                insertItemStatement.setFloat(2, item.getPrice());
                insertItemStatement.setLong(3, item.getId());
                insertItemStatement.setString(4, item.getOwner());
                int rows=insertItemStatement.executeUpdate();
                if (rows == 1) {
                    System.out.println("Market server: Item (" + item.toString()+ " has been saved in DB");
                    
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
                } else {
                    message="Item couldn't be uploaded. Try again in while.";
                }
                ResultSet rs=statement.executeQuery("select name, price, id, owner from ITEM");
                while(rs.next()){
                    System.out.println("Item: "+rs.getString(1)+", "+rs.getFloat(2)+", "+rs.getLong(3)+", "+rs.getString(4));
                }
            } catch (SQLException ex) {
                Logger.getLogger(MarketRequestImpl.class.getName()).log(Level.SEVERE, null, ex);
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
                            message="Transaction successful.";
                            
                            try {
                                deleteItemStatement.setLong(1, item.getId());
                            } catch (SQLException ex) {
                                Logger.getLogger(MarketRequestImpl.class.getName()).log(Level.SEVERE, null, ex);
                                throw new RejectedException("Couldn't remove item from DB");
                            }
                            
                            uploadedItems.remove(itemId);
                            
                            insertPurchaseStatement.setString(1, buyer.getName());
                            insertPurchaseStatement.setString(2, owner.getName());
                            insertPurchaseStatement.setFloat(3, item.getPrice());
                            int rows=insertPurchaseStatement.executeUpdate();
                            System.out.println("Purchase-insert changed "+rows+" rows");
                            owner.itemSold(item);
                        } catch (RejectedException ex) {
                            message=restoreTransaction(buyerAccount, item);
                        } catch (SQLException ex) {
                            Logger.getLogger(MarketRequestImpl.class.getName()).log(Level.SEVERE, null, ex);
                            message=restoreTransaction(buyerAccount, item);
                        }
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
    
    private String restoreTransaction(Account buyerAccount, Item item) throws RemoteException {
        String message;
        message="Couldn't deposit money.";
        try {
            buyerAccount.deposit(item.getPrice());
        } catch (RejectedException ex1) {
            Logger.getLogger(MarketRequestImpl.class.getName()).log(Level.SEVERE, null, ex1);
        }
        return message;
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
    public Message Register(User user) throws RemoteException {
        System.out.println("register. "+user.getName());
        String message="Error registering user";
        synchronized(registeredUsers){
            if(!registeredUsers.containsKey(user.getName())){
                ResultSet result = null;
//        try {
//            findUserStatement.setString(1, user.getName());
//            result = findUserStatement.executeQuery();
                
//            if (result.next()) {
//                // account exists, instantiate, put in cache and throw exception.
//                user = new UserImpl(user.getName(), result.getString(2), bank.);
//                accounts.put(name, account);
//                throw new RejectedException("Rejected: Account for: " + name
//                                                    + " already exists");
//            }
//            result.close();
//
//            // create account.
//            createAccountStatement.setString(1, name);
//            int rows = createAccountStatement.executeUpdate();
//            if (rows == 1) {
//                account = new AccountImpl(name, getConnection());
//                accounts.put(name, account);
//                System.out.println("Bank: Account: " + account
//                                           + " has been created for " + name);
//                return account;
//            } else {
//                throw new RejectedException("Cannot create an account for " + name);
//            }
//        } catch (SQLException | ClassNotFoundException e) {
//            e.printStackTrace();
//            throw new RejectedException("Cannot create an account for " + name, e);
//        }
//
//                message="Registration successful";
//            }else{
//                System.out.println("Account [" + user.getName() + "] exists.");
//            }
            registeredUsers.put(user.getName(), user);
//
            }
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
