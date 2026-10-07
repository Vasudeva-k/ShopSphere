package com.vasudev.shopsphere.repository;

import com.vasudev.shopsphere.entity.Address;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AddressRepository extends JpaRepository<Address, Long> {

    List<Address> findByAddressType(String addressType);

    List<Address> findByIsDefaultTrue();
}
