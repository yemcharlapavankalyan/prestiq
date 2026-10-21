package org.prestiq.notification.domain.entity;

public enum ReminderWindow {
    DAYS_30(30, "30_DAYS_BEFORE"),
    DAYS_7(7, "7_DAYS_BEFORE"),
    DAY_1(1, "1_DAY_BEFORE");

    private final int days;
    private final String windowCode;

    ReminderWindow(int days, String windowCode) {
        this.days = days;
        this.windowCode = windowCode;
    }

    public int getDays() {
        return days;
    }

    public String getWindowCode() {
        return windowCode;
    }

    public static ReminderWindow fromDays(int days) {
        for (ReminderWindow window : values()) {
            if (window.days == days) {
                return window;
            }
        }
        throw new IllegalArgumentException("Unsupported reminder window: " + days + " days");
    }
}
