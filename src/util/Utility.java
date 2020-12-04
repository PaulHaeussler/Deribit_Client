package util;

import main.Test2;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Objects;

public class Utility {

    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    public static HashMap<String, String> reqHeaders = new HashMap<>();
    private static byte[] quota_exceeded = null;

    public static String checkStartupArgs(String[] args){
        if(args.length == 0) Printer.printToLog("No startup params given", Printer.LOGTYPE.INFO);
        boolean isPassword = false;
        String repo = "";
        for (int i = 0; i < args.length; i++){
            if(args[i].startsWith("-")){
                switch(args[i]){
                    case "-p":
                        repo = getNextIfExistent(args, i);
                        break;
                    default:
                        break;
                }
            }
            Printer.printToLog("Found startup param " + args[i], Printer.LOGTYPE.INFO);
        }
        return repo;
    }


    public static boolean isZero(double value, double threshold){
        return value >= -threshold && value <= threshold;
    }


    private static String getNextIfExistent(String[] args, int index){
        if(index >= args.length - 1) {
            Printer.printError("Invalid params");
            System.exit(1);
            return null;
        } else {
            return args[index+1];
        }
    }

    public static int getSize(String pathToDir) {
        return Objects.requireNonNull(new File(pathToDir).list()).length;
    }

    public static int getSize(ResultSet resultSet) throws SQLException {
        resultSet.last();
        int size = resultSet.getRow();
        resultSet.beforeFirst();
        return size;
    }


    public static String hashSHA256(String strToHash) {
        String hash = "";
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(strToHash.getBytes(StandardCharsets.UTF_8));
            hash = bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            Printer.printException(e);
            e.printStackTrace();
            System.exit(1);
        }
        return hash;
    }

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }


}
