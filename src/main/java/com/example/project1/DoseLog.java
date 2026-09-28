package com.example.project1;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** One row per (schedule, date). The unique constraint is the DB-level guard against double logging. */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"schedule_id", "dose_date"}))
public class DoseLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "schedule_id", nullable = false)
    private Schedule schedule;

    @Column(name = "dose_date", nullable = false)
    private LocalDate doseDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DoseStatus status;

    @Column(nullable = false)
    private LocalDateTime loggedAt;

    public DoseLog() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Schedule getSchedule() { return schedule; }
    public void setSchedule(Schedule schedule) { this.schedule = schedule; }

    public LocalDate getDoseDate() { return doseDate; }
    public void setDoseDate(LocalDate doseDate) { this.doseDate = doseDate; }

    public DoseStatus getStatus() { return status; }
    public void setStatus(DoseStatus status) { this.status = status; }

    public LocalDateTime getLoggedAt() { return loggedAt; }
    public void setLoggedAt(LocalDateTime loggedAt) { this.loggedAt = loggedAt; }
}
