package com.ims.bpcluat.helper;

import org.json.JSONArray;
import org.json.JSONObject;

public class NozzleIDMapper {

    // Function to find and return localMPDID for a given globalNozzleID
    public String getLocalMPDIDForGlobalNozzleID(String jsonResponse, String targetGlobalNozzleID) {
        try {
            // Parse the JSON array from the response string
            JSONArray jsonArray = new JSONArray(jsonResponse);

            // Iterate through each JSON object in the array
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject bayObject = jsonArray.getJSONObject(i);
                String localMPDID = bayObject.getString("localMPDID");
                JSONArray nozzlesArray = bayObject.getJSONArray("nozzles");

                // Iterate through each nozzle object in the nozzles array
                for (int j = 0; j < nozzlesArray.length(); j++) {
                    JSONObject nozzleObject = nozzlesArray.getJSONObject(j);
                    String globalNozzleID = nozzleObject.getString("globalNozzleID");

                    // Check if the globalNozzleID matches the targetGlobalNozzleID
                    if (globalNozzleID.equals(targetGlobalNozzleID)) {
                        // Return the corresponding localMPDID
                        return localMPDID;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Return null if no matching globalNozzleID is found
        return null;
    }

}
