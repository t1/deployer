class Updates implements Serializable {
    static class Update implements Serializable {
        String from, to

        @Override
        String toString() { from + " -> " + to }
    }

    Map<String, Update> updates = [:]
    String currentVersion

    Updates(String mvnOut) {
        String[] lines = mvnOut.split('\n')
        for (int i = 0; i < lines.length; i++) {
            def line = lines[i]
            scanForVersion(line)
            scanForUpdate(line)
        }
    }

    private void scanForVersion(String line) {
        if (this.currentVersion)
            return
        def match = line =~ /\[INFO\] Building .* ([^ ]+)(-SNAPSHOT)?/
        if (match)
            this.currentVersion = match.group(1)
    }

    private void scanForUpdate(String line) {
        def match = line =~ /\[INFO\] Updated \$\{(?<name>.*)\} from (?<from>.*) to (?<to>.*)/
        if (match)
            updates.put(match.group('name'), new Update(from: match.group('from'), to: match.group('to')))
    }

    public String updateVersion() {
        List<Integer> ints = numeric(currentVersion)
        int totalDiff = updates.values().
                collect { diff(numeric(it.from), numeric(it.to)) }.
                min()
        if (totalDiff >= 0) {
            while (ints.size() <= totalDiff)
                ints += 0
            ints[totalDiff] += 1
            for (int i = totalDiff + 1; i < ints.size(); i++)
                ints[i] = 0;
        }
        return ints.join('.')
    }

    private static int diff(List<Integer> from, List<Integer> to) {
        for (int i = 0; i < from.size(); i++) {
            if (from[i] > to[i])
                throw new IllegalArgumentException('invalid version update: ' + from + " -> " + to)
            if (from[i] < to[i])
                return i
        }
        return (to.size() > from.size()) ? from.size() : Integer.MAX_VALUE
    }

    private static List<Integer> numeric(String version) {
        (version - ~'-SNAPSHOT$').split('\\.').collect { String it -> (it.isInteger()) ? it as Integer : -1 }
    }


    public boolean isEmpty() { updates.isEmpty() }

    @Override
    String toString() { "Updates in ${currentVersion}: ${updates.toString()}" }
}
