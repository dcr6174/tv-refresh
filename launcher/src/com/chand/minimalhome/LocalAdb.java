package com.chand.minimalhome;

import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.math.BigInteger;
import java.security.*;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.*;
import java.util.Base64;
import javax.crypto.Cipher;

/** Short-lived local ADB connection. Its dedicated key is generated in private app storage. */
final class LocalAdb implements AutoCloseable {
    private final Socket socket=new Socket();
    private DataInputStream input;
    private OutputStream output;
    private int nextId=1;
    private static final int CNXN=command("CNXN"),AUTH=command("AUTH"),OPEN=command("OPEN"),OKAY=command("OKAY"),WRTE=command("WRTE"),CLSE=command("CLSE");
    private static int command(String value){byte[] b=value.getBytes(StandardCharsets.US_ASCII);return (b[0]&255)|((b[1]&255)<<8)|((b[2]&255)<<16)|((b[3]&255)<<24);}
    private static final class Packet {int command,a,b;byte[] data;}
    LocalAdb(File directory,String host,int port,Runnable needsApproval)throws Exception{
        KeyPair pair=keyPair(directory);
        try{
            socket.connect(new InetSocketAddress(host,port),4000);socket.setSoTimeout(90000);
            input=new DataInputStream(socket.getInputStream());output=socket.getOutputStream();
            send(CNXN,0x01000000,4096,"host::minimal-home-cleaner\0".getBytes(StandardCharsets.UTF_8));
            boolean signed=false,publicSent=false;
            for(int attempt=0;attempt<5;attempt++){
                Packet packet=read();
                if(packet.command==CNXN){socket.setSoTimeout(120000);return;}
                if(packet.command!=AUTH||packet.a!=1)throw new IOException("Unexpected authorization response");
                if(!signed){send(AUTH,2,0,sign(pair.getPrivate(),packet.data));signed=true;}
                else if(!publicSent){
                    send(AUTH,3,0,publicKey((RSAPublicKey)pair.getPublic()));publicSent=true;
                    if(needsApproval!=null)needsApproval.run();
                }else throw new IOException("TV authorization was declined");
            }
            throw new IOException("TV authorization did not finish");
        }catch(Exception e){close();throw e;}
    }
    String shell(String command)throws IOException{
        int local=nextId++,remote=0;
        send(OPEN,local,0,("shell:"+command+"\0").getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream result=new ByteArrayOutputStream();
        for(;;){
            Packet packet=read();
            if(packet.b!=local)continue;
            if(packet.command==OKAY)remote=packet.a;
            else if(packet.command==WRTE){remote=packet.a;result.write(packet.data);send(OKAY,local,remote,new byte[0]);}
            else if(packet.command==CLSE){send(CLSE,local,packet.a,new byte[0]);return result.toString("UTF-8");}
            if(result.size()>1024*1024)throw new IOException("ADB response too large");
        }
    }
    private void send(int command,int a,int b,byte[] data)throws IOException{
        int checksum=0;for(byte value:data)checksum+=value&255;
        ByteBuffer header=ByteBuffer.allocate(24).order(ByteOrder.LITTLE_ENDIAN);
        header.putInt(command).putInt(a).putInt(b).putInt(data.length).putInt(checksum).putInt(command^0xFFFFFFFF);
        output.write(header.array());output.write(data);output.flush();
    }
    private Packet read()throws IOException{
        byte[] header=new byte[24];input.readFully(header);
        ByteBuffer bytes=ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN);Packet packet=new Packet();
        packet.command=bytes.getInt();packet.a=bytes.getInt();packet.b=bytes.getInt();int length=bytes.getInt(),checksum=bytes.getInt(),magic=bytes.getInt();
        if(length<0||length>1024*1024||magic!=(packet.command^0xFFFFFFFF))throw new IOException("Invalid ADB packet");
        packet.data=new byte[length];input.readFully(packet.data);int sum=0;for(byte value:packet.data)sum+=value&255;
        if(checksum!=0&&sum!=checksum)throw new IOException("ADB checksum mismatch");
        return packet;
    }
    private static KeyPair keyPair(File directory)throws Exception{
        directory.mkdirs();File privateFile=new File(directory,"cleaner-private.pk8"),publicFile=new File(directory,"cleaner-public.der");
        KeyFactory factory=KeyFactory.getInstance("RSA");
        if(privateFile.exists()&&publicFile.exists())return new KeyPair(factory.generatePublic(new X509EncodedKeySpec(readFile(publicFile))),factory.generatePrivate(new PKCS8EncodedKeySpec(readFile(privateFile))));
        KeyPairGenerator generator=KeyPairGenerator.getInstance("RSA");generator.initialize(2048);KeyPair pair=generator.generateKeyPair();
        writeFile(privateFile,pair.getPrivate().getEncoded());writeFile(publicFile,pair.getPublic().getEncoded());return pair;
    }
    private static byte[] readFile(File file)throws IOException{try(InputStream in=new FileInputStream(file)){ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[4096];int n;while((n=in.read(buffer))!=-1)out.write(buffer,0,n);return out.toByteArray();}}
    private static void writeFile(File file,byte[] data)throws IOException{try(OutputStream out=new FileOutputStream(file)){out.write(data);}file.setReadable(false,false);file.setWritable(false,false);file.setReadable(true,true);file.setWritable(true,true);}
    private static byte[] sign(PrivateKey key,byte[] token)throws Exception{
        byte[] prefix={0x30,0x21,0x30,0x09,0x06,0x05,0x2b,0x0e,0x03,0x02,0x1a,0x05,0x00,0x04,0x14};
        byte[] digest=new byte[prefix.length+token.length];System.arraycopy(prefix,0,digest,0,prefix.length);System.arraycopy(token,0,digest,prefix.length,token.length);
        Cipher cipher=Cipher.getInstance("RSA/ECB/PKCS1Padding");cipher.init(Cipher.ENCRYPT_MODE,key);return cipher.doFinal(digest);
    }
    private static byte[] little(BigInteger value,int size){byte[] big=value.toByteArray(),out=new byte[size];for(int i=0;i<size&&i<big.length;i++)out[i]=big[big.length-1-i];return out;}
    private static byte[] publicKey(RSAPublicKey key){
        BigInteger n=key.getModulus(),word=BigInteger.ONE.shiftLeft(32);
        int inverse=word.subtract(n.mod(word).modInverse(word)).intValue();
        ByteBuffer binary=ByteBuffer.allocate(524).order(ByteOrder.LITTLE_ENDIAN);
        binary.putInt(64).putInt(inverse).put(little(n,256)).put(little(BigInteger.ONE.shiftLeft(4096).mod(n),256)).putInt(key.getPublicExponent().intValue());
        return (Base64.getEncoder().encodeToString(binary.array())+" minimal-home-cleaner@tv\0").getBytes(StandardCharsets.UTF_8);
    }
    @Override public void close(){try{socket.close();}catch(IOException ignored){}}
}
