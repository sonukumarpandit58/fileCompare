package com.ims.bpcluat.validation;

import java.util.regex.Pattern;

public class VehicleNoValidation {
    public static boolean validateVehicleNo(String vehicleNo) {
        if (vehicleNo.length() > 11 || vehicleNo.length() < 8) {
            return false;
        }
        boolean validSequence = Pattern.matches("^[a-zA-Z0-9]+$", vehicleNo);
        if (!validSequence) {
            return false;
        }
        String stateCode = vehicleNo.substring(0, 2);
        if (!stateCode.matches("^[A-Za-z]+$")) {
            return false;
        }
        String districtCode_1 = vehicleNo.substring(2, 4);
        String districtCode_2 = vehicleNo.substring(2, 3);

        if (isNumber(districtCode_1) || isNumber(districtCode_2)) {
            if (isNumber(districtCode_1)) {
                String rtoSeries_1 = vehicleNo.substring(4, 7);
                String rtoSeries_2 = vehicleNo.substring(4, 6);
                String rtoSeries_3 = vehicleNo.substring(4, 5);

                if (rtoSeries_1.matches("^[A-Za-z]+$") && vehicleNo.length() == 11) {
                    String lastFourDigit = vehicleNo.substring(7, 11);
                    return isNumber(lastFourDigit);
                } else if (rtoSeries_2.matches("^[A-Za-z]+$") && vehicleNo.length() == 10) {
                    String lastFourDigit = vehicleNo.substring(6, 10);
                    return isNumber(lastFourDigit);
                } else if (rtoSeries_3.matches("^[A-Za-z]+$") && vehicleNo.length() == 9) {
                    String lastFourDigit = vehicleNo.substring(5, 9);
                    return isNumber(lastFourDigit);
                } else {
                    return false;
                }
            } else {
                String rtoSeries_1 = vehicleNo.substring(3, 6);
                String rtoSeries_2 = vehicleNo.substring(3, 5);
                String rtoSeries_3 = vehicleNo.substring(3, 4);

                if (rtoSeries_1.matches("^[A-Za-z]+$") && vehicleNo.length() == 10) {
                    String lastFourDigit = vehicleNo.substring(6, 10);
                    return isNumber(lastFourDigit);
                } else if (rtoSeries_2.matches("^[A-Za-z]+$") && vehicleNo.length() == 9) {
                    String lastFourDigit = vehicleNo.substring(5, 9);
                    return isNumber(lastFourDigit);
                } else if (rtoSeries_3.matches("^[A-Za-z]+$") && vehicleNo.length() == 8) {
                    String lastFourDigit = vehicleNo.substring(4, 8);
                    return isNumber(lastFourDigit);
                } else {
                    return false;
                }
            }
        } else {
            return false;
        }
    }

    private static boolean isNumber(String n) {
        return Pattern.matches("^-?\\d+(\\.\\d+)?$", n);
    }

    public static boolean bharatVehicleNoValidation(String vehicleNo) {
        if (vehicleNo.length() > 10) {
            return false;
        }
        boolean validSequence = Pattern.matches("^[a-zA-Z0-9]+$", vehicleNo);
        if (!validSequence) {
            return false;
        }
        String year = vehicleNo.substring(0, 2);
        String bhSeries = vehicleNo.substring(2, 4).toUpperCase();
        String randomFourDigit = vehicleNo.substring(4, 8);
        String lastTwoAlpha = "";
        if(vehicleNo.length() == 9 ){
            lastTwoAlpha = vehicleNo.substring(8, 9);
        }else if(vehicleNo.length() == 10){
            lastTwoAlpha = vehicleNo.substring(8, 10);
        }
        if (!isNumber(year)) {
            return false;
        }

        if (!bhSeries.matches("^[A-Za-z]+$")) {
            return false;
        }
        if (!bhSeries.equals("BH")) {
            return false;
        }
        if (!isNumber(randomFourDigit)) {
            return false;
        }
        if (!lastTwoAlpha.matches("^[A-Za-z]+$")) {
            return false;
        }
        return true;
    }
}
