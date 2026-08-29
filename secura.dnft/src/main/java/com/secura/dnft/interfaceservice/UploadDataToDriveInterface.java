package com.secura.dnft.interfaceservice;

public interface UploadDataToDriveInterface {

	String uploadDataToDrive(String fileData,
	        String fileType,
	        String uniqueId,
	        String apartmentId,String flatId);
	
	String getFileFromDrive(String path);
}
