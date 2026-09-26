package com.viaversion.viaforge.common.compatibility;

import com.google.gson.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.util.Map;
import java.util.zip.*;

/** Download into a private partial file, validate, then publish the cache entry. */
public final class ServerPackDownload {
    private static final int MAX_BYTES=50*1024*1024;
    public static Path fetch(Path directory,String address,String expected,Proxy proxy,Map<String,String> headers)throws IOException {
        return fetch(directory,address,expected,proxy,headers,MAX_BYTES);
    }
    public static Path fetch(Path directory,String address,String expected,Proxy proxy,Map<String,String> headers,int maxBytes)throws IOException {
        return fetch(directory,address,expected,proxy,headers,maxBytes,true);
    }
    public static Path fetch(Path directory,String address,String expected,Proxy proxy,Map<String,String> headers,int maxBytes,boolean validateArchive)throws IOException {
        Files.createDirectories(directory); // Also re-establish a cache removed since client startup.
        String hash=expected.matches("(?i)[a-f0-9]{40}")?expected.toLowerCase(java.util.Locale.ROOT):null;
        String key=hash!=null?hash:sha1(address.getBytes(StandardCharsets.UTF_8));
        Path cached=directory.resolve(key);
        if(hash!=null&&Files.isRegularFile(cached)&&Files.size(cached)<=maxBytes&&hash.equals(sha1(Files.readAllBytes(cached)))) {
            if(validateArchive)validate(cached);return cached;
        }
        Path partial=Files.createTempFile(directory,"viaforge-",".part");
        try {
            URL url=new URL(address);
            if(!url.getProtocol().equals("http")&&!url.getProtocol().equals("https"))throw new IOException("Unsupported resource pack URL protocol");
            HttpURLConnection connection=(HttpURLConnection)url.openConnection(proxy);
            connection.setConnectTimeout(15000);connection.setReadTimeout(15000);
            for(Map.Entry<String,String> header:headers.entrySet())connection.setRequestProperty(header.getKey(),header.getValue());
            try {
                if(connection.getResponseCode()/100!=2)throw new IOException("Resource pack HTTP "+connection.getResponseCode());
                long length=connection.getContentLengthLong();if(length>maxBytes)throw new IOException("Resource pack exceeds "+maxBytes+" bytes");
                try(InputStream input=connection.getInputStream();OutputStream output=Files.newOutputStream(partial)) {
                    byte[] bytes=new byte[8192];int count,total=0;
                    while((count=input.read(bytes))!=-1) {
                        if(Thread.currentThread().isInterrupted())throw new InterruptedIOException("Resource pack request cancelled");
                        total+=count;if(total>maxBytes)throw new IOException("Resource pack exceeds "+maxBytes+" bytes");
                        output.write(bytes,0,count);
                    }
                    if(length>=0&&total!=length)throw new EOFException("Incomplete resource pack download");
                }
            } finally {connection.disconnect();}
            if(hash!=null&&!hash.equals(sha1(Files.readAllBytes(partial))))throw new IOException("Resource pack SHA-1 mismatch");
            if(validateArchive)validate(partial);
            try {Files.move(partial,cached,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}
            catch(AtomicMoveNotSupportedException unsupported){Files.move(partial,cached,StandardCopyOption.REPLACE_EXISTING);}
            return cached;
        } finally {Files.deleteIfExists(partial);}
    }
    public static void validate(Path file)throws IOException {
        try(ZipFile zip=new ZipFile(file.toFile())) {
            ZipEntry metadata=zip.getEntry("pack.mcmeta");if(metadata==null)throw new IOException("Resource pack has no pack.mcmeta");
            try(Reader reader=new InputStreamReader(zip.getInputStream(metadata),StandardCharsets.UTF_8)) {
                JsonObject pack=new JsonParser().parse(reader).getAsJsonObject().getAsJsonObject("pack");
                if(pack==null||!pack.has("description")||!pack.has("pack_format")&&!pack.has("min_format"))throw new IOException("Invalid pack metadata");
            } catch(RuntimeException invalid){throw new IOException("Invalid pack metadata",invalid);}
        }
    }
    private static String sha1(byte[] bytes) {
        try {StringBuilder out=new StringBuilder();for(byte b:MessageDigest.getInstance("SHA-1").digest(bytes))out.append(String.format(java.util.Locale.ROOT,"%02x",b&255));return out.toString();}
        catch(NoSuchAlgorithmException impossible){throw new AssertionError(impossible);}
    }
    private ServerPackDownload(){}
}
