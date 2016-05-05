package com.gmail.jiangyang5157.cardboard.scene.projection;

import android.content.Context;

import com.gmail.jiangyang5157.cardboard.scene.AimIntersection;

/**
 * @author Yang
 * @since 5/5/2016
 */
public abstract class Rectangle extends GLModel implements Intersectable {

    protected Rectangle(Context context, int vertexShaderRawResource, int fragmentShaderRawResource) {
        super(context, vertexShaderRawResource, fragmentShaderRawResource);
    }
    
    @Override
    public AimIntersection intersect(float[] cameraPosition, float[] forwardDirection) {
        // TODO: 5/5/2016
        return null;
    }
}
