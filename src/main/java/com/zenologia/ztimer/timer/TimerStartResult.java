package com.zenologia.ztimer.timer;

public class TimerStartResult {

    public enum Type {
        STARTED,
        ALREADY_RUNNING,
        REPLACED
    }

    private final Type type;
    private final String timerId;
    private final String previousTimerId;

    private TimerStartResult(Type type, String timerId, String previousTimerId) {
        this.type = type;
        this.timerId = timerId;
        this.previousTimerId = previousTimerId;
    }

    public static TimerStartResult started(String timerId) {
        return new TimerStartResult(Type.STARTED, timerId, null);
    }

    public static TimerStartResult alreadyRunning(String timerId) {
        return new TimerStartResult(Type.ALREADY_RUNNING, timerId, null);
    }

    public static TimerStartResult replaced(String timerId, String previousTimerId) {
        return new TimerStartResult(Type.REPLACED, timerId, previousTimerId);
    }

    public Type getType() {
        return type;
    }

    public String getTimerId() {
        return timerId;
    }

    public String getPreviousTimerId() {
        return previousTimerId;
    }
}
