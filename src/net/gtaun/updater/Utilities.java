package net.gtaun.updater;

import net.gtaun.updater.constant.FileType;
import net.gtaun.updater.constant.OSType;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by MarvinWindows on 24.12.2014 in project shoebill-updater.
 * Copyright (c) 2014 Marvin Haschker. All rights reserved.
 */
public class Utilities {
    public static String excutePost(String targetURL, String urlParameters) throws IOException {
        URL url;
        HttpURLConnection connection = null;
        //Create connection
        url = new URL(targetURL);
        connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type",
                "application/x-www-form-urlencoded");

        connection.setRequestProperty("Content-Length", "" +
                Integer.toString(urlParameters.getBytes().length));
        connection.setRequestProperty("Content-Language", "en-US");

        connection.setUseCaches(false);
        connection.setDoInput(true);
        connection.setDoOutput(true);

        DataOutputStream wr = new DataOutputStream(
                connection.getOutputStream());
        wr.writeBytes(urlParameters);
        wr.flush();
        wr.close();

        InputStream is = connection.getInputStream();
        BufferedReader rd = new BufferedReader(new InputStreamReader(is));
        String line;
        StringBuilder response = new StringBuilder();
        while ((line = rd.readLine()) != null) {
            response.append(line);
            response.append('\r');
        }
        rd.close();
        if(connection != null)
            connection.disconnect();
        return response.toString();
    }

    public static FileType getFileType(String fileName) {
        fileName = fileName.toLowerCase();
        if(fileName.contains("shoebill-dependency-manager") && fileName.endsWith(".jar"))
            return FileType.DependencyManager;
        else if(fileName.contains("shoebill-launcher") && fileName.endsWith(".jar"))
            return FileType.Launcher;
        else if(fileName.contains("shoebill") && (fileName.endsWith(".dll") || fileName.endsWith(".so")))
            return FileType.Plugin;
        else
            return null;
    }

    public static String hashFile(File file, String algorithm) {
        try {
            FileInputStream inputStream = new FileInputStream(file);
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            byte[] bytesBuffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(bytesBuffer)) != -1) {
                digest.update(bytesBuffer, 0, bytesRead);
            }
            byte[] hashedBytes = digest.digest();
            inputStream.close();
            return convertByteArrayToHexString(hashedBytes);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String convertByteArrayToHexString(byte[] arrayBytes) {
        StringBuilder stringBuffer = new StringBuilder();
        for (byte arrayByte : arrayBytes) {
            stringBuffer.append(Integer.toString((arrayByte & 0xff) + 0x100, 16)
                    .substring(1));
        }
        return stringBuffer.toString();
    }


    public static OSType parseOS(String OS) {
        if(OS.contains("mac")) return OSType.Mac;
        else if(OS.contains("win")) return OSType.Windows;
        else if(OS.contains("nix") || OS.contains("nux") || OS.indexOf("aix") > 0) return OSType.Unix;
        else if(OS.contains("sunos")) return OSType.Solaris;
        return OSType.Windows;
    }

    public static boolean checkForDirectory(File sampServerFile) {
        if(!sampServerFile.exists()) {
            System.err.println("Samp Server File was not found! Make sure that you are in the correct directory.");
            return false;
        } else {
            File shoebillDirectory = new File("shoebill");
            File pluginsDirectory = new File("plugins");
            if(!shoebillDirectory.exists()) {
                System.err.println("The shoebill directory doesn't exists. Please make sure that you are in the correct directory.");
                return false;
            } else if(!pluginsDirectory.exists()) {
                System.err.println("The plugins directory doesn't exists. Please make sure that you are in the correct directory.");
                return false;
            }
            File bootstrapDirectory = new File(shoebillDirectory, "bootstrap");
            if(!bootstrapDirectory.exists()) {
                System.err.println("The shoebill/bootstrap directory doesn't exists. Please make sure that you are in the correct directory.");
                return false;
            }
            return true;
        }
    }
}
