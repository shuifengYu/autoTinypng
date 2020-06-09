package com.coder_yu.autocompressimage;

import com.google.gson.Gson;
import com.tinify.AccountException;
import com.tinify.ClientException;
import com.tinify.ConnectionException;
import com.tinify.ServerException;
import com.tinify.Source;
import com.tinify.Tinify;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class CompressUtil {

    public static boolean sCompressing = false;//防止连续重复被调用
    public static String sCompressingStart;//正在进行中的压缩开始时间

    public static final String DEFAULT_TINYFY_KEY = "VpbxYTjYBllF59CCPJqybcd011TfPSqv";
    public static final String DEFAULT_TARGET_DIR = "app/src/main/res";

    public static final String CACHE_FILE = "tinycache";

    /**
     * 已压缩的图片缓存map
     */
    HashMap<String, Double> mMap;

    /**
     * tinypng 可用key列表
     */
    ArrayList<String> mKeys;

    /**
     * 需要压缩的目标目录列表
     */
    ArrayList<String> mTargetDirs;

    /**
     * 当前在使用的key的索引
     */
    int mCurrentKeyIndex = -1;

    /**
     * 本次压缩图片的数量
     */
    int mCompressCount = 0;

    /**
     * 项目根目录
     */
    File mRootDir;

    /**
     * 项目根目录绝对路径
     */
    String mRootDirPath;


    public void compress(File dir, ArrayList<String> targetDirs, ArrayList<String> keys) {
        if (sCompressing) {
            System.out.println("上一次压缩(开始于" + sCompressingStart + ")还未结束，跳过本次压缩");
            return;
        }
        sCompressing = true;
        sCompressingStart = new Date().toLocaleString();
        System.out.println(sCompressingStart + "===========开始压缩，项目目录:" + dir + ",压缩目录：" + targetDirs + ",keys:" + keys + "===========");

        mRootDir = dir;
        mRootDirPath = mRootDir.getAbsolutePath();

        if (targetDirs == null || targetDirs.size() == 0) {//为配置则使用默认的目录:.app/src/main/res/
            mTargetDirs = new ArrayList<>();
            mTargetDirs.add(DEFAULT_TARGET_DIR);
        } else {
            mTargetDirs = targetDirs;
        }

        mKeys = new ArrayList<>();
        if (keys != null && keys.size() > 0) {
            mKeys.addAll(keys);
        }
        mKeys.add(DEFAULT_TINYFY_KEY);//添加赠送的key


        Tinify.setKey(nextKey());

        mMap = readCache();

        for (String targetDir : mTargetDirs) {
            File file = new File(mRootDir, targetDir);
            if (!file.exists()) {
                System.out.println("目标目录不存在:" + targetDir);
                continue;
            }
            if (file.isDirectory()) {
                compressDir(file);
            } else {
                compressFile(file);
            }

        }

        saveCache();
        sCompressing = false;
        System.out.println(new Date().toLocaleString() + "========================压缩结束========================");
        System.out.println("本次共压缩：" + mCompressCount + "张, 当前key[" + mKeys.get(mCurrentKeyIndex) + "]本月压缩：" + Tinify.compressionCount());
    }


    private void compressFile(File file) {
        if (!isImageFile(file)) {
            return;
        }
        String relativePath = getRelativePath(file);
        if (isFileCompressed(file, relativePath)) {
            return;
        }
        try {
            Source source = Tinify.fromFile(file.getAbsolutePath());
            source.toFile(file.getAbsolutePath());
            System.out.println("文件压缩成功：" + file);
            updateFileCompressed(file, relativePath);
        } catch (AccountException e) {
            // Verify your API key and account limit.
            System.out.println("The error message is: " + e.getMessage());
            Tinify.setKey(nextKey());
            compressFile(file);
        } catch (ClientException e) {
            // Check your source image and request options.
            System.out.println("compressFile clientexception:" + e.getMessage());
        } catch (ServerException e) {
            // Temporary issue with the Tinify API.
            System.out.println("compressFile ServerException:" + e.getMessage());
        } catch (ConnectionException e) {
            // A network connection error occurred.
            System.out.println("compressFile ConnectionException:" + e.getMessage());
        } catch (Exception e) {
            // Something else went wrong, unrelated to the Tinify API.
            System.out.println("compressFile Exception:" + e.getMessage());
        }
    }

    private void updateFileCompressed(File file, String relativePath) {
        mCompressCount++;
        mMap.put(relativePath, (double) file.length());
    }

    private String nextKey() {
        mCurrentKeyIndex++;
        if (mKeys.size() <= mCurrentKeyIndex) {
            System.out.println("本月已经没有剩余的key可以使用！");
            System.exit(0);
            sCompressing = false;
            return "";
        }
        return mKeys.get(mCurrentKeyIndex).trim();
    }

    private boolean isFileCompressed(File file, String relativePath) {
        if (!mMap.containsKey(relativePath)) {
            return false;
        }
        return file.length() == mMap.get(relativePath);
    }

    private String getRelativePath(File file) {
        String absolutePath = file.getAbsolutePath();
        return absolutePath.replace(mRootDirPath, "");
    }

    private boolean isImageFile(File file) {
        String name = file.getName().toLowerCase();
        return name.endsWith(".png") || name.endsWith(".jpg");
    }

    private void compressDir(File targetDir) {
        System.out.println("compressDir:" + targetDir + "================");
        File[] files = targetDir.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                compressDir(file);
            } else {
                compressFile(file);
            }
        }
    }

    private void saveCache() {
        FileWriter fileWriter = null;
        try {
            String json = new Gson().toJson(mMap);
            File cacheFile = new File(mRootDir, CACHE_FILE);
            boolean fileExist = cacheFile.exists();
            if (!fileExist) {
                fileExist = cacheFile.createNewFile();
            }
            if (!fileExist) {
                System.out.println("保存缓存文件失败！！！");
                return;
            }
            fileWriter = new FileWriter(cacheFile);
            fileWriter.write(json);
            fileWriter.flush();
        } catch (IOException e) {
            System.out.println("保存缓存文件失败！！！");
            e.printStackTrace();
        } finally {
            try {
                if (fileWriter != null) {
                    fileWriter.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private HashMap<String, Double> readCache() {
        HashMap<String, Double> map = new HashMap<>();
        File cacheFile = new File(mRootDir, CACHE_FILE);
        if (!cacheFile.exists() || cacheFile.isDirectory()) {
            return map;
        }
        InputStream fileReader = null;
        try {
            fileReader = new FileInputStream(cacheFile);
            int available = fileReader.available();
            if (available <= 0) {
                return map;
            }
            byte[] data = new byte[available];

            int read = fileReader.read(data);

            System.out.println("readCache result=" + read);
            if (read <= 0) {
                return map;
            }

            String json = new String(data);
            map = new Gson().fromJson(json, new HashMap<String, Double>().getClass());
            return map;
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("读取缓存文件失败:" + e.getMessage());
        } finally {
            try {
                if (fileReader != null) {
                    fileReader.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return map;
    }
}