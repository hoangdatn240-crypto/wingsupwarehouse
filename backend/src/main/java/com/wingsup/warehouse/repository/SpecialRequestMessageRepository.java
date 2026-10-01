package com.wingsup.warehouse.repository;

import com.wingsup.warehouse.model.SpecialRequest;
import com.wingsup.warehouse.model.SpecialRequestMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpecialRequestMessageRepository
        extends JpaRepository<SpecialRequestMessage, Long> {

    List<SpecialRequestMessage>
    findByRequestOrderByCreatedAtAsc(
            SpecialRequest request
    );

    long countByRequestAndReadFalse(
            SpecialRequest request
    );
}