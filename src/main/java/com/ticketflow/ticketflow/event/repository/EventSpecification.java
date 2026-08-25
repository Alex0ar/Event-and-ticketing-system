package com.ticketflow.ticketflow.event.repository;

import com.ticketflow.ticketflow.event.domain.Event;
import com.ticketflow.ticketflow.event.domain.EventStatus;
import com.ticketflow.ticketflow.event.domain.TicketTier;
import com.ticketflow.ticketflow.event.domain.Venue;
import com.ticketflow.ticketflow.event.dto.EventSearchFilter;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import javax.xml.stream.EventFilter;
import java.util.ArrayList;
import java.util.List;

public final class EventSpecification {
    private EventSpecification() {}

    public  static Specification<Event> fromFilter(EventSearchFilter f) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(root.get("status").in(EventStatus.PUBLISHED, EventStatus.SOLD_OUT));
            if (f.query() != null && !f.query().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("title")), "%" +  f.query().toLowerCase() + "%"));
            }
            if (f.category() != null && !f.category().isBlank()) {
                predicates.add(cb.equal(root.get("category"), f.category()));
            }
            if (f.startsAfter() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("startsAt"), f.startsAfter()));
            }
            if (f.startsBefore() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("startsAt"), f.startsBefore()));
            }

            //city:  event.venueId IN (select v.id from Venue v where lower(v.city) = ?)
            if (f.city() != null && !f.city().isBlank()) {
                Subquery<Long> sub = query.subquery(Long.class);
                Root<Venue> venue = sub.from(Venue.class);
                sub.select(venue.get("id"))
                        .where(cb.equal(cb.lower(venue.get("city")), f.city().toLowerCase()));
                predicates.add(root.get("venueId").in(sub));
            }

            //price:  EXISTS a tier of this event with price in [min, max]
            if (f.minPrice() != null && f.maxPrice() != null) {
                Subquery<Long> sub = query.subquery(Long.class);
                Root<TicketTier> tier = sub.from(TicketTier.class);
                List<Predicate> tierPreds = new ArrayList<>();
                tierPreds.add(cb.equal(tier.get("eventId"), root.get("id")));
                if (f.minPrice() != null) {
                    tierPreds.add(cb.greaterThanOrEqualTo(tier.get("price"), f.minPrice()));
                }
                if (f.maxPrice() != null) {
                    tierPreds.add(cb.lessThanOrEqualTo(tier.get("price"), f.maxPrice()));
                }
                sub.select(tier.get("id")).where(tierPreds.toArray(new Predicate[0]));
                predicates.add(cb.exists(sub));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
