package org.opengl.scene;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;
import org.opengl.OpenGLScene;
import org.opengl.camera.Camera;
import org.opengl.drawable.*;
import org.opengl.model.Model;
import org.opengl.shader.Shader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.function.Supplier;

import static org.joml.Math.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL15.GL_ELEMENT_ARRAY_BUFFER;
public class Scene {
    private final long window;
    private final int windowHeight;
    private final int windowWidth;
    private final String vertexShaderSource;
    private final String fragmentShaderSource;
    private final String lightSourceVertexShaderSource;
    private final String lightSourceFragmentShaderSource;
    private final Vector3f worldUp;
    private Camera camera;
    static float delta = 0.0f;    // Time between current frame and last frame
    static float lastMoment = 0.0f;


    private final Random random;
    private static final int STAR_COUNT = 50;
    private static final int SHADER_OFFSET = 1;
    private final float fogDensity = 0.03f;
    private final boolean useFog = true;
    private final Vector3f dayColor = new Vector3f(0.529f, 0.808f, 0.922f);
    private final Vector3f nightColor = new Vector3f(0f, 0f, 0f);

    public Scene(long window, int windowWidth, int windowHeight) {
        this.window = window;
        this.windowHeight = windowHeight;
        this.windowWidth = windowWidth;
        vertexShaderSource = Objects.requireNonNull(OpenGLScene.class.getClassLoader().getResource("shaders/shader.vert")).getFile();
        fragmentShaderSource = Objects.requireNonNull(OpenGLScene.class.getClassLoader().getResource("shaders/shader.frag")).getFile();
        lightSourceVertexShaderSource = Objects.requireNonNull(OpenGLScene.class.getClassLoader().getResource("shaders/lightSourceShader.vert")).getFile();
        lightSourceFragmentShaderSource = Objects.requireNonNull(OpenGLScene.class.getClassLoader().getResource("shaders/lightSourceShader.frag")).getFile();

        worldUp = new Vector3f(0, 1, 0);
        camera = new Camera();

        random = new Random();
    }

    public void loop() throws IOException {
        GL.createCapabilities();
        glEnable(GL_DEPTH_TEST);
        glClearColor(nightColor.x, nightColor.y, nightColor.z, 0.0f);
        //glClearColor(dayColor.x, dayColor.y, dayColor.z, 0.0f);

        Model zeusModel = new Model(OpenGLScene.class.getClassLoader().getResource("models/zeus.obj").getPath());
        Model jupiterModel = new Model(OpenGLScene.class.getClassLoader().getResource("models/jupiter.obj").getPath());
        Model star = new Model(OpenGLScene.class.getClassLoader().getResource("models/sun.obj").getPath());
        Model pointyHandModel = new Model(OpenGLScene.class.getClassLoader().getResource("models/pointyHand.obj").getPath());
        Model cupcakeModel = new Model(OpenGLScene.class.getClassLoader().getResource("models/cupcake.obj").getPath());

        var jupiter = new DrawableJupiter(jupiterModel);
        var zeus = new DrawableZeus(zeusModel);
        var candleLight = new DrawableCandleLight(star);
        var lightOfGabriel = new DrawableLightOfGabriel(star);
        var stars = new DrawableStars(star, STAR_COUNT, SHADER_OFFSET);
        var hand = new DrawablePointyHand(pointyHandModel);
        var cupcake = new DrawableCupcake(cupcakeModel);

        Shader shader = new Shader(vertexShaderSource, fragmentShaderSource);
        Shader lightSourceShader = new Shader(lightSourceVertexShaderSource, lightSourceFragmentShaderSource);

        shader.use();
        shader.setVec3("viewPos", camera.getCameraPos());
        shader.setVec3("backgroundColor", nightColor);
        setUpFog(shader);
        setUpLights(shader, stars, candleLight);

        lightSourceShader.use();
        lightSourceShader.setVec3("lightColor", new Vector3f(1.f, 	1.f, 1.f));
        lightSourceShader.setVec3("viewPos", camera.getCameraPos());
        setUpFog(lightSourceShader);

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);

        while (!glfwWindowShouldClose(window)) {
            if(!(glfwGetKey(window, GLFW_KEY_SPACE) == GLFW_PRESS)) glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            float currMoment = (float) glfwGetTime();
            delta = currMoment - lastMoment;
            lastMoment = currMoment;

            Matrix4f view = camera.getViewMatrix();
            Matrix4f projection = new Matrix4f().perspective(toRadians(camera.getFov()), (float) windowWidth / windowHeight, 0.1f, 1000f);

            camera.processInput(window, delta);
            //camera.startProcessingMouseMovement(window);

            moveLights(shader, stars);

            // BEGIN TRANSFORMATION
            try (MemoryStack stack = MemoryStack.stackPush()) {
                shader.setMatrix4f(stack,"view", view);
                shader.setMatrix4f(stack,"projection", projection);
                lightSourceShader.setMatrix4f(stack,"view", view);
                lightSourceShader.setMatrix4f(stack,"projection", projection);

                zeus.draw(stack, shader);
                candleLight.draw(stack, lightSourceShader);
                lightOfGabriel.draw(stack, lightSourceShader);
                stars.draw(stack, lightSourceShader);
                jupiter.draw(stack, shader);
                hand.draw(stack, shader);
                cupcake.draw(stack, shader);
            }

            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }

    private void setUpFog(Shader shader) {
        shader.setBool("fog.useFog", useFog);
        shader.setVec3("fog.color", nightColor);
        shader.setFloat("fog.density", fogDensity);
    }

    private void moveLights(Shader shader, DrawableStars stars) {
        shader.setVec3("spotLights[0].position",  new Vector3f(
                -2.25f*sin((float)glfwGetTime()),
                0.1f*sin((float)(PI*glfwGetTime())),
                -2.25f*cos((float)glfwGetTime())));
        shader.setVec3("spotLights[0].direction", new Vector3f(0f).sub(new Vector3f(-2.25f*sin((float)glfwGetTime()),0,-2.25f*cos((float)glfwGetTime()))));
        stars.moveStars(shader);
    }



    private void setUpLights(Shader shader, DrawableStars stars, DrawableCandleLight candleLight) {

        shader.setVec3("dirLight.direction",  new Vector3f(-0.2f, -1.0f, -0.3f));
        shader.setVec3("dirLight.ambient",  new Vector3f(0.005f, 0.005f, 0.005f));
        shader.setVec3("dirLight.diffuse",  new Vector3f(0.4f, 0.4f, 0.4f));
        shader.setVec3("dirLight.specular",  new Vector3f(0.5f, 0.5f, 0.5f));

        for(int i=0; i < STAR_COUNT + SHADER_OFFSET; i++){
            shader.setVec3("pointLights[" + i + "].position",
                    i == 0 ? candleLight.getPosition() : stars.getPosition(i - SHADER_OFFSET)
            );
            shader.setVec3("pointLights[" + i + "].ambient", new Vector3f(0f, 0f, 0f));
            shader.setVec3("pointLights[" + i + "].diffuse", new Vector3f(1.f, 	110/255.f, 199/255.f));
            shader.setVec3("pointLights[" + i + "].specular", new Vector3f(1.f, 	110/255.f, 199/255.f));
            shader.setFloat("pointLights[" + i + "].constant", 1.0f);
            shader.setFloat("pointLights[" + i + "].linear", 0.14f);
            shader.setFloat("pointLights[" + i + "].quadratic", 0.07f);
        }

        shader.setVec3("spotLights[0].position",  new Vector3f(-2.25f*sin((float)glfwGetTime()),0,-2.25f*cos((float)glfwGetTime())));
        shader.setVec3("spotLights[0].direction", new Vector3f(0f).sub(new Vector3f(-2.25f*sin((float)glfwGetTime()),0,-2.25f*cos((float)glfwGetTime()))));
        shader.setFloat("spotLights[0].cutOff",   cos(toRadians(8.5f)));
        shader.setFloat("spotLights[0].outerCutOff",   cos(toRadians(13.5f)));
        shader.setVec3("spotLights[0].ambient",  new Vector3f(0.0f, 0.0f, 0.0f));
        shader.setVec3("spotLights[0].diffuse",  new Vector3f(0.5f, 0.5f, 0.9f));
        shader.setVec3("spotLights[0].specular",  new Vector3f(0.5f, 0.5f, 0.9f));
        shader.setFloat("spotLights[0].constant", 1.0f);
        shader.setFloat("spotLights[0].linear", 0.027f);
        shader.setFloat("spotLights[0].quadratic", 0.0028f);
    }

}
