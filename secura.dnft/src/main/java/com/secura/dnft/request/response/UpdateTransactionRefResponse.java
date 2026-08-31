package com.secura.dnft.request.response;

public class UpdateTransactionRefResponse {
    private String message;
    private String messageCode;
	private GenericHeader genericHeader;

    public UpdateTransactionRefResponse(String message, String messageCode) {
        this.message = message;
        this.messageCode = messageCode;
    }

    // Getters and Setters
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getMessageCode() { return messageCode; }
    public void setMessageCode(String messageCode) { this.messageCode = messageCode; }

	public GenericHeader getGenericHeader() {
		return genericHeader;
	}

	public void setGenericHeader(GenericHeader genericHeader) {
		this.genericHeader = genericHeader;
	}
    
}