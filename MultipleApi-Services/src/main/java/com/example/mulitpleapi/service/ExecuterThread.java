package com.example;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;

@FunctionalInterface
interface Inter1{
	public int sum(int x, int y);
}

public class ExecuterThread extends Thread{
	
	@Override
	public void run() {
		System.out.println("ExecuterThread Run method call..."+currentThread().getName());

	}

	public static void main(String args[]) {
		ScheduledThreadPoolExecutor ft = new ScheduledThreadPoolExecutor(5);
		
		System.out.println("Thread pool executer..."+ft.getActiveCount()+"--->>"+ft.getThreadFactory());
		
		ExecutorService exeService =  Executors.newFixedThreadPool(9);
		
		for(int i=0;i<5;i++) {
			exeService.execute( ()-> {
				System.out.println("Pool Task executing via: " + Thread.currentThread().getName());
			});
		}
		exeService.shutdown();
		
		ExecuterThread et = new ExecuterThread();
		System.out.println("ExecuterThread Name..."+currentThread().getName());

		System.out.println("ExecuterThread priority..."+currentThread().getPriority());
		et.start();
		
		Runnable runner = () ->{
			System.out.println("Lambda function calling run method===>"+Thread.currentThread().getName());
		};
		
		runner.run();
		
		Inter1 i1 = (a, b) ->{
			return a+b;
		};
		
		System.out.println("Lambda function call ::: "+i1.sum(12, 13));
	}
}
