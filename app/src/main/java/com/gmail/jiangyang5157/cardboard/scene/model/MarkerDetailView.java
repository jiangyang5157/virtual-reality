package com.gmail.jiangyang5157.cardboard.scene.model;

import android.content.Context;
import android.text.Layout;

import com.gmail.jiangyang5157.tookit.android.base.AppUtils;

/**
 * @author Yang
 * @since 5/13/2016
 */
public class MarkerDetailView extends Dialog {
    private static final String TAG = "[MarkerDetailView]";

    private Event eventListener;
    public interface Event {
        void showObjModel(ObjModel model);
    }

    private AtomMarker marker;

    public MarkerDetailView(Context context, AtomMarker marker) {
        super(context);
        this.marker = marker;
        setColor(AppUtils.getColor(context, com.gmail.jiangyang5157.tookit.android.base.R.color.Red, null));
    }

    public void prepare(final Ray ray) {
        getHandler().post(new Runnable() {
            @Override
            public void run() {
                creationState = STATE_PREPARING;
                ray.addBusy();

                buildPanels();
                int iSize = panels.size();
                for (int i = 0; i < iSize; i++) {
                    panels.get(i).prepare(ray);
                }
                adjustBounds(WIDTH);

                buildTextureBuffers();
                buildData();

                ray.subtractBusy();
                creationState = STATE_BEFORE_CREATE;
            }
        });
    }

    @Override
    public void create(int program) {
        creationState = STATE_CREATING;
        super.create(program);
        bindHandles();
        bindTextureBuffers();
        bindBuffers();

        int iSize = panels.size();
        for (int i = 0; i < iSize; i++) {
            panels.get(i).create(program);
        }

        setCreated(true);
        setVisible(true);
        creationState = STATE_BEFORE_CREATE;
    }

    protected void buildPanels() {
        if (marker.getName() != null) {
            TextField p1 = new TextField(context);
            p1.setCcntent(marker.getName());
            p1.width = WIDTH;
            p1.setScale(SCALE);
            p1.modelRequireUpdate = true;
            p1.setTextSize(TextField.TEXT_SIZE_LARGE);
            p1.setAlignment(Layout.Alignment.ALIGN_CENTER);

            addPanel(p1);
        }
        if (marker.getDescription() != null) {
            DescriptionField p2 = new DescriptionField(context);
            p2.setCcntent(marker.getDescription());
            p2.width = WIDTH;
            p2.setScale(SCALE);
            p2.modelRequireUpdate = true;
            p2.setTextSize(TextField.TEXT_SIZE_TINY);
            p2.setAlignment(Layout.Alignment.ALIGN_NORMAL);

            addPanel(p2);
        }
        if (marker.getObjModel() != null) {
            TextField p3 = new TextField(context);
            p3.setCcntent(marker.getObjModel().getTitle());
            p3.width = WIDTH;
            p3.setScale(SCALE);
            p3.modelRequireUpdate = true;
            p3.setTextSize(TextField.TEXT_SIZE_TINY);
            p3.setAlignment(Layout.Alignment.ALIGN_NORMAL);
            p3.setOnClickListener(new GlModel.ClickListener() {
                @Override
                public void onClick(GlModel model) {
                    if (eventListener != null) {
                        eventListener.showObjModel(marker.getObjModel());
                    }
                }
            });

            addPanel(p3);
        }
    }

    public void setEventListener(Event eventListener) {
        this.eventListener = eventListener;
    }
}
