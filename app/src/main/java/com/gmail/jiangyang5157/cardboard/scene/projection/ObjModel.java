package com.gmail.jiangyang5157.cardboard.scene.projection;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.AsyncTask;
import android.util.Log;

import com.gmail.jiangyang5157.cardboard.vr.R;
import com.gmail.jiangyang5157.tookit.app.AppUtils;
import com.gmail.jiangyang5157.tookit.data.text.IoUtils;
import com.gmail.jiangyang5157.tookit.math.Vector3d;

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
public class ObjModel extends GLModel {
    private static final String TAG = "ObjModel ####";

    private static final int VERTEX_SHADER_RAW_RESOURCE = R.raw.obj_color_vertex_shader;
    private static final int FRAGMENT_SHADER_RAW_RESOURCE = R.raw.obj_color_fragment_shader;

    private static final int COLOR_NORMAL_RES_ID = com.gmail.jiangyang5157.tookit.R.color.DeepOrange;

    private static final float DISTANCE = 100;

    private String title;
    private String obj;

    private Vector<Float> v;
    private Vector<Float> vt;
    private Vector<Float> vn;
    private Vector<Short> fv;
    private Vector<Short> fvt;
    private Vector<Short> fvn;

    protected final int[] buffers = new int[3];

    private Creator creator;
    private class Creator extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            buildArrays();
            bindBuffers();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            setVisible(true);
        }
    }

    protected ObjModel(Context context, String title, String obj) {
        super(context, VERTEX_SHADER_RAW_RESOURCE, FRAGMENT_SHADER_RAW_RESOURCE);
        this.title = title;
        this.obj = obj;
    }

    public void create(float[] cameraPos, float[] forward, float[] up, float[] right, float[] eulerAngles) {
        initializeProgram();
        setColor(AppUtils.getColor(context, COLOR_NORMAL_RES_ID));
        setScale(10f);
        setPosition(cameraPos, forward, up, right, eulerAngles);

        buildArrays();
        bindBuffers();

        setVisible(true);
//
//        if (creator == null) {
//            creator = new Creator();
//            creator.execute();
//        }
    }

    protected void setPosition(float[] cameraPos, float[] forward, float[] up, float[] right, float[] eulerAngles) {
        com.gmail.jiangyang5157.tookit.math.Vector cameraPosVec = new Vector3d(cameraPos[0], cameraPos[1], cameraPos[2]);
        com.gmail.jiangyang5157.tookit.math.Vector forwardVec = new Vector3d(forward[0], forward[1], forward[2]).times(DISTANCE);
        com.gmail.jiangyang5157.tookit.math.Vector positionVec = cameraPosVec.plus(forwardVec);
        double[] positionVecData = positionVec.getData();
        Matrix.setIdentityM(translation, 0);
        Matrix.translateM(translation, 0, (float) positionVecData[0], (float) positionVecData[1], (float) positionVecData[2]);
        float eulerAnglesDegree0 = (float) Math.toDegrees(eulerAngles[0]);
        float eulerAnglesDegree1 = (float) Math.toDegrees(eulerAngles[1]);
        float eulerAnglesDegree2 = (float) Math.toDegrees(eulerAngles[2]);

        Matrix.setIdentityM(rotation, 0);
        Matrix.rotateM(rotation, 0, eulerAnglesDegree1, 0, 1f, 0);
        Matrix.rotateM(rotation, 0, eulerAnglesDegree0, 1f, 0, 0);
        Matrix.rotateM(rotation, 0, eulerAnglesDegree2, 0, 0f, 1f);
    }

    @Override
    protected void bindBuffers() {
        int size = v.size();
        FloatBuffer verticesBuffer = ByteBuffer.allocateDirect(size * BYTES_PER_FLOAT).order(ByteOrder.nativeOrder()).asFloatBuffer();
        for (int i = 0; i < size; i++) {
            verticesBuffer.put(v.get(i));
        }
        verticesBuffer.position(0);

        size = fv.size();
        ShortBuffer indicesBuffer = ByteBuffer.allocateDirect(size * BYTES_PER_SHORT).order(ByteOrder.nativeOrder()).asShortBuffer();
        for (int i = 0; i < size; i++) {
            indicesBuffer.put(fv.get(i));
        }
        indicesBuffer.position(0);
        indicesBufferCapacity = indicesBuffer.capacity();

        size = vn.size();
        if (size == 0) {
            vn.addAll(v);
            size = vn.size();
        }
        FloatBuffer normalsBuffer = ByteBuffer.allocateDirect(size * BYTES_PER_FLOAT).order(ByteOrder.nativeOrder()).asFloatBuffer();
        for (int i = 0; i < size; i++) {
            normalsBuffer.put(vn.get(i));
        }
        normalsBuffer.position(0);

        GLES20.glGenBuffers(buffers.length, buffers, 0);
        verticesBuffHandle = buffers[0];
        indicesBuffHandle = buffers[1];
        normalsBuffHandle = buffers[2];

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, verticesBuffHandle);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, verticesBuffer.capacity() * BYTES_PER_FLOAT, verticesBuffer, GLES20.GL_STATIC_DRAW);
        verticesBuffer.limit(0);

        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, indicesBuffHandle);
        GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, indicesBuffer.capacity() * BYTES_PER_SHORT, indicesBuffer, GLES20.GL_STATIC_DRAW);
        indicesBuffer.limit(0);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, normalsBuffHandle);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, normalsBuffer.capacity() * BYTES_PER_FLOAT, normalsBuffer, GLES20.GL_STATIC_DRAW);
        normalsBuffer.limit(0);
    }

    @Override
    protected void buildArrays() {
        v = new java.util.Vector<>();
        vt = new java.util.Vector<>();
        vn = new java.util.Vector<>();

        fv = new java.util.Vector<>();
        fvt = new java.util.Vector<>();
        fvn = new java.util.Vector<>();

        InputStream ins = context.getResources().openRawResource(context.getResources().getIdentifier(obj, "raw", context.getPackageName()));
        IoUtils.read(ins, new IoUtils.OnReadingListener() {
            @Override
            public boolean onReadLine(String line) {
                if (line == null) {
                    return false;
                } else {
                    // http://paulbourke.net/dataformats/obj/
                    if (line.startsWith("v ")) {
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
            }
        });
    }

    private void parserGeometricVertices(String line) {
        Log.d(TAG, "parserGeometricVertices: " + line);
        String[] tokens = line.split("[ ]+");
        int length = tokens.length;
        for (int i = 1; i < length; i++) {
            v.add(Float.valueOf(tokens[i]));
        }
    }

    private void parserTextureVertices(String line) {
        Log.d(TAG, "parserTextureVertices: " + line);
        String[] tokens = line.split("[ ]+");
        int length = tokens.length;
        for (int i = 1; i < length; i++) {
            vt.add(Float.valueOf(tokens[i]));
        }
    }

    private void parserVertexNormals(String line) {
        Log.d(TAG, "parserVertexNormals: " + line);
        String[] tokens = line.split("[ ]+");
        int length = tokens.length;
        for (int i = 1; i < length; i++) {
            vn.add(Float.valueOf(tokens[i]));
        }
    }

    private void parserFace(String line) {
        Log.d(TAG, "parserFace: " + line);
        String[] tokens = line.split("[ ]+");
        int length = tokens.length;

        if (tokens[1].matches("[0-9]+")) { // f: v
            if (length == 4) { // f v v v
                for (int i = 1; i < length; i++) {
                    Short s = Short.valueOf(tokens[i]);
                    s--;
                    fv.add(s);
                }
            } else { // f (triangulate)
                Vector<Short> fv2 = new Vector<Short>();
                for (int i = 1; i < tokens.length; i++) {
                    Short s = Short.valueOf(tokens[i]);
                    s--;
                    fv2.add(s);
                }
                fv.addAll(triangulate(fv2));
            }
        } else if (tokens[1].matches("[0-9]+/[0-9]+")) { // f: v/vt
            if (length == 4) { // f v/vt v/vt v/vt
                for (int i = 1; i < length; i++) {
                    String[] tokens2 = tokens[i].split("/");
                    Short s = Short.valueOf(tokens2[0]);
                    s--;
                    fv.add(s);
                    s = Short.valueOf(tokens2[1]);
                    s--;
                    fvt.add(s);
                }
            } else { // f (triangulate)
                Vector<Short> fv2 = new Vector<Short>();
                Vector<Short> fvt2 = new Vector<Short>();
                for (int i = 1; i < tokens.length; i++) {
                    String[] tokens2 = tokens[i].split("/");
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
        } else if (tokens[1].matches("[0-9]+//[0-9]+")) { // f: v//vn
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
                Vector<Short> fv2 = new Vector<Short>();
                Vector<Short> fvn2 = new Vector<Short>();
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
        } else if (tokens[1].matches("[0-9]+/[0-9]+/[0-9]+")) { // f: v/vt/vn
            if (length == 4) { // f v/vt/vn v/vt/vn v/vt/vn
                for (int i = 1; i < length; i++) {
                    String[] tokens2 = tokens[i].split("/");
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
                Vector<Short> fv2 = new Vector<Short>();
                Vector<Short> fvt2 = new Vector<Short>();
                Vector<Short> fvn2 = new Vector<Short>();
                for (int i = 1; i < tokens.length; i++) {
                    String[] tokens2 = tokens[i].split("/");
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
    protected void initializeHandle() {
        mvMatrixHandle = GLES20.glGetUniformLocation(program, MODEL_VIEW_HANDLE);
        mvpMatrixHandle = GLES20.glGetUniformLocation(program, MODEL_VIEW_PROJECTION_HANDLE);
        colorHandle = GLES20.glGetUniformLocation(program, COLOR_HANDLE);
        lightPosHandle = GLES20.glGetUniformLocation(program, LIGHT_POSITION_HANDLE);

        vertexHandle = GLES20.glGetAttribLocation(program, VERTEX_HANDLE);
        normalHandle = GLES20.glGetAttribLocation(program, NORMAL_HANDLE);
    }

    @Override
    public void draw() {
        if (!isVisible || !isProgramCreated()) {
            return;
        }

        GLES20.glUseProgram(program);
        GLES20.glEnableVertexAttribArray(vertexHandle);
        GLES20.glEnableVertexAttribArray(normalHandle);

        GLES20.glUniformMatrix4fv(mvMatrixHandle, 1, false, modelView, 0);
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, modelViewProjection, 0);
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

        checkGlEsError("ObjModel - draw end");
    }

    public String getTitle() {
        return title;
    }

    @Override
    public void update(float[] view, float[] perspective) {
        Matrix.rotateM(rotation, 0, 0.5f, 0, 1, 0);
        super.update(view, perspective);
    }

    @Override
    public void destroy() {
        super.destroy();
        Log.d("ObjModel", "destroy");
        GLES20.glDeleteBuffers(buffers.length, buffers, 0);
    }
}
