package antifraud;

import antifraud.request.AntiFraudRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;


@Repository
public interface AntiFraudRepository extends JpaRepository<AntiFraudRequest,Long> {

    List<AntiFraudRequest> findAllByDateBetweenAndNumber(@Param("startDate")LocalDateTime dateStart, @Param("endDate") LocalDateTime dateEnd, String number);

}
