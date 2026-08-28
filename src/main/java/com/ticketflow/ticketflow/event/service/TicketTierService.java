package com.ticketflow.ticketflow.event.service;

import com.ticketflow.ticketflow.common.error.NotFoundException;
import com.ticketflow.ticketflow.event.domain.TicketTier;
import com.ticketflow.ticketflow.event.repository.TicketTierRepository;
import org.springframework.stereotype.Service;

@Service
public class TicketTierService {
    private final TicketTierRepository ticketTierRepository;

    public TicketTierService(TicketTierRepository ticketTierRepository) {
        this.ticketTierRepository = ticketTierRepository;
    }

    public boolean checkTierAvailability(Long tierId) {
        TicketTier t = ticketTierRepository.findById(tierId)
                .orElseThrow(() -> new NotFoundException("Tier not found"));
        if (t.getSoldQuantity() + t.getReservedQuantity() == t.getTotalQuantity()) {
            return true;
        }
        return false;
    }
}
