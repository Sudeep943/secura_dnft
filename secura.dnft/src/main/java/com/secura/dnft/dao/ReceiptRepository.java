package com.secura.dnft.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import com.secura.dnft.entity.Receipt;
import com.secura.dnft.entity.ReceiptId;

public interface ReceiptRepository extends JpaRepository<Receipt, ReceiptId> {

	List<Receipt> findByAprmtIdAndReceiptId(String aprmtId, String receiptId);

	List<Receipt> findByReceiptId(String receiptId);

	long countByAprmtIdAndReceiptIdStartingWith(String aprmtId, String receiptIdPrefix);

}
