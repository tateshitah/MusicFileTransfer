/*
Copyright (c) 2015 Hiroaki Tateshita

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
 */
package org.braincopy.mft;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

/**
 * This client program will get music files from server.
 *
 * @author Hiroaki Tateshita
 * @version 0.0.2
 */
public class Client extends Thread {
    public static final int INPUT_STREAM_BUFFER = 512;
    public static final int FILE_WRITE_BUFFER = 512;
    protected static String BASE_FOLDER = "tempMusic/";

    // private static String playlistFileName = "tokuniosuki.m3u";

    private String absolutePathOfMucisFolderOnServer;
    /**
     * list of relative path of music files
     */
    private ArrayList<String> copyTargetFilesPath;
    private ArrayList<String> filesPathsForM3U;
    // private String commandInputReader;
    private Socket socket;
    int waitCount = 0; // タイムアウト用
    byte[] fileBuff = new byte[FILE_WRITE_BUFFER]; // サーバからのファイル出力を受け取る

    int recvFileSize; // InputStreamから受け取ったファイルのサイズ

    private String hostname;
    private int portNumber;
    private boolean isRunning;
    private boolean updated;
    private boolean isCompleted;
    private String updatedStatus;
    private File playListOnClient;
    private String playListName = "gesuotome.m3u";

    Client() {
        this.copyTargetFilesPath = new ArrayList<String>();
        // check music folder in client
        BASE_FOLDER = Environment
                .getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
                + "/mft/";
        File musicFolder = new File(BASE_FOLDER);
        if (!musicFolder.exists()) {
            musicFolder.mkdirs();
        }
        this.filesPathsForM3U = new ArrayList<String>();
        this.updatedStatus = "";
        // this.musicFolder = musicFolder;
    }

    public final void run() {
        this.isCompleted = false;
        try {
            InetAddress address = InetAddress.getByName(getHostname());
            socket = new Socket(address, getPortNumber());
            isRunning = true;
            String command = "hello! this is client of Android.";
            this.setSocket(socket);
            try {
                sendCommand(command);
                InputStream inStream = socket.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(
                        inStream));

             //   setAbsolutePathOfMucisFolderOnServerFromServer();
               // command = "getpath";
                //sendCommand(command);

                //String[] getArgs = command.split(" ");
                //String buff = null;

                command = "playlist " + this.playListName;
                sendCommand(command);

                if (this.copyTargetFilesPath.size() == 0) {
                    Log.e("mft", "no target music files? wired.");
                    this.updateStatus("no target music files? wired.\n");
                } else {
                    startCopy();
                }
                command = "end";
                sendCommand(command);
                this.isCompleted = true;
            } catch (InterruptedException e) {
                Log.e("mft", "sending command: " + e);
                e.printStackTrace();
            }

        } catch (UnknownHostException e) {
            Log.e("error",
                    "something happnens in connection thread.unhostname? " + e);
        } catch (ConnectException e) {
            Log.e("error",
                    "hostname is correct? hostname: " + hostname + " : " + e);
            updateStatus("could not connect. network setting is correct? hostname: " + hostname);
        } catch (IOException e) {
            Log.e("error", "something happnens in connection thread. " + e);
        } finally {
            try {
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException e) {
                Log.e("error", "something happnens in connection thread. " + e);
            }
        }
    }

//    private void setAbsolutePathOfMucisFolderOnServerFromServer() throws IOException, InterruptedException {
  //      //command = "getpath";
    //    sendCommand("getpath");
    //}

    public boolean isCompleted() {
        return this.isCompleted;
    }

    private String sendCommand(String command) throws IOException,
            InterruptedException {
        String result = null;
        String[] getArgs = command.split(" ");
        String buff = null;
        try {
            OutputStream outStream = this.socket.getOutputStream();
            InputStream inStream = this.socket.getInputStream();
            BufferedReader messageReaderFromServer = new BufferedReader(
                    new InputStreamReader(inStream));
            // outStream.write(command.getBytes(), 0, command.length());
            outStream.write(command.getBytes());
            Log.i("mft", "send command: " + command);
            this.updateStatus("send command: " + command + "\n");
            waitCount = 0;
            while (true) {
                if (inStream.available() > 0) {
                    if (getArgs[0].equals("getpath")) {
                        buff = messageReaderFromServer.readLine();
                        //setAbsolutePathOfMusicFolderOnServer(buff);
                        //Todo why client needs this information, which should be server inside information.
                        Log.i("mft", "set AbsolutePathOfMusicFolderOnServer: " + buff);
                        updateStatus("get absolute path info from server: " + buff);
                        break;
                    } else if (getArgs[0].equals("playlist")) {
                        this.playListOnClient = new File(this.BASE_FOLDER + getArgs[1]);
                        FileOutputStream playListOS = new FileOutputStream(playListOnClient);
                        //OutputStreamWriter writer = new OutputStreamWriter(playListOS,"UTF-8");
                        String tempFilePath = null;
                        while ((buff = messageReaderFromServer.readLine()) != null) {
                            //if (buff.startsWith(absolutePathOfMucisFolderOnServer)) {
                            if (!buff.startsWith("#")) {
                                Log.i("mft", "playlist: " + buff);
                                checkAndAddMusicFile(buff);
                                //tempFilePath = buff.substring(absolutePathOfMucisFolderOnServer.length());
                                tempFilePath = buff;
                                playListOS.write((BASE_FOLDER + tempFilePath + "\n").getBytes());
                                //writer.write(BASE_FOLDER + tempFilePath + "\n");
                                this.filesPathsForM3U.add(BASE_FOLDER + tempFilePath);
                            } else if (buff.startsWith("#")) {
                                playListOS.write((buff + "\n").getBytes());
                            } else {
                                break;
                            }
                        }
                        break;
                    } else if (getArgs[0].equals("getsize")) {
                        buff = messageReaderFromServer.readLine();
                        result = buff;
                    } else if (getArgs[0].equals("get")) {
                        String filePathString = command.substring("get ".length());
                        String folderName = filePathString.substring(0,
                                filePathString.lastIndexOf("/"));
                        File folder = new File(BASE_FOLDER + folderName);
                        if (!folder.exists()) {
                            folder.mkdirs();
                        }
                        // create a music file by using outputStream getting from Server
                        FileOutputStream musicFileStreamInClient = new FileOutputStream(BASE_FOLDER
                                + filePathString);
                        while (true) {
                            if (inStream.available() > 0) {
                                recvFileSize = inStream.read(fileBuff);
                                musicFileStreamInClient.write(fileBuff, 0, recvFileSize);
                            } else {
                                waitCount++;
                                try {
                                    Thread.sleep(100);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                    throw new InterruptedException("trying to sleep: "
                                            + e.getMessage());
                                }
                                if (waitCount > 10)
                                    break;
                            }
                        }
                        break;
                    }
                }
                // タイムアウト処理
                else {
                    waitCount++;
                        try {
                            Thread.sleep(100);
                        Log.i("mft", "waiting... " + waitCount);
                        //updateStatus("waiting... " + waitCount + "\n");
                    } catch (InterruptedException e) {
                        throw new InterruptedException("timeout process: " + e);
                    }
                    if (waitCount > 10)
                        break;
                }
            }
            // messageReaderFromServer.close();
            // inStream.close();
            // outStream.close();
        } catch (IOException e) {
            throw new IOException("#trying to send command: " + e);
        }
        return result;

    }

    private void updateStatus(String statusMessage) {
        this.updatedStatus += statusMessage;
        this.updated = true;
    }

    public CharSequence getUpdatedStatus() {
        String result = null;
        result = new String(this.updatedStatus);
        this.updatedStatus = "";
        this.updated = false;
        return result;
    }

    public boolean isUpdated() {
        return updated;
    }

    /**
     * @param sock
     */
    public void setSocket(Socket sock) {
        this.socket = sock;
    }

    private void startCopy() throws IOException, InterruptedException {
        waitCount = 0;
        // InputStream from Server
        for (int i = 0; i < this.copyTargetFilesPath.size(); i++) {
            getMusicFile(this.copyTargetFilesPath.get(i));
            //sendCommand("get " + this.copyTargetFilesPath.get(i));
            checkMusicFile(this.copyTargetFilesPath.get(i));
        }
    }

    private boolean checkMusicFile(String filePathString) throws IOException,
            InterruptedException {
        boolean result = false;
        long client, server = Long.MIN_VALUE;
        String serverResponse = null;
        File fileInClient = new File(BASE_FOLDER + filePathString);
        client = fileInClient.length();
        try {
            serverResponse = sendCommand("getsize " + filePathString);
            if (serverResponse != null) {
                try {
                    server = Long.parseLong(serverResponse);
                } catch (NumberFormatException e) {
                    System.err.println("response is wired" + serverResponse);
                    Log.e("mft", "response is wired" + serverResponse);
                    server = Long.MIN_VALUE;
                }
            } else {
                Log.e("mft", "why server response is null? : "
                        + filePathString);
            }
            if (client != server) {
                System.out.println("different size: Client is: " + client
                        + ", Server is: " + server);
                fileInClient.delete();
            } else {
                System.out.println("Good! size of Client is: " + client
                        + ", size of Server is: " + server);
                result = true;
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new IOException("checking file: " + e);
        }
        return result;
    }


    /**
     * @param filePathString should be relative path
     * @throws IOException
     * @throws InterruptedException
     */
    private void getMusicFile(String filePathString) throws IOException,
            InterruptedException {
        FileOutputStream musicFileStreamInClient = null;
        String folderName = null;
        try {
            // send command to server to get stream
            OutputStream commandStreamToServer = socket.getOutputStream();
            InputStream inputStreamFromServer = socket.getInputStream();
            commandStreamToServer.write(("get " + filePathString + "\n")
                    .getBytes());
            folderName = filePathString.substring(0,
                    filePathString.lastIndexOf("/"));
            File folder = new File(BASE_FOLDER + folderName);
            if (!folder.exists()) {
                folder.mkdirs();
            }
            // create a music file by using outputStream getting from Server
            musicFileStreamInClient = new FileOutputStream(BASE_FOLDER
                    + filePathString);
            waitCount = 0;

            while (true) {
                if (inputStreamFromServer.available() > 0) {
                    recvFileSize = inputStreamFromServer.read(fileBuff);
                    musicFileStreamInClient.write(fileBuff, 0, recvFileSize);
                } else {
                    waitCount++;
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        throw new InterruptedException("trying to sleep: "
                                + e.getMessage());
                    }
                    if (waitCount > 100)
                        break;
                }
            }
            musicFileStreamInClient.close();
        } catch (IOException e) {
            throw new IOException(
                    "When I tryed to get output stream or input steam: " + e);
        }

    }

    private void addCopyTargetFilesPath(String buff) {
        this.copyTargetFilesPath.add(buff);
    }

    /**
     * Input String should an absolute file path in Server side. The absolute
     * path of music folder on server side should be sent by Server side. really?
     *
     * @param buff
     * @return
     */
    private void checkAndAddMusicFile(String buff) throws IOException, InterruptedException{
        String filePath = null;
        File copyFile = null;
//        filePath = buff.substring(this.absolutePathOfMucisFolderOnServer
  //              .length());
        filePath = buff;
        copyFile = new File(BASE_FOLDER + filePath);
        if (copyFile.exists()) {
            if (!checkMusicFile(filePath)) {
                //copyFile.delete();
                this.addCopyTargetFilesPath(filePath);
            }
            //compare the file size.
        } else {
            this.addCopyTargetFilesPath(filePath);
        }

    }


 //   private void setAbsolutePathOfMusicFolderOnServer(String path) {
   //     this.absolutePathOfMucisFolderOnServer = path;
    //}

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public int getPortNumber() {
        return portNumber;
    }

    public void setPortNumber(int portNumber) {
        this.portNumber = portNumber;
    }


    public String[] getMusicFilesPaths() {
        String[] result = null;
        if (this.playListOnClient != null) {
            result = new String[1 + this.filesPathsForM3U.size()];
            result[0] = this.playListOnClient.getAbsolutePath();
            for (int i = 0; i < this.filesPathsForM3U.size(); i++) {
                result[i + 1] = this.filesPathsForM3U.get(i);
            }
        }
        return result;
    }

    public void setRunning(Boolean _running) {
        this.isRunning = _running;
    }

    public void setUpdated(boolean updated) {
        this.updated = updated;
    }

    public void finish() {
        setRunning(false);
    }
}
