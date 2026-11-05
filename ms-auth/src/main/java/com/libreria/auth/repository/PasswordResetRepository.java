package com.libreria.auth.repository;
import com.libreria.auth.model.PasswordReset;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
public interface PasswordResetRepository extends JpaRepository<PasswordReset,String> {
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select p from PasswordReset p where p.tokenHash=:hash")
 Optional<PasswordReset> lockByHash(@Param("hash") String hash);
}
