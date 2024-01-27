package lab.solva.user.transaction.service;

import lab.solva.user.transaction.dto.AmountLimitDateDto;
import lab.solva.user.transaction.dto.AmountLimitDto;
import lab.solva.user.transaction.dto.TransactionExceededLimitDto;

import java.util.List;

public interface ClientService {

    // Получение всех лимитов из БД в Dto формате
    List<AmountLimitDateDto> getAllAmountLimitDateDto();

    // Установка и сохранение лимита в БД
    void setAmountLimitDto(AmountLimitDto amountLimitDto);

    // Получение списка всех транзакции превысивших установленный лимит
    List<TransactionExceededLimitDto> getTransactionExceededLimitDto();
}
