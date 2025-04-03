package com.ims.bpcluat.helper;

import android.os.Build;
import java.util.HashMap;
import java.util.Map;

public class OfflineProductHelper {

    // Creating a map for localProductID and product name
    private static final Map<Integer, String> productMap = new HashMap<>();

    static {
        // Populate the map with the product list
        productMap.put(1, "MS");
        productMap.put(2, "SPEED");
        productMap.put(3, "SPEED 97");
        productMap.put(4, "HSD");
        productMap.put(5, "HI-SPD HSD");
        productMap.put(10, "MS E20");
        productMap.put(11, "Speed 97 E20");
        productMap.put(12, "Speed E20");
    }

    // Method to get the product name by localProductID
    public static String getProductName(int localProductID) {
        // For Android N and above, use getOrDefault
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return productMap.getOrDefault(localProductID, "Unknown Product");
        } else {
            // For older Android versions, use get() with a null check
            String productName = productMap.get(localProductID);
            return (productName != null) ? productName : "Unknown Product";
        }
    }

}
