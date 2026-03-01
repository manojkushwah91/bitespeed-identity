package com.bitespeed.identity;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ContactRepository extends JpaRepository<Contact, Integer> {

    @Query("""
        SELECT c FROM Contact c
        WHERE c.deletedAt IS NULL
        AND (
            (:email IS NOT NULL AND c.email = :email)
            OR
            (:phone IS NOT NULL AND c.phoneNumber = :phone)
        )
    """)
    List<Contact> findActiveByEmailOrPhone(
        @Param("email") String email,
        @Param("phone") String phone
    );

    List<Contact> findByIdOrLinkedId(Integer id, Integer linkedId);
}
