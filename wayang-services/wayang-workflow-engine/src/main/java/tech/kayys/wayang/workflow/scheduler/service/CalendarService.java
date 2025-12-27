package tech.kayys.wayang.workflow.scheduler.service;

import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/**
 * CalendarService: Business calendar management.
 * 
 * Features:
 * - Holiday tracking
 * - Business day calculation
 * - Custom calendar support per tenant
 * - Region-specific holidays
 */
@ApplicationScoped
public class CalendarService {

    private static final Logger LOG = Logger.getLogger(CalendarService.class);

    // In-memory holiday store (replace with database)
    private final Set<LocalDate> holidays = new HashSet<>();

    /**
     * Check if a date is a business day.
     * Business days are weekdays that are not holidays.
     */
    public boolean isBusinessDay(LocalDate date) {
        // Check if weekend
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            return false;
        }

        // Check if holiday
        return !holidays.contains(date);
    }

    /**
     * Add a holiday to the calendar.
     */
    public void addHoliday(LocalDate date) {
        holidays.add(date);
        LOG.infof("Added holiday: %s", date);
    }

    /**
     * Remove a holiday from the calendar.
     */
    public void removeHoliday(LocalDate date) {
        holidays.remove(date);
        LOG.infof("Removed holiday: %s", date);
    }

    /**
     * Get next business day after given date.
     */
    public LocalDate getNextBusinessDay(LocalDate date) {
        LocalDate next = date.plusDays(1);
        while (!isBusinessDay(next)) {
            next = next.plusDays(1);
        }
        return next;
    }

    /**
     * Calculate number of business days between two dates.
     */
    public int getBusinessDaysBetween(LocalDate start, LocalDate end) {
        int count = 0;
        LocalDate current = start;

        while (!current.isAfter(end)) {
            if (isBusinessDay(current)) {
                count++;
            }
            current = current.plusDays(1);
        }

        return count;
    }
}
