package org.netbeans.modules.python.options;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.text.StringEscapeUtils;
import org.javatuples.Triplet;
import org.json.JSONArray;
import org.json.JSONObject;
import org.netbeans.modules.python.PythonUtility;
import org.openide.util.Exceptions;
import org.openide.util.Pair;

/**
 *
 * @author albilu
 */
public class PythonPlatformManager {

    public static File getPathFile() {
        return PythonUtility.ENVS;
    }

    private static File getPlatformFile() {
        return PythonUtility.PLATFORMS;
    }

    public static List<Triplet<String, String, Boolean>> getPythonExes() {
        List<Triplet<String, String, Boolean>> exes = new ArrayList<>();
        try {
            JSONArray platformJsonArray = new JSONArray(Files.readString(getPlatformFile().toPath()));
            for (int i = 0; i < platformJsonArray.length(); i++) {
                JSONObject jsonObject = platformJsonArray.getJSONObject(i);
                exes.add(Triplet.with(jsonObject.getString("cmd"), jsonObject.getString("version"),
                        jsonObject.getBoolean("state")));

            }
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        return exes;
    }

    public static String getPythonPath() {
        try {
            JSONArray pathsJsonArray = new JSONArray(Files.readString(getPathFile().toPath()));
            for (int i = 0; i < pathsJsonArray.length(); i++) {
                JSONObject jsonObject = pathsJsonArray.getJSONObject(i);
                if (jsonObject.has("pythonpath")) {
                    return jsonObject.getString("pythonpath");

                }

            }
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        return "";
    }

    public static void refresh() {
        try {
            List<Pair<String, String>> pythonExes = PythonUtility.getPythonExes();
            JSONArray platformJsonArray = new JSONArray(Files.readString(getPlatformFile().toPath()));
            for (Pair<String, String> pythonExe : pythonExes) {
                if (!platformJsonArray.toString().contains(StringEscapeUtils.escapeJava(pythonExe.second()))) {
                    Map<String, String> hashMap = new HashMap<>();
                    hashMap.put("cmd", pythonExe.second());
                    hashMap.put("version", pythonExe.first());
                    hashMap.put("state", "false");
                    platformJsonArray.put(new JSONObject(hashMap));
                }
                savePlaltformJson(platformJsonArray);
            }
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    private static void savePlaltformJson(JSONArray array) {
        try {
            Files.writeString(getPlatformFile().toPath(), array.toString());
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    static void delete(String value0) {
        try {
            JSONArray platformJsonArray = new JSONArray(Files.readString(getPlatformFile().toPath()));
            for (int i = 0; i < platformJsonArray.length(); i++) {
                JSONObject jsonObject = platformJsonArray.getJSONObject(i);
                if (jsonObject.getString("cmd").equals(value0)) {
                    platformJsonArray.remove(i);
                }

            }
            savePlaltformJson(platformJsonArray);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }

    }

    static void setSelected(String value1, String state) {
        try {
            JSONArray platformJsonArray = new JSONArray(Files.readString(getPlatformFile().toPath()));
            for (int i = 0; i < platformJsonArray.length(); i++) {
                JSONObject jsonObject = platformJsonArray.getJSONObject(i);
                if (Boolean.parseBoolean(jsonObject.getString("state"))) {
                    jsonObject.put("state", "false");
                }

                if (jsonObject.getString("cmd").equals(value1)) {
                    jsonObject.put("state", state);
                }
            }

            savePlaltformJson(platformJsonArray);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    static void insert(String cmd) {
        try {
            JSONArray platformJsonArray = new JSONArray(Files.readString(getPlatformFile().toPath()));
            if (!platformJsonArray.toString().contains(StringEscapeUtils.escapeJava(cmd))) {
                Map<String, String> hashMap = new HashMap<>();
                hashMap.put("cmd", cmd);
                hashMap.put("version", PythonUtility.getVersion(cmd));
                hashMap.put("state", "false");
                platformJsonArray.put(new JSONObject(hashMap));
            }
            savePlaltformJson(platformJsonArray);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    public static String getDefault() throws IOException {
        try {
            JSONArray platformJsonArray = new JSONArray(Files.readString(getPlatformFile().toPath()));
            for (int i = 0; i < platformJsonArray.length(); i++) {
                JSONObject jsonObject = platformJsonArray.getJSONObject(i);
                if (Boolean.parseBoolean(jsonObject.getString("state"))) {
                    return jsonObject.getString("cmd");
                }

            }

        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }

        List<Pair<String, String>> pythonExes = PythonUtility.getPythonExes();
        return !pythonExes.isEmpty() ? pythonExes.get(0).second() : "";
    }

}
