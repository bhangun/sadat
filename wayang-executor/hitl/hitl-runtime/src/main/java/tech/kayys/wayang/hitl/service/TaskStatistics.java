package tech.kayys.wayang.hitl.service;

record TaskStatistics(
    long activeTasks,
    long completedToday,
    long overdueTasks
) {}