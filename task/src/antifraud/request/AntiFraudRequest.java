package antifraud.request;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.time.LocalDateTime;

@Data
@RequiredArgsConstructor
@Entity
public class AntiFraudRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private static final String regionRegex = "EAP|ECA|HIC|LAC|MENA|SA|SSA";

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


}
