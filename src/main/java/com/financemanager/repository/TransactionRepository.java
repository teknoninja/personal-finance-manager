package com.financemanager.repository;

import com.financemanager.entity.Category;
import com.financemanager.entity.Transaction;
import com.financemanager.entity.TransactionType;
import com.financemanager.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByUserOrderByDateDescIdDesc(User user);

    boolean existsByCategory(Category category);

    @Query("SELECT t FROM Transaction t WHERE t.user = :user "
            + "AND (:startDate IS NULL OR t.date >= :startDate) "
            + "AND (:endDate IS NULL OR t.date <= :endDate) "
            + "AND (:categoryId IS NULL OR t.category.id = :categoryId) "
            + "AND (:type IS NULL OR t.type = :type) "
            + "ORDER BY t.date DESC, t.id DESC")
    List<Transaction> findFiltered(@Param("user") User user,
                                    @Param("startDate") LocalDate startDate,
                                    @Param("endDate") LocalDate endDate,
                                    @Param("categoryId") Long categoryId,
                                    @Param("type") TransactionType type);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t "
            + "WHERE t.user = :user AND t.type = :type AND t.date >= :sinceDate")
    BigDecimal sumByUserAndTypeSince(@Param("user") User user,
                                      @Param("type") TransactionType type,
                                      @Param("sinceDate") LocalDate sinceDate);

    @Query("SELECT t FROM Transaction t WHERE t.user = :user "
            + "AND YEAR(t.date) = :year AND MONTH(t.date) = :month")
    List<Transaction> findByUserAndYearAndMonth(@Param("user") User user,
                                                 @Param("year") int year,
                                                 @Param("month") int month);

    @Query("SELECT t FROM Transaction t WHERE t.user = :user AND YEAR(t.date) = :year")
    List<Transaction> findByUserAndYear(@Param("user") User user, @Param("year") int year);
}
