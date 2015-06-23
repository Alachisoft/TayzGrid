package tangible;

public final class DotNetToJavaStringHelper {

    public static boolean isNullOrEmpty(String string) {
        return string == null || string.equals("");
    }

    public static String join(String separator, String[] stringarray) {
        if (stringarray == null) {
            return null;
        } else {
            return join(separator, stringarray, 0, stringarray.length);
        }
    }

    public static String join(String separator, String[] stringarray, int startindex, int count) {
        String result = "";

        if (stringarray == null) {
            return null;
        }

        for (int index = startindex; index < stringarray.length && index - startindex < count; index++) {
            if (separator != null && index > startindex) {
                result += separator;
            }

            if (stringarray[index] != null) {
                result += stringarray[index];
            }
        }

        return result;
    }

    public static String trimEnd(String string, Character... charsToTrim) {
        if (string == null || charsToTrim == null) {
            return string;
        }

        int lengthToKeep = string.length();
        for (int index = string.length() - 1; index >= 0; index--) {
            boolean removeChar = false;
            if (charsToTrim.length == 0) {
                if (Character.isWhitespace(string.charAt(index))) {
                    lengthToKeep = index;
                    removeChar = true;
                }
            } else {
                for (int trimCharIndex = 0; trimCharIndex < charsToTrim.length; trimCharIndex++) {
                    if (string.charAt(index) == charsToTrim[trimCharIndex]) {
                        lengthToKeep = index;
                        removeChar = true;
                        break;
                    }
                }
            }
            if (!removeChar) {
                break;
            }
        }
        return string.substring(0, lengthToKeep);
    }

    public static String trimStart(String string, Character... charsToTrim) {
        if (string == null || charsToTrim == null) {
            return string;
        }

        int startingIndex = 0;
        for (int index = 0; index < string.length(); index++) {
            boolean removeChar = false;
            if (charsToTrim.length == 0) {
                if (Character.isWhitespace(string.charAt(index))) {
                    startingIndex = index + 1;
                    removeChar = true;
                }
            } else {
                for (int trimCharIndex = 0; trimCharIndex < charsToTrim.length; trimCharIndex++) {
                    if (string.charAt(index) == charsToTrim[trimCharIndex]) {
                        startingIndex = index + 1;
                        removeChar = true;
                        break;
                    }
                }
            }
            if (!removeChar) {
                break;
            }
        }
        return string.substring(startingIndex);
    }

    public static String trim(String string, Character... charsToTrim) {
        return trimEnd(trimStart(string, charsToTrim), charsToTrim);
    }

    public static boolean stringsEqual(String s1, String s2) {
        if (s1 == null && s2 == null) {
            return true;
        } else {
            return s1 != null && s1.equals(s2);
        }
    }
}