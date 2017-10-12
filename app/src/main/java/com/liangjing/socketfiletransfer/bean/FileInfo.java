package com.liangjing.socketfiletransfer.bean;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.Serializable;
import java.util.List;

/**
 * Created by liangjing on 2017/10/9.
 * <p>
 * function:文件信息(对其进行序列化，以使其对象能够转换成可存储或可传输的状态)
 */

public class FileInfo implements Serializable {

    //文件传输结果标志：1 成功，-1 失败
    public static final int FLAG_SUCCESS = 1;
    public static final int FLAG_FAILURE = -1;

    //文件路径
    private String filePath;
    //文件类型
    private int fileType;
    //文件大小
    private long size;
    //文件名
    private String fileName;
    //文件发送结果
    private int result;
    //文件传输进度
    private int progress;

    //该文件在列表中的位置值
    private int position;

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public int getFileType() {
        return fileType;
    }

    public void setFileType(int fileType) {
        this.fileType = fileType;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public int getResult() {
        return result;
    }

    public void setResult(int result) {
        this.result = result;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    @Override
    public String toString() {
        return "FileInfo{" +
                "filePath='" + filePath + '\'' +
                ", fileType=" + fileType +
                ", size=" + size +
                ", position=" + position +
                '}';
    }

    public FileInfo(int position, String filePath, long size) {
        this.position = position;
        this.filePath = filePath;
        this.size = size;
    }

    public FileInfo() {
    }

    /**
     * function:将所传入的文件信息实体类转换为json数据形式
     *
     * @param fileInfo
     * @return
     */
    public static String toJsonStr(FileInfo fileInfo) {
        return new Gson().toJson(fileInfo);
    }

    /**
     * function:将所传入的文件信息实体类的集合转换为json数据形式
     *
     * @param fileInfos
     * @return
     */
    public static String toJsonStr(List<FileInfo> fileInfos) {
        return new Gson().toJson(fileInfos);
    }

    /**
     * function:将传进来的json数据转换为对应的实体类
     *
     * @param jsonStr
     * @return
     */
    public static FileInfo toObject(String jsonStr) {
        return new Gson().fromJson(jsonStr, FileInfo.class);
    }


    /**
     * function:将传进来的json数据转换为对应的实体类的一个List集合的形式
     *
     * @param jsonStr
     * @return
     */
    public static List<FileInfo> toObjectList(String jsonStr) {
        return new Gson().fromJson(jsonStr, new TypeToken<FileInfo>() {
        }.getType());
    }
}
