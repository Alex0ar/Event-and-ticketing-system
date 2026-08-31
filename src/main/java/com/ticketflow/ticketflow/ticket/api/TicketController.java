package com.ticketflow.ticketflow.ticket.api;

import com.ticketflow.ticketflow.ticket.domain.Ticket;
import com.ticketflow.ticketflow.ticket.dto.TicketResponse;
import com.ticketflow.ticketflow.ticket.service.QrCodeService;
import com.ticketflow.ticketflow.ticket.service.TicketService;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tockets")
@PreAuthorize("hasRole('CUSTOMER')")
public class TicketController {
    private final TicketService ticketService;
    private final QrCodeService qrCodeService;

    public TicketController(TicketService ticketService, QrCodeService qrCodeService) {
        this.ticketService = ticketService;
        this.qrCodeService = qrCodeService;
    }

    @GetMapping("/listMyTickets")
    public List<TicketResponse> listMyTickets() {
        return ticketService.listOwnedTickets();
    }

    @GetMapping(value = "/{code}/qr", produces = MediaType.IMAGE_PNG_VALUE)
    public byte[] qr(@PathVariable String code) {
        Ticket ticket = ticketService.getOwnedByCode(code);
        return qrCodeService.toPng(ticket.getUuidCode(), 300);
    }
}
