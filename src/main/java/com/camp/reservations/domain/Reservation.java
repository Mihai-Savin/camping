package com.camp.reservations.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "reservations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campsite_id", nullable = false)
    private Campsite campsite;

    @NotBlank
    @Column(nullable = false)
    private String guestName;

    @NotBlank
    @Email
    @Column(nullable = false)
    private String guestEmail;

    private String guestPhone;

    @NotNull
    @Column(nullable = false)
    private LocalDate checkIn;

    @NotNull
    @Column(nullable = false)
    private LocalDate checkOut;

    @NotNull
    @Min(1)
    @Column(nullable = false)
    private Integer numberOfGuests;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status;

    @NotNull
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice;

    private String notes;

    @Builder.Default
    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
