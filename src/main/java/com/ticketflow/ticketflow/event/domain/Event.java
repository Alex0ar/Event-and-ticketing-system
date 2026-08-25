package com.ticketflow.ticketflow.event.domain;

import com.ticketflow.ticketflow.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "events")
public class Event extends BaseEntity {
    @Column(nullable = false)
    private Long organizerId;

    @Column(nullable = false)
    private Long venueId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private Instant startsAt;

    @Column(nullable = false)
    private Instant endsAt;

    @Column(nullable = false)
    private Instant salesStartsAt;

    @Column(nullable = false)
    private Instant salesEndsAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventStatus status = EventStatus.DRAFT;

    private String bannerUrl;

    @Version
    private Long version;
}
