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
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
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
    private PreparedStatement findAllItemStatement;
    private PreparedStatement deleteItemStatement;
    
    private PreparedStatement insertUserStatement;
    private PreparedStatement findUserStatement;
    private PreparedStatement deleteUserStatement;
    
    private PreparedStatement insertPurchaseStatement;
    private PreparedStatement findTotalBoughtAndSoldStatement;
//    private PreparedStatement deletePurchaseStatement;
    
    // The key is the item-ID
    private final Map<Long, Item> uploadedItems=new HashMap<>();
    private final Map<String, List<Item>> wishedItems=new HashMap<>();
    private final Map<String, User> registeredUsers=new HashMap<>();
    private final Map<String, User> loggedInUsers=new HashMap<>();
    private final Map<String, Bank> banks=new HashMap<>();
    
    public MarketRequestImpl() throws RemoteException
    {
        try {
            connect();
        } catch (SQLException ex) {
            Logger.getLogger(MarketRequestImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
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
        try {
            // This will load the MySQL driver, each DB has its own driver
            Class.forName("com.mysql.jdbc.Driver");
            // Setup the connection with the DB
            conn = DriverManager.getConnection("jdbc:mysql://localhost/homework3?user=root&password=sqlpass");
            
            statement=conn.createStatement();
            createTable();
            
            restoreState();
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(MarketRequestImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void createTable() {
        int ro;
        try {
            ro=statement.executeUpdate("CREATE TABLE ITEM (name VARCHAR(64) NOT NULL, price FLOAT NOT NULL, id BIGINT PRIMARY KEY, owner VARCHAR(64) NOT NULL)");
            System.out.println("ITEM CREATION changed "+ro+" rows");
        } catch (SQLException ex) {
            Logger.getLogger(MarketRequestImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            ro=statement.executeUpdate("CREATE TABLE OWNER (name VARCHAR(64) PRIMARY KEY, password VARCHAR(256) NOT NULL, bankname varchar(64) NOT NULL)");
            System.out.println("OWNER CREATION changed "+ro+" rows");
        } catch (SQLException ex) {
            Logger.getLogger(MarketRequestImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            ro=statement.executeUpdate("CREATE TABLE PURCHASE (buyer VARCHAR(64) NOT NULL, seller VARCHAR(64) NOT NULL, price FLOAT NOT NULL, item VARCHAR(64) NOT NULL)");
            System.out.println("PURCHASE CREATION changed "+ro+" rows");
        } catch (SQLException ex) {
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
            findAllItemStatement=conn.prepareStatement("SELECT name, price, id, owner FROM ITEM");
            deleteItemStatement=conn.prepareStatement("DELETE FROM ITEM WHERE id=?");
            // User statements
            insertUserStatement=conn.prepareStatement("INSERT INTO OWNER (name, password, bankname) VALUES (?, ?, ?)");
            findUserStatement=conn.prepareStatement("SELECT name, password, bankname FROM OWNER WHERE name=?");
            deleteUserStatement=conn.prepareStatement("DELETE FROM OWNER WHERE name=?");
            // Purchase statements
            insertPurchaseStatement=conn.prepareStatement("INSERT INTO PURCHASE (buyer, seller, price, item) VALUES (?, ?, ?, ?)");
            findTotalBoughtAndSoldStatement=conn.prepareStatement("SELECT SUM(buyer = ?) As bought, SUM(seller = ?) AS sold FROM PURCHASE");
            
            findTotalBoughtAndSoldStatement.setString(1,"david");
            findTotalBoughtAndSoldStatement.setString(2,"david");
            rs=findTotalBoughtAndSoldStatement.executeQuery();
            while(rs.next()){
                System.out.println("Total purchases: bought = "+rs.getInt(1)+", sold = "+rs.getInt(2));
            }
//        deletePurchaseStatement=conn.prepareStatement("DELETE FROM PURCHASE WHERE name=?");
        } catch (SQLException ex) {
            Logger.getLogger(MarketRequestImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println("DB created and statements initialized");
    }
    
    @Override
    public Message sellItem(Item item) throws RemoteException {
        System.out.println("sell item. "+item.toString());
        
        if(!loggedInUsers.containsKey(item.getOwner())){
            Message msg=new Message();
            msg.message="You are not logged in.";
            return msg;
        }
        
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
                                try{
                                    if(item.getName().equals(wishedItem.getName()) && item.getPrice()<=wishedItem.getPrice()){
                                        User owner=registeredUsers.get(wishedItem.getOwner());
                                        if(owner!=null)
                                            owner.wishAvaible(item);
                                    }
                                }catch(RemoteException ex){
                                    System.err.println("Couldn't send wish. User not online.");
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
    public Message buyItem(long itemId, User buyer) throws RemoteException {
        if(!loggedInUsers.containsKey(buyer.getName())){
            Message msg=new Message();
            msg.message="You are not logged in.";
            return msg;
        }
        
        String message="Unspecified error occured when buying item. No item bought.";
        synchronized(uploadedItems){
            Item item=uploadedItems.get(itemId);
            System.out.println("buy item. "+item.toString());
            if(item!=null){
                System.out.println("buy item. item = "+item.toString()+", buyer = "+buyer.toString());
                User owner=tryLoadUser(item.getOwner());
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
                            try {
                                deleteItemStatement.setLong(1, item.getId());
                                int rows=deleteItemStatement.executeUpdate();
                                if(rows==1){
                                    try{
                                        sellerAccount.deposit(item.getPrice());
                                        message="Transaction successful.";
                                        
                                        uploadedItems.remove(itemId);
                                        
                                        insertPurchaseStatement.setString(1, buyer.getName());
                                        insertPurchaseStatement.setString(2, owner.getName());
                                        insertPurchaseStatement.setFloat(3, item.getPrice());
                                        insertPurchaseStatement.setString(4, item.getName());
                                        rows=insertPurchaseStatement.executeUpdate();
                                        System.out.println("Purchase-insert changed "+rows+" rows");
                                        try{
                                            owner.itemSold(item);
                                        }catch(Exception e){}
                                    }catch(RemoteException ex){
                                        Logger.getLogger(MarketRequestImpl.class.getName()).log(Level.SEVERE, null, ex);
                                        message="Couldn't rollback transaction.";
                                    }
                                }else{
                                    message=restoreTransaction(buyerAccount, item);
                                    System.err.println(message);
                                    message="Problem occured.";
                                }
                            } catch (SQLException ex) {
                                Logger.getLogger(MarketRequestImpl.class.getName()).log(Level.SEVERE, null, ex);
                                throw new RejectedException("Couldn't remove item from DB");
                            }
                        } catch (RejectedException ex) {
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
    public Message addWish(Item item) throws RemoteException {
        System.out.println("add item. "+item.toString());
        
        if(!loggedInUsers.containsKey(item.getOwner())){
            Message msg=new Message();
            msg.message="You are not logged in.";
            return msg;
        }
        
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
    public Message register(User user) throws RemoteException {
        System.out.println("register. "+user.getName());
        String message="Error registering user";
        if(user.getPassword().length()<8){
            message="Your password is too short (8 or more characters required).";
        }else{
            user.setPassword(BCrypt.hashpw(user.getPassword(), BCrypt.gensalt()));
            synchronized(registeredUsers){
                User loadedUser=tryLoadUser(user.getName());
                if(loadedUser==null){
                    try {
                        // Create user
                        insertUserStatement.setString(1, user.getName());
                        insertUserStatement.setString(2, user.getPassword());
                        insertUserStatement.setString(3, user.getBankName());
                        int rows=insertUserStatement.executeUpdate();
                        if(rows==1){
                            try {
                                Bank bankobj = getBank(user.getBankName());
                                user.setBankAccount(bankobj.getAccount(user.getName()));
                                registeredUsers.put(user.getName(), user);
                                System.out.println("Account [" + user.getName() + "] created.");
                                message="User ["+user.getName()+"] registered.";
                            } catch (Exception ex) {
                                message="Bank problem. Not registered.";
                            }
                        }else{
                            System.out.println("Account [" + user.getName() + "] NOT created.");
                            message="User not registered.";
                        }
                    } catch (SQLException ex) {
                        Logger.getLogger(MarketRequestImpl.class.getName()).log(Level.SEVERE, null, ex);
                        message="User not registered.";
                    }
                } else {
                    System.out.println("Account [" + user.getName() + "] exists.");
                    message="User name already exist. Try another name.";
                }
            }
        }
        Message msg=new Message();
        msg.message=message;
        return msg;
    }
    
    private User tryLoadUser(String userName){
        User user=registeredUsers.get(userName);
        if(user==null || !isUserOnline(user)){
            ResultSet result = null;
            try {
                findUserStatement.setString(1, userName);
                result = findUserStatement.executeQuery();
                
                Bank bankobj;
                if (result.next()) {
                    String bankName=result.getString(3);
                    try {
                        bankobj = getBank(bankName);
                        try {
                            user = new UserImpl(result.getString(1), result.getString(2), bankobj.getAccount(userName), bankName);
                        } catch (Exception ex) {
                            Logger.getLogger(MarketRequestImpl.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    } catch (Exception ex) {
                    }
                }
            }catch(SQLException ex){
                Logger.getLogger(MarketRequestImpl.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return user;
    }
    
    private boolean isUserOnline(User user) {
        try {
            user.getName();
            return true;
        } catch (RemoteException ex) {
//            Logger.getLogger(MarketRequestImpl.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
    }
    
    private Bank getBank(String bankName) throws Exception {
        Bank bankobj=banks.get(bankName);
        if(bankobj==null){
            try {
                bankobj = (Bank) Naming.lookup(bankName);
                banks.put(bankName, bankobj);
            } catch (NotBoundException ex) {
                Logger.getLogger(ClientPanel.class.getName()).log(Level.SEVERE, null, ex);
                throw new Exception();
            } catch (MalformedURLException ex) {
                Logger.getLogger(MarketRequestImpl.class.getName()).log(Level.SEVERE, null, ex);
                throw new Exception();
            }
        }
        return bankobj;
    }
    
    @Override
    public Message unregister(User owner) throws RemoteException {
        System.out.println("unregister. "+owner.getName());
        String message="Error unregistering user";
        synchronized(registeredUsers){
            try {
                deleteUserStatement.setString(1, owner.getName());
                registeredUsers.remove(owner.getName());
                List<Item> c=new ArrayList<>();
                // Remove items the owner has uploaded
                for(Item item:uploadedItems.values()){
                    if(item.getOwner().equals(owner.getName()))
                        c.add(item);
                }
                uploadedItems.values().removeAll(c);
                message="Unregistration successful";
            } catch (SQLException ex) {
                Logger.getLogger(MarketRequestImpl.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        Message msg=new Message();
        msg.message=message;
        return msg;
    }
    
    @Override
    public Message logIn(User user, String name, String password) throws RemoteException {
        System.out.println("Try to log in user ["+name+"]");
        Message msg=new Message();
        try {
            User loadedUser=tryLoadUser(name);
            if(loadedUser!=null){
//                System.out.println("pass = "+password+", hash = "+loadedUser.getPassword());
                if(BCrypt.checkpw(password, loadedUser.getPassword())){
                    user.createFromUser(loadedUser);
                    registeredUsers.put(user.getName(), user);
                    loggedInUsers.put(user.getName(), user);
                    msg.message="Logged in.";
                }else {
                    msg.message="Username or password was incorrect.";
                }
            }else{
                msg.message="Username or password was incorrect.";
            }
            
        } catch (Exception ex) {
            Logger.getLogger(MarketRequestImpl.class.getName()).log(Level.SEVERE, null, ex);
            msg.message="Problem. Try again.";
        }
        return msg;
    }
    
    @Override
    public Message logOut(User user) throws RemoteException {
        System.out.println("Try to log out user ["+user.getName()+"]");
        Message msg=new Message();
        try {
            if(user!=null){
                loggedInUsers.remove(user.getName());
                msg.message="Logged out";
            }
                
        } catch (Exception ex) {
            Logger.getLogger(MarketRequestImpl.class.getName()).log(Level.SEVERE, null, ex);
            msg.message="Problem. Try again.";
        }
        return msg;
    }
    
    @Override
    public Message getUserActivity(User user) throws RemoteException {
        if(!loggedInUsers.containsKey(user.getName())){
            Message msg=new Message();
            msg.message="You are not logged in.";
            return msg;
        }
        
        Message msg=new Message();
        try {
            findTotalBoughtAndSoldStatement.setString(1, user.getName());
            findTotalBoughtAndSoldStatement.setString(2, user.getName());
            ResultSet result=findTotalBoughtAndSoldStatement.executeQuery();
            if(result.next()){
                int bought=result.getInt("bought");
                int sold=result.getInt("sold");
                msg.message=user.getName()+" has bought "+bought+" items, and sold "+sold+" items.";
            }else{
                msg.message="Couldn't find any indicators.";
            }
        } catch (SQLException ex) {
            Logger.getLogger(MarketRequestImpl.class.getName()).log(Level.SEVERE, null, ex);
            msg.message="Problem finding indicators. Try again.";
        }
        return msg;
    }
}
