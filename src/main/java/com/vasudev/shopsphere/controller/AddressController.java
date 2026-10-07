package com.vasudev.shopsphere.controller;

import com.vasudev.shopsphere.entity.Address;
import com.vasudev.shopsphere.service.AddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/addresses")
@Tag(name = "Addresses", description = "User address management operations")
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @PostMapping
    @Operation(summary = "Create address", description = "Creates a new address for a user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Address created successfully")
    })
    public ResponseEntity<Address> createAddress(@RequestBody Address address) {
        Address savedAddress = addressService.saveAddress(address);
        return new ResponseEntity<>(savedAddress, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get address by ID", description = "Retrieves an address by unique address ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Address retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Address not found")
    })
    public ResponseEntity<Address> getAddressById(@Parameter(description = "Address ID", required = true) @PathVariable Long id) {
        return addressService.getAddressById(id)
                .map(address -> new ResponseEntity<>(address, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @GetMapping("/type/{addressType}")
    @Operation(summary = "Get addresses by type", description = "Retrieves addresses matching a specific address type (e.g. SHIPPING, BILLING)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Addresses retrieved successfully")
    })
    public ResponseEntity<List<Address>> getAddressesByType(@Parameter(description = "Address type", required = true) @PathVariable String addressType) {
        List<Address> addresses = addressService.getAddressesByType(addressType);
        return new ResponseEntity<>(addresses, HttpStatus.OK);
    }

    @GetMapping("/default")
    @Operation(summary = "Get default addresses", description = "Retrieves all default addresses")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Default addresses retrieved successfully")
    })
    public ResponseEntity<List<Address>> getDefaultAddresses() {
        List<Address> addresses = addressService.getDefaultAddresses();
        return new ResponseEntity<>(addresses, HttpStatus.OK);
    }

    @GetMapping
    @Operation(summary = "Get all addresses", description = "Retrieves all addresses in the system")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Addresses retrieved successfully")
    })
    public ResponseEntity<List<Address>> getAllAddresses() {
        List<Address> addresses = addressService.getAllAddresses();
        return new ResponseEntity<>(addresses, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete address", description = "Deletes an address by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Address deleted successfully")
    })
    public ResponseEntity<Void> deleteAddress(@Parameter(description = "Address ID", required = true) @PathVariable Long id) {
        addressService.deleteAddress(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
