package antifraud.transaction;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.time.LocalDateTime;


@Entity
@NoArgsConstructor
@Getter
@Setter
@ToString
public class TransactionFeedback {

    private static final String regionRegex = "EAP|ECA|HIC|LAC|MENA|SA|SSA";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long transactionId;

    @Min(value = 1, message = "Amount must be greater than 0")
    private long amount;

    @NotEmpty
    private String ip;

    @NotEmpty
    private String number;

    @NotEmpty
    @Pattern(regexp = regionRegex)
    private String region;

    @NotNull
    private LocalDateTime date;

    @NotNull
    private String result;

    private String feedback = "";

    @JsonIgnore
    boolean hasFeedback = false;

    public TransactionFeedback(long amount, String ip, String number, String region, LocalDateTime date, String result) {
        this.amount = amount;
        this.ip = ip;
        this.number = number;
        this.region = region;
        this.date = date;
        this.result = result;
    }

}
