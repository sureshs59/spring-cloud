package com.example.goldsilverapp;

import java.util.HashMap;
import java.util.Map;

public class TestMain {
    public static void main(String[] args) {
        System.out.println("Hello World");
        Map<String, Double> map = new HashMap<String, Double>();
        map.put("INRXAG", 7107.8533584121);
        map.put("INRXAU",441856.6874453751);

        double goldRate = map.get("INRXAU");
        Double silverRate = map.get("INRXAG");
        System.out.println("Gold Rate: " + goldRate+"--silverRate--"+silverRate);
        System.out.println("After Gold Rate: " +Math.round((goldRate / 31.1034768 * 10) * 100.0) / 100.0);
        System.out.println("After Silver Rate: " +Math.round((silverRate / 31.1034768 * 1000) * 100.0) / 100.0);
    }
}
