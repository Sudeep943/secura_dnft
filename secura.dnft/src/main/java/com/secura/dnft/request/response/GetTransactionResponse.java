package com.secura.dnft.request.response;

import java.math.BigDecimal;
import java.util.List;

public class GetTransactionResponse {

	private GenericHeader genericHeader;
	private String message;
	private String messageCode;
	private List<TransactionResponseItem> transactionList;
	private Integer totalPage;
	private Long totalTransaction;
	private BigDecimal totalCredit;
	private BigDecimal totalDebit;

	public GenericHeader getGenericHeader() {
		return genericHeader;
	}

	public void setGenericHeader(GenericHeader genericHeader) {
		this.genericHeader = genericHeader;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getMessageCode() {
		return messageCode;
	}

	public void setMessageCode(String messageCode) {
		this.messageCode = messageCode;
	}

	public List<TransactionResponseItem> getTransactionList() {
		return transactionList;
	}

	public void setTransactionList(List<TransactionResponseItem> transactionList) {
		this.transactionList = transactionList;
	}

	public Integer getTotalPage() {
		return totalPage;
	}

	public void setTotalPage(Integer totalPage) {
		this.totalPage = totalPage;
	}

	public Long getTotalTransaction() {
		return totalTransaction;
	}

	public void setTotalTransaction(Long totalTransaction) {
		this.totalTransaction = totalTransaction;
	}

	public BigDecimal getTotalCredit() {
		return totalCredit;
	}

	public void setTotalCredit(BigDecimal totalCredit) {
		this.totalCredit = totalCredit;
	}

	public BigDecimal getTotalDebit() {
		return totalDebit;
	}

	public void setTotalDebit(BigDecimal totalDebit) {
		this.totalDebit = totalDebit;
	}
}
