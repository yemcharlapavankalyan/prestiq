package org.prestiq.notification.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class ReminderProperties {

    @Value("${prestiq.notifications.batch-size:50}")
    private int batchSize;

    @Value("${prestiq.notifications.reminder-windows:30,7,1}")
    private String reminderWindows;

    public int getBatchSize() {
        return Math.max(1, batchSize);
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public String getReminderWindows() {
        return reminderWindows;
    }

    public void setReminderWindows(String reminderWindows) {
        this.reminderWindows = reminderWindows;
    }

    public List<Integer> getParsedWindows() {
        if (reminderWindows == null || reminderWindows.isBlank()) {
            return List.of(30, 7, 1);
        }
        return Arrays.stream(reminderWindows.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Integer::parseInt)
                .sorted((a, b) -> Integer.compare(b, a)) // descending order: 30, 7, 1
                .toList();
    }
}
