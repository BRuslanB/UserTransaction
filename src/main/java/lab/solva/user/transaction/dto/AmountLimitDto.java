package lab.solva.user.transaction.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@AllArgsConstructor
@NoArgsConstructor
public class AmountLimitDto {

    public double limit_sum;

    public String limit_currency_shortname;

    public String expense_category;
}
