package com.ticketflow.ticketflow.ticket.api;

import com.ticketflow.ticketflow.ticket.dto.TicketResponse;
import com.ticketflow.ticketflow.ticket.service.TicketService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tockets")
@PreAuthorize("hasRole('CUSTOMER')")
public class TicketController {
    private final TicketService ticketService;
    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping("/listMyTickets")
    public List<TicketResponse> listMyTickets() {
        // DE APELAT listOwnedTickets() DIN TicketService()
    }

    // de construit endpointul pentru generare qr code

}
