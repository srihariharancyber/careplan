package com.example.project1;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

public final class Dtos {
    private Dtos() { }

    /** TAKEN/MISSED come from a log entry; OVERDUE = unlogged and >1h past due; PENDING = not due yet / within grace hour. */
    public enum SlotStatus { TAKEN, MISSED, PENDING, OVERDUE }

    public record PatientRequest(
            @NotBlank(message = "name is required") String name,
            @NotNull(message = "age is required") @Min(value = 1, message = "age must be 1-130") @Max(130) Integer age,
            @NotBlank(message = "caregiverName is required") String caregiverName,
            String phone) { }

    public record PatientResponse(Long id, String name, Integer age, String caregiverName, String phone) { }

    public record MedicineRequest(
            @NotBlank(message = "name is required") String name,
            @NotBlank(message = "dosage is required") String dosage,
            @NotNull(message = "frequency is required") @Min(1) @Max(12) Integer frequency,
            @NotEmpty(message = "at least one time is required") List<@NotNull LocalTime> times,
            @NotNull(message = "startDate is required") LocalDate startDate,
            LocalDate endDate) { }

    public record MedicineUpdate(
            @NotBlank String name,
            @NotBlank String dosage,
            @NotNull LocalDate startDate,
            LocalDate endDate) { }

    public record MedicineResponse(Long id, Long patientId, String name, String dosage, Integer frequency,
                                   List<LocalTime> times, LocalDate startDate, LocalDate endDate) { }

    public record DoseLogRequest(
            @NotNull(message = "scheduleId is required") Long scheduleId,
            @NotNull(message = "date is required") LocalDate date,
            @NotNull(message = "status must be TAKEN or MISSED") DoseStatus status) { }

    public record DoseSlot(Long scheduleId, Long medicineId, String medicineName, String dosage,
                           LocalDate date, LocalTime time, SlotStatus status, Long doseLogId) { }

    public record Alert(Long patientId, String patientName, DoseSlot dose, String message) { }

    public record ApiError(int status, String error, String message, Instant timestamp,
                           Map<String, String> fieldErrors) { }
}
