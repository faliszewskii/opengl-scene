package org.opengl.camera;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFWCursorPosCallback;

import static java.lang.Math.*;
import static java.lang.Math.toRadians;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;

public class Camera {

    private Vector3f cameraPos = new Vector3f(0f,0f,-7f);
    private Vector3f cameraUp = new Vector3f(0, 1, 0);
    private Vector3f cameraFront = new Vector3f(0, 0, 1);
    private float lastX = 400, lastY = 300;

    private float yaw = -0f, pitch = 0;
    private float fov = 45;
    private float movementSpeed = 2.5f;
    private boolean firstMouse = true;
    private GLFWCursorPosCallback mouseCallback;
    public Camera(Vector3f cameraPos, Vector3f cameraUp, Vector3f cameraFront,
                  float lastX, float lastY, float yaw, float pitch, float fov, float movementSpeed) {
        this.cameraPos = cameraPos;
        this.cameraUp = cameraUp;
        this.cameraFront = cameraFront;
        this.lastX = lastX;
        this.lastY = lastY;
        this.yaw = yaw;
        this.pitch = pitch;
        this.fov = fov;
        this.movementSpeed = movementSpeed;
    }

    public Camera() {}

    public Vector3f getCameraPos() {
        return cameraPos;
    }

    public void setCameraPos(Vector3f cameraPos) {
        this.cameraPos = cameraPos;
    }

    public void setCameraFront(Vector3f cameraFront) {
        this.cameraFront = cameraFront;
    }

    public Matrix4f getViewMatrix() {
        return new Matrix4f()
                .lookAt(cameraPos, new Vector3f(cameraPos).add(cameraFront), cameraUp);
    }

    public Vector3f getCameraFront() {
        return cameraFront;
    }

    public float getFov() {
        return fov;
    }

    public void processInput(long window, float deltaTime) {
        float cameraSpeed = movementSpeed * deltaTime; // adjust accordingly
        if (glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS)
            cameraPos.add(new Vector3f(cameraFront).mul(cameraSpeed));
        if (glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS)
            cameraPos.sub(new Vector3f(cameraFront).mul(cameraSpeed));
        if (glfwGetKey(window, GLFW_KEY_A) == GLFW_PRESS)
            cameraPos.sub(new Vector3f(cameraFront).cross(cameraUp).normalize().mul(cameraSpeed));
        if (glfwGetKey(window, GLFW_KEY_D) == GLFW_PRESS)
            cameraPos.add(new Vector3f(cameraFront).cross(cameraUp).normalize().mul(cameraSpeed));
    }

    public void startProcessingMouseMovement(long window) {
        if (mouseCallback != null) return;
        mouseCallback = glfwSetCursorPosCallback(window, (window1, xpos, ypos) -> {
            if (firstMouse)
            {
                lastX = (float) xpos;
                lastY = (float) ypos;
                firstMouse = false;
            }

            float xoffset = (float) xpos - lastX;
            float yoffset = lastY - (float) ypos; // reversed since y-coordinates range from bottom to top
            lastX = (float) xpos;
            lastY = (float) ypos;

            float sensitivity = 0.1f;
            xoffset *= sensitivity;
            yoffset *= sensitivity;

            yaw += xoffset;
            pitch += yoffset;

            if (pitch > 89.0f)
                pitch = 89.0f;
            if (pitch < -89.0f)
                pitch = -89.0f;

            cameraFront.set(cos(toRadians(yaw)) * cos(toRadians(pitch)),
                    sin(toRadians(pitch)),
                    sin(toRadians(yaw)) * cos(toRadians(pitch))).normalize();
        });
        glfwSetScrollCallback(window, (window1, xoffset, yoffset) -> {
            fov -= (float)yoffset;
            if (fov < 1.0f)
                fov = 1.0f;
            if (fov > 90.0f)
                fov = 90.0f;
        });

        glfwSetScrollCallback(window, (window1, xoffset, yoffset) -> {
            fov -= (float)yoffset;
            if (fov < 1.0f)
                fov = 1.0f;
            if (fov > 90.0f)
                fov = 90.0f;
        });
    }

    public void stopProcessingMouseMovement(long window) {
        if (mouseCallback != null) {
            mouseCallback.close();
            glfwSetCursorPosCallback(window, null);
            mouseCallback = null;
        }
    }

}
