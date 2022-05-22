package antifraud.ip;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface IpRepository extends JpaRepository<IpEntity, Long> {

    Optional<IpEntity> findIpEntityByIp(String ip);

    List<IpEntity> getAllByOrderByIdAsc();

    Optional<IpEntity> findByIp(String ip);
}
