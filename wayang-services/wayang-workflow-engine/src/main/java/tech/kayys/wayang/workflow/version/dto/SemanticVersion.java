package tech.kayys.wayang.workflow.version.dto;

/**
 * SemanticVersion: Immutable semantic version with ordering.
 */
public record SemanticVersion(int major, int minor, int patch, String prerelease)
        implements Comparable<SemanticVersion> {

    public static SemanticVersion parse(String version) {
        String[] parts = version.split("-", 2);
        String[] numbers = parts[0].split("\\.");

        if (numbers.length != 3) {
            throw new IllegalArgumentException(
                    "Invalid semantic version: " + version);
        }

        int major = Integer.parseInt(numbers[0]);
        int minor = Integer.parseInt(numbers[1]);
        int patch = Integer.parseInt(numbers[2]);
        String prerelease = parts.length > 1 ? parts[1] : null;

        return new SemanticVersion(major, minor, patch, prerelease);
    }

    @Override
    public int compareTo(SemanticVersion other) {
        int majorCmp = Integer.compare(this.major, other.major);
        if (majorCmp != 0)
            return majorCmp;

        int minorCmp = Integer.compare(this.minor, other.minor);
        if (minorCmp != 0)
            return minorCmp;

        int patchCmp = Integer.compare(this.patch, other.patch);
        if (patchCmp != 0)
            return patchCmp;

        // Prerelease comparison
        if (this.prerelease == null && other.prerelease == null)
            return 0;
        if (this.prerelease == null)
            return 1;
        if (other.prerelease == null)
            return -1;

        return this.prerelease.compareTo(other.prerelease);
    }

    @Override
    public String toString() {
        return major + "." + minor + "." + patch +
                (prerelease != null ? "-" + prerelease : "");
    }
}
