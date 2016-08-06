package com.gmail.jiangyang5157.cardboard.scene.model;

import android.content.Context;
import android.opengl.GLES20;
import android.text.Layout;
import android.util.ArrayMap;

import com.gmail.jiangyang5157.cardboard.vr.R;

/**
 * @author Yang
 * @since 5/13/2016
 */
public class MarkerDetailView extends Dialog {

    private Event eventListener;

    public interface Event {
        void showObjModel(ObjModel model);
    }

    private AtomMarker marker;

    public MarkerDetailView(Context context, AtomMarker marker) {
        super(context);
        this.marker = marker;
    }

    @Override
    public void create(int program) {
        super.create(program);

        setCreated(true);
        setVisible(true);
    }

    @Override
    protected void createPanels() {
        ArrayMap<Integer, Integer> shaders = new ArrayMap<>();
        shaders.put(GLES20.GL_VERTEX_SHADER, R.raw.panel_vertex_shader);
        shaders.put(GLES20.GL_FRAGMENT_SHADER, R.raw.panel_fragment_shader);

        if (marker.getName() != null) {
            SubPanel tf1 = new SubPanel(context);
            tf1.setText(marker.getName());
            tf1.width = WIDTH;
            tf1.setScale(SCALE);
            tf1.modelRequireUpdate = true;
            tf1.setTextSize(SubPanel.TEXT_SIZE_LARGE);
            tf1.setAlignment(Layout.Alignment.ALIGN_CENTER);
            tf1.create(shaders);
            addPanel(tf1);
        }
        if (marker.getDescription() != null) {
            SubPanel tf2 = new SubPanel(context);
            tf2.setText(marker.getDescription());
            tf2.width = WIDTH;
            tf2.setScale(SCALE);
            tf2.modelRequireUpdate = true;
            tf2.setTextSize(SubPanel.TEXT_SIZE_TINY);
            tf2.setAlignment(Layout.Alignment.ALIGN_NORMAL);
            tf2.create(shaders);
            addPanel(tf2);
        }
        if (marker.getObjModel() != null) {
            SubPanel tf3 = new SubPanel(context);
            tf3.setText(marker.getObjModel().getTitle());
            tf3.width = WIDTH;
            tf3.setScale(SCALE);
            tf3.modelRequireUpdate = true;
            tf3.setTextSize(SubPanel.TEXT_SIZE_TINY);
            tf3.setAlignment(Layout.Alignment.ALIGN_NORMAL);
            tf3.create(shaders);
            tf3.setOnClickListener(new GlModel.ClickListener() {
                @Override
                public void onClick(GlModel model) {
                    if (eventListener != null) {
                        eventListener.showObjModel(marker.getObjModel());
                    }
                }
            });
            addPanel(tf3);
        }
    }

    public void setEventListener(Event eventListener) {
        this.eventListener = eventListener;
    }
}
