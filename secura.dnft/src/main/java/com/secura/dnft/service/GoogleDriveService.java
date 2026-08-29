package com.secura.dnft.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;

import com.google.api.client.http.ByteArrayContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.secura.dnft.generic.bean.SecuraConstants;
import com.secura.dnft.interfaceservice.UploadDataToDriveInterface;


@Service
public class GoogleDriveService implements UploadDataToDriveInterface{


    private final Drive driveService;

    // Root Google Drive Folder ID
    private static final String ROOT_FOLDER_ID = "14rSDQyc_EYv2wvOn4iSSHY_esCJ0SrMi";

    public GoogleDriveService(Drive driveService) {
        this.driveService = driveService;
    }

    /**
     * Upload Base64 file into:
     *
     * ROOT/APARTMENT_ID/FILE_TYPE/UNIQUE_ID.extension
     *
     * @return Relative file path
     */
    @Override
    public String uploadDataToDrive(
            String fileData,
            String fileType,
            String uniqueId,
            String apartmentId,
            String flatId) {

        try {
        	
            StringBuilder filePath = new StringBuilder();
            // Decode Base64
            byte[] fileBytes = decodeBase64(fileData);

            // Detect MIME and Extension
            FileTypeInfo fileTypeInfo = detectFileType(fileData, fileBytes);

            // Find/Create Apartment Folder
            String apartmentFolderId =
                    findOrCreateFolder(apartmentId, ROOT_FOLDER_ID);
            filePath=filePath.append(apartmentId);
            filePath=filePath.append("/");
            // Find/Create File Type Folder
            String fileTypeFolderId =
                    findOrCreateFolder(fileType.toUpperCase(), apartmentFolderId);
            filePath=filePath.append(fileType.toUpperCase());
            filePath=filePath.append("/");
            if(fileType.equals(SecuraConstants.FILE_TYPE_TRANSACTION)) {
            	fileTypeFolderId=findOrCreateFolder(flatId.toUpperCase(), fileTypeFolderId);
            	filePath=filePath.append(flatId.toUpperCase());
                filePath=filePath.append("/");
            }

            // File Name
            String fileName =
                    uniqueId + "." + fileTypeInfo.extension();
            filePath=filePath.append(fileName);
            // Metadata
            File metadata = new File();
            metadata.setName(fileName);
            metadata.setParents(Collections.singletonList(fileTypeFolderId));

            // Upload Content
            ByteArrayContent mediaContent =
                    new ByteArrayContent(
                            fileTypeInfo.mimeType(),
                            fileBytes);

            driveService.files()
                    .create(metadata, mediaContent)
                    .setFields("id,name,mimeType,parents")
                    .execute();

            // Return Relative Path
            return filePath.toString();

        } catch (Exception e) {

            throw new RuntimeException(
                    "Error uploading file to Google Drive", e);
        }
    }


    /**
     * Reads file from Google Drive path
     * and returns Base64 encoded string.
     *
     * Example Input:
     *
     * APRT001/NOTICE/UUID.pdf
     */
    @Override
    public String getFileFromDrive(String path) {

        try {

            String[] parts = path.split("/");

            if (parts.length > 4) {
                throw new IllegalArgumentException(
                        "Invalid path format");
            }
            String flatId=null;
            String fileName=null;
            String apartmentId = parts[0];
            String fileType = parts[1];
            if(fileType.equals(SecuraConstants.FILE_TYPE_TRANSACTION)) {
            	flatId=parts[2];
            	fileName = parts[3];
            }
            else {
            	fileName = parts[2];
            }
           

            // Find Apartment Folder
            String apartmentFolderId =
                    findFolder(apartmentId, ROOT_FOLDER_ID);

            if (apartmentFolderId == null) {
                throw new RuntimeException(
                        "Apartment folder not found");
            }

            // Find File Type Folder
            String fileTypeFolderId =
                    findFolder(fileType, apartmentFolderId);

            if (fileTypeFolderId == null) {
                throw new RuntimeException(
                        "File type folder not found");
            }
            String fileId =null;
            // Find FlatFolder
            if(flatId!=null) {
            	 String flatTypeFolderId =
                         findFile(flatId, fileTypeFolderId);
            fileId =findFile(fileName, flatTypeFolderId);
            }
            else {
            // Find File
            fileId =
                    findFile(fileName, fileTypeFolderId);
            }

            if (fileId == null) {
                throw new RuntimeException(
                        "File not found");
            }

            // Download File
            ByteArrayOutputStream outputStream =
                    new ByteArrayOutputStream();

            driveService.files()
                    .get(fileId)
                    .executeMediaAndDownloadTo(outputStream);

            byte[] fileBytes = outputStream.toByteArray();

            // Convert to Base64
            return Base64.getEncoder()
                    .encodeToString(fileBytes);

        } catch (Exception e) {

            throw new RuntimeException(
                    "Error retrieving file from Google Drive", e);
        }
    }


    /**
     * Find folder inside parent.
     */
    private String findFolder(
            String folderName,
            String parentFolderId) throws IOException {

        String query =
                "name='" + escapeQuery(folderName) + "'"
                        + " and '" + parentFolderId + "' in parents"
                        + " and mimeType='application/vnd.google-apps.folder'"
                        + " and trashed=false";

        FileList result =
                driveService.files()
                        .list()
                        .setQ(query)
                        .setSpaces("drive")
                        .setFields("files(id,name)")
                        .execute();

        List<File> folders = result.getFiles();

        if (folders == null || folders.isEmpty()) {
            return null;
        }

        return folders.get(0).getId();
    }


    /**
     * Find file inside parent folder.
     */
    private String findFile(
            String fileName,
            String parentFolderId) throws IOException {

        String query =
                "name='" + escapeQuery(fileName) + "'"
                        + " and '" + parentFolderId + "' in parents"
                        + " and trashed=false";

        FileList result =
                driveService.files()
                        .list()
                        .setQ(query)
                        .setSpaces("drive")
                        .setFields("files(id,name)")
                        .execute();

        List<File> files = result.getFiles();

        if (files == null || files.isEmpty()) {
            return null;
        }

        return files.get(0).getId();
    }


    /**
     * Find folder.
     * If not present create it.
     */
    private String findOrCreateFolder(
            String folderName,
            String parentFolderId) throws IOException {

        String folderId =
                findFolder(folderName, parentFolderId);

        if (folderId != null) {
            return folderId;
        }

        File folderMetadata = new File();

        folderMetadata.setName(folderName);
        folderMetadata.setMimeType(
                "application/vnd.google-apps.folder");
        folderMetadata.setParents(
                Collections.singletonList(parentFolderId));

        File createdFolder =
                driveService.files()
                        .create(folderMetadata)
                        .setFields("id,name")
                        .execute();

        return createdFolder.getId();
    }


    /**
     * Decode Base64.
     *
     * Supports both:
     *
     * JVBERi0xLjQ...
     *
     * and
     *
     * data:application/pdf;base64,JVBERi0xLjQ...
     */
    private byte[] decodeBase64(String fileData) {

        if (fileData == null || fileData.isBlank()) {
            throw new IllegalArgumentException(
                    "File data cannot be null");
        }

        String base64Data = fileData;

        if (fileData.contains(",")) {
            base64Data =
                    fileData.substring(fileData.indexOf(",") + 1);
        }

        return Base64.getDecoder().decode(base64Data);
    }


    /**
     * Detect extension and MIME type.
     */
    private FileTypeInfo detectFileType(
            String fileData,
            byte[] bytes) {

        if (fileData.startsWith("data:")) {

            String mime =
                    fileData.substring(
                            5,
                            fileData.indexOf(";"));

            return switch (mime) {

                case "application/pdf" ->
                        new FileTypeInfo("pdf", mime);

                case "image/jpeg" ->
                        new FileTypeInfo("jpg", mime);

                case "image/png" ->
                        new FileTypeInfo("png", mime);

                case "application/vnd.ms-excel" ->
                        new FileTypeInfo("xls", mime);

                case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" ->
                        new FileTypeInfo("xlsx", mime);

                default ->
                        new FileTypeInfo("bin", mime);
            };
        }

        /*
         * If plain Base64 without MIME header,
         * detect common signatures.
         */

        if (bytes.length >= 4
                && bytes[0] == '%'
                && bytes[1] == 'P'
                && bytes[2] == 'D'
                && bytes[3] == 'F') {

            return new FileTypeInfo(
                    "pdf",
                    "application/pdf");
        }

        if (bytes.length >= 3
                && (bytes[0] & 0xFF) == 0xFF
                && (bytes[1] & 0xFF) == 0xD8
                && (bytes[2] & 0xFF) == 0xFF) {

            return new FileTypeInfo(
                    "jpg",
                    "image/jpeg");
        }

        if (bytes.length >= 8
                && bytes[0] == (byte) 0x89
                && bytes[1] == 0x50
                && bytes[2] == 0x4E
                && bytes[3] == 0x47) {

            return new FileTypeInfo(
                    "png",
                    "image/png");
        }

        return new FileTypeInfo(
                "bin",
                "application/octet-stream");
    }


    /**
     * Escape Google Drive query.
     */
    private String escapeQuery(String value) {

        return value
                .replace("\\", "\\\\")
                .replace("'", "\\'");
    }


    /**
     * Holds Extension and MIME type.
     */
    private record FileTypeInfo(
            String extension,
            String mimeType) {
    	
    }

}
