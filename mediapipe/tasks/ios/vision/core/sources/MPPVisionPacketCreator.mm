// Copyright 2023 The MediaPipe Authors.
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

#import "mediapipe/tasks/ios/vision/core/sources/MPPVisionPacketCreator.h"
#import "mediapipe/tasks/ios/vision/core/utils/sources/MPPImage+Utils.h"

#include "mediapipe/framework/formats/image.h"

namespace {
using ::mediapipe::Image;
using ::mediapipe::ImageFrame;
using ::mediapipe::MakePacket;
using ::mediapipe::Packet;
}  // namespace

struct freeDeleter {
  void operator()(void *ptr) { free(ptr); }
};

@implementation MPPVisionPacketCreator

+ (Packet)createPacketWithMPPImage:(MPPImage *)image error:(NSError **)error {
  std::unique_ptr<ImageFrame> imageFrame = [image imageFrameWithError:error];

  if (!imageFrame) {
    return Packet();
  }

  return MakePacket<Image>(std::move(imageFrame));
}

@end
