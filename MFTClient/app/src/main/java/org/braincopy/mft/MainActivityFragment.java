package org.braincopy.mft;

import android.app.Fragment;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.OnScanCompletedListener;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;
import java.net.Socket;


/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment {

    public MainActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final View rootView = inflater.inflate(R.layout.fragment_main,
                container, false);

        final TextView status = (TextView) rootView.findViewById(R.id.statusTextView);
        Button startButton = (Button) rootView.findViewById(R.id.startButton);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                status.append("started!\n");
                Client client = new Client();
                int port = 6001;
                String host = "192.168.1.3";
//                String host = "10.222.0.141";
                client.setHostname(host);
                client.setPortNumber(port);
                client.start();
                while (client.isAlive()) {
                    if (client.isUpdated()) {
                        status.append(client.getUpdatedStatus());
                        client.setUpdated(false);
                    } else if (client.isCompleted()) {
                        String[] paths = client.getMusicFilesPaths();
                        if (paths != null) {
                            MediaScannerConnection.scanFile(getActivity().getApplicationContext(), paths, null, new OnScanCompletedListener() {

                                @Override
                                public void onScanCompleted(String path, Uri uri) {
                                    Log.i("mft", path + " scanned");
                                    //status.append(path+" is scanned.\n");
                                }
                            });
                        } else {
                            status.append("no music files! Client did not work as we expected.\n");
                            Log.e("mft", "wired. no music files! Client did not work as we expected.");
                        }
                        client.finish();
                    } else {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                /*
                if (client.isCompleted()) {
                    String[] paths = client.getMusicFilesPaths();
                    if (paths != null) {
                        MediaScannerConnection.scanFile(getActivity().getApplicationContext(), paths, null, new OnScanCompletedListener() {

                            @Override
                            public void onScanCompleted(String path, Uri uri) {
                                Log.i("mft", path + " scanned");
                                //status.append(path+" is scanned.\n");
                            }
                        });
                    } else {
                        status.append("no music files! Client did not work as we expected.\n");
                        Log.e("mft", "wired. no music files! Client did not work as we expected.");
                    }
                } else {
                    status.append(client.getUpdatedStatus());
                }*/
            }
        });
        return rootView;
    }
}
