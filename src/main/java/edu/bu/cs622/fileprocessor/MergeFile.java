package edu.bu.cs622.fileprocessor;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class MergeFile {
  /**
   * @param fromPathname the target file that you want to read from
   * @param toPathname   the destination file that you want to write to
   */
  public static void mergeDirectoryToSingleFile(String fromPathname,String toPathname){
    try{
      // If the target file has already been there, clear its content.
      // In case generate duplicate data.
      new PrintWriter(toPathname).close();

      File folder = new File(fromPathname);

      File[] listOfFiles = folder.listFiles();

      if(listOfFiles == null)
        return;
      for (int i = 0; i < listOfFiles.length; i++) {
        if(listOfFiles[i].isFile()) {
          mergeZipFileToSingleFile(fromPathname + File.separator + listOfFiles[i].getName(),
              toPathname);
        }
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  /**
   *
   * @param zipFilePath the file folder which contains zip files inside you want to read from
   * @param toFilePath the file that you want to write in
   */
  private static void mergeZipFileToSingleFile(String zipFilePath, String toFilePath) {
    byte[] buffer = new byte[4096];
    try{
      ZipInputStream in = new ZipInputStream(new FileInputStream(zipFilePath));
      ZipEntry zip = in.getNextEntry();

      File targetFile = new File(toFilePath);

      FileOutputStream out = new FileOutputStream(targetFile,true);

      while(zip != null) {
        int length;
        while((length = in.read(buffer)) > 0) {
          out.write(buffer,0,length);
        }
        zip = in.getNextEntry();
      }
      out.close();
      in.closeEntry();
      in.close();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }
}
