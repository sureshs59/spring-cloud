package com.expensetracker.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Represents a monthly budget limit for a category.
 * Used to show GraphQL relationships between two entity types.
 *
 * GraphQL schema extension (add to schema.graphqls):
 *
 *   type Budget {
 *     id:          ID!
 *     category:    Category!
 *     monthYear:   String!     # "2025-03"
 *     limitAmount: Float!
 *     spentAmount: Float!      # resolved dynamically via @SchemaMapping
 *     remaining:   Float!      # resolved dynamically via @SchemaMapping
 *     isOverBudget: Boolean!
 *   }
 *
 *   type Query {
 *     budgets(monthYear: String!): [Budget!]!
 *     budget(category: Category!, monthYear: String!): Budget
 *   }
 *
 *   type Mutation {
 *     setBudget(category: Category!, monthYear: String!, limit: Float!): Budget!
 *   }
 */
@Entity
@Table(
    name = "budget",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"category", "month_year"},
        name = "uk_budget_category_month"
    )
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Budget {

    public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Category getCategory() {
		return category;
	}

	public void setCategory(Category category) {
		this.category = category;
	}

	public String getMonthYear() {
		return monthYear;
	}

	public void setMonthYear(String monthYear) {
		this.monthYear = monthYear;
	}

	public Double getLimitAmount() {
		return limitAmount;
	}

	public void setLimitAmount(Double limitAmount) {
		this.limitAmount = limitAmount;
	}

	public Boolean getIsOverBudget() {
		return isOverBudget;
	}

	public void setIsOverBudget(Boolean isOverBudget) {
		this.isOverBudget = isOverBudget;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(LocalDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    /**
     * Month this budget applies to, stored as "yyyy-MM" string (e.g. "2025-03").
     * Using String keeps it simple — no LocalYearMonth complexity.
     */
    @NotBlank
    @Column(name = "month_year", nullable = false)
    private String monthYear;

    @Positive(message = "Budget limit must be a positive amount")
    @Column(name = "limit_amount", nullable = false)
    private Double limitAmount;

    @Column(name = "is_over_budget", nullable = false)
    @Builder.Default
    private Boolean isOverBudget = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
