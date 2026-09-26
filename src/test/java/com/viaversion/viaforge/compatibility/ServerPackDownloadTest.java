package com.viaversion.viaforge.compatibility;

import com.viaversion.viaforge.common.compatibility.ServerPackDownload;
import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.*;
import org.junit.*;
import static org.junit.Assert.*;

public class ServerPackDownloadTest {
    private Path root;
    private HttpServer server;
    private String address;
    private byte[] valid;
    private final AtomicInteger requests=new AtomicInteger();
    @Before public void setup()throws Exception {
        root=Files.createTempDirectory("viaforge-pack-test");
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        try(ZipOutputStream zip=new ZipOutputStream(bytes)) {
            zip.putNextEntry(new ZipEntry("pack.mcmeta"));zip.write("{\"pack\":{\"pack_format\":1,\"description\":\"test\"}}".getBytes("UTF-8"));zip.closeEntry();
            zip.putNextEntry(new ZipEntry("assets/minecraft/textures/test.txt"));zip.write(new byte[]{1,2,3});zip.closeEntry();
        }
        valid=bytes.toByteArray();server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/",exchange->{
            requests.incrementAndGet();String path=exchange.getRequestURI().getPath();byte[] body=path.equals("/valid")?valid:new byte[]{1,2,3};
            exchange.sendResponseHeaders(path.equals("/error")?503:200,body.length);
            try(OutputStream out=exchange.getResponseBody()){out.write(body);}
        });server.start();address="http://127.0.0.1:"+server.getAddress().getPort();
    }
    @After public void teardown()throws Exception {
        server.stop(0);
        try(java.util.stream.Stream<Path> files=Files.walk(root)){for(Path p:(Iterable<Path>)files.sorted(Comparator.reverseOrder())::iterator)Files.delete(p);}
    }
    private Path fetch(Path cache,String path,String hash)throws IOException{return ServerPackDownload.fetch(cache,address+path,hash,Proxy.NO_PROXY,Collections.<String,String>emptyMap());}
    private String hash()throws Exception {
        StringBuilder result=new StringBuilder();for(byte b:java.security.MessageDigest.getInstance("SHA-1").digest(valid))result.append(String.format("%02x",b&255));return result.toString();
    }
    @Test public void absentCacheAndVerifiedReuse()throws Exception {
        Path cache=root.resolve("absent/nested");assertFalse(Files.exists(cache));
        Path result=fetch(cache,"/valid",hash());assertArrayEquals(valid,Files.readAllBytes(result));
        assertEquals(result,fetch(cache,"/error",hash()));assertEquals(1,requests.get());
    }
    @Test public void ordinaryFileAtCachePathIsPreservedAndFails()throws Exception {
        Path cache=root.resolve("cache");byte[] user={9,8,7};Files.write(cache,user);
        try{fetch(cache,"/valid",hash());fail("Non-directory must fail");}catch(IOException expected){}
        assertArrayEquals(user,Files.readAllBytes(cache));assertEquals(0,requests.get());
    }
    @Test public void errorsAndInvalidArchivesDoNotPublish()throws Exception {
        Path cache=root.resolve("cache");
        for(String path:new String[]{"/invalid","/error"}) {
            try{fetch(cache,path,"");fail("Invalid download must fail");}catch(IOException expected){}
            try(java.util.stream.Stream<Path> files=Files.list(cache)){assertEquals(0,files.count());}
        }
    }
    @Test public void wrongHashDoesNotReplaceExistingCacheOrUserFiles()throws Exception {
        Path cache=root.resolve("cache");Files.createDirectories(cache);String wrong="0000000000000000000000000000000000000000";
        Files.write(cache.resolve(wrong),new byte[]{7});Files.write(cache.resolve("user.zip"),new byte[]{8});
        try{fetch(cache,"/valid",wrong);fail("Hash mismatch must fail");}catch(IOException expected){}
        assertArrayEquals(new byte[]{7},Files.readAllBytes(cache.resolve(wrong)));assertArrayEquals(new byte[]{8},Files.readAllBytes(cache.resolve("user.zip")));
    }
    @Test public void recreatesCacheRemovedBetweenRequests()throws Exception {
        Path cache=root.resolve("cache");Path file=fetch(cache,"/valid",hash());Files.delete(file);Files.delete(cache);
        assertArrayEquals(valid,Files.readAllBytes(fetch(cache,"/valid",hash())));assertEquals(2,requests.get());
    }
    @Test public void actualSizeLimitLeavesCacheUnpublished()throws Exception {
        Path cache=root.resolve("limited");
        try{ServerPackDownload.fetch(cache,address+"/valid",hash(),Proxy.NO_PROXY,Collections.emptyMap(),valid.length-1);fail();}
        catch(IOException expected){assertTrue(expected.getMessage().contains("exceeds"));}
        try(java.util.stream.Stream<Path> files=Files.list(cache)){assertEquals(0,files.count());}
    }
    @Test public void modernDownloadAndArchiveValidationAreDistinctStages()throws Exception {
        Path file=ServerPackDownload.fetch(root.resolve("modern"),address+"/invalid","",Proxy.NO_PROXY,Collections.emptyMap(),1024,false);
        assertArrayEquals(new byte[]{1,2,3},Files.readAllBytes(file));
        try{ServerPackDownload.validate(file);fail();}catch(IOException expected){}
    }
}
