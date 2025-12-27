package tech.kayys.wayang.workflow.scheduler.dto;

public enum ScheduleType {
    CRON, // Cron expression
    INTERVAL, // Fixed interval (milliseconds)
    CALENDAR, // Calendar-based (business days)
    ONE_TIME // Execute once
}