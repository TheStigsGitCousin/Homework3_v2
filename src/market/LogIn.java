/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package market;

import java.util.Calendar;

/**
 *
 * @author David
 */
public class LogIn {
    public User user;
    public Calendar timeOfLogIn;
    
    public LogIn(User user){
        this.user=user;
        this.timeOfLogIn=Calendar.getInstance();
    }
}
