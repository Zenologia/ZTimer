package com.zenologia.ztimer.timer;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class PendingTeleport {

    private final String timerId;
    private final List<String> commands;

    public PendingTeleport(String timerId, List<String> commands) {
        this.timerId = timerId;
        this.commands = commands == null ? Collections.emptyList() : Collections.unmodifiableList(commands);
    }

    public String getTimerId() {
        return timerId;
    }

    public List<String> getCommands() {
        return commands;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PendingTeleport)) return false;
        PendingTeleport that = (PendingTeleport) o;
        return Objects.equals(timerId, that.timerId) &&
                Objects.equals(commands, that.commands);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timerId, commands);
    }
}
