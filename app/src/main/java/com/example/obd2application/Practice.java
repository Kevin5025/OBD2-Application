package com.example.obd2application;

import java.math.BigInteger;

/**
 * Created by linkk on 12/13/2017.
 */

public class Practice {
    public static void main(String[] args) {
//        String availablePidsCommand_01_20Result = "BE1FA811";//"BE1FA811";
//        System.out.println(new BigInteger(availablePidsCommand_01_20Result, 16).toString(2));

        String[] availablePidsCommandResults;

//        availablePidsCommandResults = new String[] {"BE1FA811", "90000000"};//Toyota Corolla S 2007
        availablePidsCommandResults = new String[] {"BE3FA813", "B007E011", "FAD08001", "00000001", "00000002"};//Honda 2017
        for (int i=0; i<availablePidsCommandResults.length; i++) {
            String availablePidsCommandResults_20_Binary = new BigInteger(availablePidsCommandResults[i], 16).toString(2);
            String availablePidsCommandResults_20_Binary_Leading_Zeros = ("00000000000000000000000000000000" + availablePidsCommandResults_20_Binary).substring(availablePidsCommandResults_20_Binary.length());
//            System.out.println(availablePidsCommandResults_20_Binary_Leading_Zeros);
            char[] availablePidsCommandResults_20 = availablePidsCommandResults_20_Binary_Leading_Zeros.toCharArray();
            for (int j=0; j<availablePidsCommandResults_20.length; j++) {
                System.out.println(availablePidsCommandResults_20[j]);
            }
        }
//        System.out.println();


//        for (int i=0; i<256; i++) {
//            System.out.println(new BigInteger(""+i).toString(16).toUpperCase());
//        }
//        for (int i=0; i<256; i++) {
//            System.out.println(i);
//        }
    }
}
