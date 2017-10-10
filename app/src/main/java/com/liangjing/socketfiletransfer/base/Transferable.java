package com.liangjing.socketfiletransfer.base;

/**
 * Created by liangjing on 2017/10/10.
 * <p>
 * function:接口--定义发送端以及接收端共有的关于文件传输的一些方法:文件传输之初始化操作，开始进行文件传输，文件传输过程完毕.
 */

public interface Transferable {


    /**
     * function:初始化操作
     *
     * @throws Exception
     */
    void init() throws Exception;


    /**
     * function:发送/接收文件实体数据(发送端--通过将文件实体数据转换为流的方式发送出去，接收端--将文件实体数据对应的流转换为文件的形式)
     *
     * @throws Exception
     */
    void parseBody() throws Exception;


    /**
     * function:文件传输完毕(发送/接收完毕)
     *
     * @throws Exception
     */
    void finishTransfer() throws Exception;
}
