# AndroidCamera
### Summary

This is a simple framework for Android camera operations with Camera2 API. Follow the official guide, we can build our project to fetch camera data from hardware device then display it on the screen. However, what we can do if we want to do frame preprocessing? In other words, we want to do additional work on the fetched frame data, then display it on the screen. This is a normal requirement. Think that we are building a face detection application. For every frame fetched from camera, we want to use out algorithm to detect the rectangle bounding box, then draw it on the image, finally display it on screen.

One solution is to use OpenGL to do your custom drawing work. However it is too complicate for some applications. So we do it with **Native Window API** with the help of **JNI**. And we use **render script** to do image format conversion.

This framework is very useful for computer vision algorithm verification on Android platform. You implement your core algorithm in C++, build it with NDK, ported it with JNI. Then use this framework to test the algorithm's result.

### Usage

You only need to pay attention to `CameraBridge` class's `onFrameArray` method to provide your own processing logic. Use `JNIDisplay` class's `SimpleRGBADisplay` method to display the processed image on the screen. So you can do anything you want, pretty simple. 

