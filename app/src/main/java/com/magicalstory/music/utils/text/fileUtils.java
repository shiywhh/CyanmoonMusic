package com.magicalstory.music.utils.text;

import android.content.Context;
import android.media.MediaScannerConnection;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.DecimalFormat;

public class fileUtils {
    public static boolean isVideo(String path) {
        path = path.toLowerCase();
        return path.endsWith(".mp4") || path.endsWith(".wmv") || path.endsWith(".avi");
    }

    public static boolean isPic(String path) {
        path = path.toLowerCase();
        return path.endsWith(".png") || path.endsWith(".jpg") || path.endsWith(".jpeg");
    }

    /**
     * 转换文件大小
     *
     * @param fileS
     * @return
     */
    public static String FormetFileSize(long fileS) {
        DecimalFormat df = new DecimalFormat("#.00");
        String fileSizeString = "";
        String wrongSize = "0B";
        if (fileS == 0) {
            return wrongSize;
        }
        if (fileS < 1024) {
            fileSizeString = df.format((double) fileS) + "B";
        } else if (fileS < 1048576) {
            fileSizeString = df.format((double) fileS / 1024) + "KB";
        } else if (fileS < 1073741824) {
            fileSizeString = df.format((double) fileS / 1048576) + "MB";
        } else {
            fileSizeString = df.format((double) fileS / 1073741824) + "GB";
        }
        return fileSizeString;
    }

    /**
     * 移动文件
     *
     * @param srcFileName 源文件完整路径
     * @param destDirName 目的目录完整路径
     * @return 文件移动成功返回true，否则返回false
     */
    public static boolean moveFile(String srcFileName, String destDirName) {

        File srcFile = new File(srcFileName);
        if (!srcFile.exists() || !srcFile.isFile())
            return false;

        File destDir = new File(destDirName);
        if (!destDir.exists()) {
            destDir.mkdirs();
        }
        return srcFile.renameTo(new File(destDirName + File.separator + srcFile.getName()));
    }



    /**
     * 移动文件
     *
     * @param oldName 源文件
     * @param newName 新文件
     * @return 文件移动成功返回true，否则返回false
     */
    public static boolean rename(String oldName, String newName) {

        File srcFile = new File(oldName);
        if (!srcFile.exists())
            return false;

        return srcFile.renameTo(new File(newName));
    }

    /**
     * 移动目录
     *
     * @param srcDirName  源目录完整路径
     * @param destDirName 目的目录完整路径
     * @return 目录移动成功返回true，否则返回false
     */
    public static boolean moveDirectory(String srcDirName, String destDirName) {

        File srcDir = new File(srcDirName);
        if (!srcDir.exists() || !srcDir.isDirectory()) {
            return false;
        }

        File destDir = new File(destDirName);
        if (!destDir.exists()) {
            destDir.mkdirs();
        }

        /**
         * 如果是文件则移动，否则递归移动文件夹。删除最终的空源文件夹
         * 注意移动文件夹时保持文件夹的树状结构
         */
        File[] sourceFiles = srcDir.listFiles();
        for (File sourceFile : sourceFiles) {
            if (sourceFile.isFile()) {
                moveFile(sourceFile.getAbsolutePath(), destDir.getAbsolutePath());
            } else if (sourceFile.isDirectory()) {
                moveDirectory(sourceFile.getAbsolutePath(),
                        destDir.getAbsolutePath() + File.separator + sourceFile.getName());
            }

        }
        return srcDir.delete();
    }

    /**
     * 复制文件
     *
     * @param srcPathStr 源目录完整路径
     * @param desPathStr 目的目录完整路径
     * @return 目录移动成功返回true，否则返回false
     */
    public static void copy(String srcPathStr, String desPathStr) {
        //获取源文件的名称
        String newFileName = new File(srcPathStr).getName(); //目标文件地址
        desPathStr = desPathStr + File.separator + newFileName; //源文件地址
        try {
            FileInputStream fis = new FileInputStream(srcPathStr);//创建输入流对象
            FileOutputStream fos = new FileOutputStream(desPathStr); //创建输出流对象
            byte datas[] = new byte[1024 * 8];//创建搬运工具
            int len = 0;//创建长度
            while ((len = fis.read(datas)) != -1)//循环读取数据
            {
                fos.write(datas, 0, len);
            }
            fos.close();//释放资源
            fis.close();//释放资源
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * 复制文件,直接根据路径
     *
     * @param srcPathStr 源目录完整路径
     * @param desPathStr 目的目录完整路径
     * @return 目录移动成功返回true，否则返回false
     */
    public static void copyWithPath(String srcPathStr, String desPathStr) {
        try {
            FileInputStream fis = new FileInputStream(srcPathStr);//创建输入流对象
            FileOutputStream fos = new FileOutputStream(desPathStr); //创建输出流对象
            byte datas[] = new byte[1024 * 8];//创建搬运工具
            int len = 0;//创建长度
            while ((len = fis.read(datas)) != -1)//循环读取数据
            {
                fos.write(datas, 0, len);
            }
            fos.close();//释放资源
            fis.close();//释放资源
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 通知android媒体库更新文件夹
     *
     * @param filePath ilePath 文件绝对路径，、/sda/aaa/jjj.jpg
     */
    public static void scanFile(Context context, String filePath) {
        try {
            MediaScannerConnection.scanFile(context, new String[]{filePath}, null,
                    (path, uri) -> {

                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * 通知android媒体库更新文件夹
     *
     * @param filePath ilePath 文件绝对路径，、/sda/aaa/jjj.jpg
     */
    public static void scanFiles(Context context, String[] filePath) {
        try {
            MediaScannerConnection.scanFile(context, filePath, null,
                    (path, uri) -> {

                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 先根遍历序递归删除文件夹
     *
     * @param dirFile 要被删除的文件或者目录
     * @return 删除成功返回true, 否则返回false
     */
    public static boolean deleteFile(File dirFile) {
        // 如果dir对应的文件不存在，则退出
        if (!dirFile.exists()) {
            return false;
        }

        if (dirFile.isFile()) {
            return dirFile.delete();
        } else {

            for (File file : dirFile.listFiles()) {
                deleteFile(file);
            }
        }

        return dirFile.delete();
    }
}
