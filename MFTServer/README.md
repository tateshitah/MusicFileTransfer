# MusicFileTransfer

* Concept
I want to sync my music files between MAC and android, but I could not find 
any free software to implement my request. So I will try to make it by myself.
This software will consist of server on MAC and client on Android.

* Server part should have following functions:
- wait for client first requests
- response copy plan of music files by using play list file
- wait for client specific requests for the music files.
- send requested files

* Client part should have following functions:
- send a first request.
- check existing music file comparing with copy plan from server
- send a specific request for files
- receive files.
 
