/*
* To change this license header, choose License Headers in Project Properties.
* To change this template file, choose Tools | Templates
* and open the template in the editor.
*/
package views;

import Market.Item;
import Market.MarketRequest;
import Market.Message;
import Market.Owner;
import Market.OwnerImpl;
import bank.Bank;
import bank.Account;
import bank.RejectedException;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

/**
 *
 * @author David
 */
public class ClientPanel extends Panel {
    public static ClientPanel clientPanel;
    
    private static MarketRequest market;
    private static Bank bankobj;
    private static final String DEFAULT_BANK_NAME = "Nordea";
    private static Owner owner;
    private static List<Item> currentlyListedItems;
    
    // Game components
    private JLabel statusMessageLabel=new JLabel("STATUS MESSAGE:");
    private JLabel itemsLabel=new JLabel("format: [id]. [item]", SwingConstants.CENTER);
    private JTextField accountNameTextField=new JTextField(10);
    private JButton registerButton=new JButton("Register/Log in");
    private JButton unregisterButton=new JButton("Unregister");
    private JButton listItemsButton=new JButton("List items");
    // Buy/Sell components
    private JTextField itemNameTextField=new JTextField(10);
    private JTextField itemPriceTextField=new JTextField(10);
    private JButton sellButton=new JButton("sell");
    private JButton wishButton=new JButton("wish");
    private JButton newBankAccountButton=new JButton("new bank account");
    
    private JTextField buyItemNameTextField=new JTextField(10);
    private JButton buyButton=new JButton("buy");
    
    public ClientPanel(String bankName) throws RemoteException{
        
        try {
            LocateRegistry.getRegistry(1099).list();
        } catch (RemoteException e) {
            LocateRegistry.createRegistry(1099);
        }
        try {
            market=(MarketRequest)Naming.lookup("MarketRequest");
            bankobj = (Bank) Naming.lookup(bankName);
        } catch (NotBoundException ex) {
            Logger.getLogger(ClientPanel.class.getName()).log(Level.SEVERE, null, ex);
            if(market==null){
                registerButton.setEnabled(false);
                listItemsButton.setEnabled(false);
                statusChanged("The market wasn't found");
            }
            if(bankobj==null){
                statusChanged("The bank wasn't found");
            }
        } catch (MalformedURLException ex) {
            Logger.getLogger(ClientPanel.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        setLayout(new BorderLayout());
        constructComponents();
        
        clientPanel=this;
    }
    
    public ClientPanel() throws RemoteException {
        this(DEFAULT_BANK_NAME);
    }
    
    private void constructComponents(){
        
        registerButton.addActionListener((ActionEvent e)->{ register(); });
        accountNameTextField.addActionListener((ActionEvent e)->{ register(); });
        unregisterButton.addActionListener((ActionEvent e)->{ unregister(); });
        newBankAccountButton.addActionListener((ActionEvent e)->{ newBankAccount(); });
        
        buyButton.addActionListener((ActionEvent e)->{ buy(); });
        // buyItemNameTextField.addActionListener((ActionEvent e)->{ buy(); });
        
        // itemNameTextField.addActionListener((ActionEvent e)->{ sell(); });
        sellButton.addActionListener((ActionEvent e)->{ sell(); });
        wishButton.addActionListener((ActionEvent e)->{ wish(); });
        
        listItemsButton.addActionListener((ActionEvent e)->{ listItems();});
        JPanel gamePanel=new JPanel();
        gamePanel.setLayout(new BorderLayout());
        JPanel guessPanel=new JPanel();
        guessPanel.setLayout(new FlowLayout());
        guessPanel.add(new JLabel("account name"));
        guessPanel.add(accountNameTextField);
        guessPanel.add(registerButton);
        guessPanel.add(unregisterButton);
        guessPanel.add(listItemsButton);
        guessPanel.add(newBankAccountButton);
        gamePanel.add(guessPanel, BorderLayout.NORTH);
        add(gamePanel, BorderLayout.NORTH);
        
        JPanel tPanel=new JPanel();
        tPanel.setLayout(new BorderLayout());
        
        JPanel sellAndWishPanel=new JPanel();
        sellAndWishPanel.setLayout(new FlowLayout());
        sellAndWishPanel.add(new JLabel("name"));
        sellAndWishPanel.add(itemNameTextField);
        sellAndWishPanel.add(new JLabel("price"));
        sellAndWishPanel.add(itemPriceTextField);
        sellAndWishPanel.add(sellButton);
        sellAndWishPanel.add(wishButton);
        // Add connectionPanel to ClientPanel
        tPanel.add(sellAndWishPanel, BorderLayout.NORTH);
        
        JPanel buyPanel=new JPanel();
        buyPanel.setLayout(new FlowLayout());
        buyPanel.add(new JLabel("id"));
        buyPanel.add(buyItemNameTextField);
        buyPanel.add(buyButton);
        // Add connectionPanel to ClientPanel
        tPanel.add(buyPanel, BorderLayout.CENTER);
        add(tPanel,BorderLayout.CENTER);
        
        JPanel itemsAndStatusPanel=new JPanel();
        itemsAndStatusPanel.setLayout(new BorderLayout());
        itemsAndStatusPanel.add(itemsLabel, BorderLayout.NORTH);
        itemsAndStatusPanel.add(statusMessageLabel, BorderLayout.CENTER);
        // Add connectionPanel to ClientPanel
        add(itemsAndStatusPanel, BorderLayout.SOUTH);
        
        buyButton.setEnabled(false);
        sellButton.setEnabled(false);
        wishButton.setEnabled(false);
        unregisterButton.setEnabled(false);
    }
    
    private void listItems(){
        new Thread( new Runnable() {
            @Override
            public void run() {
                try {
                    Message msg=market.ListItems();
                    if(msg==null)
                        return;
                    
                    currentlyListedItems=(List<Item>)msg.obj;
                    System.out.println(currentlyListedItems.size());
                    StringBuilder sb=new StringBuilder();
                    int index=0;
                    sb.append("format: [id]. [item]<br>");
                    for(Item item:currentlyListedItems){
                        System.out.println(item.toString());
                        sb.append(index++).append(". ");
                        sb.append(item.toString());
                        sb.append("<br>");
                    }
                    itemsLabel.setText("<html>"+sb.toString()+"</html>");
                    itemsLabel.getParent().revalidate();
                } catch (RemoteException ex) {
                    Logger.getLogger(ClientPanel.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }).start();
    }
    
    private void register(){
        new Thread( new Runnable() {
            @Override
            public void run() {
                if(!accountNameTextField.getText().equals("")){
                    try {
                        Account account=bankobj.getAccount(accountNameTextField.getText());
                        if(account == null)
                        {
                            statusChanged("No bank account with the given name was found");
                            return;
                        }
                        
                        String password="passwordNameTextField.getText()";
                        try {
                            owner=new OwnerImpl(accountNameTextField.getText(), password,  account);
                        } catch (Exception ex) {
                            Logger.getLogger(ClientPanel.class.getName()).log(Level.SEVERE, null, ex);
                            statusChanged("Catastrophic error with password encrypting.");
                            return;
                        }
                        Message msg=market.Register(owner);
                        String result=msg.message;
                        statusChanged(result);
                        
                        buyButton.setEnabled(true);
                        sellButton.setEnabled(true);
                        wishButton.setEnabled(true);
                        unregisterButton.setEnabled(true);
                    } catch (RemoteException ex) {
                        Logger.getLogger(ClientPanel.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }).start();
    }
    
    private void sell(){
        new Thread( new Runnable() {
            @Override
            public void run() {
                float price;
                try{
                    price=Float.parseFloat(itemPriceTextField.getText());
                }catch(NumberFormatException e){
                    return;
                }
                if(itemNameTextField.getText().equals(""))
                    return;
                
                Item item;
                try {
                    item = new Item(itemNameTextField.getText(), price, owner.getName());
                    String result=market.SellItem(item).message;
                    statusChanged(result);
                } catch (RemoteException ex) {
                    Logger.getLogger(ClientPanel.class.getName()).log(Level.SEVERE, null, ex);
                    statusChanged("Remote exception");
                }
            }
        }).start();
    }
    
    private void buy(){
        new Thread( new Runnable() {
            @Override
            public void run() {
                if(buyItemNameTextField.getText().equals(""))
                    return;
                
                int index;
                try{
                    index=Integer.parseInt(buyItemNameTextField.getText());
                }catch(NumberFormatException e){
                    return;
                }
                
                if(index<0 || index>=currentlyListedItems.size())
                    return;
                
                try {
                    String result=market.BuyItem(currentlyListedItems.get(index).getId(), owner).message;
                    statusChanged(result);
                } catch (RemoteException ex) {
                    Logger.getLogger(ClientPanel.class.getName()).log(Level.SEVERE, null, ex);
                    statusChanged("Remote exception");
                }
            }
        }).start();
    }
    
    private void wish(){
        new Thread( new Runnable() {
            @Override
            public void run() {
                float price;
                try{
                    price=Float.parseFloat(itemPriceTextField.getText());
                }catch(NumberFormatException e){
                    return;
                }
                if(itemNameTextField.getText().equals(""))
                    return;
                
                try {
                    Item item=new Item(itemNameTextField.getText(), price, owner.getName());
                    String result=market.AddWish(item).message;
                    statusChanged(result);
                } catch (RemoteException ex) {
                    Logger.getLogger(ClientPanel.class.getName()).log(Level.SEVERE, null, ex);
                    statusChanged("Remote exception");
                }
            }
        }).start();
    }
    
    private void newBankAccount(){
        new Thread( new Runnable() {
            @Override
            public void run() {
                if(accountNameTextField.getText().equals(""))
                    return;
                
                try {
                    try {
                        bankobj.newAccount(accountNameTextField.getText());
                        statusChanged("New bank account successfully created");
                    } catch (RejectedException ex) {
                        Logger.getLogger(ClientPanel.class.getName()).log(Level.SEVERE, null, ex);
                        statusChanged("Bank account already exist");
                    }
                    buyButton.setEnabled(false);
                    sellButton.setEnabled(false);
                    wishButton.setEnabled(false);
                    unregisterButton.setEnabled(false);
                } catch (RemoteException ex) {
                    Logger.getLogger(ClientPanel.class.getName()).log(Level.SEVERE, null, ex);
                    statusChanged("Remote exception");
                }
            }
        }).start();
    }
    
    private void unregister(){
        new Thread( new Runnable() {
            @Override
            public void run() {
                if(accountNameTextField.getText().equals(""))
                    return;
                
                try {
                    Message msg=market.Unregister(owner);
                    statusChanged(msg.message);
                } catch (RemoteException ex) {
                    Logger.getLogger(ClientPanel.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }).start();
    }
    
    public void statusChanged(String statusMessage){
        SwingUtilities.invokeLater(new Runnable()
        {
            @Override
            public void run()
            {
                System.out.println("Client: Status = "+statusMessage);
                statusMessageLabel.setText(statusMessage);
            }
        });
    }
    
    public void messageReceived(String response){
        SwingUtilities.invokeLater(new Runnable()
        {
            @Override
            public void run()
            {
                System.out.println("Client: Event = "+response);
                itemsLabel.setText(response);
                if(response.contains("GAME OVER! Score ") || response.contains("Congratulations! Word"))
                    registerButton.setEnabled(false);
            }
        });
    }
}
