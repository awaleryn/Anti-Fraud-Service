package antifraud.request;

import lombok.*;

import javax.validation.constraints.NotNull;

@Data
@AllArgsConstructor
public class TransactionFeedbackRequest {

    @NotNull
    private long id;

    @NotNull
    private String feedback;
}
