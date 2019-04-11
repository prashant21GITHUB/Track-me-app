package com.pyb.trackme;

public class ValidationUtils {


    public static boolean isValidName(String name) {
        return name != null && name.matches("(?i)[a-z][a-z0-9_]*");
    }

    public static boolean isValidNumber(String mobile) {
        return mobile != null && mobile.matches("[6-9]{1}[0-9]{9}");
    }

    public static boolean isValidPassword(String pwd) {
        return pwd != null && !pwd.isEmpty();
    }
}
