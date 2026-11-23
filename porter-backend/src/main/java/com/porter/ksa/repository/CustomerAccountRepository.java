package com.porter.ksa.repository;

import com.porter.ksa.entity.CustomerAccount;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CustomerAccountRepository extends JpaRepository<CustomerAccount, Long> {

    @EntityGraph(attributePaths = "commsEmailList")
    @Query("select c from CustomerAccount c order by c.id asc")
    List<CustomerAccount> findAllWithCommsEmailList();

    @EntityGraph(attributePaths = "commsEmailList")
    @Query("select c from CustomerAccount c where c.id = :id")
    Optional<CustomerAccount> findByIdWithCommsEmailList(Long id);
}
