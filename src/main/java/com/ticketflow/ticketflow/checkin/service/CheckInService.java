package com.ticketflow.ticketflow.checkin.service;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.ticketflow.ticketflow.common.error.ConflictException;
import com.ticketflow.ticketflow.security.CurrentUserProvider;
import com.ticketflow.ticketflow.ticket.domain.Ticket;
import com.ticketflow.ticketflow.ticket.domain.TicketStatus;
import com.ticketflow.ticketflow.ticket.repository.TicketRepository;
import com.ticketflow.ticketflow.ticket.service.TicketService;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Instant;

@Service
public class CheckInService {
    private final TicketService ticketService;
    private final TicketRepository ticketRepository;
    private final CurrentUserProvider currentUser;

    public CheckInService(TicketService ticketService, TicketRepository ticketRepository, CurrentUserProvider currentUser) {
        this.ticketService = ticketService;
        this.ticketRepository = ticketRepository;
        this.currentUser = currentUser;
    }

    public void checkIn(byte[] pngBytes) throws IOException, NotFoundException {
        BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(pngBytes));
        var source = new BufferedImageLuminanceSource(bufferedImage);
        var bitmap = new BinaryBitmap(new HybridBinarizer(source));
        String payload = new MultiFormatReader().decode(bitmap).getText();

        int updateSuccess = ticketRepository.validateTicket(Instant.now(), currentUser.currentUserId(), payload);
        if (updateSuccess > 0) {
            return;
        }
        Ticket ticket = ticketRepository.findByUuidCode(payload)
                .orElseThrow(() -> new com.ticketflow.ticketflow.common.error.NotFoundException("Ticket not found"));
        switch(ticket.getStatus()) {
            case USED -> throw new ConflictException("Ticket has already been used");
            case REFUNDED ->  throw new ConflictException("Ticket has already been refunded");
            case VOID ->  throw new ConflictException("Ticket has already been void");
        }
        if (ticket.getStatus() != TicketStatus.VALID){
            throw new com.ticketflow.ticketflow.common.error.ConflictException("Ticket is not valid");
        }
    }
}
