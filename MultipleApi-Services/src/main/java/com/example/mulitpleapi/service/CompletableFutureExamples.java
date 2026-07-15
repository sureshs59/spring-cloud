package com.example.mulitpleapi.service;

import java.util.Timer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * @configut
 * Class MongoConfig{
 * }
 */


public class CompletableFutureExamples {
	
	public CompletableFuture<String> getWeatherData() {
	   CompletableFuture<String> weatherFuture = CompletableFuture.supplyAsync(() -> {
		   try {
			   System.out.println("getWeatherData Thread completed in 3000MS !"+Thread.currentThread().getName());
			Thread.sleep(3000);
		   } catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		   }
		   return "Weather Data Test";
	   });
	   
	   return weatherFuture;
	}
	
	public CompletableFuture<String> getlatestNews() {
		CompletableFuture<String> newsFuture = CompletableFuture.supplyAsync( () -> {
			   try {
				   System.out.println("getlatestNews Thread completed in 2000MS !!"+Thread.currentThread().getName());
					Thread.sleep(2000);
				   } catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				   }
			   
			return "Latest NEWS sending to everyone";
		}).exceptionally(ex ->{
			System.out.println("exceptionally handled in  getlatestNews()---->"+ex);
			return "";
		});
		
		return newsFuture;
	}
	
	public CompletableFuture<Object> getConversionValue() {
		CompletableFuture<Object> stockFuture =  CompletableFuture.supplyAsync( () -> {
			   try {
				   System.out.println("getConversionValue Thread completed in 1000MS !!"+Thread.currentThread().getName());
					Thread.sleep(1000);
				   } catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				   }
			 throw  new RuntimeException("forcefully raised Exception in getConversionValue..");
			//return 96;
		})
				// Handle exceptions individually for each api like using below exceptionally block 
				// If you want handle it on global way means for all apis, if any api failed due to some issue then it will handle it using 
				// HANDLE(result, exception) method line number 127
		.exceptionally(ex ->{
			System.out.println("exceptionally handled in  getConversionValue()"+ex);
			return 0;
		});
		
		return stockFuture;
	}
	
	public CompletableFuture<String> getStockExchangeValue(int exchangeValue) {
		CompletableFuture<String> output = CompletableFuture.supplyAsync(() ->{
			System.out.println("getStockExchangeValue Thread !"+Thread.currentThread().getName());
			return "USD to INR value===>"+exchangeValue;
		});
		
		return output;
	}
	
	public static void main(String[] args) throws InterruptedException, ExecutionException {
		System.out.println("Hello I am testing CompletableFuture !");
	
		long startTime = System.nanoTime();
		System.out.println("Start time :: "+startTime);
		CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				throw new IllegalStateException(e);
			}
			return "Hello";
		});
		System.out.println("Displaying the Future task :: " +future.join());
		
				
		CompletableFutureExamples gram = new CompletableFutureExamples();
		
		CompletableFuture<String> weatherTask = gram.getWeatherData();
		
	    CompletableFuture<String> newsTask  = gram.getlatestNews();
		CompletableFuture<Object> stockTask = gram.getConversionValue();
		CompletableFuture<Void>  allTasks =  CompletableFuture.allOf(weatherTask , newsTask, stockTask);
		
		// Multiple tasks executing at once using allOf() and get the results from thenRun() 
		// allOf() method are holding different threads for independent tasks
		
		allTasks.thenRun( 
				 () -> {
				 String T1 = weatherTask.join();
				 String T2 = newsTask.join();
				 Object T3 = stockTask.join();
				 System.out.println("Tuple::: "+T1+"====>"+T2+"====>"+T3);
				}).join();
		
		long endTime = System.nanoTime();
		long durationNano = endTime - startTime; 
        long durationMillis = durationNano / 1_000_000; // Convert to milliseconds

        System.out.println("Execution time: " + durationMillis + " ms");
        System.out.println("####################################################################");
        System.out.println("getStockExchangeValue() method is dependable task on getConversionValue method::: ");
        // getStockExchangeValue() method is dependable task on conversionValue method
        // thenCompose() or thenComposeAsync() method are holding same thread for dependent tasks 
        CompletableFuture<String> conversionValue = gram.getConversionValue().thenComposeAsync( (a)-> gram.getStockExchangeValue((int) a))
        		.handle( (result, ex) ->{
        			if (ex != null) {
        				System.out.println("Operation failed due to some exceptin in one if the service...");
        			}
        			return result;
        		});
				
        System.out.println("conversionValue from stock exchange method:: " + conversionValue.get());
        
        // No need to wait for all api calls
        // If anything is completed then show the results 
        System.out.println("####################################################################");
        System.out.println("No need to wait for all api calls use anyOf() method::: ");
        CompletableFuture<Object>  anyOfTasks =  CompletableFuture.anyOf( weatherTask, newsTask, stockTask);
		
		// Multiple tasks executing at once using allOf() and get the results from thenRun() 
		// allOf() method are holding different threads for independent tasks
		
        anyOfTasks.thenAccept( 
				 (firstResponse) -> {
				    System.out.println("firstResponse from anyOf::: "+firstResponse+ "<<<<<---->>>"+Thread.currentThread().getName());
				}).join();
	
	}

	// Output is: 
	/**
	 *  Hello I am testing CompletableFuture !
		Start time :: 1152292801278500
		getWeatherData Thread !ForkJoinPool.commonPool-worker-2
		getlatestNews Thread !ForkJoinPool.commonPool-worker-3
		getConversionValue Thread !ForkJoinPool.commonPool-worker-4
		Tuple::: Weather Data Test====>Latest NEWS sending to everyone====>96
		Execution time: 3034 ms
		getConversionValue Thread !ForkJoinPool.commonPool-worker-2
		getStockExchangeValue Thread !ForkJoinPool.commonPool-worker-2
		conversionValue from stock exchange method:: USD to INR value===>96
	 */
}
	
