package com.vasudev.shopsphere.service;

import com.vasudev.shopsphere.entity.Address;
import com.vasudev.shopsphere.repository.AddressRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class AddressService {

    private final AddressRepository addressRepository;

    public AddressService(AddressRepository addressRepository) {
        this.addressRepository = addressRepository;
    }

    public Address saveAddress(Address address) {
        return addressRepository.save(address);
    }

    public Optional<Address> getAddressById(Long id) {
        return addressRepository.findById(id);
    }

    public List<Address> getAddressesByType(String addressType) {
        return addressRepository.findByAddressType(addressType);
    }

    public List<Address> getDefaultAddresses() {
        return addressRepository.findByIsDefaultTrue();
    }

    public List<Address> getAllAddresses() {
        return addressRepository.findAll();
    }

    public void deleteAddress(Long id) {
        addressRepository.deleteById(id);
    }
}
