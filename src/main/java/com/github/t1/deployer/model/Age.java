package com.github.t1.deployer.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Value;

import java.time.Duration;

import static lombok.AccessLevel.PRIVATE;

/** A human readable {@link Duration} */
@Value
@NoArgsConstructor(access = PRIVATE, force = true)
@RequiredArgsConstructor
@JsonSerialize(using = ToStringSerializer.class)
public class Age {
    public static Age ofMinutes(int minutes) { return new Age(Duration.ofMinutes(minutes)); }

    @JsonCreator public Age(String string) { this.duration = parseDuration(string); }

    private static final long SECONDS_PER_MINUTE = 60;
    private static final long NANOS_PER_MILLI = 1_000_000L;
    private static final long MILLIS_PER_SECOND = 1000;
    private static final long NANOS_PER_SECOND = NANOS_PER_MILLI * MILLIS_PER_SECOND;

    private static Duration parseDuration(String string) {
        String[] split = string.split(" ", 2);
        if (split.length < 2)
            throw new IllegalArgumentException("missing duration type literal in '" + string + "'");
        int amount = Integer.parseUnsignedInt(split[0]);
        switch (split[1]) {
        case "milliseconds":
        case "millisecond":
        case "millis":
        case "milli":
        case "ms":
            return Duration.ofMillis(amount);
        case "seconds":
        case "second":
        case "s":
            return Duration.ofSeconds(amount);
        case "minutes":
        case "minute":
        case "min":
            return Duration.ofMinutes(amount);
        default:
            throw new IllegalArgumentException("unknown duration type literal '" + split[1] + "'");
        }
    }

    private final Duration duration;

    @Override public String toString() {
        long nanos = duration.getNano();
        long seconds = duration.getSeconds();
        if (nanos == 0) {
            if (seconds % SECONDS_PER_MINUTE == 0)
                return seconds / SECONDS_PER_MINUTE + " min";
            else
                return seconds + " s";
        } else if (nanos % NANOS_PER_MILLI == 0) {
            return (nanos / NANOS_PER_MILLI + seconds * MILLIS_PER_SECOND) + " ms";
        } else {
            return (nanos + seconds * NANOS_PER_SECOND) + " ns";
        }
    }

    public long asMinutes() {
        return duration.getSeconds() / SECONDS_PER_MINUTE + duration.getNano();
    }
}
