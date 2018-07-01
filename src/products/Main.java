package products;

import java.io.*;

public class Main {
    private static String readFromStdin() {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(System.in));
            System.out.println("Enter link for ceneo:");
            return br.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return "";
    }

    public static void main(String[] args) {
        GenerateXML generateXML = new GenerateXML(readFromStdin());
    }
}
