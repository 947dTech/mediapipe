// Copyright 2020 The MediaPipe Authors.
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

// NOTE: This file is copied from posetrackinggpu and modified to use holistictracking.
//  Author of modification: Hiroaki Yaguchi, 947D-Tech.

package com.google.mediapipe.apps.holistictrackinggpu;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import com.google.mediapipe.formats.proto.LandmarkProto.NormalizedLandmark;
import com.google.mediapipe.formats.proto.LandmarkProto.NormalizedLandmarkList;
import com.google.mediapipe.formats.proto.LandmarkProto.Landmark;
import com.google.mediapipe.formats.proto.LandmarkProto.LandmarkList;
import com.google.mediapipe.framework.PacketGetter;
import com.google.protobuf.InvalidProtocolBufferException;

/** Main activity of MediaPipe hlistic tracking app. */
public class MainActivity extends com.google.mediapipe.apps.basic.MainActivity implements SensorEventListener {
  private static final String TAG = "MainActivity";

  private JSONObject jsonObj;

  private SensorManager sensorManager;
  private Sensor sensor;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
 
    jsonObj = new JSONObject();

    sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
    sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);

    // pose
    processor.addPacketCallback(
      "pose_landmarks",
      (packet) -> {
        Log.v(TAG, "Received pose landmarks packet.");
        try {
          byte[] landmarksRaw = PacketGetter.getProtoBytes(packet);
          NormalizedLandmarkList landmarks =
            NormalizedLandmarkList.parseFrom(landmarksRaw);

          if (landmarks.getLandmarkCount() > 0) {
            JSONArray jsonArr = convertLandmarksToJson(landmarks);
            synchronized(this) {
              try {
                jsonObj.put("pose_landmarks", jsonArr);
                long pose_stamp = packet.getTimestamp();
                jsonObj.put("pose_landmarks_stamp", pose_stamp);

                // check timestamp for each landmarks
                if (jsonObj.has("pose_world_landmarks_stamp")) {
                  long pose_world_stamp = jsonObj.getLong("pose_world_landmarks_stamp");
                  if ((pose_world_stamp - pose_stamp) < -3e5) {
                    jsonObj.remove("pose_world_landmarks");
                    jsonObj.remove("pose_world_landmarks_stamp");
                  }
                }

                if (jsonObj.has("face_landmarks_stamp")) {
                  long face_stamp = jsonObj.getLong("face_landmarks_stamp");
                  if ((face_stamp - pose_stamp) < -3e5) {
                    jsonObj.remove("face_landmarks");
                    jsonObj.remove("face_landmarks_stamp");
                  }
                }

                if (jsonObj.has("right_hand_landmarks_stamp")) {
                  long rhand_stamp = jsonObj.getLong("right_hand_landmarks_stamp");
                  if ((rhand_stamp - pose_stamp) < -3e5) {
                    jsonObj.remove("right_hand_landmarks");
                    jsonObj.remove("right_hand_landmarks_stamp");
                  }
                }

                if (jsonObj.has("left_hand_landmarks_stamp")) {
                  long lhand_stamp = jsonObj.getLong("left_hand_landmarks_stamp");
                  if ((lhand_stamp - pose_stamp) < -3e5) {
                    jsonObj.remove("left_hand_landmarks");
                    jsonObj.remove("left_hand_landmarks_stamp");
                  }
                }

                // call setJsonData ONLY in pose_landmarks
                sender.setJsonData(jsonObj);
              } catch (JSONException e) {
                Log.e(TAG, "Pose: " + e.getMessage());
              }
            }
          }
        } catch (InvalidProtocolBufferException exception) {
          Log.e(TAG, "Pose: Failed to get proto.", exception);
        }
      });

    // pose_world
    processor.addPacketCallback(
      "pose_world_landmarks",
      (packet) -> {
        Log.v(TAG, "Received pose world landmarks packet.");
        try {
          byte[] landmarksRaw = PacketGetter.getProtoBytes(packet);
          LandmarkList landmarks =
            LandmarkList.parseFrom(landmarksRaw);

          if (landmarks.getLandmarkCount() > 0) {
            JSONArray jsonArr = convertLandmarksToJson(landmarks);
            synchronized(this) {
              try {
                jsonObj.put("pose_world_landmarks", jsonArr);
                jsonObj.put("pose_world_landmarks_stamp", packet.getTimestamp());
              } catch (JSONException e) {
                Log.e(TAG, "Pose Wolrd: " + e.getMessage());
              }
            }
          }
        } catch (InvalidProtocolBufferException exception) {
          Log.e(TAG, "Pose World: Failed to get proto.", exception);
        }
      });

    // face
    processor.addPacketCallback(
      "face_landmarks",
      (packet) -> {
        Log.v(TAG, "Received face landmarks packet.");
        try {
          byte[] landmarksRaw = PacketGetter.getProtoBytes(packet);
          NormalizedLandmarkList landmarks =
            NormalizedLandmarkList.parseFrom(landmarksRaw);

          if (landmarks.getLandmarkCount() > 0) {
            JSONArray jsonArr = convertLandmarksToJson(landmarks);
            synchronized(this) {
              try {
                jsonObj.put("face_landmarks", jsonArr);
                jsonObj.put("face_landmarks_stamp", packet.getTimestamp());
              } catch (JSONException e) {
                Log.e(TAG, "Face: " + e.getMessage());
              }
            }
          }
        } catch (InvalidProtocolBufferException exception) {
          Log.e(TAG, "Face: Failed to get proto.", exception);
        }
      });

    // left hand
    processor.addPacketCallback(
      "left_hand_landmarks",
      (packet) -> {
        Log.v(TAG, "Received left hand landmarks packet.");
        try {
          byte[] landmarksRaw = PacketGetter.getProtoBytes(packet);
          NormalizedLandmarkList landmarks =
            NormalizedLandmarkList.parseFrom(landmarksRaw);

          if (landmarks.getLandmarkCount() > 0) {
            JSONArray jsonArr = convertLandmarksToJson(landmarks);
            synchronized(this) {
              try {
                jsonObj.put("left_hand_landmarks", jsonArr);
                jsonObj.put("left_hand_landmarks_stamp", packet.getTimestamp());
              } catch (JSONException e) {
                Log.e(TAG, "Left hand: " + e.getMessage());
              }
            }
          }
        } catch (InvalidProtocolBufferException exception) {
          Log.e(TAG, "Left hand: Failed to get proto.", exception);
        }
      });

    // right hand
    processor.addPacketCallback(
      "right_hand_landmarks",
      (packet) -> {
        Log.v(TAG, "Received right hand landmarks packet.");
        try {
          byte[] landmarksRaw = PacketGetter.getProtoBytes(packet);
          NormalizedLandmarkList landmarks =
            NormalizedLandmarkList.parseFrom(landmarksRaw);

          if (landmarks.getLandmarkCount() > 0) {
            JSONArray jsonArr = convertLandmarksToJson(landmarks);
            synchronized(this) {
              try {
                jsonObj.put("right_hand_landmarks", jsonArr);
                jsonObj.put("right_hand_landmarks_stamp", packet.getTimestamp());
              } catch (JSONException e) {
                Log.e(TAG, "Right hand: " + e.getMessage());
              }
            }
          }
        } catch (InvalidProtocolBufferException exception) {
          Log.e(TAG, "Right hand: Failed to get proto.", exception);
        }
      });

  }


  @Override
  protected void onResume() {
    super.onResume();
    sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
  }

  @Override
  protected void onPause() {
    super.onPause();
    sensorManager.unregisterListener(this);
  }

  @Override
  public final void onAccuracyChanged(Sensor sensor, int accuracy) {
  }

  @Override
  public final void onSensorChanged(SensorEvent event) {
    synchronized(this) {
      try {
        JSONArray jsonArr = new JSONArray();
        for (int i = 0; i < 3; i++) {
          jsonArr.put(event.values[i]);
        }
        jsonObj.put("gravity", jsonArr);
        jsonObj.put("gravity_stamp", event.timestamp);
      } catch (JSONException e) {
        Log.e(TAG, "Gravity: " + e.getMessage());
      }
    }
  }


  private static String getLandmarksDebugString(NormalizedLandmarkList landmarks, String streamName) {
    String landmarkStr = streamName + " landmarks: " + landmarks.getLandmarkCount() + "\n";
    int landmarkIndex = 0;
    for (NormalizedLandmark landmark : landmarks.getLandmarkList()) {
      landmarkStr +=
          "\tLandmark ["
              + landmarkIndex
              + "]: ("
              + landmark.getX()
              + ", "
              + landmark.getY()
              + ", "
              + landmark.getZ()
              + ")\n";
      ++landmarkIndex;
    }
    return landmarkStr;
  }

  private static JSONArray convertLandmarksToJson(NormalizedLandmarkList landmarks) {
    JSONArray json_arr = new JSONArray();
    try {
      if (landmarks.getLandmarkCount() > 0) {
        int landmarkIndex = 0;
        for (NormalizedLandmark landmark : landmarks.getLandmarkList()) {
          JSONObject json_pt = new JSONObject();
          json_pt.put("x", landmark.getX());
          json_pt.put("y", landmark.getY());
          json_pt.put("z", landmark.getZ());
          json_pt.put("visibility", landmark.getVisibility());
          json_pt.put("presence", landmark.getPresence());
          json_arr.put(json_pt);
          ++landmarkIndex;
        }
      }
    } catch (JSONException e) {
      Log.w(TAG, e.getMessage());
    }
    return json_arr;
  }
  
  private static JSONArray convertLandmarksToJson(LandmarkList landmarks) {
    JSONArray json_arr = new JSONArray();
    try {
      if (landmarks.getLandmarkCount() > 0) {
        int landmarkIndex = 0;
        for (Landmark landmark : landmarks.getLandmarkList()) {
          JSONObject json_pt = new JSONObject();
          json_pt.put("x", landmark.getX());
          json_pt.put("y", landmark.getY());
          json_pt.put("z", landmark.getZ());
          json_pt.put("visibility", landmark.getVisibility());
          json_pt.put("presence", landmark.getPresence());
          json_arr.put(json_pt);
          ++landmarkIndex;
        }
      }
    } catch (JSONException e) {
      Log.w(TAG, e.getMessage());
    }
    return json_arr;
  }

}
