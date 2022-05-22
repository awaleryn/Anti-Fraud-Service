package antifraud;

import antifraud.card.Card;
import antifraud.ip.IpEntity;
import antifraud.request.AntiFraudRequest;
import antifraud.transaction.TransactionFeedback;
import antifraud.transaction.TransactionResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;;
import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("api/antifraud/")
@Validated
public class AntiFraudRestController {

    private final AntiFraudService service;
    @Autowired
    public AntiFraudRestController(AntiFraudService service) {
        this.service = service;
    }

    @PostMapping("transaction")
    public TransactionResult checkTransaction(@RequestBody @Valid AntiFraudRequest request) {
        if (StringUtils.isEmpty(request.getAmount()) ||
                StringUtils.isEmpty(request.getIp()) ||
                StringUtils.isEmpty(request.getNumber()) ||
                StringUtils.isEmpty(request.getRegion())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        return service.checkTransaction(request);
    }

    @PutMapping("transaction")
    public TransactionFeedback putFeedback(@RequestBody Map<String, String> trIDAndFeedback) {
        return service.putFeedback(trIDAndFeedback);
    }

    @GetMapping("history")
    public List<TransactionFeedback> feedbackList() {
        return service.getTransactionsList();
    }

    @GetMapping("history/{number}")
    public List<TransactionFeedback> feedbackListForSpecifiedNumber(@PathVariable String number) {
        return service.getTransactionsListForSpecifiedNumber(number);
    }

    @PostMapping("suspicious-ip")
    @ResponseStatus(HttpStatus.OK)
    public IpEntity postSuspiciousIp(@RequestBody @Valid IpEntity ip) {
        return service.postSuspiciousIp(ip);
    }

    @DeleteMapping("suspicious-ip/{ip}")
    @ResponseStatus(HttpStatus.OK)
    public Map<String, String> deleteSuspiciousIp(@PathVariable String ip) {
        return service.deleteSuspiciousIp(ip);
    }

    @GetMapping("suspicious-ip")
    @ResponseStatus(HttpStatus.OK)
    public List<IpEntity> getSuspiciousIpList() {
        return service.getSuspiciousIpList();
    }

    @PostMapping("stolencard")
    @ResponseStatus(HttpStatus.OK)
    public Card postStolenCard(@RequestBody Card card) {
        return service.postStolenCard(card);
    }

    @DeleteMapping("stolencard/{number}")
    @ResponseStatus(HttpStatus.OK)
    public Map<String, String> deleteStolenCard(@PathVariable String number) {
        return service.deleteStolenCard(number);
    }

    @GetMapping("stolencard")
    @ResponseStatus(HttpStatus.OK)
    public List<Card> getStolenCardList() {
        return service.getStolenCardList();
    }



}
