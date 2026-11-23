package com.porter.ksa.repository;

import com.porter.ksa.entity.TransportOrder;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface TransportOrderRepository extends JpaRepository<TransportOrder, Long> {

    @EntityGraph(attributePaths = "customerAccount")
    @Query("select o from TransportOrder o order by o.id desc")
    List<TransportOrder> findAllWithCustomerAccount();

    @EntityGraph(attributePaths = "customerAccount")
    @Query("select o from TransportOrder o where o.id = :id")
    Optional<TransportOrder> findByIdWithCustomerAccount(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "customerAccount")
    @Query("select o from TransportOrder o where o.id = :id")
    Optional<TransportOrder> findByIdWithCustomerAccountForUpdate(Long id);
}
