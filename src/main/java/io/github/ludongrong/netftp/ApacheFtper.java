package io.github.ludongrong.netftp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPConnectionClosedException;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTPSClient;
import org.apache.commons.net.ftp.parser.ParserInitializationException;
import org.apache.commons.net.util.TrustManagerUtils;

import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import io.github.ludongrong.netftp.util.PathHelper;
import lombok.Getter;
import lombok.Setter;

/**
 * Ftp 协议 apache common 实现版.
 *
 * @author <a href="mailto:736779458@qq.com">ludongrong</a>
 * @since 2020-11-27
 */
public class ApacheFtper extends AbstsactFtper {

    /** 客户端 */
    @Getter
    @Setter
    private FTPClient ftpClient;

    /**
     * 构造.
     *
     * @param ftpClient
     *            客户端
     * @param ftperConfig
     *            配置
     */
    public ApacheFtper(FTPClient ftpClient, FtperConfig ftperConfig) {
        super();
        this.ftpClient = ftpClient;
        this.ftperConfig = ftperConfig;
    }

    /**
     * @see io.github.ludongrong.netftp.AbstsactFtper#ls(java.lang.String)
     */
    @Override
    protected List<FtperFile> ls(String dst) throws LsException {

        List<FtperFile> flist = new ArrayList<FtperFile>();

        String fnameEncoding;
        try {
            fnameEncoding = new String(dst.getBytes(), ftpClient.getControlEncoding());
        } catch (UnsupportedEncodingException e1) {
            return flist;
        }

        try {
            FTPFile[] ftpFiles = ftpClient.listFiles(fnameEncoding);

            for (FTPFile ftpFile : ftpFiles) {
                if (StrUtil.isEmpty(ftpFile.getName())) {
                    continue;
                }

                byte[] buf = ftpFile.getName().getBytes(ftpClient.getControlEncoding());
                String name = new String(buf, CharsetUtil.UTF_8);

                FtperFile resfile = new FtperFile();
                resfile.setName(name);
                resfile.setType(ftpFile.getType() == it.sauronsoftware.ftp4j.FTPFile.TYPE_DIRECTORY ? 1 : 0);
                resfile.setSize(ftpFile.getSize());
                resfile.setTimestamp(ftpFile.getTimestamp());
                resfile.setDirPath(dst);
                resfile.setExists(true);
                flist.add(resfile);
            }
        } catch (UnsupportedEncodingException e) {
            throw new LsException("", e);
        } catch (FTPConnectionClosedException e) {
            throw new LsException("", e);
        } catch (IOException e) {
            throw new LsException("", e);
        } catch (ParserInitializationException e) {
            throw new LsException("", e);
        }

        return flist;
    }

    /**
     * @see io.github.ludongrong.netftp.AbstsactFtper#listFunc(java.lang.String, io.github.ludongrong.netftp.Proccesser)
     */
    protected <T extends Proccesser<?>> T listFunc(String dst, T proccesser) {

        String[] items = PathHelper.split(dst);

        String dir = StrUtil.SLASH;

        List<FtperFile> ftpFiles = new ArrayList<FtperFile>();
        try {
            ftpFiles.addAll(ls(StrUtil.SLASH));
        } catch (LsException e) {
            return proccesser;
        }

        int i = 1;
        for (; i < items.length; i++) {

            String fname = items[i];

            FtperFile matchFile = FtperFile.matchSelector(ftpFiles, fname);
            ftpFiles.clear();

            boolean edir;

            if (matchFile != null) {
                if (matchFile.isDirectory() == false) {
                    break;
                }
                edir = true;
            } else {
                edir = false;
            }

            boolean go = proccesser.pre(dir, fname, edir);
            if (go == false) {
                break;
            }

            if (edir) {
                try {
                    ftpFiles.addAll(ls(matchFile.getAbsolutePath()));
                } catch (LsException e) {
                    break;
                }
            }

            dir = FtperFile.getAbsolutePath(dir, fname);
        }

        if (i == items.length) {
            ftpFiles.stream().forEach(obj -> {
                obj.setDirPath(dst);
                obj.setExists(true);
            });
            proccesser.last(ftpFiles);
        }

        return proccesser;
    }

    /**
     * @see io.github.ludongrong.netftp.AbstsactFtper#changeWorkingDirectory(java.lang.String)
     */
    @Override
    protected boolean changeWorkingDirectory(String dst) {
        String fnameEncoding;
        try {
            fnameEncoding = new String(dst.getBytes(), ftpClient.getControlEncoding());
        } catch (UnsupportedEncodingException e1) {
            return false;
        }

        try {
            return ftpClient.changeWorkingDirectory(fnameEncoding);
        } catch (FTPConnectionClosedException e) {
            return false;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * @see io.github.ludongrong.netftp.AbstsactFtper#mkdir(java.lang.String)
     */
    @Override
    protected boolean mkdir(String fname) {
        String fnameEncoding;
        try {
            fnameEncoding = new String(fname.getBytes(), ftpClient.getControlEncoding());
        } catch (UnsupportedEncodingException e1) {
            return false;
        }

        try {
            return ftpClient.makeDirectory(fnameEncoding);
        } catch (FTPConnectionClosedException e) {
            return false;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * @see io.github.ludongrong.netftp.AbstsactFtper#rmdir(java.lang.String)
     */
    @Override
    protected boolean rmdir(String fname) {
        String fnameEncoding;
        try {
            fnameEncoding = new String(fname.getBytes(), ftpClient.getControlEncoding());
        } catch (UnsupportedEncodingException e1) {
            return false;
        }

        try {
            return ftpClient.removeDirectory(fnameEncoding);
        } catch (FTPConnectionClosedException e) {
            return false;
        } catch (IOException e) {
            return false;
        } catch (ParserInitializationException e) {
            return false;
        }
    }

    /**
     * @see io.github.ludongrong.netftp.AbstsactFtper#rm(java.lang.String)
     */
    @Override
    protected boolean rm(String fname) {
        String fnameEncoding;
        try {
            fnameEncoding = new String(fname.getBytes(), ftpClient.getControlEncoding());
        } catch (UnsupportedEncodingException e1) {
            return false;
        }

        try {
            return ftpClient.deleteFile(fnameEncoding);
        } catch (FTPConnectionClosedException e) {
            return false;
        } catch (IOException e) {
            return false;
        } catch (ParserInitializationException e) {
            return false;
        }
    }

    /**
     * @see io.github.ludongrong.netftp.AbstsactFtper#put(java.lang.String, java.lang.String, java.io.InputStream)
     */
    @Override
    protected boolean put(String dst, String fname, InputStream is) {

        String pname = fname + ".part";

        String fnameEncoding;
        try {
            fnameEncoding =
                new String(FtperFile.getAbsolutePath(dst, fname).getBytes(), ftpClient.getControlEncoding());
        } catch (UnsupportedEncodingException e) {
            return false;
        }

        String pnameEncoding;
        try {
            pnameEncoding =
                new String(FtperFile.getAbsolutePath(dst, pname).getBytes(), ftpClient.getControlEncoding());
        } catch (UnsupportedEncodingException e1) {
            return false;
        }

        try {
            boolean res = ftpClient.storeFile(pnameEncoding, is);
            if (res) {
                res = ftpClient.rename(pnameEncoding, fnameEncoding);
            }
            return res;
        } catch (FTPConnectionClosedException e) {
            return false;
        } catch (IOException e) {
            return false;
        } catch (ParserInitializationException e) {
            return false;
        }
    }

    /**
     * @see io.github.ludongrong.netftp.AbstsactFtper#get(io.github.ludongrong.netftp.FtperFile, java.io.OutputStream)
     */
    @Override
    protected boolean get(FtperFile matchFile, OutputStream os) {

        String srcEncoding;
        try {
            srcEncoding = new String(matchFile.getAbsolutePath().getBytes(), ftpClient.getControlEncoding());
        } catch (UnsupportedEncodingException e) {
            return false;
        }

        try {
            return ftpClient.retrieveFile(srcEncoding, os);
        } catch (FTPConnectionClosedException e) {
            return false;
        } catch (IOException e) {
            return false;
        } catch (ParserInitializationException e) {
            return false;
        }
    }

    /**
     * @see io.github.ludongrong.netftp.AbstsactFtper#rename(io.github.ludongrong.netftp.FtperFile, java.lang.String,
     *      java.lang.String)
     */
    @Override
    protected boolean rename(FtperFile matchFile, String dst, String dname) {

        String srcEncoding;
        try {
            srcEncoding = new String(matchFile.getAbsolutePath().getBytes(), ftpClient.getControlEncoding());
        } catch (UnsupportedEncodingException e) {
            return false;
        }

        String dstEncoding;
        try {
            dstEncoding = new String(FtperFile.getAbsolutePath(dst, dname).getBytes(), ftpClient.getControlEncoding());
        } catch (UnsupportedEncodingException e) {
            return false;
        }

        try {
            return ftpClient.rename(srcEncoding, dstEncoding);
        } catch (FTPConnectionClosedException e) {
            return false;
        } catch (IOException e) {
            return false;
        } catch (ParserInitializationException e) {
            return false;
        }
    }

    /**
     * @see io.github.ludongrong.netftp.IFtper#close()
     */
    @Override
    public void close() {

        close(ftpClient);
    }

    /**
     * 关闭连接.
     *
     * @param ftpClient
     *            客户端
     */
    private static void close(FTPClient ftpClient) {

        if (ObjectUtil.isNull(ftpClient)) {
            return;
        }

        try {
            ftpClient.logout();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            ftpClient.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 构建者.
     *
     * @author <a href="mailto:736779458@qq.com">ludongrong</a>
     * @since 2020-11-27
     */
    public static class Builder {

        /** 文件传输类型 */
        private int fileType = FTP.BINARY_FILE_TYPE;

        /**
         * 设置文件类型.
         *
         * <p>
         * client.setType(FTPClient.TYPE_TEXTUAL);
         * client.setType(FTPClient.TYPE_BINARY);
         * client.setType(FTPClient.TYPE_AUTO);
         * 
         * @param fileType
         *            文件类型
         * @return 构建者
         */
        public Builder withFileType(int fileType) {
            this.fileType = fileType;
            return this;
        }

        /**
         * 设置文件类型.
         *
         * @param ftpClient
         *            客户端
         * @param ftperConfig
         *            配置
         * @throws FtperException
         *             初始化客户端异常
         */
        private void setFileType(FTPClient ftpClient, FtperConfig ftperConfig) throws FtperException {
            boolean res;
            try {
                res = ftpClient.setFileType(fileType);
            } catch (Exception e) {
                close(ftpClient);
                throw new FtperException(FtperException.CONFIG_CODE, MessageFormat
                    .format(FtperException.CONFIG_DESCRIPTION, ftperConfig.getHost(), ftperConfig.getUsername()));
            }

            if (!res) {
                close(ftpClient);
                throw new FtperException(FtperException.CONFIG_CODE, MessageFormat
                    .format(FtperException.CONFIG_DESCRIPTION, ftperConfig.getHost(), ftperConfig.getUsername()));
            }
        }

        /**
         * 设置传输模式.
         *
         * @param ftpClient
         *            客户端
         * @param ftperConfig
         *            配置
         * @throws FtperException
         *             初始化客户端异常
         */
        private void setFileTransferMode(FTPClient ftpClient, FtperConfig ftperConfig) throws FtperException {
            boolean res;
            try {
                res = ftpClient.setFileTransferMode(FTPClient.STREAM_TRANSFER_MODE);
            } catch (Exception e) {
                close(ftpClient);
                throw new FtperException(FtperException.CONFIG_CODE, MessageFormat
                    .format(FtperException.CONFIG_DESCRIPTION, ftperConfig.getHost(), ftperConfig.getUsername()));
            }

            if (!res) {
                close(ftpClient);
                throw new FtperException(FtperException.CONFIG_CODE, MessageFormat
                    .format(FtperException.CONFIG_DESCRIPTION, ftperConfig.getHost(), ftperConfig.getUsername()));
            }
        }

        /**
         * 连接.
         *
         * @param ftpClient
         *            客户端
         * @param ftperConfig
         *            配置
         * @throws FtperException
         *             初始化客户端异常
         */
        private void connect(FTPClient ftpClient, FtperConfig ftperConfig) throws FtperException {
            try {
                ftpClient.connect(ftperConfig.getHost(), ftperConfig.getPort());
            } catch (Exception e) {
                throw new FtperException(FtperException.CONNECT_CODE, MessageFormat
                    .format(FtperException.CONNECT_DESCRIPTION, ftperConfig.getHost(), ftperConfig.getUsername()));
            }

            if (FTPReply.isPositiveCompletion(ftpClient.getReplyCode()) == false) {
                close(ftpClient);
                throw new FtperException(FtperException.CONNECT_CODE, MessageFormat
                    .format(FtperException.CONNECT_DESCRIPTION, ftperConfig.getHost(), ftperConfig.getUsername()));
            }
        }

        /**
         * 登录.
         *
         * @param ftpClient
         *            客户端
         * @param ftperConfig
         *            配置
         * @throws FtperException
         *             初始化客户端异常
         */
        private void login(FTPClient ftpClient, FtperConfig ftperConfig) throws FtperException {
            boolean login;
            try {
                login = ftpClient.login(ftperConfig.getUsername(), ftperConfig.getPassword());
            } catch (Exception e) {
                close(ftpClient);
                throw new FtperException(FtperException.LOGIN_CODE, MessageFormat
                    .format(FtperException.LOGIN_DESCRIPTION, ftperConfig.getHost(), ftperConfig.getUsername()));
            }

            if (!login) {
                close(ftpClient);
                throw new FtperException(FtperException.LOGIN_CODE, MessageFormat
                    .format(FtperException.LOGIN_DESCRIPTION, ftperConfig.getHost(), ftperConfig.getUsername()));
            }
        }

        /**
         * 创建 ftp 客户端.
         *
         * @param ftperConfig
         *            配置
         * @return ftp 客户端
         * @throws FtperException
         *             初始化客户端异常
         */
        private FTPClient createFtpClient(FtperConfig ftperConfig) throws FtperException {

            FTPClient ftpClient = new FTPClient();

            connect(ftpClient, ftperConfig);

            login(ftpClient, ftperConfig);

            return ftpClient;
        }

        /**
         * 创建 ftps 客户端.
         *
         * @param ftperConfig
         *            配置
         * @return ftps 客户端
         * @throws FtperException
         *             初始化客户端异常
         */
        private FTPSClient createFtpsClient(FtperConfig ftperConfig) throws FtperException {

            FTPSClient ftpsClient = new FTPSClient(true);
            ftpsClient.setTrustManager(TrustManagerUtils.getAcceptAllTrustManager());

            connect(ftpsClient, ftperConfig);

            login(ftpsClient, ftperConfig);

            try {
                // Set protection buffer size
                ftpsClient.execPBSZ(0);
                // Set data channel protection to private
                ftpsClient.execPROT("P");
            } catch (IOException e) {
                throw new FtperException(FtperException.CONFIG_CODE, MessageFormat
                    .format(FtperException.CONFIG_DESCRIPTION, ftperConfig.getHost(), ftperConfig.getUsername()));
            }

            return ftpsClient;
        }

        /**
         * 创建.
         *
         * @param ftperConfig
         *            配置
         * @return 客户端
         * @throws FtperException
         *             初始化客户端异常
         */
        public ApacheFtper build(FtperConfig ftperConfig) throws FtperException {

            FTPClient ftpClient = null;

            switch (ftperConfig.getProtocol()) {
                case ftp:
                    ftpClient = createFtpClient(ftperConfig);
                    break;
                case ftps:
                    ftpClient = createFtpsClient(ftperConfig);
                    break;
                default:
                    throw new IllegalStateException("Unsupported protocol");
            }

            // 设置被动模式
            if (ftperConfig.isPasvMode()) {
                ftpClient.enterLocalPassiveMode();
            }

            // 设置传输模式
            setFileTransferMode(ftpClient, ftperConfig);

            // 设置文件类型（二进制）
            setFileType(ftpClient, ftperConfig);

            // 设置字符集
            if (ftperConfig.getFencoding() != null) {
                ftpClient.setControlEncoding(ftperConfig.getFencoding());
            }

            ftpClient.setControlKeepAliveTimeout(30);
            ftpClient.setControlKeepAliveReplyTimeout(ftperConfig.getTimeout() * 1000);

            return new ApacheFtper(ftpClient, ftperConfig);
        }
    }

    /**
     * @see io.github.ludongrong.netftp.IFtper#cloneFtper()
     */
    @Override
    public ApacheFtper cloneFtper() throws FtperException {
        return new Builder().build(ftperConfig);
    }
}
