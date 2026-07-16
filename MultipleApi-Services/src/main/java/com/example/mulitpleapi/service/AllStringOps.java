package com.example;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class AllStringOps {

	public static void main(String[] args) {

		System.out.println("String programs #############");
		
		String str1 = "SR@gmail.com";
		
		for ( int i = str1.length()-1; i>=0; i--) {
			System.out.println(" ===> "+str1.charAt(i));
		}
		
		String[] strArray = str1.split("@");
		
		for(var ss : strArray) {
			System.out.println(" ===> "+ss);
		}
		
		String finalOP = "";
		char[] firstStr =  strArray[0].toCharArray();
		if (strArray[0].length() == 2) {
			finalOP =  strArray[0] + strArray[1];
		} else {
//			for ( int i=0; i<strArray[0].length(); i++) {
//				if(i>=2) {
//					finalOP = strArray[0].replace(firstStr[i], '*');
//				}
//				System.out.println("firstStr ===> "+finalOP);
//			}
			String sub1 = strArray[0].substring(0, 2);
			String sub2 = strArray[0].substring(2, strArray[0].length());
			String replacedValue = sub2.replaceAll(sub2, "*".repeat(sub2.length()));
			System.out.println("firstStr $$$$$$$===> "+sub2+"---replacedValue---"+replacedValue);
			finalOP = sub1 + replacedValue + strArray[1];
		}
		
		System.out.println("finalOP ===> "+finalOP);
		

		String s = "abcbba";
		
		char[] ch = s.toLowerCase().toCharArray();
		
		Map<Character, Integer> map = new HashMap<>();
		
		int vowels = 0, cons = 0;
		for(int i=0;i <ch.length;i++){
			if (ch[i] == 'a' || ch[i] == 'e' ||ch[i] == 'i' ||ch[i] == 'o' ||ch[i] == 'u' ){
				//map.put('a', map.getOrDefault(ch[i], 0) + 1);
				vowels++;
			}else {
				cons++;
			}
			map.put(ch[i], map.getOrDefault(ch[i], 0) + 1);
		}
		
		System.out.println("number of vowels : "+vowels+" consonents ::"+cons);
		
		for (Entry<Character, Integer> e: map.entrySet()) {
			if (e.getValue() > 0) {
				System.out.println(e.getKey()+"<---->entry value "+e.getValue());
			}
		}
		
		int number = 5688861;
		
		String s2 = Integer.toString(number);
		Map<Integer, Integer> numMap = new HashMap<>();
		while(number > 0) {
			int i = number % 10;
			System.out.println(i);
			numMap.put(i, numMap.getOrDefault(i, 0)+1);
			number = number/10;
		}
		
		for (Entry<Integer, Integer> e: numMap.entrySet()) {
			if (e.getValue() > 0) {
				System.out.println(e.getKey()+"<---->entry value "+e.getValue());
			}
		}
		
		String dups = "Hello Worlda";
		char[] checkChars = dups.toCharArray();
		int vowelCnt = 0, consCount = 0;
		for (int i=0; i<checkChars.length;i++ )
		{
			if("Suresh".indexOf(checkChars[i]) != -1) {
				vowelCnt++;
			}else {
				consCount++;
			}
		}
		
		System.out.println("<---->vowelCnt value "+vowelCnt+"---consonents ::"+consCount);
		
		int[] arr1 = {3,6,7,9,12};
		int[] arr2 = {1,5,3,9};
		int[] arr3 = new int[arr1.length];
		
		for (int i=0;i < arr1.length;i++) {
			for( int j = i; j<arr2.length;j++) {
				if ( arr1[i] == arr2[j] ) {
					 arr3[j] = arr2[j];
				}
			}
		}
 //  arr3.toArray(new Integer[0]);
        
		for (int k=0; k < arr3.length;k++) {
			System.out.println("<---->array 3 value "+arr3[k]);
		}
		
		
		Integer[] array1 = {1, 2, 3, 4, 5,7};
        Integer[] array2 = {3, 4, 5, 6, 7};
        
        // Find common values
        List<Integer> list1 = new ArrayList<>(Arrays.asList(array1));
        List<Integer> list2 = new ArrayList<>(Arrays.asList(array2));
        list1.retainAll(list2); // This keeps only common elements
        
        System.out.println(Arrays.toString(list1.toArray()));
        // Convert the List to a 3rd Array
        Integer[] array3 = list1.toArray(new Integer[0]);
        
        System.out.println(Arrays.toString(array3));
	
        String str = "hello world from java";
        
        String[] splitStrArray = str.split(" ");
        
        for (int i = splitStrArray.length-1; i>=0;i-- ) {
        	System.out.print(splitStrArray[i]+" ");
        }
        /// Anagram check
      
        String ang1 = "listen";
        String ang2 = "silent";
        
        char[] anagram1 = ang1.toCharArray();
        char[] anagram2 = ang2.toCharArray();
        
        Arrays.sort(anagram1);
        Arrays.sort(anagram2);
        String afterSortStr = "", afterSortStr2 = "";
        for(char cc: anagram1) {
        	System.out.println(" 11 Outside "+cc);         
        	afterSortStr = afterSortStr + cc;
        }
        for(char cc: anagram2) {
        	System.out.println("22 Outside "+cc);         
        	afterSortStr2 = afterSortStr2 + cc;
        }
        if (ang1.length() == ang2.length() && afterSortStr.indexOf(afterSortStr2) != -1) {
        	System.out.println("\n both are anagrams "+afterSortStr+"--->check with ::"+afterSortStr2);
        } else {
        	System.out.println("\n both are not anagrams strings "+afterSortStr+"--->check with ::"+afterSortStr2);
        }
        
	}
	
	
}
