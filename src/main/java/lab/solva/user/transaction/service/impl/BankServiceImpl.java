package lab.solva.user.transaction.service.impl;

import lab.solva.user.transaction.dto.ExpenseTransactionDto;
import lab.solva.user.transaction.enumeration.CurrencyType;
import lab.solva.user.transaction.enumeration.ExpenseCategory;
import lab.solva.user.transaction.model.AmountLimitEntity;
import lab.solva.user.transaction.model.ExchangeRateEntity;
import lab.solva.user.transaction.model.ExpenseTransactionEntity;
import lab.solva.user.transaction.repository.AmountLimitRepository;
import lab.solva.user.transaction.repository.ExpenseTransactionRepository;
import lab.solva.user.transaction.service.BankService;
import lab.solva.user.transaction.service.ExchangeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BankServiceImpl implements BankService {

    private final static double DEFAULT_LIMIT_SUM = 100.0;
    private final static String DEFAULT_LIMIT_CURRENCY_CODE = CurrencyType.EUR.name();

    private final ExpenseTransactionRepository expenseTransactionRepository;
    private final AmountLimitRepository amountLimitRepository;
    private final ExchangeService exchangeService;

    @Override
    public void saveExpenseTransactionDto(ExpenseTransactionDto expenseTransactionDto) {
        // Сохранение полученных данных из expenseTransactionDto
        if (expenseTransactionDto != null) {
            ExpenseTransactionEntity expenseTransactionEntity = new ExpenseTransactionEntity();
            expenseTransactionEntity.setAccountClient(expenseTransactionDto.account_from);
            expenseTransactionEntity.setAccountCounterparty(expenseTransactionDto.account_to);
            expenseTransactionEntity.setCurrencyCode(expenseTransactionDto.currency_shortname);
            expenseTransactionEntity.setTransactionSum(expenseTransactionDto.Sum);
            // Проверка Expense Category на допустимое значение
            String expenseCategory = expenseTransactionDto.expense_category;
            if (ExpenseCategory.SERVICE.name().equals(expenseCategory.toUpperCase()) ||
                    ExpenseCategory.PRODUCT.name().equals(expenseCategory.toUpperCase())) {
                expenseTransactionEntity.setExpenseCategory(expenseCategory);
            } else {
                // Не допустимое значение, категория расходов не найдена в списке
                // Сохраняем этот факт в Лог
                return;
            }
            // Проверка Date и Time на допустимое значение
            Timestamp transactionTimestamp = expenseTransactionDto.datetime;
            LocalDateTime transactionDateTime = transactionTimestamp.toLocalDateTime();
            LocalDateTime currentDateTime = LocalDateTime.now();
            if (transactionDateTime.isBefore(currentDateTime)) {
                expenseTransactionEntity.setTransactionDateTime(transactionTimestamp);
            } else {
                // Не допустимое значение, дата и время транзации позже текущей дата и времени
                // Сохраняем этот факт в Лог
                return;
            }
            // Вычисление значение для поля limitExceeded
            expenseTransactionEntity.setLimitExceeded(getLimitExceeded(expenseTransactionDto.account_from,
                    expenseTransactionDto.expense_category, expenseTransactionDto.currency_shortname,
                    expenseTransactionDto.Sum));
            // Сохраняем ссылку на родительскую сущность
            expenseTransactionEntity.setAmountLimitEntity(getAmountLimit(expenseTransactionDto.account_from,
                    expenseTransactionDto.expense_category));
            // Логирование действия
            expenseTransactionRepository.save(expenseTransactionEntity);
        }
    }

    private boolean getLimitExceeded(String accountClient, String expenseCategory, String currencyCode,
                                     double currentTransactionSum){

        double currentLimit; // Сумма лимита за текущий месяц
        double sumTransactionResult = 0.0; // Итоговая сумма всех транзакции за месяц, сконвертируемая в валюте лимита

        // Получение текущего месяца и года
        LocalDateTime currentDateTime = LocalDateTime.now();
        int currentMonth = currentDateTime.getMonthValue();
        int currentYear = currentDateTime.getYear();

        // Вычисление суммы всех транзакции за месяц по каждой валюте
        double sumTransactionKZT = expenseTransactionRepository.calcTransactionSum(
                accountClient, expenseCategory, CurrencyType.KZT.name(), currentMonth, currentYear);
        double sumTransactionUSD = expenseTransactionRepository.calcTransactionSum(
                accountClient, expenseCategory, CurrencyType.USD.name(), currentMonth, currentYear);
        double sumTransactionEUR = expenseTransactionRepository.calcTransactionSum(
                accountClient, expenseCategory, CurrencyType.EUR.name(), currentMonth, currentYear);
        double sumTransactionRUB = expenseTransactionRepository.calcTransactionSum(
                accountClient, expenseCategory, CurrencyType.RUB.name(), currentMonth, currentYear);

        // Добавляние суммы по текущей транзакции к определенной сумме
        switch (CurrencyType.valueOf(currencyCode)) {
            case KZT:
                sumTransactionKZT += currentTransactionSum;
                break;
            case USD:
                sumTransactionUSD += currentTransactionSum;
                break;
            case EUR:
                sumTransactionEUR += currentTransactionSum;
                break;
            case RUB:
                sumTransactionRUB += currentTransactionSum;
                break;
            // Другие кейсы для остальных видов валют (при необходимости)
            default:
                // Получен неизвестный вид валюты
                // Сохраняем этот факт в Лог
                return false;
        }

        // Получение текущего курса валют из БД
        List<ExchangeRateEntity> exchangeRateEntityList = exchangeService.gettingRates().stream().toList();
        // Создаем и заполняем HashMap с курсами валют
        Map<String, Double> exchangeRateMap = new HashMap<>();
        for (ExchangeRateEntity exchangeRateEntity : exchangeRateEntityList) {
            exchangeRateMap.put(exchangeRateEntity.getCurrencyCode(), exchangeRateEntity.getExchangeRate());
        }

        // Получение лимита из БД
        AmountLimitEntity amountLimitEntity = getAmountLimit(accountClient, expenseCategory);
        if (amountLimitEntity != null) {
            currentLimit = amountLimitEntity.getLimitSum();
            String limitCurrencyCode = amountLimitEntity.getLimitCurrencyCode();

            // Конвертирование всех сумм транзакции к Currency Code установленного лимита
            switch (CurrencyType.valueOf(limitCurrencyCode)) {
                case KZT:
                    sumTransactionResult = sumTransactionKZT ;
                    if (exchangeRateMap.get(CurrencyType.USD.name()) != null) {
                        sumTransactionResult += sumTransactionUSD * exchangeRateMap.get(CurrencyType.USD.name());
                    }
                    if (exchangeRateMap.get(CurrencyType.EUR.name()) != null) {
                        sumTransactionResult += sumTransactionEUR * exchangeRateMap.get(CurrencyType.EUR.name());
                    }
                    if (exchangeRateMap.get(CurrencyType.RUB.name()) != null) {
                        sumTransactionResult += sumTransactionRUB * exchangeRateMap.get(CurrencyType.RUB.name());
                    }
                    break;
                case USD:
                    sumTransactionResult = sumTransactionUSD ;
                    if (exchangeRateMap.get(CurrencyType.USD.name()) != null) {
                        sumTransactionResult += sumTransactionKZT / exchangeRateMap.get(CurrencyType.USD.name());
                    }
                    if (exchangeRateMap.get(CurrencyType.EUR.name()) != null &&
                            exchangeRateMap.get(CurrencyType.USD.name()) != null) {
                        sumTransactionResult += sumTransactionEUR *
                                (exchangeRateMap.get(CurrencyType.EUR.name()) / exchangeRateMap.get(CurrencyType.USD.name()));
                    }
                    if (exchangeRateMap.get(CurrencyType.RUB.name()) != null &&
                            exchangeRateMap.get(CurrencyType.USD.name()) != null) {
                        sumTransactionResult += sumTransactionRUB *
                                (exchangeRateMap.get(CurrencyType.RUB.name()) / exchangeRateMap.get(CurrencyType.USD.name()));
                    }
                    break;
                case EUR:
                    sumTransactionResult = sumTransactionEUR ;
                    if (exchangeRateMap.get(CurrencyType.EUR.name()) != null) {
                        sumTransactionResult += sumTransactionKZT / exchangeRateMap.get(CurrencyType.EUR.name());
                    }
                    if (exchangeRateMap.get(CurrencyType.USD.name()) != null &&
                            exchangeRateMap.get(CurrencyType.EUR.name()) != null) {
                        sumTransactionResult += sumTransactionUSD *
                                (exchangeRateMap.get(CurrencyType.USD.name()) / exchangeRateMap.get(CurrencyType.EUR.name()));
                    }
                    if (exchangeRateMap.get(CurrencyType.RUB.name()) != null &&
                            exchangeRateMap.get(CurrencyType.EUR.name()) != null) {
                        sumTransactionResult += sumTransactionRUB *
                                (exchangeRateMap.get(CurrencyType.RUB.name()) / exchangeRateMap.get(CurrencyType.EUR.name()));
                    }
                    break;
                case RUB:
                    sumTransactionResult = sumTransactionRUB ;
                    if (exchangeRateMap.get(CurrencyType.RUB.name()) != null) {
                        sumTransactionResult += sumTransactionKZT / exchangeRateMap.get(CurrencyType.RUB.name());
                    }
                    if (exchangeRateMap.get(CurrencyType.USD.name()) != null &&
                            exchangeRateMap.get(CurrencyType.RUB.name()) != null) {
                        sumTransactionResult += sumTransactionUSD *
                                (exchangeRateMap.get(CurrencyType.USD.name()) / exchangeRateMap.get(CurrencyType.RUB.name()));
                    }
                    if (exchangeRateMap.get(CurrencyType.EUR.name()) != null &&
                            exchangeRateMap.get(CurrencyType.RUB.name()) != null) {
                        sumTransactionResult += sumTransactionEUR *
                                (exchangeRateMap.get(CurrencyType.EUR.name()) / exchangeRateMap.get(CurrencyType.RUB.name()));
                    }
                    break;
                // Другие кейсы для остальных видов валют (при необходимости)
            }
        } else {
            // Лимит не установлен и не получен из БД
            // Сохраняем этот факт в Лог
            return false;
        }

        return currentLimit < sumTransactionResult;
    }

    private AmountLimitEntity getAmountLimit(String accountClient, String expenseCategory){

        AmountLimitEntity amountLimitEntity;

        // Получение текущего месяца и года
        LocalDateTime currentDateTime = LocalDateTime.now();
        int currentMonth = currentDateTime.getMonthValue();
        int currentYear = currentDateTime.getYear();

         // Получение последнего установленного лимита из БД
        Optional<AmountLimitEntity> lastAmountLimitOptional = amountLimitRepository.findAmountLimit(
                accountClient, expenseCategory, currentMonth, currentYear);
        // Получаем лимит если лимит есть в БД, или сохраняем лимит заданный по умолчанию
        amountLimitEntity = lastAmountLimitOptional.orElseGet(() -> saveAmountLimit(accountClient, expenseCategory));

        return amountLimitEntity;
    }

    private AmountLimitEntity saveAmountLimit(String accountClient, String expenseCategory) {
        // Получаем 1 число текущего месяца с началом времени 00:00:00
        LocalDateTime firstDayOfMonth = LocalDateTime.now().withDayOfMonth(1).toLocalDate().atStartOfDay();

        AmountLimitEntity amountLimitEntity  = new AmountLimitEntity();
        amountLimitEntity.setAccountClient(accountClient);
        // Используем значение по умолчанию для суммы лимита
        amountLimitEntity.setLimitSum(DEFAULT_LIMIT_SUM);
        // Используем значение firstDayOfMonth
        amountLimitEntity.setLimitDateTime(Timestamp.valueOf(firstDayOfMonth));
        // Используем значение по умолчанию для вида валюты лимита
        amountLimitEntity.setLimitCurrencyCode(DEFAULT_LIMIT_CURRENCY_CODE);
        amountLimitEntity.setExpenseCategory(expenseCategory);
        // Логирование действия
        amountLimitRepository.save(amountLimitEntity);

        return amountLimitEntity;
    }
}
