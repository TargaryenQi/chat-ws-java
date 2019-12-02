package edu.bu.cs622.fileprocessor;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public class MergeFile {
  /**
   * @param fromPathname the target file that you want to read from
   * @param toPathname   the destination file that you want to write to
   */
  public static void mergeDirectoryToSingleFile(String fromPathname,String toPathname) throws IOException {
    BufferedWriter bufferedWriter = null;
    try{
      // If the target file has already been there, clear its content.
      // In case generate duplicate data.
      new PrintWriter(toPathname).close();

      File folder = new File(fromPathname);

      File[] listOfFiles = folder.listFiles();

      if(listOfFiles == null)
        return;

      bufferedWriter = new BufferedWriter(new FileWriter(toPathname));
      for (int i = 0; i < listOfFiles.length; i++) {
        if(listOfFiles[i].isFile()) {
          mergeZipFileToSingleFile(fromPathname + File.separator + listOfFiles[i].getName(),
              bufferedWriter);
        }
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    } finally {
      if(bufferedWriter != null) {
        bufferedWriter.close();
      }
    }
  }

  /**
   *
   * @param zipFilePath the file folder which contains zip files inside you want to read from
   */
  private static void mergeZipFileToSingleFile(String zipFilePath, BufferedWriter bufferedWriter) {
    try{
      ZipFile zipFile = new ZipFile(zipFilePath);
      ZipInputStream inputStream = new ZipInputStream(new FileInputStream(zipFilePath));
      ZipEntry zipEntry = inputStream.getNextEntry();
      while(zipEntry != null) {
        InputStream input = zipFile.getInputStream(zipEntry);
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
        String line = bufferedReader.readLine();
        while(line != null) {
          if(line.startsWith("{"))
            bufferedWriter.write(line + "\n");
          line = bufferedReader.readLine();
        }
        zipEntry = inputStream.getNextEntry();
      }
      inputStream.closeEntry();
      inputStream.close();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }
}
