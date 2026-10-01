package com.wingsup.warehouse.repository;

import com.wingsup.warehouse.model.AppUser;
import com.wingsup.warehouse.model.SpecialRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpecialRequestRepository
        extends JpaRepository<SpecialRequest, Long> {

    List<SpecialRequest>
    findByUserOrderByUpdatedAtDesc(AppUser user);

    List<SpecialRequest>
    findAllByOrderByUpdatedAtDesc();
}