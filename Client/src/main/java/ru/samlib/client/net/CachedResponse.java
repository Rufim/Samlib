package ru.samlib.client.net;


import java.io.*;
import java.net.URI;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Created by Dmitry on 29.06.2015.
 */
public class CachedResponse extends File {
    private final Request request;
    public boolean isDownloadOver = false;
    public boolean isCached = false;
    public boolean arched = false;

    public CachedResponse(File dir, String name, Request request) {
        super(dir, name);
        this.request = request;
    }

    public CachedResponse(String path, Request request) {
        super(path);
        this.request = request;
    }

    public CachedResponse(String dirPath, String name, Request request) {
        super(dirPath, name);
        this.request = request;
    }

    public CachedResponse(URI uri, Request request) {
        super(uri);
        this.request = request;
    }

    public boolean isArched() {
        return arched;
    }

    public void setArched(boolean arched) {
        this.arched = arched;
    }

    public CachedResponse pack() throws IOException {
        byte[] buffer = new byte[4096];
        GZIPOutputStream zos = null;
        FileInputStream is = null;
        CachedResponse cachedResponse = new CachedResponse(getParentFile(), getName() + ".gzip", request.clone());
        try {
            zos = new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream(cachedResponse, false)));
            is = new FileInputStream(this);
            while (readStream(is, zos, buffer));
        } finally {
            if(zos != null) zos.close();
            if(is != null) is.close();;
        }
        return cachedResponse;
    }

    public CachedResponse unpack() throws IOException {
        byte[] buffer = new byte[4096];
        FileOutputStream os = null;
        InputStream zis = null;
        CachedResponse cachedResponse = new CachedResponse(getParentFile(), getName().replace(".gzip", ""), request.clone());
        try {
            os = new FileOutputStream(cachedResponse);
            zis = new GZIPInputStream(new FileInputStream(this));
            while (readStream(zis, os, buffer)) ;
        } finally {
            if (os != null) os.close();
            if (zis != null) zis.close();
        }
        return cachedResponse;
    }

    public static boolean readStream(InputStream is, OutputStream os, byte[] buffer) throws IOException {
        int count = 0;
        if ((count = is.read(buffer)) != -1) {
            if (count > 0) {
                os.write(buffer, 0, count);
            }
            return true;
        } else {
            return false;
        }
    }

    public static boolean readStream(InputStream is, DataOutput output, byte[] buffer) throws IOException {
        int count = 0;
        if ((count = is.read(buffer)) != -1) {
            if (count > 0) {
                output.write(buffer, 0, count);
            }
            return true;
        } else {
            return false;
        }
    }

    public String getEncoding() {
        return request.getEncoding();
    }

    public Request getRequest() {
        return request;
    }
}
