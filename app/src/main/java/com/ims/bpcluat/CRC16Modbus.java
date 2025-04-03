package com.ims.bpcluat;

public class CRC16Modbus {

    public static int calculateCRC(byte[] data) {
        int crc = 0xFFFF; // Initial value

        for (byte b : data) {
            crc ^= (b & 0xFF); // XOR byte into least sig. byte of crc

            for (int j = 0; j < 8; j++) { // Loop over each bit
                if ((crc & 0x0001) != 0) { // If the LSB is set
                    crc = (crc >> 1) ^ 0xA001; // Shift right and XOR 0xA001
                } else {
                    crc >>= 1; // Just shift right
                }
            }
        }

        return crc;
    }

    public static void main(String[] args) {
        byte[] data = {0x00, 0x03, 0x26, 0x01, 0x02}; // example data
        int crc = calculateCRC(data);
        System.out.printf("CRC16: %04X\n", crc); // Expected output: DE45
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }
}


