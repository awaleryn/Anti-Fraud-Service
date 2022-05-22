package antifraud.transaction;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<TransactionFeedback, Long> {

    Optional<TransactionFeedback> findTransactionFeedbackByTransactionId(long id);

    List<TransactionFeedback> getAllByOrderByTransactionIdAsc();

    List<TransactionFeedback> findAllByNumberOrderByTransactionIdAsc(String number);


}
