package antifraud;


import antifraud.card.Card;
import antifraud.card.CardChecker;
import antifraud.card.CardRepository;
import antifraud.ip.IpEntity;
import antifraud.ip.IpRepository;
import antifraud.request.AntiFraudRequest;
import antifraud.transaction.TransactionFeedback;
import antifraud.transaction.TransactionRepository;
import antifraud.transaction.TransactionResult;
import antifraud.transaction.TransactionResultType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class AntiFraudService {

    private final IpRepository ipRepository;
    private final CardRepository cardRepository;
    private final AntiFraudRepository antiFraudRepository;
    private final TransactionRepository transactionRepository;
    private static final String IPV4_PATTERN =
            "^(([0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])(\\.(?!$)|$)){4}$";

    @Autowired
    public AntiFraudService(IpRepository ipRepository,
                            CardRepository cardRepository,
                            AntiFraudRepository antiFraudRepository,
                            TransactionRepository transactionRepository) {
        this.ipRepository = ipRepository;
        this.cardRepository = cardRepository;
        this.antiFraudRepository = antiFraudRepository;
        this.transactionRepository = transactionRepository;
    }

    public TransactionResult checkTransaction(AntiFraudRequest request) {

        if (!isIpNumberValid(request.getIp()) || !isCardNumberValid(request.getNumber())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        if (cardRepository.findByNumber(request.getNumber()).isEmpty()) {
            cardRepository.save(new Card(request.getNumber(), false));
        }

        antiFraudRepository.save(request);

        Set<String> reasons = new TreeSet<>();

        boolean allowed = false;
        boolean manual = false;
        boolean prohibited = false;

        if (cardRepository.findCardByNumber(request.getNumber()).isPresent()) {
            if (cardRepository.findCardByNumber(request.getNumber()).get().isLocked()) {
                reasons.add("card-number");
                prohibited = true;
            }
        }

        if (ipRepository.findIpEntityByIp(request.getIp()).isPresent()) {
            reasons.add("ip");
            prohibited = true;
        }

        LocalDateTime end = request.getDate();

        LocalDateTime start = end.minusHours(1);

        List<AntiFraudRequest> transactionsInLastHour = antiFraudRepository.findAllByDateBetweenAndNumber(start, end, request.getNumber());

        long regionsCount = transactionsInLastHour.stream().map(AntiFraudRequest::getRegion).distinct().count();

        long ipCount = transactionsInLastHour.stream().map(AntiFraudRequest::getIp).distinct().count();

        Card card = cardRepository.findByNumber(request.getNumber())
                .orElseThrow(AssertionError::new);

        int allowedAmount = (int) card.getAllowedLimit();
        int allowedManual = card.getManualLimit();

        if (regionsCount > 2) {
            reasons.add("region-correlation");
            if (regionsCount == 3) manual = true;
            else prohibited = true;
        }
        if (ipCount > 2) {
            reasons.add("ip-correlation");
            if (ipCount == 3) manual = true;
            else prohibited = true;
        }

        long amount = request.getAmount();

        if (amount <= allowedAmount && !prohibited && !manual) {
            allowed = true;
            reasons.add("none");
        }


        if (amount > allowedAmount && amount <= allowedManual && !prohibited) {
            reasons.add("amount");
            manual = true;
        }

        if (amount > allowedManual) {
            reasons.add("amount");
            prohibited = true;
        }

        String info = String.join(", ", reasons);

        if (prohibited) {
            transactionRepository.save(new TransactionFeedback(request.getAmount(),
                    request.getIp(),
                    request.getNumber(),
                    request.getRegion(),
                    request.getDate(),
                    "PROHIBITED"));
            return new TransactionResult(TransactionResultType.PROHIBITED, info);
        }
        if (allowed) {
            transactionRepository.save(new TransactionFeedback(request.getAmount(),
                    request.getIp(),
                    request.getNumber(),
                    request.getRegion(),
                    request.getDate(),
                    "ALLOWED"));
            return new TransactionResult(TransactionResultType.ALLOWED, info);
        }
        else {
            transactionRepository.save(new TransactionFeedback(request.getAmount(),
                    request.getIp(),
                    request.getNumber(),
                    request.getRegion(),
                    request.getDate(),
                    "MANUAL_PROCESSING"));
            return new TransactionResult(TransactionResultType.MANUAL_PROCESSING, info);
        }

    }

    @Transactional
    public TransactionFeedback putFeedback(Map<String, String> trIDAndFeedback) {
        long transactionId = Long.parseLong(trIDAndFeedback.get("transactionId"));
        String feedback = trIDAndFeedback.get("feedback");

        if (!feedback.equals("ALLOWED") && !feedback.equals("MANUAL_PROCESSING") && !feedback.equals("PROHIBITED")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        Optional<TransactionFeedback> feedbackRequest = transactionRepository.findTransactionFeedbackByTransactionId(transactionId);

        if (feedbackRequest.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction is not found");
        } else if (feedbackRequest.get().getResult().equals(feedback)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY);
        } else if (!feedbackRequest.get().getFeedback().equals("")) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }

        changeFeedbackAndLimit(feedbackRequest.get(), feedback);
        transactionRepository.save(feedbackRequest.get());
        return feedbackRequest.get();

    }

    public List<TransactionFeedback> getTransactionsList() {
        return transactionRepository.getAllByOrderByTransactionIdAsc();
    }

    public List<TransactionFeedback> getTransactionsListForSpecifiedNumber(String number) {
        if (!CardChecker.isValidCreditCardNumber(number)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Wrong card format");
        }

        List<TransactionFeedback> returnList = transactionRepository.findAllByNumberOrderByTransactionIdAsc(number);

        if (returnList.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        } else {
            return returnList;
        }
    }

    // ===================================== IP METHODS =========================================
    public IpEntity postSuspiciousIp(IpEntity ip) {
        Optional<IpEntity> optionalIp = ipRepository.findIpEntityByIp(ip.getIp());

        if (optionalIp.isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ip is already in database");
        }

        ipRepository.save(ip);
        return ip;
    }


    public Map<String, String> deleteSuspiciousIp(String ip) {
        Optional<IpEntity> optionalIp = ipRepository.findIpEntityByIp(ip);

        if (!isIpNumberValid(ip)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Wrong Ip address");
        }

        if (optionalIp.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ip was not found in database");
        }



        ipRepository.delete(optionalIp.get());
        return Map.of("status", "IP " + ip + " successfully removed!");

    }

    public List<IpEntity> getSuspiciousIpList() {
        return ipRepository.getAllByOrderByIdAsc();
    }

    // ===================================== CARD METHODS =========================================

    public Card postStolenCard(Card card) {
        Optional<Card> optionalCard = cardRepository.findCardByNumber(card.getNumber());

        if (optionalCard.isPresent() && optionalCard.get().isLocked()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Card is already in database");
        }

        if (!isCardNumberValid(card.getNumber())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Wrong card number format");
        }

        Card cardToSave;

        if (cardRepository.findByNumber(card.getNumber()).isPresent()) {
            if (cardRepository.existsByNumberAndLockedTrue(card.getNumber())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT);
            } else {
                cardToSave = cardRepository.findByNumber(card.getNumber()).get();
                cardToSave.setLocked(true);
            }
        } else {
            cardToSave = new Card(card.getNumber(), true);
        }

        cardRepository.save(cardToSave);
        return cardToSave;
    }

    public Map<String, String> deleteStolenCard(String cardNumber) {
        Optional<Card> optionalCard = cardRepository.findCardByNumber(cardNumber);

        if (!isCardNumberValid(cardNumber)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Wrong card number format");
        }

        if (optionalCard.isEmpty() || !optionalCard.get().isLocked()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Card was not found in database");
        }

        if (optionalCard.get().isLocked()) {
            optionalCard.get().setLocked(false);
            cardRepository.save(optionalCard.get());
        }

        return Map.of("status", "Card " + cardNumber + " successfully removed!");
    }

    public List<Card> getStolenCardList() {
        return cardRepository.findAllByLockedTrue();
    }

    // ===================================== OTHER METHODS =========================================
    private boolean isCardNumberValid(String cardNumber) {
        return CardChecker.isValidCreditCardNumber(cardNumber);
    }

    private boolean isIpNumberValid(String ipNumber) {
        return ipNumber.matches(IPV4_PATTERN);
    }


    private void changeFeedbackAndLimit(TransactionFeedback transaction, String feedback) {

        String trResult = transaction.getResult();
        Card card = cardRepository.findByNumber(transaction.getNumber())
                .orElseThrow(AssertionError::new);

        int increasedAllowed = (int) Math.ceil(0.8 * card.getAllowedLimit()+ 0.2 * transaction.getAmount());
        int decreasedAllowed = (int) Math.ceil(0.8 * card.getAllowedLimit() - 0.2 * transaction.getAmount());
        int increasedManual = (int) Math.ceil(0.8 * card.getManualLimit() + 0.2 * transaction.getAmount());
        int decreasedManual = (int) Math.ceil(0.8 * card.getManualLimit() - 0.2 * transaction.getAmount());

        if (feedback.equals("MANUAL_PROCESSING") && trResult.equals("ALLOWED")) {
            card.setAllowedLimit(decreasedAllowed);
        } else if (feedback.equals("PROHIBITED") && trResult.equals("ALLOWED")) {
            card.setAllowedLimit(decreasedAllowed);
            card.setManualLimit(decreasedManual);
        } else if (feedback.equals("ALLOWED") && trResult.equals("MANUAL_PROCESSING")) {
            card.setAllowedLimit(increasedAllowed);
        } else if (feedback.equals("PROHIBITED") && trResult.equals("MANUAL_PROCESSING")) {
            card.setManualLimit(decreasedManual);
        } else if (feedback.equals("ALLOWED") && trResult.equals("PROHIBITED")) {
            card.setAllowedLimit(increasedAllowed);
            card.setManualLimit(increasedManual);
        } else if (feedback.equals("MANUAL_PROCESSING") && trResult.equals("PROHIBITED")) {
            card.setManualLimit(increasedManual);
        }

        transaction.setFeedback(feedback);
        cardRepository.save(card);
    }

}