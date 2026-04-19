package com.expensetracker.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.context.annotation.Bean;

/**
 * GraphQL runtime wiring configuration.
 *
 * In this project, Spring Boot auto-detects @Controller resolvers,
 * so we don't need to register them manually here.
 *
 * This class is the right place to add:
 *   - Custom scalar types (e.g. Date, BigDecimal)
 *   - DataLoader batching registrations (prevents N+1 queries)
 *   - Custom directives (e.g. @auth, @deprecated)
 *
 * N+1 Example — if you added a User type with many Expenses,
 * you would register a DataLoader here to batch-load all users
 * in one DB call instead of one call per expense.
 */
@Configuration
public class GraphQLConfig {

    /**
     * Example: register a custom scalar for LocalDate.
     *
     * Uncomment and add graphql-java-extended-scalars dependency to use:
     *
     * @Bean
     * public RuntimeWiringConfigurer runtimeWiringConfigurer() {
     *     return wiringBuilder -> wiringBuilder
     *             .scalar(ExtendedScalars.Date)
     *             .scalar(ExtendedScalars.GraphQLBigDecimal);
     * }
     */

    /**
     * WebSocket CORS configuration for GraphQL Subscriptions.
     * Allows the Angular frontend (localhost:4200) to connect.
     */
	@Configuration
	public class CorsConfig implements WebMvcConfigurer {
	    @Override
	    public void addCorsMappings(CorsRegistry registry) {
	        registry.addMapping("/graphql")
	                .allowedOrigins("*")
	                .allowedMethods("GET", "POST", "OPTIONS");
	    }
	}
}
