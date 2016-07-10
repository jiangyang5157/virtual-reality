package com.gmail.jiangyang5157.cardboard.scene.model;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;

import com.gmail.jiangyang5157.cardboard.scene.RayIntersection;
import com.gmail.jiangyang5157.cardboard.scene.Head;
import com.gmail.jiangyang5157.tookit.data.buffer.BufferUtils;
import com.gmail.jiangyang5157.tookit.math.Vector;
import com.gmail.jiangyang5157.tookit.math.Vector3d;
import com.gmail.jiangyang5157.tookit.opengl.GlUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * @author Yang
 * @since 5/5/2016
 */
public abstract class Panel extends Rectangle implements GlModel.BindableBuffer, GlModel.BindableTextureBuffer {
    private static final String TAG = "[Panel]";

    protected float[] vertices;
    protected float[] normals;
    protected short[] indices;
    protected float[] textures;

    private Vector tl_vec;
    private Vector bl_vec;
    private Vector tr_vec;
    private Vector br_vec;

    protected final int[] buffers = new int[3];
    protected final int[] texBuffers = new int[1];

    protected Panel(Context context) {
        super(context);
    }

    @Override
    public void create(int program) {
        bindTextureBuffers();
        buildData();

        super.create(program);
        bindHandles();
        bindBuffers();
    }

    public void setPosition(float[] cameraPos, float[] forward, float distance, float[] quaternion, float[] up, float[] right) {
        float[] position = new float[]{
                cameraPos[0] + forward[0] * distance,
                cameraPos[1] + forward[1] * distance,
                cameraPos[2] + forward[2] * distance,
        };

        Matrix.setIdentityM(translation, 0);
        Matrix.translateM(translation, 0, position[0], position[1], position[2]);

        Matrix.setIdentityM(rotation, 0);
        // it should face to eye
        float[] q = new float[]{-quaternion[0], -quaternion[1], -quaternion[2], quaternion[3]};
        Matrix.multiplyMM(rotation, 0, Head.getQquaternionMatrix(q), 0, rotation, 0);

        // build corners' vector, they are for intersect calculation
        buildCorners(up, right, position);
    }

    @Override
    public RayIntersection onIntersection(Head head) {
        if (!isCreated() || !isVisible()) {
            return null;
        }

        float[] cameraPos = head.getCamera().getPosition();
        float[] forward = head.getForward();
        Vector cameraPos_vec = new Vector(cameraPos[0], cameraPos[1], cameraPos[2]);
        Vector forward_vec = new Vector(forward[0], forward[1], forward[2]);

        Vector tl_tr_vec = new Vector3d(tr_vec.minus(tl_vec));
        Vector tl_bl_vec = new Vector3d(bl_vec.minus(tl_vec));
        Vector normal_vec = ((Vector3d) tl_tr_vec).cross((Vector3d) tl_bl_vec).direction();
        Vector ray_vec = (cameraPos_vec.plus(forward_vec)).minus(cameraPos_vec).direction();
        double ndotdRay = normal_vec.dot(ray_vec);
        if (Math.abs(ndotdRay) < Vector.EPSILON) {
            // perpendicular
            return null;
        }
        double t = normal_vec.dot(tl_vec.minus(cameraPos_vec)) / ndotdRay;
        if (t <= 0) {
            // eliminate squares behind the ray
            return null;
        }

        Vector iPlane = cameraPos_vec.plus(ray_vec.times(t));
        Vector tl_iPlane = iPlane.minus(tl_vec);
        double u = tl_iPlane.dot(tl_tr_vec);
        double v = tl_iPlane.dot(tl_bl_vec);

        boolean intersecting = u >= 0 && u <= tl_tr_vec.dot(tl_tr_vec) && v >= 0 && v <= tl_bl_vec.dot(tl_bl_vec);
        if (!intersecting) {
            // intersection is out of boundary
            return null;
        }

        return new RayIntersection(this, t);
    }

    @Override
    protected void bindHandles() {
        mvpMatrixHandle = GLES20.glGetUniformLocation(program, MODEL_VIEW_PROJECTION_HANDLE);
        texIdHandle = GLES20.glGetUniformLocation(program, TEXTURE_ID_HANDLE);

        vertexHandle = GLES20.glGetAttribLocation(program, VERTEX_HANDLE);
        texCoordHandle = GLES20.glGetAttribLocation(program, TEXTURE_COORDS_HANDLE);
    }

    protected void buildCorners(float[] up, float[] right) {
        Vector up_vec = new Vector(up[0], up[1], up[2]);
        Vector right_vec = new Vector(right[0], right[1], right[2]);
        double[] normals_temp = (new Vector3d(right_vec)).cross(new Vector3d(up_vec)).getData();
        normals = new float[]{
                (float) normals_temp[0], (float) normals_temp[1], (float) normals_temp[2]
        };

        final float HALF_WIDTH = width / 2.0f;
        final float HALF_HEIGHT = height / 2.0f;
        up_vec = up_vec.times(HALF_HEIGHT);
        right_vec = right_vec.times(HALF_WIDTH);

        tl_vec = up_vec.plus(right_vec.negate());
        bl_vec = up_vec.negate().plus(right_vec.negate());
        tr_vec = up_vec.plus(right_vec);
        br_vec = up_vec.negate().plus(right_vec);
    }

    protected void buildCorners(float[] up, float[] right, float[] position) {
        buildCorners(up, right);

        tl_vec = tl_vec.times(scale);
        bl_vec = bl_vec.times(scale);
        tr_vec = tr_vec.times(scale);
        br_vec = br_vec.times(scale);

        Vector pos_vec = new Vector(position[0], position[1], position[2]);
        tl_vec = tl_vec.plus(pos_vec);
        bl_vec = bl_vec.plus(pos_vec);
        tr_vec = tr_vec.plus(pos_vec);
        br_vec = br_vec.plus(pos_vec);
    }

    @Override
    protected void buildData() {
        buildCorners(UP, RIGHT);

        double[] tl = tl_vec.getData();
        double[] bl = bl_vec.getData();
        double[] tr = tr_vec.getData();
        double[] br = br_vec.getData();

        vertices = new float[]{
                (float) tl[0], (float) tl[1], (float) tl[2],
                (float) bl[0], (float) bl[1], (float) bl[2],
                (float) tr[0], (float) tr[1], (float) tr[2],
                (float) br[0], (float) br[1], (float) br[2]
        };

        // GL_CCW
        // more details: face culling
        indices = new short[]{
                0, 1, 2, 3
        };

        textures = new float[]{
                0.0f, 0.0f, // tl_vec
                0.0f, 1.0f, // bl_vec
                1.0f, 0.0f, // tr_vec
                1.0f, 1.0f // br_vec
        };
    }

    @Override
    public void bindBuffers() {
        FloatBuffer verticesBuffer = ByteBuffer.allocateDirect(vertices.length * BufferUtils.BYTES_PER_FLOAT).order(ByteOrder.nativeOrder()).asFloatBuffer();
        verticesBuffer.put(vertices).position(0);
        vertices = null;

        ShortBuffer indicesBuffer = ByteBuffer.allocateDirect(indices.length * BufferUtils.BYTES_PER_SHORT).order(ByteOrder.nativeOrder()).asShortBuffer();
        indicesBuffer.put(indices).position(0);
        indices = null;
        indicesBufferCapacity = indicesBuffer.capacity();

        FloatBuffer texturesBuffer = ByteBuffer.allocateDirect(textures.length * BufferUtils.BYTES_PER_FLOAT).order(ByteOrder.nativeOrder()).asFloatBuffer();
        texturesBuffer.put(textures).position(0);
        textures = null;

        GLES20.glGenBuffers(buffers.length, buffers, 0);
        verticesBuffHandle = buffers[0];
        indicesBuffHandle = buffers[1];
        texturesBuffHandle = buffers[2];

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, verticesBuffHandle);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, verticesBuffer.capacity() * BufferUtils.BYTES_PER_FLOAT, verticesBuffer, GLES20.GL_STATIC_DRAW);
        verticesBuffer.limit(0);

        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, indicesBuffHandle);
        GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, indicesBuffer.capacity() * BufferUtils.BYTES_PER_SHORT, indicesBuffer, GLES20.GL_STATIC_DRAW);
        indicesBuffer.limit(0);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, texturesBuffHandle);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, texturesBuffer.capacity() * BufferUtils.BYTES_PER_FLOAT, texturesBuffer, GLES20.GL_STATIC_DRAW);
        texturesBuffer.limit(0);
    }

    @Override
    public void draw() {
        if (!isCreated() || !isVisible()) {
            return;
        }

        GLES20.glUseProgram(program);
        GLES20.glEnableVertexAttribArray(vertexHandle);
        GLES20.glEnableVertexAttribArray(texCoordHandle);

        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, modelViewProjection, 0);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, verticesBuffHandle);
        GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT, false, 0, 0);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, texturesBuffHandle);
        GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 0, 0);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texBuffers[0]);
        GLES20.glUniform1i(texIdHandle, 0);

        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, indicesBuffHandle);
        GLES20.glDrawElements(GLES20.GL_TRIANGLE_STRIP, indicesBufferCapacity, GLES20.GL_UNSIGNED_SHORT, 0);

        GLES20.glDisableVertexAttribArray(vertexHandle);
        GLES20.glDisableVertexAttribArray(texCoordHandle);
        GLES20.glUseProgram(0);

        GlUtils.printGlError(TAG + " - draw end");
    }

    @Override
    public void destroy() {
        super.destroy();
        GLES20.glDeleteBuffers(buffers.length, buffers, 0);
        GLES20.glDeleteTextures(texBuffers.length, texBuffers, 0);
    }
}