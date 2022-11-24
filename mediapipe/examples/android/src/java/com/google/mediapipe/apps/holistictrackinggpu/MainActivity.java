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
import android.util.Size;
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

  private JSONArray jsonGravityArr;
  private JSONArray jsonPoseArr;
  private JSONArray jsonPoseWorldArr;
  private JSONArray jsonFaceArr;
  private JSONArray jsonRHandArr;
  private JSONArray jsonLHandArr;

  private long gravity_stamp;
  private long pose_stamp;
  private long pose_world_stamp;
  private long face_stamp;
  private long left_hand_stamp;
  private long right_hand_stamp;

  private SensorManager sensorManager;
  private Sensor sensor;

  private boolean is_sent;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    jsonGravityArr = new JSONArray();
    jsonPoseArr = new JSONArray();
    jsonPoseWorldArr = new JSONArray();
    jsonFaceArr = new JSONArray();
    jsonRHandArr = new JSONArray();
    jsonLHandArr = new JSONArray();

    gravity_stamp = 0;
    pose_stamp = 0;
    pose_world_stamp = 0;
    face_stamp = 0;
    left_hand_stamp = 0;
    right_hand_stamp = 0;

    sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
    sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);

    is_sent = true;

    // pose
    processor.addPacketCallback(
      "pose_landmarks",
      (packet) -> {
        // Log.v(TAG, "Received pose landmarks packet.");
        try {
          byte[] landmarksRaw = PacketGetter.getProtoBytes(packet);
          NormalizedLandmarkList landmarks =
            NormalizedLandmarkList.parseFrom(landmarksRaw);
          // Log.v(TAG,
          //   "[TS:" + packet.getTimestamp() + "] "
          //   + getLandmarksDebugString(landmarks, "pose"));
          if (landmarks.getLandmarkCount() > 0) {
            synchronized(this) {
              jsonPoseArr = convertLandmarksToJson(landmarks);
              pose_stamp = packet.getTimestamp();
              if (is_sent) {
                is_sent = false;
              } else {
                if ((pose_stamp - pose_world_stamp) < 16000) {
                  sendJsonData(pose_stamp);
                  is_sent = true;
                }
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
        // Log.v(TAG, "Received pose world landmarks packet.");
        try {
          byte[] landmarksRaw = PacketGetter.getProtoBytes(packet);
          LandmarkList landmarks =
            LandmarkList.parseFrom(landmarksRaw);
          // Log.v(TAG,
          //   "[TS:" + packet.getTimestamp() + "] "
          //   + getLandmarksDebugString(landmarks, "face"));
          if (landmarks.getLandmarkCount() > 0) {
            synchronized(this) {
              jsonPoseWorldArr = convertLandmarksToJson(landmarks);
              pose_world_stamp = packet.getTimestamp();
              if (is_sent) {
                is_sent = false;
              } else {
                if ((pose_world_stamp - pose_stamp) < 16000) {
                  sendJsonData(pose_world_stamp);
                  is_sent = true;
                }
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
        // Log.v(TAG, "Received face landmarks packet.");
        try {
          byte[] landmarksRaw = PacketGetter.getProtoBytes(packet);
          NormalizedLandmarkList landmarks =
            NormalizedLandmarkList.parseFrom(landmarksRaw);
          // Log.v(TAG,
          //   "[TS:" + packet.getTimestamp() + "] "
          //   + getLandmarksDebugString(landmarks, "face"));
          if (landmarks.getLandmarkCount() > 0) {
            synchronized(this) {
              jsonFaceArr = convertLandmarksToJson(landmarks, false, false);
              face_stamp = packet.getTimestamp();
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
        // Log.v(TAG, "Received left hand landmarks packet.");
        try {
          byte[] landmarksRaw = PacketGetter.getProtoBytes(packet);
          NormalizedLandmarkList landmarks =
            NormalizedLandmarkList.parseFrom(landmarksRaw);
          // Log.v(TAG,
          //   "[TS:" + packet.getTimestamp() + "] "
          //   + getLandmarksDebugString(landmarks, "left_hand"));
          if (landmarks.getLandmarkCount() > 0) {
            synchronized(this) {
              jsonLHandArr = convertLandmarksToJson(landmarks);
              left_hand_stamp = packet.getTimestamp();
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
        // Log.v(TAG, "Received right hand landmarks packet.");
        try {
          byte[] landmarksRaw = PacketGetter.getProtoBytes(packet);
          NormalizedLandmarkList landmarks =
            NormalizedLandmarkList.parseFrom(landmarksRaw);
          // Log.v(TAG,
          //   "[TS:" + packet.getTimestamp() + "] "
          //   + getLandmarksDebugString(landmarks, "right_hand"));
          if (landmarks.getLandmarkCount() > 0) {
            synchronized(this) {
              jsonRHandArr = convertLandmarksToJson(landmarks);
              right_hand_stamp = packet.getTimestamp();
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
        jsonGravityArr = new JSONArray();
        for (int i = 0; i < 3; i++) {
          jsonGravityArr.put(event.values[i]);
        }
        gravity_stamp = event.timestamp;
      } catch (JSONException e) {
        Log.e(TAG, "Gravity: " + e.getMessage());
      }
    }
  }


  private void sendJsonData(long base_stamp) {
    JSONObject jsonObj = new JSONObject();

    // camera params
    JSONObject cameraParams = new JSONObject();
    try {
      cameraParams.put("focal_length", cameraHelper.getFocalLengthPixels());
      cameraParams.put("frame_width", cameraHelper.getFrameSize().getWidth());
      cameraParams.put("frame_height", cameraHelper.getFrameSize().getHeight());
      jsonObj.put("camera_params", cameraParams);
    } catch (JSONException e) {
      Log.e(TAG, "CameraParams: " + e.getMessage());
    }

    try {
      // check timestamp for each landmarks
      if ((pose_stamp - base_stamp) > -3e5) {
        jsonObj.put("pose_landmarks", jsonPoseArr);
        jsonObj.put("pose_landmarks_stamp", pose_stamp);
      }

      if ((pose_world_stamp - base_stamp) > -3e5) {
        jsonObj.put("pose_world_landmarks", jsonPoseWorldArr);
        jsonObj.put("pose_world_landmarks_stamp", pose_world_stamp);
      }

      if ((face_stamp - base_stamp) > -3e5) {
        jsonObj.put("face_landmarks", jsonFaceArr);
        jsonObj.put("face_landmarks_stamp", face_stamp);
      }

      if ((right_hand_stamp - base_stamp) > -3e5) {
        jsonObj.put("right_hand_landmarks", jsonRHandArr);
        jsonObj.put("right_hand_landmarks_stamp", right_hand_stamp);
      }

      if ((left_hand_stamp - base_stamp) > -3e5) {
        jsonObj.put("left_hand_landmarks", jsonLHandArr);
        jsonObj.put("left_hand_landmarks_stamp", left_hand_stamp);
      }

      if ((gravity_stamp - base_stamp) > -3e5) {
        jsonObj.put("gravity", jsonGravityArr);
        jsonObj.put("gravity_stamp", gravity_stamp);
      }

      // call setJsonData ONLY in this function
      sender.setJsonData(jsonObj);
    } catch (JSONException e) {
      Log.e(TAG, "Pose: " + e.getMessage());
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

  private static double truncateNumber(double vi) {
    return ((int)(0.5 + vi * 1e3)) * 1e-3;
  }

  private static JSONArray convertLandmarksToJson(
    NormalizedLandmarkList landmarks
  ) {
    return convertLandmarksToJson(landmarks, true, true);
  }

  private static JSONArray convertLandmarksToJson(
    NormalizedLandmarkList landmarks,
    boolean visibility,
    boolean presence
  ) {
    JSONArray json_arr = new JSONArray();
    try {
      if (landmarks.getLandmarkCount() > 0) {
        int landmarkIndex = 0;
        for (NormalizedLandmark landmark : landmarks.getLandmarkList()) {
          JSONObject json_pt = new JSONObject();
          // json_pt.put("x", landmark.getX());
          // json_pt.put("y", landmark.getY());
          // json_pt.put("z", landmark.getZ());
          json_pt.put("x", truncateNumber(landmark.getX()));
          json_pt.put("y", truncateNumber(landmark.getY()));
          json_pt.put("z", truncateNumber(landmark.getZ()));
          if (visibility) {
            // json_pt.put("visibility", landmark.getVisibility());
            json_pt.put("visibility", truncateNumber(landmark.getVisibility()));
          }
          if (presence) {
            // json_pt.put("presence", landmark.getPresence());
            json_pt.put("presence", truncateNumber(landmark.getPresence()));
          }
          json_arr.put(json_pt);
          ++landmarkIndex;
        }
      }
    } catch (JSONException e) {
      Log.w(TAG, e.getMessage());
    }
    return json_arr;
  }

  private static JSONArray convertLandmarksToJson(
    LandmarkList landmarks
  ) {
    return convertLandmarksToJson(landmarks, true, true);
  }

  private static JSONArray convertLandmarksToJson(
    LandmarkList landmarks,
    boolean visibility,
    boolean presence
  ) {
    JSONArray json_arr = new JSONArray();
    try {
      if (landmarks.getLandmarkCount() > 0) {
        int landmarkIndex = 0;
        for (Landmark landmark : landmarks.getLandmarkList()) {
          JSONObject json_pt = new JSONObject();
          // json_pt.put("x", landmark.getX());
          // json_pt.put("y", landmark.getY());
          // json_pt.put("z", landmark.getZ());
          json_pt.put("x", truncateNumber(landmark.getX()));
          json_pt.put("y", truncateNumber(landmark.getY()));
          json_pt.put("z", truncateNumber(landmark.getZ()));
          if (visibility) {
            // json_pt.put("visibility", landmark.getVisibility());
            json_pt.put("visibility", truncateNumber(landmark.getVisibility()));
          }
          if (presence) {
            // json_pt.put("presence", landmark.getPresence());
            json_pt.put("presence", truncateNumber(landmark.getPresence()));
          }
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
