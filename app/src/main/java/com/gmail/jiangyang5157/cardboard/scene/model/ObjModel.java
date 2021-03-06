package com.gmail.jiangyang5157.cardboard.scene.model;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import com.gmail.jiangyang5157.cardboard.net.FilePrepare;
import com.gmail.jiangyang5157.cardboard.scene.Creation;
import com.gmail.jiangyang5157.cardboard.scene.Head;
import com.gmail.jiangyang5157.cardboard.vr.AssetFile;
import com.gmail.jiangyang5157.cardboard.vr.AssetUtils;
import com.gmail.jiangyang5157.tookit.android.base.AppUtils;
import com.gmail.jiangyang5157.tookit.base.data.BufferUtils;
import com.gmail.jiangyang5157.tookit.base.data.IoUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.Vector;

/**
 * @author Yang
 * @since 5/27/2016
 */
public class ObjModel extends GlModel implements GlModel.BindableBuffer, Creation {
    private static final String TAG = "[ObjModel]";

    public static final float DISTANCE = 10;

    private static final float TIME_DELTA_ROTATION = 0.2f;

    private String title;
    private String url;

    private Vector<Float> v;
    private Vector<Float> vt; // cache, unsupported
    private Vector<Float> vn;
    private Vector<Short> fv;
    private Vector<Short> fvt; // cache, but unsupported
    private Vector<Short> fvn;

    protected final int[] buffers = new int[3];

    protected int creationState = STATE_BEFORE_PREPARE;

    protected ObjModel(Context context, String title, String url) {
        super(context);
        this.title = title;
        this.url = url;
        setColor(AppUtils.getColor(context, com.gmail.jiangyang5157.tookit.android.base.R.color.DeepOrange, null));
    }

    public void prepare(final Ray ray) {
        getHandler().post(() -> {
            File file = new File(AssetUtils.getAbsolutePath(context, AssetUtils.getAssetPath(url)));
            AssetFile assetFile = new AssetFile(file, url);
            new FilePrepare(assetFile, new FilePrepare.PrepareListener() {
                @Override
                public void onStart() {
                    creationState = STATE_PREPARING;
                    ray.addBusy();
                }

                @Override
                public void onComplete(AssetFile assetFile) {
                    if (assetFile.getFile().exists()) {
                        buildData();
                        ray.subtractBusy();
                        creationState = STATE_BEFORE_CREATE;
                    } else {
                        ray.subtractBusy();
                        creationState = STATE_BEFORE_PREPARE;
                    }
                }
            }).start();
        });
    }

    @Override
    public void create(int program) {
        creationState = STATE_CREATING;
        super.create(program);
        bindHandles();
        bindBuffers();

        setCreated(true);
        setVisible(true);
        creationState = STATE_BEFORE_CREATE;
    }

    public void setPosition(float[] cameraPos, float[] forward, float distance, float[] quaternion, float[] up, float[] right) {
        float[] position = new float[]{
                cameraPos[0] + forward[0] * distance,
                cameraPos[1] + forward[1] * distance,
                cameraPos[2] + forward[2] * distance,
        };

        Matrix.setIdentityM(translationMatrix, 0);
        Matrix.translateM(translationMatrix, 0, position[0], position[1], position[2]);

        Matrix.setIdentityM(rotationMatrix, 0);
        // Reverse direction for facing to eye
        float[] q = new float[]{-quaternion[0], -quaternion[1], -quaternion[2], quaternion[3]};
        Matrix.multiplyMM(rotationMatrix, 0, Head.getQuaternionMatrix(q), 0, rotationMatrix, 0);

        modelRequireUpdate = true;
    }

    @Override
    public void bindBuffers() {
        int fvSize = fv.size();
        int vSize = fvSize * 3;
        int fvnSize = fvn.size();
        int vnSize = vn.size();
//        Log.d(TAG, "fvSize/vSize/fvnSize/vnSize: " + fvSize + "," + vSize + "," + fvnSize + "," + vnSize);
        FloatBuffer verticesBuffer = ByteBuffer.allocateDirect(vSize * BufferUtils.BYTES_PER_FLOAT).order(ByteOrder.nativeOrder()).asFloatBuffer();
        FloatBuffer normalsBuffer = ByteBuffer.allocateDirect(vSize * BufferUtils.BYTES_PER_FLOAT).order(ByteOrder.nativeOrder()).asFloatBuffer();
        ShortBuffer indicesBuffer = ByteBuffer.allocateDirect(fvSize * BufferUtils.BYTES_PER_SHORT).order(ByteOrder.nativeOrder()).asShortBuffer();
        for (int i = 0; i < fvSize; i++) {
            short vIndex = fv.get(i);
            verticesBuffer.put(v.get(vIndex * 3));
            verticesBuffer.put(v.get(vIndex * 3 + 1));
            verticesBuffer.put(v.get(vIndex * 3 + 2));

            if (fvnSize == fvSize) {
                short vnIndex = fvn.get(i);
                normalsBuffer.put(vn.get(vnIndex * 3));
                normalsBuffer.put(vn.get(vnIndex * 3 + 1));
                normalsBuffer.put(vn.get(vnIndex * 3 + 2));
            }

            indicesBuffer.put((short) i);
        }

        verticesBuffer.position(0);
        normalsBuffer.position(0);
        indicesBuffer.position(0);
        indicesBufferCapacity = indicesBuffer.capacity();

        GLES20.glGenBuffers(buffers.length, buffers, 0);
        verticesBuffHandle = buffers[0];
        indicesBuffHandle = buffers[1];
        normalsBuffHandle = buffers[2];

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, verticesBuffHandle);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, verticesBuffer.capacity() * BufferUtils.BYTES_PER_FLOAT, verticesBuffer, GLES20.GL_STATIC_DRAW);
        verticesBuffer.limit(0);

        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, indicesBuffHandle);
        GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, indicesBuffer.capacity() * BufferUtils.BYTES_PER_SHORT, indicesBuffer, GLES20.GL_STATIC_DRAW);
        indicesBuffer.limit(0);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, normalsBuffHandle);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, normalsBuffer.capacity() * BufferUtils.BYTES_PER_FLOAT, normalsBuffer, GLES20.GL_STATIC_DRAW);
        normalsBuffer.limit(0);
    }

    protected void buildData() {
        v = new java.util.Vector<>();
        vt = new java.util.Vector<>();
        vn = new java.util.Vector<>();
        fv = new java.util.Vector<>();
        fvt = new java.util.Vector<>();
        fvn = new java.util.Vector<>();

        // TODO: 7/5/2016 OBJ Loader
        InputStream in = null;
        try {
            in = new FileInputStream(new File(AssetUtils.getAbsolutePath(context, AssetUtils.getAssetPath(url))));
            IoUtils.read(in, line -> {
                if (line == null) {
                    return false;
                } else {
                    // http://paulbourke.net/dataformats/obj/
                    if (line.startsWith("#")) {
                        parserComments(line);
                    } else if (line.startsWith("v ")) {
                        parserGeometricVertices(line);
                    } else if (line.startsWith("vt ")) {
                        parserTextureVertices(line);
                    } else if (line.startsWith("vn ")) {
                        parserVertexNormals(line);
                    } else if (line.startsWith("f ")) {
                        parserFace(line);
                    } else {
                        Log.w(TAG, "Unsupported regex: " + line);
                    }
                    return true;
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void parserComments(String line) {
//        Log.d(TAG, "parserComments: " + line);
    }

    private void parserGeometricVertices(String line) {
//        Log.d(TAG, "parserGeometricVertices: " + line);
        String[] tokens = line.split("[ ]+");
        int length = tokens.length;
        for (int i = 1; i < length; i++) {
            v.add(Float.valueOf(tokens[i]));
        }
    }

    private void parserTextureVertices(String line) {
//        Log.d(TAG, "parserTextureVertices: " + line);
        String[] tokens = line.split("[ ]+");
        int length = tokens.length;
        for (int i = 1; i < length; i++) {
            vt.add(Float.valueOf(tokens[i]));
        }
    }

    private void parserVertexNormals(String line) {
//        Log.d(TAG, "parserVertexNormals: " + line);
        String[] tokens = line.split("[ ]+");
        int length = tokens.length;
        for (int i = 1; i < length; i++) {
            vn.add(Float.valueOf(tokens[i]));
        }
    }

    private void parserFace(String line) {
//        Log.d(TAG, "parserFace: " + line);
        String[] tokens = line.split("[ ]+");
        int length = tokens.length;

        if (tokens[1].matches("[0-9]+")) { // f v ...
            if (length == 4) { // f v v v
                for (int i = 1; i < length; i++) {
                    Short s = Short.valueOf(tokens[i]);
                    s--;
                    fv.add(s);
                }
            } else { // f (triangulate)
                Vector<Short> fv2 = new Vector<>();
                for (int i = 1; i < tokens.length; i++) {
                    Short s = Short.valueOf(tokens[i]);
                    s--;
                    fv2.add(s);
                }
                fv.addAll(triangulate(fv2));
            }
        } else if (tokens[1].matches("[0-9]+/[0-9]+")) { // f v/vt ...
            if (length == 4) { // f v/vt v/vt v/vt
                for (int i = 1; i < length; i++) {
                    String[] tokens2 = tokens[i].split(File.separator);
                    Short s = Short.valueOf(tokens2[0]);
                    s--;
                    fv.add(s);
                    s = Short.valueOf(tokens2[1]);
                    s--;
                    fvt.add(s);
                }
            } else { // f (triangulate)
                Vector<Short> fv2 = new Vector<>();
                Vector<Short> fvt2 = new Vector<>();
                for (int i = 1; i < tokens.length; i++) {
                    String[] tokens2 = tokens[i].split(File.separator);
                    Short s = Short.valueOf(tokens2[0]);
                    s--;
                    fv2.add(s);
                    s = Short.valueOf(tokens2[1]);
                    s--;
                    fvt2.add(s);
                }
                fv.addAll(triangulate(fv2));
                fvt.addAll(triangulate(fvt2));
            }
        } else if (tokens[1].matches("[0-9]+//[0-9]+")) { // f v//vn ...
            if (length == 4) { // f v/vn v/vn v/vn
                for (int i = 1; i < length; i++) {
                    String[] tokens2 = tokens[i].split("//");
                    Short s = Short.valueOf(tokens2[0]);
                    s--;
                    fv.add(s);
                    s = Short.valueOf(tokens2[1]);
                    s--;
                    fvn.add(s);
                }
            } else { // f (triangulate)
                Vector<Short> fv2 = new Vector<>();
                Vector<Short> fvn2 = new Vector<>();
                for (int i = 1; i < tokens.length; i++) {
                    String[] tokens2 = tokens[i].split("//");
                    Short s = Short.valueOf(tokens2[0]);
                    s--;
                    fv2.add(s);
                    s = Short.valueOf(tokens2[1]);
                    s--;
                    fvn2.add(s);
                }
                fv.addAll(triangulate(fv2));
                fvn.addAll(triangulate(fvn2));
            }
        } else if (tokens[1].matches("[0-9]+/[0-9]+/[0-9]+")) { // f v/vt/vn ...
            if (length == 4) { // f v/vt/vn v/vt/vn v/vt/vn
                for (int i = 1; i < length; i++) {
                    String[] tokens2 = tokens[i].split(File.separator);
                    Short s = Short.valueOf(tokens2[0]);
                    s--;
                    fv.add(s);
                    s = Short.valueOf(tokens2[1]);
                    s--;
                    fvt.add(s);
                    s = Short.valueOf(tokens2[2]);
                    s--;
                    fvn.add(s);
                }
            } else { // f (triangulate)
                Vector<Short> fv2 = new Vector<>();
                Vector<Short> fvt2 = new Vector<>();
                Vector<Short> fvn2 = new Vector<>();
                for (int i = 1; i < tokens.length; i++) {
                    String[] tokens2 = tokens[i].split(File.separator);
                    Short s = Short.valueOf(tokens2[0]);
                    s--;
                    fv2.add(s);
                    s = Short.valueOf(tokens2[1]);
                    s--;
                    fvt2.add(s);
                    s = Short.valueOf(tokens2[2]);
                    s--;
                    fvn2.add(s);
                }
                fv.addAll(triangulate(fv2));
                fvt.addAll(triangulate(fvt2));
                fvn.addAll(triangulate(fvn2));
            }
        }
    }

    private Vector<Short> triangulate(Vector<Short> polygon) {
        Vector<Short> triangles = new Vector<>();
        int length = polygon.size();
        for (int i = 1; i < length - 1; i++) {
            triangles.add(polygon.get(0));
            triangles.add(polygon.get(i));
            triangles.add(polygon.get(i + 1));
        }
        return triangles;
    }

    @Override
    protected void bindHandles() {
        modelHandle = GLES20.glGetUniformLocation(program, MODEL_HANDLE);
        viewHandle = GLES20.glGetUniformLocation(program, VIEW_HANDLE);
        perspectiveHandle = GLES20.glGetUniformLocation(program, PERSPECTIVE_HANDLE);

        colorHandle = GLES20.glGetUniformLocation(program, COLOR_HANDLE);
        lightPosHandle = GLES20.glGetUniformLocation(program, LIGHT_POSITION_HANDLE);

        vertexHandle = GLES20.glGetAttribLocation(program, VERTEX_HANDLE);
        normalHandle = GLES20.glGetAttribLocation(program, NORMAL_HANDLE);
    }

    @Override
    public void draw() {
        if (!isCreated() || !isVisible()) {
            return;
        }

        GLES20.glUseProgram(program);
        GLES20.glEnableVertexAttribArray(vertexHandle);
        GLES20.glEnableVertexAttribArray(normalHandle);

        GLES20.glUniformMatrix4fv(modelHandle, 1, false, model, 0);
        GLES20.glUniformMatrix4fv(viewHandle, 1, false, view, 0);
        GLES20.glUniformMatrix4fv(perspectiveHandle, 1, false, perspective, 0);

        GLES20.glUniform3fv(colorHandle, 1, color, 0);
        if (lighting != null) {
            GLES20.glUniform3fv(lightPosHandle, 1, lighting.getLightPosInCameraSpace(), 0);
        }

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, verticesBuffHandle);
        GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT, false, 0, 0);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, normalsBuffHandle);
        GLES20.glVertexAttribPointer(normalHandle, 3, GLES20.GL_FLOAT, false, 0, 0);

        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, indicesBuffHandle);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indicesBufferCapacity, GLES20.GL_UNSIGNED_SHORT, 0);

        GLES20.glDisableVertexAttribArray(vertexHandle);
        GLES20.glDisableVertexAttribArray(normalHandle);
        GLES20.glUseProgram(0);

        GlesUtils.printGlError(TAG + " - draw end");
    }

    public String getTitle() {
        return title;
    }

    @Override
    public void update() {
        super.update();
        Matrix.rotateM(rotationMatrix, 0, TIME_DELTA_ROTATION, 0.5f, 1.0f, 0.5f);
        modelRequireUpdate = true;
    }

    @Override
    public void destroy() {
        super.destroy();
        GLES20.glDeleteBuffers(buffers.length, buffers, 0);
    }

    @Override
    public int getCreationState() {
        return creationState;
    }
}
