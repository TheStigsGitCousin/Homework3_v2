/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package market;

import java.util.Calendar;
import java.util.Random;

/**
 *
 * @author David
 */
public class Handler {
    private static Random random=new Random();
    
    public static long getRandomLong(){
        return Math.abs(random.nextLong());
    }
}
