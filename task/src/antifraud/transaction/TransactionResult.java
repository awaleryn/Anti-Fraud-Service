package antifraud.transaction;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor

public class TransactionResult {

    private String result;
    private String info;

    public TransactionResult(TransactionResultType type, String info) {
        this.result = type.name();
        this.info = info;
    }
}
