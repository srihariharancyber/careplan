package com.example.project1;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DoseLogRepository extends JpaRepository<DoseLog, Long> {
    Optional<DoseLog> findByScheduleIdAndDoseDate(Long scheduleId, LocalDate doseDate);

    @Query("select d from DoseLog d where d.schedule.medicine.patient.id = :pid "
         + "and d.doseDate between :from and :to")
    List<DoseLog> findForPatient(@Param("pid") Long pid, @Param("from") LocalDate from, @Param("to") LocalDate to);
}
