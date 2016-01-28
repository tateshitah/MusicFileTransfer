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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.util.ArrayList;

/**
 * This client program will get music files from server. In the future, this
 * program will be available on android!
 * 
 * @author Hiroaki Tateshita
 * @version 0.1.1
 *
 */
public class Client {
	public static final int INPUT_STREAM_BUFFER = 512;
	public static final int FILE_WRITE_BUFFER = 512;
	protected static final String BASE_FOLDER = "tempMusic/";

	// private static String playlistFileName = "tokuniosuki.m3u";

	private String absolutePathOfMucisFolderOnServer;
	/**
	 * list of relative path of music files
	 */
	private ArrayList<String> copyTargetFilesPath;
	// private String commandInputReader;
	private Socket socket;
	int waitCount = 0; // タイムアウト用
	byte[] fileBuff = new byte[FILE_WRITE_BUFFER]; // サーバからのファイル出力を受け取る

	int recvFileSize; // InputStreamから受け取ったファイルのサイズ

	// private File musicFolder;

	Client() {
		this.copyTargetFilesPath = new ArrayList<String>();
		// check music folder in client
		File musicFolder = new File(BASE_FOLDER);
		if (!musicFolder.exists()) {
			musicFolder.mkdirs();
		}
		// this.musicFolder = musicFolder;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Client client = new Client();
		/*
		 * if (args.length != 2) { System.err.println("argument error");
		 * System.exit(1); }
		 */
		int port = 6001;
		String host = "192.168.1.3";
		OutputStream outStream; // 送信用ストリーム
		InputStream inStream; // 受信用ストリーム
		// FileOutputStream fileOutStream; // ファイルの書き込むためのストリーム

		// byte[] inputBuff = new byte[INPUT_STREAM_BUFFER]; // サーバからのls出力を受け取る
		// byte[] fileBuff = new byte[FILE_WRITE_BUFFER]; // サーバからのファイル出力を受け取る

		String command; // キーボードからの入力を格納
		// int recvFileSize; // InputStreamから受け取ったファイルのサイズ
		// int recvByteLength = 0; // 受信したファイルのバイト数格納

		try {

			Socket sock = new Socket(host, port);

			client.setSocket(sock);

			outStream = sock.getOutputStream();
			inStream = sock.getInputStream();

			command = "getpath";
			client.sendCommand(command);

			command = "playlist WestSideStory.m3u";
			client.sendCommand(command);

			client.startCopy();

			command = "end";
			client.sendCommand(command);

			outStream.close();
			inStream.close();
			sock.close();
		} catch (ConnectException e) {
			System.err.println("hostname is correct? host = " + host + " : "
					+ e);
		} catch (Exception e) {
			// 例外表示
			System.err.println(e);
			e.printStackTrace();
			System.exit(1);
		}
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
			System.out.println("send command: " + command);
			waitCount = 0;
			while (true) {
				if (inStream.available() > 0) {
					if (getArgs[0].equals("getpath")) {
						buff = messageReaderFromServer.readLine();
						setAbsolutePathOfMusicFolderOnServer(buff);
						// System.out.println("#path " + buff);
						break;
					} else if (getArgs[0].equals("playlist")) {
						File playListOnClient = new File(this.BASE_FOLDER
								+ getArgs[1]);
						FileWriter playListWriter = new FileWriter(
								playListOnClient);
						String tempFilePath = null;
						String baseFolderName = new File(BASE_FOLDER)
								.getAbsolutePath();
						while ((buff = messageReaderFromServer.readLine()) != null) {
							if (buff.startsWith(absolutePathOfMucisFolderOnServer)) {
								// System.out.println("#1 " + buff);
								checkAndAddMusicFile(buff);
								tempFilePath = baseFolderName
										+ File.separator
										+ buff.substring(absolutePathOfMucisFolderOnServer
												.length());
								playListWriter.write(tempFilePath + "\n");
							} else if (buff.startsWith("#")) {
								playListWriter.write(buff + "\n");
							} else {
								break;
							}
						}
						playListWriter.close();
						break;
					} else if (getArgs[0].equals("getsize")) {
						buff = messageReaderFromServer.readLine();
						result = buff;
						System.out.println("size is:" + buff);
						break;
					}
				} else {
					waitCount++;
					try {
						Thread.sleep(100);
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

	private void setSocket(Socket sock) {
		this.socket = sock;
	}

	private void startCopy() throws IOException, InterruptedException {
		waitCount = 0;
		// InputStream from Server
		for (int i = 0; i < this.copyTargetFilesPath.size(); i++) {
			getMusicFile(this.copyTargetFilesPath.get(i));
			checkMusicFile(this.copyTargetFilesPath.get(i));
		}
	}

	private void checkMusicFile(String filePathString) throws IOException,
			InterruptedException {
		long client, server = Long.MIN_VALUE;
		String serverResponse = null;
		File fileInClient = new File(BASE_FOLDER + filePathString);
		client = fileInClient.length();
		try {
			serverResponse = sendCommand("getsize " + filePathString);
			if (serverResponse != null) {
				server = Long.parseLong(serverResponse);
			} else {
				System.err.println("why server response is null? : "
						+ filePathString);
			}
			if (client != server) {
				System.out.println("different! size of Client is: " + client
						+ ", size of Server is: " + server
						+ " So the copying file was deleted.");
				fileInClient.delete();
			} else {
				System.out.println("Good! size of Client is: " + client
						+ ", size of Server is: " + server);
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new IOException("checking file: " + e);
		}
	}

	/**
	 * 
	 * @param filePathString
	 *            should be relative path
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
				}

				else {
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
			musicFileStreamInClient.close();
		} catch (IOException e) {
			throw new IOException(
					"When I tryed to get output stream or input steam: " + e);
		}

	}

	private void add(String buff) {
		this.copyTargetFilesPath.add(buff);
	}

	/**
	 * Input String should an absolute file path in Server side. The absolute
	 * path of music folder on server side should be sent by Server side.
	 * 
	 * @param buff
	 * @return
	 */
	private void checkAndAddMusicFile(String buff) {
		String filePath = null;
		File file = new File(buff);
		File copyFile = null;
		if (file.exists() && buff.startsWith(absolutePathOfMucisFolderOnServer)) {
			filePath = buff.substring(this.absolutePathOfMucisFolderOnServer
					.length());
			copyFile = new File(BASE_FOLDER + filePath);
			if (!copyFile.exists()) {
				this.add(filePath);
			}
		}
	}

	private void setAbsolutePathOfMusicFolderOnServer(String path) {
		this.absolutePathOfMucisFolderOnServer = path;
	}
}
