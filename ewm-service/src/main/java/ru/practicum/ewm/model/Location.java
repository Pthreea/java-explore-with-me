package ru.practicum.ewm.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "locations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Location {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String name;

    @Column(nullable = false)
    private Float lat;

    @Column(nullable = false)
    private Float lon;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private Float radius;
}