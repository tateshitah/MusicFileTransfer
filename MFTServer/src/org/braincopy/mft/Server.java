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
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * for MAC
 * 
 * @author Hiroaki Tateshita
 * @version 0.2.1
 *
 */
public class Server {
	public static final int INPUT_STREAM_BUFFER = 512;
	public static final int FILE_READ_BUFFER = 512;
	private static final String MUSIC_FOLDER = "/Users/thiro/Music/iTunes/iTunes Media/Music/";

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		ServerSocket servSock = null;
		Socket sock = null;
		int port = 6001;
		OutputStream outStream = null;
		InputStream inStream = null;
		FileInputStream fileInStream;
		byte[] inputBuff = new byte[INPUT_STREAM_BUFFER];
		byte[] fileBuff = new byte[FILE_READ_BUFFER];

		String absolutePath = new File(MUSIC_FOLDER).getAbsolutePath();
		File file = new File(absolutePath);
		String[] fileList = file.list();
		// Boolean hiddenFileFlag;
		// Boolean lsFlag;
		int recvByteLength;
		boolean isRunning = true;

		try {
			// this is socket of server itself
			servSock = new ServerSocket(port);
			servSock.setReuseAddress(true);

			InetAddress address = InetAddress.getLocalHost();
			System.out.println("server started! IP is: " + address.getHostAddress());

			// sock = servSock.accept();
			// outStream = sock.getOutputStream();
			// inStream = sock.getInputStream();
		} catch (IOException e) {
			System.err.println("Exception on Server: " + e);
			e.printStackTrace();
			System.exit(1);
		}

		File playlistFile = null;
		BufferedReader bufferedReader = null;
		String tempStr;

		while (isRunning) {
			if (sock == null || sock.isClosed()) {
				try {
					// this is socket between client and server.
					// when the client connects, this socket will be activated.
					sock = servSock.accept();
					outStream = sock.getOutputStream();
					inStream = sock.getInputStream();
				} catch (IOException e) {
					System.err.println("tried to create socket between server and client but...");
					e.printStackTrace();
				}
			}

			try {
				recvByteLength = inStream.read(inputBuff);
				if (recvByteLength > 0) {
					String buff = new String(inputBuff, 0, recvByteLength);
					System.out.println("received message from client: " + buff);

					String[] getArgs = buff.split("\\s");

					// in case of list playlistName
					if (getArgs[0].equals("playlist") && getArgs.length > 1) {
						playlistFile = new File(MUSIC_FOLDER + getArgs[1]);
						if (playlistFile.exists()) {
							System.out.println(playlistFile.getName() + " exists!");
							bufferedReader = new BufferedReader(new FileReader(playlistFile));
							while ((tempStr = bufferedReader.readLine()) != null) {
								// if (!tempStr.startsWith("#")) {
								outStream.write(tempStr.getBytes());
								outStream.write("\n".getBytes());
								System.out.println("send to client: " + tempStr);
								// }
							}
							outStream.write(("playlist: " + getArgs[1]).getBytes());
							outStream.write("\n".getBytes());

						} else {
							outStream.write(("no playlist: " + playlistFile.getAbsolutePath()).getBytes());
							outStream.write("\n".getBytes());
						}
					}
					// getpath
					if (getArgs[0].equals("getpath")) {
						outStream.write(MUSIC_FOLDER.getBytes());
						outStream.write("\n".getBytes());
					}

					// getsize
					if (getArgs[0].equals("getsize")) {
						getArgs[1] = buff.substring("getsize ".length());
						String fileName = MUSIC_FOLDER + getArgs[1];
						String temp = null;
						File fileInServer = new File(fileName);
						if (fileInServer.exists()) {
							temp = String.valueOf(fileInServer.length());
							outStream.write(temp.getBytes());
							outStream.write("\n".getBytes());
							System.out.println("#3 size of " + fileName + " is: " + temp);
						}
					}

					// ls
					if (getArgs[0].equals("ls")) {
						for (int i = 0; i < fileList.length; i++) {
							outStream.write(fileList[i].getBytes());
							outStream.write("\n".getBytes());
						}
					}

					if (getArgs[0].equals("get")) {
						// last char is "¥n", so I deleted by using substring.
						getArgs[1] = buff.substring("get ".length(), buff.length() - 1);
						fileInStream = new FileInputStream((MUSIC_FOLDER + getArgs[1]));
						int fileLength = 0;
						// System.out.println("Create stream " + getArgs[1] );

						System.out.println("sending file to client: " + MUSIC_FOLDER + getArgs[1]);
						int i = 0;
						while ((fileLength = fileInStream.read(fileBuff)) != -1) {
							i++;
							outStream.write(fileBuff, 0, fileLength);
							if (i % 20 == 0) {
								System.out.print(".");
							}
						}
						System.out.println("\nClose file input stream on server side : " + getArgs[1]);
						fileInStream.close();
					}
					if (getArgs[0].equals("end")) {
						sock.close();
						System.out.println("one client connection ended.");
					}
					// exitの場合
					if (getArgs[0].equals("exit")) {
						outStream.close();
						System.out.println("close server");
						break;
					}
				}
			} catch (IOException e) {
				System.err.println("something related to I/O happened.");
				e.printStackTrace();
				// System.exit(0);
			}

		}

		try {
			outStream.close();
			inStream.close();
		} catch (IOException e) {
			System.err.println("Exception on Server: " + e);
			e.printStackTrace();
		}

		try {
			sock.close();
			servSock.close();
		} catch (IOException e) {
			System.err.println("Exception on Server: " + e);
			e.printStackTrace();
		}

	}

}
