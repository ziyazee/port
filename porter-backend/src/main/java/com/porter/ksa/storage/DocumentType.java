package com.porter.ksa.storage;

public enum DocumentType {
    REGISTRATION("registrationDocument"),
    VAT("vatDocument"),
    NATIONAL_ADDRESS("nationalAddressDocument");

    private final String storageFilename;

    DocumentType(String storageFilename) {
        this.storageFilename = storageFilename;
    }

    public String storageFilename() {
        return storageFilename;
    }
}
