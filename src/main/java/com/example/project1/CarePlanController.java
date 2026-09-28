package com.example.project1;

import com.example.project1.Dtos.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Tag(name = "Care Plan Controller", description = "Endpoints for managing patients, medications, doses, alerts, and stats")
@RestController
@RequestMapping("/api")
public class CarePlanController {
    private final CarePlanService svc;

    public CarePlanController(CarePlanService svc) {
        this.svc = svc;
    }

    // ---- Patients ----
    @Operation(summary = "Get all patients", description = "Returns the complete list of all registered patients in the database")
    @GetMapping({"/patients/all", "/patients/getAll"})
    public List<PatientResponse> getAllPatients() { return svc.getAllPatients(); }

    @Operation(summary = "Search or list patients", description = "Search patients by name or list all patients if name is omitted")
    @GetMapping("/patients")
    public List<PatientResponse> patients(@RequestParam(required = false) String name) { return svc.listPatients(name); }

    @Operation(summary = "Get patient by ID")
    @GetMapping("/patients/{id}")
    public PatientResponse patient(@PathVariable Long id) { return svc.getPatient(id); }

    @Operation(summary = "Register new patient")
    @PostMapping("/patients") @ResponseStatus(HttpStatus.CREATED)
    public PatientResponse createPatient(@Valid @RequestBody PatientRequest r) { return svc.createPatient(r); }

    @Operation(summary = "Update patient by ID")
    @PutMapping("/patients/{id}")
    public PatientResponse updatePatient(@PathVariable Long id, @Valid @RequestBody PatientRequest r) { return svc.updatePatient(id, r); }

    @Operation(summary = "Delete patient by ID")
    @DeleteMapping("/patients/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePatient(@PathVariable Long id) { svc.deletePatient(id); }

    // ---- Medicines (with schedules) ----
    @Operation(summary = "List medicines for a patient")
    @GetMapping("/patients/{id}/medicines")
    public List<MedicineResponse> medicines(@PathVariable Long id) { return svc.listMedicines(id); }

    @Operation(summary = "Add a medicine schedule to a patient")
    @PostMapping("/patients/{id}/medicines") @ResponseStatus(HttpStatus.CREATED)
    public MedicineResponse addMedicine(@PathVariable Long id, @Valid @RequestBody MedicineRequest r) { return svc.addMedicine(id, r); }

    @Operation(summary = "Update medicine details")
    @PutMapping("/medicines/{id}")
    public MedicineResponse updateMedicine(@PathVariable Long id, @Valid @RequestBody MedicineUpdate r) { return svc.updateMedicine(id, r); }

    @Operation(summary = "Delete medicine by ID")
    @DeleteMapping("/medicines/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMedicine(@PathVariable Long id) { svc.deleteMedicine(id); }

    // ---- Doses ----
    @Operation(summary = "Get today's dose schedule for a patient")
    @GetMapping("/patients/{id}/doses/today")
    public List<DoseSlot> today(@PathVariable Long id) { return svc.todayDoses(id); }

    @Operation(summary = "Get dose schedule history within a date range")
    @GetMapping("/patients/{id}/doses")
    public List<DoseSlot> history(@PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) { return svc.doseHistory(id, from, to); }

    @Operation(summary = "Get missed or overdue doses for a patient")
    @GetMapping("/patients/{id}/doses/missed")
    public List<DoseSlot> missed(@PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) { return svc.missedDoses(id, from, to); }

    @Operation(summary = "Log dose status (TAKEN or MISSED)")
    @PostMapping("/dose-logs") @ResponseStatus(HttpStatus.CREATED)
    public DoseSlot logDose(@Valid @RequestBody DoseLogRequest r) { return svc.logDose(r); }

    // ---- Alerts & stats ----
    @Operation(summary = "Get alerts for a patient")
    @GetMapping("/patients/{id}/alerts")
    public List<Alert> alerts(@PathVariable Long id) { return svc.alerts(id); }

    @Operation(summary = "Get all system-wide overdue alerts")
    @GetMapping("/alerts")
    public List<Alert> allAlerts() { return svc.allAlerts(); }

    @Operation(summary = "Get overall dashboard statistics")
    @GetMapping("/stats")
    public Map<String, Long> stats() { return svc.stats(); }
}
