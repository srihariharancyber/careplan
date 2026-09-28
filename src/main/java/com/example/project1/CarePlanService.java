package com.example.project1;

import com.example.project1.Dtos.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@Transactional
public class CarePlanService {
    private static final long OVERDUE_AFTER_HOURS = 1;
    private static final long MAX_RANGE_DAYS = 92;

    private final PatientRepository patients;
    private final MedicineRepository medicines;
    private final ScheduleRepository schedules;
    private final DoseLogRepository logs;

    public CarePlanService(PatientRepository patients, MedicineRepository medicines, ScheduleRepository schedules,
            DoseLogRepository logs) {
        this.patients = patients;
        this.medicines = medicines;
        this.schedules = schedules;
        this.logs = logs;
    }

    // ---------------- Patients ----------------
    private Patient patient(Long id) {
        return patients.findById(id).orElseThrow(() -> ApiException.notFound("Patient " + id + " not found"));
    }

    private PatientResponse map(Patient p) {
        return new PatientResponse(p.getId(), p.getName(), p.getAge(), p.getCaregiverName(), p.getPhone());
    }

    private void apply(Patient p, PatientRequest r) {
        p.setName(r.name().trim());
        p.setAge(r.age());
        p.setCaregiverName(r.caregiverName().trim());
        p.setPhone(r.phone());
    }

    @Transactional(readOnly = true)
    public List<PatientResponse> listPatients(String name) {
        List<Patient> l = (name == null || name.isBlank()) ? patients.findAll()
                : patients.findByNameContainingIgnoreCase(name.trim());
        return l.stream().map(this::map).toList();
    }

    @Transactional(readOnly = true)
    public List<PatientResponse> getAllPatients() {
        return patients.findAll().stream().map(this::map).toList();
    }

    @Transactional(readOnly = true)
    public PatientResponse getPatient(Long id) {
        return map(patient(id));
    }

    public PatientResponse createPatient(PatientRequest r) {
        Patient p = new Patient();
        apply(p, r);
        return map(patients.save(p));
    }

    public PatientResponse updatePatient(Long id, PatientRequest r) {
        Patient p = patient(id);
        apply(p, r);
        return map(p);
    }

    public void deletePatient(Long id) {
        patients.delete(patient(id));
    }

    // ---------------- Medicines ----------------
    private MedicineResponse map(Medicine m) {
        List<LocalTime> times = m.getSchedules().stream().map(Schedule::getScheduledTime).sorted().toList();
        return new MedicineResponse(m.getId(), m.getPatient().getId(), m.getName(), m.getDosage(),
                m.getFrequency(), times, m.getStartDate(), m.getEndDate());
    }

    private void checkDates(LocalDate start, LocalDate end) {
        if (end != null && end.isBefore(start))
            throw ApiException.badRequest("endDate cannot be before startDate");
    }

    public MedicineResponse addMedicine(Long patientId, MedicineRequest r) {
        Patient p = patient(patientId);
        checkDates(r.startDate(), r.endDate());
        List<LocalTime> times = r.times().stream().map(t -> t.truncatedTo(ChronoUnit.MINUTES)).distinct().sorted()
                .toList();
        if (times.size() != r.frequency())
            throw ApiException.badRequest(
                    "frequency (" + r.frequency() + ") must match the number of distinct times (" + times.size() + ")");
        Medicine m = new Medicine();
        m.setPatient(p);
        m.setName(r.name().trim());
        m.setDosage(r.dosage().trim());
        m.setFrequency(r.frequency());
        m.setStartDate(r.startDate());
        m.setEndDate(r.endDate());
        for (LocalTime t : times) {
            Schedule s = new Schedule();
            s.setMedicine(m);
            s.setScheduledTime(t);
            m.getSchedules().add(s);
        }
        return map(medicines.save(m));
    }

    @Transactional(readOnly = true)
    public List<MedicineResponse> listMedicines(Long patientId) {
        patient(patientId);
        return medicines.findByPatientId(patientId).stream().map(this::map).toList();
    }

    public MedicineResponse updateMedicine(Long id, MedicineUpdate r) {
        Medicine m = medicines.findById(id).orElseThrow(() -> ApiException.notFound("Medicine " + id + " not found"));
        checkDates(r.startDate(), r.endDate());
        m.setName(r.name().trim());
        m.setDosage(r.dosage().trim());
        m.setStartDate(r.startDate());
        m.setEndDate(r.endDate());
        return map(m);
    }

    public void deleteMedicine(Long id) {
        medicines.delete(
                medicines.findById(id).orElseThrow(() -> ApiException.notFound("Medicine " + id + " not found")));
    }

    // ---------------- Doses ----------------
    private DoseSlot slot(Medicine m, Schedule s, LocalDate d, DoseLog l, LocalDateTime now) {
        Dtos.SlotStatus st;
        if (l != null)
            st = l.getStatus() == DoseStatus.TAKEN ? Dtos.SlotStatus.TAKEN : Dtos.SlotStatus.MISSED;
        else if (now.isAfter(d.atTime(s.getScheduledTime()).plusHours(OVERDUE_AFTER_HOURS)))
            st = Dtos.SlotStatus.OVERDUE;
        else
            st = Dtos.SlotStatus.PENDING;
        return new DoseSlot(s.getId(), m.getId(), m.getName(), m.getDosage(), d, s.getScheduledTime(), st,
                l == null ? null : l.getId());
    }

    private List<DoseSlot> slots(Long pid, LocalDate from, LocalDate to) {
        if (to.isBefore(from))
            throw ApiException.badRequest("'to' cannot be before 'from'");
        if (ChronoUnit.DAYS.between(from, to) > MAX_RANGE_DAYS)
            throw ApiException.badRequest("Date range too large (max " + MAX_RANGE_DAYS + " days)");
        LocalDateTime now = LocalDateTime.now();
        Map<String, DoseLog> logged = new HashMap<>();
        for (DoseLog l : logs.findForPatient(pid, from, to))
            logged.put(l.getSchedule().getId() + "|" + l.getDoseDate(), l);
        List<DoseSlot> out = new ArrayList<>();
        for (Medicine m : medicines.findByPatientId(pid)) {
            for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
                if (d.isBefore(m.getStartDate()) || (m.getEndDate() != null && d.isAfter(m.getEndDate())))
                    continue;
                for (Schedule s : m.getSchedules())
                    out.add(slot(m, s, d, logged.get(s.getId() + "|" + d), now));
            }
        }
        out.sort(Comparator.comparing(DoseSlot::date).thenComparing(DoseSlot::time));
        return out;
    }

    @Transactional(readOnly = true)
    public List<DoseSlot> todayDoses(Long pid) {
        patient(pid);
        LocalDate t = LocalDate.now();
        return slots(pid, t, t);
    }

    @Transactional(readOnly = true)
    public List<DoseSlot> doseHistory(Long pid, LocalDate from, LocalDate to) {
        patient(pid);
        return slots(pid, from, to);
    }

    /**
     * Missed = explicitly logged MISSED, or never logged and already more than 1h
     * overdue.
     */
    @Transactional(readOnly = true)
    public List<DoseSlot> missedDoses(Long pid, LocalDate from, LocalDate to) {
        patient(pid);
        return slots(pid, from, to).stream()
                .filter(s -> s.status() == Dtos.SlotStatus.MISSED || s.status() == Dtos.SlotStatus.OVERDUE).toList();
    }

    public DoseSlot logDose(DoseLogRequest r) {
        Schedule s = schedules.findById(r.scheduleId())
                .orElseThrow(() -> ApiException.notFound("Schedule " + r.scheduleId() + " not found"));
        Medicine m = s.getMedicine();
        LocalDate d = r.date();
        if (d.isAfter(LocalDate.now()))
            throw ApiException.badRequest("Cannot log a dose for a future date");
        if (d.isBefore(m.getStartDate()) || (m.getEndDate() != null && d.isAfter(m.getEndDate())))
            throw ApiException.badRequest(m.getName() + " is not scheduled on " + d);
        Optional<DoseLog> existing = logs.findByScheduleIdAndDoseDate(s.getId(), d);
        if (existing.isPresent())
            throw ApiException.conflict("This dose (" + m.getName() + " at " + s.getScheduledTime() + " on " + d
                    + ") is already logged as " + existing.get().getStatus());
        DoseLog l = new DoseLog();
        l.setSchedule(s);
        l.setDoseDate(d);
        l.setStatus(r.status());
        l.setLoggedAt(LocalDateTime.now());
        try {
            logs.saveAndFlush(l);
        } catch (DataIntegrityViolationException e) { // race: DB unique constraint caught a duplicate
            throw ApiException.conflict("This dose slot has already been logged");
        }
        return slot(m, s, d, l, LocalDateTime.now());
    }

    // ---------------- Alerts & stats ----------------
    private Alert alert(Patient p, DoseSlot s) {
        return new Alert(p.getId(), p.getName(), s,
                s.medicineName() + " (" + s.dosage() + ") was due at " + s.time().truncatedTo(ChronoUnit.MINUTES)
                        + " and is overdue");
    }

    @Transactional(readOnly = true)
    public List<Alert> alerts(Long pid) {
        Patient p = patient(pid);
        LocalDate t = LocalDate.now();
        return slots(pid, t, t).stream().filter(s -> s.status() == Dtos.SlotStatus.OVERDUE).map(s -> alert(p, s))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Alert> allAlerts() {
        List<Alert> out = new ArrayList<>();
        LocalDate t = LocalDate.now();
        for (Patient p : patients.findAll())
            slots(p.getId(), t, t).stream().filter(s -> s.status() == Dtos.SlotStatus.OVERDUE)
                    .forEach(s -> out.add(alert(p, s)));
        return out;
    }

    @Transactional(readOnly = true)
    public Map<String, Long> stats() {
        long taken = 0, pending = 0, overdue = 0, missed = 0;
        LocalDate t = LocalDate.now();
        for (Patient p : patients.findAll())
            for (DoseSlot s : slots(p.getId(), t, t))
                switch (s.status()) {
                    case TAKEN -> taken++;
                    case PENDING -> pending++;
                    case OVERDUE -> overdue++;
                    case MISSED -> missed++;
                }
        Map<String, Long> m = new LinkedHashMap<>();
        m.put("patients", patients.count());
        m.put("medicines", medicines.count());
        m.put("takenToday", taken);
        m.put("pendingToday", pending);
        m.put("overdueToday", overdue);
        m.put("missedToday", missed);
        return m;
    }
}
