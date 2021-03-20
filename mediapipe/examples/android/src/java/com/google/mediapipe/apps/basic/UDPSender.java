// Copyright 2021 Hiroaki Yaguchi, 947D-Tech.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.mediapipe.apps.basic;

import android.util.Log;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.UnknownHostException;
import java.net.SocketException;
import org.json.JSONException;

public class UDPSender extends Thread {
  private static final String TAG = "MainActivity";

  String json_str;
  boolean updated;
  boolean running;
  InetAddress address;
  int port;

  UDPSender() {
    json_str = new String("");
    updated = false;
    running = true;
  }

  public void setIP(String addr) {
    try {
      address = InetAddress.getByName(addr);
    } catch (UnknownHostException e) {
      Log.w(TAG, e.getMessage());
    }
  }

  public void setPort(int p) {
    port = p;
  }

  public void setRunning(boolean r) {
    running = r;
  }

  public void setJsonData(JSONObject json_data) {
    Log.v(TAG, "get json data");
    synchronized(this) {
      json_str = json_data.toString();
      updated = true;
    }
  }

  public void run() {
    try {
      DatagramSocket socket = new DatagramSocket();
      while (running) {
        synchronized(this) {
          if (updated) {
            try {
              byte[] buffer = json_str.getBytes("UTF-8");
              DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, port);
              try {
                socket.send(packet);
              } catch (IOException e) {
                Log.w(TAG, e.getMessage());
              }
            } catch (UnsupportedEncodingException e) {
              Log.w(TAG, e.getMessage());
            }
            updated = false;
          }
        }
        try {
          Thread.sleep(16);
        } catch (InterruptedException e) {
          Log.w(TAG, e.getMessage());
        }
      }
      socket.close();
    } catch (SocketException e) {
      Log.w(TAG, e.getMessage());
    }
  }

}