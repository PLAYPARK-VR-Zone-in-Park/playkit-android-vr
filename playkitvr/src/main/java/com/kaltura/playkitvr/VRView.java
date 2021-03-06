package com.kaltura.playkitvr;

/**
 * Created by anton.afanasiev on 23/07/2017.
 */

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Color;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.kaltura.android.exoplayer2.Player;
import com.kaltura.android.exoplayer2.text.Cue;
import com.kaltura.android.exoplayer2.text.TextOutput;
import com.kaltura.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.kaltura.android.exoplayer2.video.VideoListener;
import com.kaltura.playkit.PKLog;
import com.kaltura.android.exoplayer2.SimpleExoPlayer;
import com.kaltura.android.exoplayer2.ui.SubtitleView;
import com.kaltura.playkit.player.BaseExoplayerView;
import com.kaltura.playkit.player.PKSubtitlePosition;

import java.util.ArrayList;
import java.util.List;

class VRView extends BaseExoplayerView {

    private static final PKLog log = PKLog.get("VRView");
    private View shutterView;
    private SubtitleView subtitleView;
    private PKSubtitlePosition subtitleViewPosition;
    private AspectRatioFrameLayout contentFrame;

    private SimpleExoPlayer player;
    private ComponentListener componentListener;
    private Player.EventListener playerEventListener;

    private GLSurfaceView surface;
    
    VRView(Context context) {
        this(context, null);
    }

    VRView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    VRView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        componentListener = new ComponentListener();
        playerEventListener = getPlayerEventListener();

        initContentFrame();
        initGLSurfaceView();
        initSubtitleLayout();
        initPosterView();
    }

    private Player.EventListener getPlayerEventListener() {
        return new Player.EventListener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                switch (playbackState) {
                    case Player.STATE_READY:
                        if (player != null && player.getPlayWhenReady()) {
                            log.d("VRView READY. playWhenReady => true");
                            if (shutterView != null) {
                                shutterView.setVisibility(INVISIBLE);
                            }
                        }
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                log.d("VRView onIsPlayingChanged isPlaying = " + isPlaying);
                if (isPlaying && shutterView != null) {
                    shutterView.setVisibility(INVISIBLE);
                }
            }
        };
    }

    /**
     * Set the {@link SimpleExoPlayer} to use. If ExoplayerView instance already has
     * player attached to it, it will remove and clear videoSurface first.
     *
     * @param player           - The {@link SimpleExoPlayer} to use.
     * @param isSurfaceSecured - should allow secure rendering of the surface
     */
    @Override
    public void setPlayer(SimpleExoPlayer player, boolean useTextureView, boolean isSurfaceSecured, boolean hideVideoViews) {
        if (this.player == player) {
            return;
        }

        if (this.player != null) {
            removeVideoSurface();
        }
        this.player = player;
        addVideoSurface(isSurfaceSecured);
    }

    /**
     * Swap the video surface view that player should render.
     *
     * @param useTextureView   - if should use {@link TextureView} for rendering
     * @param isSurfaceSecured - should allow secure rendering of the surface
     */
    @Override
    public void setVideoSurfaceProperties(boolean useTextureView, boolean isSurfaceSecured, boolean hideVideoViews) {
        if (player != null) {
            removeVideoSurface();
            addVideoSurface(isSurfaceSecured);
        }
    }

    /**
     * Create and set relevant surface and listeners to player and attach Surface to the view hierarchy.
     *
     * @param isSurfaceSecured - should allow secure rendering of the surface
     */
    @TargetApi(17)
    private void addVideoSurface(boolean isSurfaceSecured) {
        resetViews();

        Player.TextComponent newTextComponent = player.getTextComponent();
        player.addListener(playerEventListener);

        if (newTextComponent != null) {
            newTextComponent.addTextOutput(componentListener);
        }

        surface.setSecure(isSurfaceSecured);

        if (contentFrame != null && contentFrame.getChildCount() > 0 &&  !(contentFrame.getChildAt(0) instanceof GLSurfaceView)) {
            contentFrame.addView(surface, 0);
        }
    }

    /**
     * Clear all the listeners and detach Surface from view hierarchy.
     */
    private void removeVideoSurface() {
        Player.TextComponent oldTextComponent = player.getTextComponent();
        if (playerEventListener != null) {
            player.removeListener(playerEventListener);
        }

        if (oldTextComponent != null) {
            oldTextComponent.removeTextOutput(componentListener);
        }
    }

    @Override
    public void hideVideoSurface() {
        if (surface == null || subtitleView == null) {
            return;
        }
        surface.setVisibility(GONE);
        subtitleView.setVisibility(GONE);
    }

    @Override
    public void showVideoSurface() {
        if (surface == null || subtitleView == null) {
            return;
        }

        surface.setVisibility(VISIBLE);
        subtitleView.setVisibility(VISIBLE);
    }

    @Override
    public void hideVideoSubtitles() {
        if (subtitleView == null) {
            return;
        }
        subtitleView.setVisibility(GONE);
    }

    @Override
    public void toggleVideoViewVisibility(boolean isVisible) {
        if (surface != null) {
            surface.setVisibility(isVisible ? GONE : VISIBLE);
        }
    }

    @Override
    public void showVideoSubtitles() {
        subtitleView.setVisibility(VISIBLE);
    }

    @Override
    public SubtitleView getSubtitleView() {
        return subtitleView;
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        if (surface != null) {
            surface.setVisibility(visibility);
        }
    }

    @Override
    public void setSubtitleViewPosition(PKSubtitlePosition subtitleViewPosition) {
        this.subtitleViewPosition = subtitleViewPosition;
    }

    private void initContentFrame() {
        contentFrame = new AspectRatioFrameLayout(getContext());
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        params.gravity = Gravity.CENTER;
        contentFrame.setLayoutParams(params);
        addView(contentFrame);
    }

    private void initPosterView() {
        shutterView = new View(getContext());
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        shutterView.setLayoutParams(params);
        shutterView.setBackgroundColor(Color.BLACK);
        contentFrame.addView(shutterView);
    }

    private void initSubtitleLayout() {
        subtitleView = new SubtitleView(getContext());
        subtitleView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        subtitleView.setUserDefaultStyle();
        subtitleView.setUserDefaultTextSize();
        contentFrame.addView(subtitleView);
    }

    private void initGLSurfaceView() {
        surface = new GLSurfaceView(getContext());
        surface.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    void setSurface(Surface surface) {
        if (player != null) {
            player.setVideoSurface(surface);
        }
    }

    GLSurfaceView getGlSurface() {
        return surface;
    }

    private void resetViews() {
        if (shutterView != null) {
            shutterView.setVisibility(VISIBLE);
        }
        if (subtitleView != null) {
            subtitleView.setCues(null);
        }
    }

    /**
     * Local listener implementation.
     */
    private final class ComponentListener implements TextOutput, VideoListener, OnLayoutChangeListener {

        @Override
        public void onCues(List<Cue> cues) {
            if (subtitleViewPosition != null) {
                cues = getModifiedSubtitlePosition(cues, subtitleViewPosition);
            }

            if (subtitleView != null) {
                subtitleView.onCues(cues);
            }
        }

        @Override
        public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {

        }

        @Override
        public void onRenderedFirstFrame() {
            if (shutterView != null) {
                shutterView.setVisibility(GONE);
            }
        }

        @Override
        public void onLayoutChange(
                View view,
                int left,
                int top,
                int right,
                int bottom,
                int oldLeft,
                int oldTop,
                int oldRight,
                int oldBottom) {
        }
    }

    /**
     * Creates new cue configuration if `isIgnoreCueSettings` is set to true by application
     * Checks if the application wants to ignore the in-stream CueSettings otherwise goes with existing Cue configuration
     *
     * @param cueList cue list coming in stream
     * @param subtitleViewPosition subtitle view position configuration set by application
     * @return List of modified Cues
     */
    public List<Cue> getModifiedSubtitlePosition(List<Cue> cueList, PKSubtitlePosition subtitleViewPosition) {
        if (cueList != null && !cueList.isEmpty()) {
            List<Cue> newCueList = new ArrayList<>();
            for (Cue cue : cueList) {
                if ((cue.line !=  Cue.DIMEN_UNSET || cue.position != Cue.DIMEN_UNSET)
                        && !subtitleViewPosition.isOverrideInlineCueConfig()) {
                    newCueList.add(cue);
                    continue;
                }
                CharSequence text = cue.text;
                if (text != null) {
                    Cue newCue = new Cue.Builder().
                            setText(text).
                            setTextAlignment(subtitleViewPosition.getSubtitleHorizontalPosition()).
                            setLine(subtitleViewPosition.getVerticalPositionPercentage(), subtitleViewPosition.getLineType()).
                            setLineAnchor(cue.lineAnchor).
                            setPosition(cue.position).
                            setPositionAnchor(cue.positionAnchor).
                            setSize(subtitleViewPosition.getHorizontalPositionPercentage()).build();
                    newCueList.add(newCue);
                }
            }
            return newCueList;
        }

        return cueList;
    }
}

