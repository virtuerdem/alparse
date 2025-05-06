/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ttgint.downloadEngine.engines;

import com.ttgint.downloadEngine.common.CommonLibrary;
import com.ttgint.downloadEngine.main.DownloadApp;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.util.Enumeration;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import net.sf.sevenzipjbinding.IInArchive;
import net.sf.sevenzipjbinding.ISequentialOutStream;
import net.sf.sevenzipjbinding.SevenZip;
import net.sf.sevenzipjbinding.SevenZipException;
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream;
import net.sf.sevenzipjbinding.simple.ISimpleInArchive;
import net.sf.sevenzipjbinding.simple.ISimpleInArchiveItem;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.z.ZCompressorInputStream;

/**
 *
 * @author erdigurbuz
 */
public class UnzipOperation {

    public static Boolean unzipAsn1ToCsv(String inputFile) throws IOException {
        String procOutput = "";
        try {
            StringBuilder stringBuilder = new StringBuilder();

            Process proc = Runtime.getRuntime().exec("perl ./STSASN1decoder.pl " + inputFile);
            BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            String line = "";
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line + "\n");
            }
            proc.waitFor();
            reader.close();

            procOutput = stringBuilder.toString();

            File file = new File(inputFile);
            if (file.isDirectory()) {
                for (File each : file.listFiles()) {
                    if (each.getName().endsWith(".asn1")) {
                        each.delete();
                    }
                }
            } else if (file.getName().endsWith(".asn1")) {
                file.delete();
            }

            if (!procOutput.contains("Completed in ")) {
                CommonLibrary.errorLogger("*** Corrupted file deleted: " + inputFile + "\n" + procOutput, 2);
                return false;
            }

            return true;
        } catch (Exception e) {
            CommonLibrary.errorLogger("**** Corrupted file deleted: " + inputFile + "\n" + procOutput, 2);

            File file = new File(inputFile);
            if (file.isDirectory()) {
                for (File each : file.listFiles()) {
                    if (each.getName().endsWith(".asn1")) {
                        each.delete();
                    }
                }
            } else if (file.getName().endsWith(".asn1")) {
                file.delete();
            }

            return false;
        }
    }

    public static String uncompresssTgz(File inFile, String outPath) throws IOException {
        try {
            TarArchiveInputStream tarIn = new TarArchiveInputStream(new GZIPInputStream(new FileInputStream(inFile)));
            TarArchiveEntry entry = null;
            String uncompressedFile = "";

            while ((entry = (TarArchiveEntry) tarIn.getNextEntry()) != null) {
                if (entry.isDirectory()) {

                    File f = new File(outPath + entry.getName());
                    f.mkdirs();
                } else {
                    int count;
                    byte data[] = new byte[1024];

                    FileOutputStream fos = new FileOutputStream(outPath + entry.getName());
                    uncompressedFile = entry.getName();
                    BufferedOutputStream dest = new BufferedOutputStream(fos, 1024);
                    while ((count = tarIn.read(data, 0, 1024)) != -1) {
                        dest.write(data, 0, count);
                    }
                    dest.close();
                }
            }
            tarIn.close();
            inFile.delete();
            return uncompressedFile;
        } catch (IOException ex) {
            CommonLibrary.errorLogger("**** Corrupted file deleted: " + inFile.getName() + " " + ex.getMessage(), 0);
            throw ex;
        }
    }

    public static boolean uncompressTarGz(File inFile, String outPath) throws FileNotFoundException, IOException {
        try {
            FileInputStream fin = new FileInputStream(inFile);
            BufferedInputStream in = new BufferedInputStream(fin);
            GzipCompressorInputStream gzIn = new GzipCompressorInputStream(in);
            TarArchiveInputStream tarIn = new TarArchiveInputStream(gzIn);

            TarArchiveEntry entry = null;

            while ((entry = (TarArchiveEntry) tarIn.getNextEntry()) != null) {
                if (entry.isDirectory()) {

                    File f = new File(outPath + entry.getName());
                    f.mkdirs();
                } else {
                    int count;
                    byte data[] = new byte[1024];

                    FileOutputStream fos = new FileOutputStream(outPath + entry.getName());
                    BufferedOutputStream dest = new BufferedOutputStream(fos, 1024);
                    while ((count = tarIn.read(data, 0, 1024)) != -1) {
                        dest.write(data, 0, count);
                    }
                    dest.close();
                }
            }
            tarIn.close();
            inFile.delete();
            return true;
        } catch (IOException e) {
            CommonLibrary.errorLogger("**** Corrupted file deleted: " + inFile.getName() + " " + e.getMessage(), 0);
            throw e;
        }
    }

    public static void uncompressTarGzInFolder(File tarFile, File dest) throws IOException {
        try {
            dest.mkdir();
            TarArchiveInputStream tarIn = null;
            tarIn = new TarArchiveInputStream(new GzipCompressorInputStream(new BufferedInputStream(new FileInputStream(tarFile))));

            TarArchiveEntry tarEntry = tarIn.getNextTarEntry();
            while (tarEntry != null) {
                File destPath = new File(dest, tarEntry.getName());
                if (tarEntry.isDirectory()) {
                    destPath.mkdirs();
                } else {
                    destPath.createNewFile();
                    byte[] btoRead = new byte[1024];
                    BufferedOutputStream bout = new BufferedOutputStream(new FileOutputStream(destPath));
                    int len = 0;

                    while ((len = tarIn.read(btoRead)) != -1) {
                        bout.write(btoRead, 0, len);
                    }

                    bout.close();
                    btoRead = null;

                }
                tarEntry = tarIn.getNextTarEntry();
            }
            tarIn.close();
        } catch (IOException ex) {
            CommonLibrary.errorLogger("**** Corrupted file deleted: " + tarFile.getName() + " " + ex.getMessage(), 0);
            throw ex;
        }
    }

    public static boolean extract_Z_Files(final String filePath, final String fileName) {
        File file = new File(filePath + fileName);
        final File outputFile = new File(filePath + fileName.substring(0, fileName.length() - 2));
        try {
            try (RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r")) {
                IInArchive inArchive = SevenZip.openInArchive(null, new RandomAccessFileInStream(randomAccessFile));
                ISimpleInArchive simpleInArchive = inArchive.getSimpleInterface();
                ISimpleInArchiveItem files[] = simpleInArchive.getArchiveItems();

                final FileOutputStream out = new FileOutputStream(outputFile);

                files[0].extractSlow(new ISequentialOutStream() {

                    @Override
                    public int write(byte[] data) throws SevenZipException {
                        try {
                            out.write(data);
                        } catch (IOException e) {
                        }
                        return data.length;
                    }
                });
                out.close();
                inArchive.close();
            }
        } catch (FileNotFoundException e) {
            CommonLibrary.errorLogger("**** File Not Found : " + filePath + " " + e.getMessage(), 0);
            outputFile.delete();
            return false;
        } catch (IOException e) {
            CommonLibrary.errorLogger("**** Corrupted file deleted : " + filePath + " " + e.getMessage(), 0);
            outputFile.delete();
            return false;
        } finally {
        }

        file.delete();
        return true;
    }

    public static Boolean unzipGzFile(String inputGzFile) throws IOException {
        try {

            String outPutFileName = inputGzFile.replace(".gz", "");

            byte[] buffer = new byte[1024];

            GZIPInputStream gzis = new GZIPInputStream(new FileInputStream(inputGzFile));

            FileOutputStream out = new FileOutputStream(outPutFileName);

            int len;
            while ((len = gzis.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }

            gzis.close();
            out.close();
            new File(inputGzFile).delete();
            return true;
        } catch (EOFException e) {
            new File(inputGzFile).delete();
            CommonLibrary.errorLogger("**** Corrupted file deleted: " + inputGzFile + " " + e.getMessage(), 2);
            return false;
        }
    }

    public static Boolean unzipGzFile(String inputGzFile, String outputFile) throws IOException {
        try {
            byte[] buffer = new byte[1024];

            GZIPInputStream gzis = new GZIPInputStream(new FileInputStream(inputGzFile));

            FileOutputStream out = new FileOutputStream(outputFile);

            int len;
            while ((len = gzis.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }

            gzis.close();
            out.close();
            new File(inputGzFile).delete();
            return true;
        } catch (IOException e) {
            CommonLibrary.errorLogger("**** Corrupted file deleted: " + inputGzFile + " " + e.getMessage(), 0);
            throw e;
        }
    }

    public static Boolean unzipZFile(String fullFileName) throws IOException {
        try {
            String outPutFileName;
            if (fullFileName.endsWith(".z")) {
                outPutFileName = fullFileName.replace(".z", "");
            } else if (fullFileName.endsWith(".Z")) {
                outPutFileName = fullFileName.replace(".Z", "");
            } else {
                throw new IOException("Not a Z file");
            }
            try (ZCompressorInputStream zzipInputStream = new ZCompressorInputStream(new FileInputStream(new File(fullFileName)))) {
                FileOutputStream output = null;
                byte[] block = new byte[1024];
                int len = 0;
                while ((len = zzipInputStream.read(block)) != -1) {
                    if (output == null) {
                        output = new FileOutputStream(new File(outPutFileName));
                    }
                    output.write(block, 0, len);
                }
                if (output != null) {
                    output.close();
                }
            }

            new File(fullFileName).delete();
            return true;
        } catch (IOException ex) {
            CommonLibrary.errorLogger("**** Corrupted file deleted: " + fullFileName + " " + ex.getMessage(), 0);
            throw ex;
        }
    }

    public static Boolean unzipZFile(String fullFileName, String fullOutpuPath) throws IOException {
        try (ZCompressorInputStream zzipInputStream = new ZCompressorInputStream(new FileInputStream(new File(fullFileName)))) {
            FileOutputStream output = null;
            byte[] block = new byte[1024];
            int len = 0;
            while ((len = zzipInputStream.read(block)) != -1) {
                if (output == null) {
                    output = new FileOutputStream(new File(fullOutpuPath));
                }
                output.write(block, 0, len);
            }
            if (output != null) {
                output.close();
            }
        } catch (IOException ex) {
            CommonLibrary.errorLogger("**** Corrupted file deleted: " + fullFileName + " " + ex.getMessage(), 0);
            throw ex;
        }

        new File(fullFileName).delete();
        return true;
    }

    public static Boolean unzipZipFile(String localFileName) throws IOException {
        File archiveFIle = new File(localFileName);
        ZipArchiveInputStream archive = new ZipArchiveInputStream(new FileInputStream(archiveFIle));
        ZipArchiveEntry entry;
        while ((entry = archive.getNextZipEntry()) != null) {

            if (entry.isDirectory() == false) {
                String newFileName = entry.getName();

                String prefix = "";
                if (localFileName.contains("+")) {
                    prefix = localFileName.split("/")[localFileName.split("/").length - 1].split("\\+")[0] + "+";
                }

                newFileName = newFileName.replace(".zip", "");
                String fullLocalPath = DownloadApp.LOCALFILEPATH + prefix + newFileName;

                File file = new File(fullLocalPath);
                int len = 0;

                byte[] blocks = new byte[1024];
                try {
                    FileOutputStream out = new FileOutputStream(file);

                    while ((len = archive.read(blocks)) != -1) {
                        out.write(blocks, 0, len);
                    }

                    out.close();

                } catch (Exception e) {
                    CommonLibrary.errorLogger("**** Corrupted file deleted: " + localFileName + " " + e.getMessage(), 2);
                    e.printStackTrace();
                }
                archiveFIle.delete();
            }
        }
        archive.close();
        archiveFIle.delete();

        return true;
    }

    public static Boolean unzipZipFile(String localFileName, String localNewFileName) throws IOException {
        File archiveFIle = new File(localFileName);
        ZipArchiveInputStream archive = new ZipArchiveInputStream(new FileInputStream(archiveFIle));
        ZipArchiveEntry entry;
        while ((entry = archive.getNextZipEntry()) != null) {

            if (entry.isDirectory() == false) {
                File file = new File(localNewFileName);
                int len = 0;

                byte[] blocks = new byte[1024];
                try {
                    FileOutputStream out = new FileOutputStream(file);

                    while ((len = archive.read(blocks)) != -1) {
                        out.write(blocks, 0, len);
                    }

                    out.close();

                } catch (Exception e) {
                    CommonLibrary.errorLogger("**** Corrupted file deleted: " + localFileName + " " + e.getMessage(), 2);
                    e.printStackTrace();
                }
                archiveFIle.delete();
            }
        }
        archive.close();
        archiveFIle.delete();

        return true;
    }

    public static Boolean unzipZipFile(String localFileName, String addPostfix, String acceptFileExtension, String discardFileExtension) throws IOException {
        File archiveFIle = new File(localFileName);
        ZipArchiveInputStream archive = new ZipArchiveInputStream(new FileInputStream(archiveFIle));
        ZipArchiveEntry entry;
        while ((entry = archive.getNextZipEntry()) != null) {

            if (!entry.isDirectory()
                    && (!entry.getName().endsWith(discardFileExtension) || discardFileExtension.isBlank())
                    && (entry.getName().endsWith(acceptFileExtension) || acceptFileExtension.isBlank())) {
                String newFileName = entry.getName();

                String prefix = "";
                if (localFileName.contains("+")) {
                    prefix = localFileName.split("/")[localFileName.split("/").length - 1].split("\\+")[0] + "+";
                }

                if (addPostfix.length() > 1) {
                    String fileExtension = "." + newFileName.split("\\.")[newFileName.split("\\.").length - 1];
                    newFileName = newFileName.replace(fileExtension, "_" + addPostfix + fileExtension);
                }

                newFileName = newFileName.replace(".zip", "");
                String fullLocalPath = DownloadApp.LOCALFILEPATH + prefix + newFileName;

                File file = new File(fullLocalPath);
                int len = 0;

                byte[] blocks = new byte[1024];
                try {
                    FileOutputStream out = new FileOutputStream(file);

                    while ((len = archive.read(blocks)) != -1) {
                        out.write(blocks, 0, len);
                    }

                    out.close();

                } catch (Exception e) {
                    CommonLibrary.errorLogger("**** Corrupted file deleted: " + localFileName + " " + e.getMessage(), 2);
                    e.printStackTrace();
                }
            }
        }
        archive.close();
        archiveFIle.delete();

        return true;
    }

    public static boolean unzipZipFileToSubDirectory(String strZipFile) {

        File fSourceZip = fSourceZip = new File(strZipFile);
        ZipFile zipFile = null;

        try {
            /*
		* STEP 1 : Create directory with the name of the zip file
		* 
		* For e.g. if we are going to extract c:/demo.zip create c:/demo 
		* directory where we can extract all the zip entries
		* 
             */
            String zipPath = strZipFile.substring(0, strZipFile.length() - 4);
            File temp = new File(zipPath);
            temp.mkdir();
            System.out.println(zipPath + " created");

            /*
		* STEP 2 : Extract entries while creating required
		* sub-directories
		* 
             */
            zipFile = new ZipFile(fSourceZip);
            Enumeration e = zipFile.entries();

            while (e.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) e.nextElement();
                File destinationFilePath = new File(zipPath, entry.getName());

                //create directories if required.
                destinationFilePath.getParentFile().mkdirs();

                //if the entry is directory, leave it. Otherwise extract it.
                if (entry.isDirectory()) {
                    continue;
                } else {
                    System.out.println("Extracting " + destinationFilePath);

                    /*
			* Get the InputStream for current entry
			* of the zip file using
			* 
			* InputStream getInputStream(Entry entry) method.
                     */
                    BufferedInputStream bis = new BufferedInputStream(zipFile
                            .getInputStream(entry));

                    int b;
                    byte buffer[] = new byte[1024];

                    /*
					 * read the current entry from the zip file, extract it
					 * and write the extracted file.
                     */
                    FileOutputStream fos = new FileOutputStream(destinationFilePath);
                    BufferedOutputStream bos = new BufferedOutputStream(fos,
                            1024);

                    while ((b = bis.read(buffer, 0, 1024)) != -1) {
                        bos.write(buffer, 0, b);
                    }

                    //flush the output stream and close it.
                    bos.flush();
                    bos.close();

                    //close the input stream.
                    bis.close();
                }
            }
            zipFile.close();
            fSourceZip.delete();
        } catch (IOException ioe) {
            System.out.println("IOError :" + ioe);
            CommonLibrary.errorLogger("**** Corrupted file deleted: " + strZipFile + " " + ioe.getMessage(), 2);
            return false;
        }
        return true;
    }

}
