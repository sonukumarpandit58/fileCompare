package com.ims.bpcluat.model.AlpModels.SaleModel;

import java.io.Serializable;
import java.util.List;

public class ProgramOutput implements Serializable {
    private String redirect;
    private List<Program> programs;
    private String status;
    private String statusCode;

    public String getRedirect() { return redirect; }
    public void setRedirect(String redirect) { this.redirect = redirect; }

    public List<Program> getPrograms() { return programs; }
    public void setPrograms(List<Program> programs) { this.programs = programs; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getStatusCode() { return statusCode; }
    public void setStatusCode(String statusCode) { this.statusCode = statusCode; }
}
