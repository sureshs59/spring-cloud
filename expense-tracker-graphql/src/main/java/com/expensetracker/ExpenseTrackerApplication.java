package com.expensetracker;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Expense Tracker — Spring Boot + GraphQL
 *
 * Once running, open: GraphiQL IDE → http://localhost:8080/graphiql H2 Console
 * → http://localhost:8080/h2-console GraphQL WS →
 * ws://localhost:8080/graphql-ws (for subscriptions)
 */
@SpringBootApplication
@Slf4j
public class ExpenseTrackerApplication {

	public static void main(String[] args) {
		SpringApplication.run(ExpenseTrackerApplication.class, args);
		System.out.println("=================================================");
		System.out.println("  Expense Tracker GraphQL API is running!");
		System.out.println("  GraphiQL IDE : http://localhost:8080/graphiql");
		System.out.println("  H2 Console   : http://localhost:8080/h2-console");
		System.out.println("  GraphQL WS   : ws://localhost:8080/graphql-ws");
		System.out.println("=================================================");
	}
}
