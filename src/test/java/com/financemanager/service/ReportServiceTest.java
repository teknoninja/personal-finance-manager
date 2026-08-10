package com.financemanager.service;

import com.financemanager.dto.response.MonthlyReportResponse;
import com.financemanager.entity.*;
import com.financemanager.exception.ValidationException;
import com.financemanager.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock private TransactionRepository transactionRepository;

    @InjectMocks
    private ReportService reportService;

    private User user;
    private Category salary;
    private Category food;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        salary = Category.builder().id(1L).name("Salary").type(CategoryType.INCOME).build();
        food = Category.builder().id(2L).name("Food").type(CategoryType.EXPENSE).build();
    }

    @Test
    void getMonthlyReport_throwsValidation_whenMonthOutOfRange() {
        assertThrows(ValidationException.class, () -> reportService.getMonthlyReport(user, 2024, 13));
    }

    @Test
    void getMonthlyReport_aggregatesIncomeAndExpensesByCategory() {
        Transaction income = Transaction.builder().id(1L).user(user).category(salary)
                .amount(BigDecimal.valueOf(3000)).date(LocalDate.of(2024, 1, 15)).type(TransactionType.INCOME).build();
        Transaction expense = Transaction.builder().id(2L).user(user).category(food)
                .amount(BigDecimal.valueOf(400)).date(LocalDate.of(2024, 1, 20)).type(TransactionType.EXPENSE).build();

        when(transactionRepository.findByUserAndYearAndMonth(user, 2024, 1)).thenReturn(List.of(income, expense));

        MonthlyReportResponse report = reportService.getMonthlyReport(user, 2024, 1);

        assertEquals(BigDecimal.valueOf(3000), report.getTotalIncome().get("Salary"));
        assertEquals(BigDecimal.valueOf(400), report.getTotalExpenses().get("Food"));
        assertEquals(BigDecimal.valueOf(2600), report.getNetSavings());
    }
}
