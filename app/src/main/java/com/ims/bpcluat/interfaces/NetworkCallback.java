package com.ims.bpcluat.interfaces;

import java.io.IOException;

public interface NetworkCallback {
    void onSuccess(String response);
    void onFailure(IOException e);
}

