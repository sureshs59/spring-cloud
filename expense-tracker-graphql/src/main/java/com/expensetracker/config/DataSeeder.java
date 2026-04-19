package com.expensetracker.config;

import com.expensetracker.model.Category;
import com.expensetracker.model.Expense;
import com.expensetracker.repository.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Seeds the H2 in-memory database with realistic sample expenses on startup.
 * This runs every time the application starts (H2 is reset on each start).
 *
 * In production (with PostgreSQL), remove this class or guard it with a profile:
 *   @Profile("dev")
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final ExpenseRepository expenseRepository;

    @Override
    public void run(String... args) {
        System.out.println("Seeding sample expense data...");

        LocalDate today = LocalDate.now();

        List<Expense> sampleExpenses = List.of(

            // ── January ──────────────────────────────────────────────────────
            expense("Monthly groceries",      180.50, Category.FOOD,          today.minusDays(90), "Supermarket run"),
            expense("Electricity bill",        95.00, Category.UTILITIES,     today.minusDays(88), "January bill"),
            expense("Netflix subscription",    15.99, Category.ENTERTAINMENT, today.minusDays(87), null),
            expense("Bus pass",                45.00, Category.TRANSPORT,     today.minusDays(85), "Monthly pass"),
            expense("Gym membership",          40.00, Category.HEALTH,        today.minusDays(84), "January renewal"),
            expense("Python book",             34.99, Category.EDUCATION,     today.minusDays(82), "Clean Architecture"),
            expense("Weekend dinner",          62.40, Category.FOOD,          today.minusDays(80), "Family dinner"),
            expense("Uber ride",               12.30, Category.TRANSPORT,     today.minusDays(79), null),

            // ── February ─────────────────────────────────────────────────────
            expense("Monthly groceries",      165.20, Category.FOOD,          today.minusDays(60), "Supermarket"),
            expense("Internet bill",           55.00, Category.UTILITIES,     today.minusDays(58), "Broadband"),
            expense("Cinema tickets",          28.00, Category.ENTERTAINMENT, today.minusDays(56), "2 tickets"),
            expense("Dentist visit",           80.00, Category.HEALTH,        today.minusDays(54), "Routine checkup"),
            expense("Taxi to airport",         38.50, Category.TRANSPORT,     today.minusDays(52), null),
            expense("Spotify",                  9.99, Category.ENTERTAINMENT, today.minusDays(51), "Premium monthly"),
            expense("Work lunch",              14.50, Category.FOOD,          today.minusDays(50), "Canteen"),
            expense("New running shoes",       89.99, Category.SHOPPING,      today.minusDays(48), "Adidas"),
            expense("Udemy course",            19.99, Category.EDUCATION,     today.minusDays(46), "Spring Boot Advanced"),

            // ── March ────────────────────────────────────────────────────────
            expense("Monthly groceries",      190.00, Category.FOOD,          today.minusDays(30), "Bulk shopping"),
            expense("Gas bill",               110.00, Category.UTILITIES,     today.minusDays(28), "Heating"),
            expense("Concert tickets",         75.00, Category.ENTERTAINMENT, today.minusDays(26), "Live event"),
            expense("Pharmacy",                22.40, Category.HEALTH,        today.minusDays(24), "Vitamins"),
            expense("Train tickets",           56.00, Category.TRANSPORT,     today.minusDays(22), "Weekend trip"),
            expense("Coffee shop",             18.75, Category.FOOD,          today.minusDays(20), "Remote work"),
            expense("New headphones",         129.00, Category.SHOPPING,      today.minusDays(18), "Sony WH-1000XM5"),
            expense("Online course",           29.99, Category.EDUCATION,     today.minusDays(16), "AI/ML intro"),
            expense("Lunch with team",         48.60, Category.FOOD,          today.minusDays(14), "Team outing"),
            expense("Water bill",              32.00, Category.UTILITIES,     today.minusDays(12), null),
            expense("Taxi rides",              24.50, Category.TRANSPORT,     today.minusDays(10), "This week"),
            expense("Vitamins restock",        35.00, Category.HEALTH,        today.minusDays(8),  null),
            expense("Weekend groceries",       72.30, Category.FOOD,          today.minusDays(5),  null),
            expense("T-shirts x3",             44.97, Category.SHOPPING,      today.minusDays(3),  "Online order"),
            expense("Coffee & snacks",         11.20, Category.FOOD,          today.minusDays(1),  "Airport")
        );

        expenseRepository.saveAll(sampleExpenses);
        System.out.println("Seeded {} expenses successfully."+ sampleExpenses.size());
    }

    private Expense expense(String title, double amount, Category category,
                             LocalDate date, String description) {
        return Expense.builder()
                .title(title)
                .amount(amount)
                .category(category)
                .date(date)
                .description(description)
                .build();
    }
}
