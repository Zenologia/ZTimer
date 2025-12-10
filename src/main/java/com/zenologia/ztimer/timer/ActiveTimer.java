package com.zenologia.ztimer.timer;

public class ActiveTimer {

    private final String timerId;
    private final long startMillis;

    public ActiveTimer(String timerId, long startMillis) {
        this.timerId = timerId;
        this.startMillis = startMillis;
    }

    public String getTimerId() {
        return timerId;
    }

    public long getStartMillis() {
        return startMillis;
    }

    public long getElapsedMillis() {
        return System.currentTimeMillis() - startMillis;
    }
}
