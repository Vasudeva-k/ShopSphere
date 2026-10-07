package com.vasudev.shopsphere.repository;

import com.vasudev.shopsphere.entity.Invoice;
import com.vasudev.shopsphere.entity.Order;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    Optional<Invoice> findByOrder(Order order);

    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);
}
