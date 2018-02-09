package com.example.obd2application;

import java.math.BigInteger;

/**
 * Created by linkk on 12/13/2017.
 */

public class Practice {

    //Using to generate data columns in excel

    public static void main(String[] args) {
        System.out.println("Start");

        System.out.println(new Integer(12).toString(16));

//        String availablePidsCommand_01_20Result = "BE1FA811";//"BE1FA811";
//        System.out.println(new BigInteger(availablePidsCommand_01_20Result, 16).toString(2));

//        System.out.println("0123456789ABCDEF".substring(4, 12));

//        String[] availablePidsCommandResults;
//        availablePidsCommandResults = new String[] {"BE1FA811", "90000000"};//Toyota Corolla S 2007
//        availablePidsCommandResults = new String[] {"BE3FA813", "B007E011", "FAD08001", "00000001", "00000002"};//Honda ?? 2017
//        availablePidsCommandResults = new String[] {"981A8013", "A007F011", "FED08C81"};//Mazda 6 2017 incomplete data
//        availablePidsCommandResults = new String[] {"FE3FA818", "80018001", "C0800000"};//Mazda 6 2017 other data
//        availablePidsCommandResults = new String[] {"BFBFA893", "801FF119", "FAD00000"};//Honda Odyssey 2012
//        availablePidsCommandResults = new String[] {"BE1FA813", "9005B015", "FEDC2000"};//Toyota Camry 2010??
//        availablePidsCommandResults = new String[] {"98188013", "80018001", "C00C0000"};//Toyota Camry 2010?? other data
//        availablePidsCommandResults = new String[] {"BE3EE813", "A007B011", "FED08401", "00010000"};//Volkswagen GTI 2017
//        availablePidsCommandResults = new String[] {"98180001", "00000001", "C0800000"};//Volkswagen GTI 2017 other data
//        for (int i=0; i<availablePidsCommandResults.length; i++) {
//            String availablePidsCommandResults_20_Binary = new BigInteger(availablePidsCommandResults[i], 16).toString(2);
//            String availablePidsCommandResults_20_Binary_Leading_Zeros = ("00000000000000000000000000000000" + availablePidsCommandResults_20_Binary).substring(availablePidsCommandResults_20_Binary.length());
////            System.out.println(availablePidsCommandResults_20_Binary_Leading_Zeros);
//            char[] availablePidsCommandResults_20 = availablePidsCommandResults_20_Binary_Leading_Zeros.toCharArray();
//            for (int j=0; j<availablePidsCommandResults_20.length; j++) {
//                System.out.println(availablePidsCommandResults_20[j]);
//            }
//        }
//        System.out.println();


//        for (int i=0; i<256; i++) {
//            System.out.println(new BigInteger(""+i).toString(16).toUpperCase());
//        }
//        for (int i=0; i<256; i++) {
//            System.out.println(i);
//        }

        System.out.println("Finish");
    }
}
