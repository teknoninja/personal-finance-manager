package com.financemanager.service;

import com.financemanager.dto.response.MonthlyReportResponse;
import com.financemanager.dto.response.YearlyReportResponse;
import com.financemanager.entity.Transaction;
import com.financemanager.entity.TransactionType;
import com.financemanager.entity.User;
import com.financemanager.exception.ValidationException;
import com.financemanager.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final TransactionRepository transactionRepository;

    public MonthlyReportResponse getMonthlyReport(User user, int year, int month) {
        if (month < 1 || month > 12) {
            throw new ValidationException("month must be between 1 and 12");
        }
        List<Transaction> transactions = transactionRepository.findByUserAndYearAndMonth(user, year, month);

        Map<String, BigDecimal> income = aggregateByCategory(transactions, TransactionType.INCOME);
        Map<String, BigDecimal> expenses = aggregateByCategory(transactions, TransactionType.EXPENSE);
        BigDecimal net = sumAll(income).subtract(sumAll(expenses));

        return new MonthlyReportResponse(month, year, income, expenses, net);
    }

    public YearlyReportResponse getYearlyReport(User user, int year) {
        List<Transaction> transactions = transactionRepository.findByUserAndYear(user, year);

        Map<String, BigDecimal> income = aggregateByCategory(transactions, TransactionType.INCOME);
        Map<String, BigDecimal> expenses = aggregateByCategory(transactions, TransactionType.EXPENSE);
        BigDecimal net = sumAll(income).subtract(sumAll(expenses));

        return new YearlyReportResponse(year, income, expenses, net);
    }

    private Map<String, BigDecimal> aggregateByCategory(List<Transaction> transactions, TransactionType type) {
        return transactions.stream()
                .filter(t -> t.getType() == type)
                .collect(Collectors.groupingBy(
                        t -> t.getCategory().getName(),
                        TreeMap::new,
                        Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)
                ));
    }

    private BigDecimal sumAll(Map<String, BigDecimal> map) {
        return map.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
