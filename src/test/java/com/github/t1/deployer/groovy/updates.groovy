import java.util.regex.Pattern

class Updates implements Serializable {
    private static final Pattern VERSION = Pattern.compile('\\[INFO\\] Building .* ([^ ]+)(-SNAPSHOT)?')
    private static final Pattern UPDATE = Pattern.compile('\\[INFO\\] Updated \\$\\{(.*)\\} from (.*) to (.*)')

    static class Update implements Serializable {
        String from, to;

        @Override
        String toString() { from + " -> " + to }
    }

    Map<String, Update> updates = [:]
    String version

    Updates(String mvnOut) {
        String[] lines = mvnOut.split('\n')
        for (int i = 0; i < lines.length; i++) {
            def line = lines[i]
            scanForVersion(line)
            scanForUpdate(line)
        }
    }

    private void scanForVersion(String line) {
        if (this.version == null) {
            def versionMatcher = VERSION.matcher(line)
            if (versionMatcher.matches()) {
                this.version = versionMatcher.group(1)
            }
        }
    }

    private void scanForUpdate(String line) {
        def update = UPDATE.matcher(line)
        if (update.matches()) {
            updates.put(update.group(1), new Update(from: update.group(2), to: update.group(3)))
        }
    }

    public boolean isEmpty() { updates.isEmpty() }

    @Override
    String toString() { updates.toString() }
}
