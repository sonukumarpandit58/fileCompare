package com.ims.bpcluat.model.AlpModels.SaleModel;

import java.io.Serializable;
import java.util.List;

public class Program implements Serializable {
    private String program;
    private List<ProgramWallet> programWallet;
    private String accountNumber;
    private String cardNumber;
    private String programID;

    public Program(String accountNumber, String cardNumber, String programID) {

        this.accountNumber = accountNumber;
        this.cardNumber = cardNumber;
        this.programID = programID;
    }

    public String getProgram() {
        return program;
    }

    public void setProgram(String program) {
        this.program = program;
    }

    public List<ProgramWallet> getProgramWallet() {
        return programWallet;
    }

    public void setProgramWallet(List<ProgramWallet> programWallet) {
        this.programWallet = programWallet;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public String getProgramID() {
        return programID;
    }

    public void setProgramID(String programID) {
        this.programID = programID;
    }
}
