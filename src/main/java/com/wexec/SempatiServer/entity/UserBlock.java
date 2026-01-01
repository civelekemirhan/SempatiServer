package com.wexec.SempatiServer.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_blocks", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "blocker_id", "blocked_id" }) // Aynı kişiyi 2 kere engelleyemesin
})
public class UserBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "blocker_id", nullable = false)
    private User blocker; // Engelleyen (Ben)

    @ManyToOne
    @JoinColumn(name = "blocked_id", nullable = false)
    private User blocked; // Engellenen (O)

    private LocalDateTime createdAt;
}