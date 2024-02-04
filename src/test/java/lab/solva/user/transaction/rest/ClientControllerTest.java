package lab.solva.user.transaction.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import lab.solva.user.transaction.dto.AmountLimitDateDto;
import lab.solva.user.transaction.dto.AmountLimitDto;
import lab.solva.user.transaction.dto.ExpenseTransactionDto;
import lab.solva.user.transaction.dto.TransactionExceededLimitDto;
import lab.solva.user.transaction.service.ClientService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
public class ClientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ClientService clientService;

    @Test
    public void testGetAllAmountLimitDateByAccountClient() throws Exception {

        // Arrange
        String accountClient = "0000000001";
        List<AmountLimitDateDto> amountLimitDateDtoList = Arrays.asList(
                createAmountLimitDateDto(accountClient, 1000.0,"RUB", "Service",
                        ZonedDateTime.parse("2024-01-01T00:00:00+06:00", DateTimeFormatter.ISO_OFFSET_DATE_TIME)),
                createAmountLimitDateDto(accountClient, 500.0,"EUR", "Service",
                        ZonedDateTime.parse("2024-01-30T15:35:34+06:00", DateTimeFormatter.ISO_OFFSET_DATE_TIME))
        );

        when(clientService.getAllAmountLimitDateDtoByAccountClient(accountClient)).thenReturn(amountLimitDateDtoList);

        // Act & Assert
        mockMvc.perform(get("/api/client/{account_client}", accountClient)
                .param("account_client", accountClient)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(amountLimitDateDtoList.size())))
                .andExpect(jsonPath("$[0].account_from", is(accountClient)))
                .andExpect(jsonPath("$[0].limit_sum", is(1000.0)))
                .andExpect(jsonPath("$[0].limit_currency_shortname", is("RUB")))
                .andExpect(jsonPath("$[0].expense_category", is("Service")))
                .andExpect(jsonPath("$[0].limit_datetime", is("2024-01-01T00:00:00+06:00")))
                .andExpect(jsonPath("$[1].account_from", is(accountClient)))
                .andExpect(jsonPath("$[1].limit_sum", is(500.0)))
                .andExpect(jsonPath("$[1].limit_currency_shortname", is("EUR")))
                .andExpect(jsonPath("$[1].expense_category", is("Service")))
                .andExpect(jsonPath("$[1].limit_datetime", is("2024-01-30T15:35:34+06:00")));
    }

    @Test
    public void testGetAllTransactionExceededLimitByAccountClient() throws Exception {

        // Arrange
        String accountClient = "0000000001";
        String accountCounterparty = "9000000000";
        List<TransactionExceededLimitDto> transactionExceededLimitDtoList = Arrays.asList(
                createTransactionExceededLimitDto(accountClient, accountCounterparty,
                        "RUB", 1000.0, "Service",
                        ZonedDateTime.parse("2024-01-30T16:30:45+06:00", DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                        500.0,"EUR",
                        ZonedDateTime.parse("2024-01-30T15:35:34+06:00", DateTimeFormatter.ISO_OFFSET_DATE_TIME))
        );

        when(clientService.getAllTransactionExceededLimitDtoByAccountClient(accountClient)).thenReturn(transactionExceededLimitDtoList);

        // Act & Assert
        mockMvc.perform(get("/api/client/transaction/{account_client}", accountClient)
                .param("account_client", accountClient)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(transactionExceededLimitDtoList.size())))
                .andExpect(jsonPath("$[0].account_from", is(accountClient)))
                .andExpect(jsonPath("$[0].account_to", is(accountCounterparty)))
                .andExpect(jsonPath("$[0].currency_shortname", is("RUB")))
                .andExpect(jsonPath("$[0].Sum", is(1000.0)))
                .andExpect(jsonPath("$[0].expense_category", is("Service")))
                .andExpect(jsonPath("$[0].datetime", is("2024-01-30T16:30:45+06:00")))
                .andExpect(jsonPath("$[0].limit_sum", is(500.0)))
                .andExpect(jsonPath("$[0].limit_currency_shortname", is("EUR")))
                .andExpect(jsonPath("$[0].limit_datetime", is("2024-01-30T15:35:34+06:00")));
    }

    @Test
    public void testSetAmountLimit() throws Exception {

        // Arrange
        AmountLimitDto amountLimitDto = new AmountLimitDto();

        amountLimitDto.setAccount_from("0000000001");
        amountLimitDto.setLimit_sum(500.0);
        amountLimitDto.setLimit_currency_shortname("EUR");
        amountLimitDto.setExpense_category("Product");

        // Act & Assert
        mockMvc.perform(post("/api/client")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(amountLimitDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.account_from", is("0000000001")))
                .andExpect(jsonPath("$.limit_sum", is(500.0)))
                .andExpect(jsonPath("$.limit_currency_shortname", is("EUR")))
                .andExpect(jsonPath("$.expense_category", is("Product")));

        // Verify that the service method was called
        verify(clientService, times(1)).setAmountLimitDto(ArgumentMatchers.any(AmountLimitDto.class));
    }

    // Метод для преобразования объекта в строку JSON
    private static String asJsonString(final Object obj) {
        try {
            final ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX"));
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Method for create object of AmountLimitDateDto
    private AmountLimitDateDto createAmountLimitDateDto(String account_from, double limit_sum,
                String limit_currency_shortname, String expense_category, ZonedDateTime limit_datetime) {

        AmountLimitDateDto amountLimitDateDto = new AmountLimitDateDto();

        amountLimitDateDto.setAccount_from(account_from);
        amountLimitDateDto.setLimit_sum(limit_sum);
        amountLimitDateDto.setLimit_currency_shortname(limit_currency_shortname);
        amountLimitDateDto.setExpense_category(expense_category);
        amountLimitDateDto.setLimit_datetime(limit_datetime);

        return amountLimitDateDto;
    }

    // Method for create object of AmountLimitDateDto
    private TransactionExceededLimitDto createTransactionExceededLimitDto(String account_from, String account_to,
                String currency_shortname, double Sum, String expense_category, ZonedDateTime datetime,
                double limit_sum, String limit_currency_shortname, ZonedDateTime limit_datetime) {

        TransactionExceededLimitDto transactionExceededLimitDto = new TransactionExceededLimitDto();

        transactionExceededLimitDto.setAccount_from(account_from);
        transactionExceededLimitDto.setAccount_to(account_to);
        transactionExceededLimitDto.setCurrency_shortname(currency_shortname);
        transactionExceededLimitDto.setSum(Sum);
        transactionExceededLimitDto.setExpense_category(expense_category);
        transactionExceededLimitDto.setDatetime(datetime);
        transactionExceededLimitDto.setLimit_sum(limit_sum);
        transactionExceededLimitDto.setLimit_currency_shortname(limit_currency_shortname);
        transactionExceededLimitDto.setLimit_datetime(limit_datetime);

        return transactionExceededLimitDto;
    }
}