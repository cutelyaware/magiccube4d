package com.superliminal.magiccube4d;

import java.net.URL;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;

import com.superliminal.util.PropertyManager;
import com.superliminal.util.PropertyManager.PropertyListener;


/**
 * Services for playing audio clips.
 * 
 * @author Melinda Green
 */
public class Audio {
    public final static Audio instance = new Audio();

    public enum Sound {
        HIGHLIGHT, TWISTING, SNAP, CORRECT, FANFARE
    };
    private static Clip
        highlight,
        twisting,
        snap,
        correct,
        fanfare;

    // This is a workaround for issue #114 on Ubuntu: "Exception thrown initializing audio on Ubuntu".
    // If any exceptions are thrown by the audio system I set this to true and check it before
    // performing future operations. So once broken, there will be no audio for the session.
    private static boolean audioBroken = false;

    public Audio() {
        try {
            twisting = get("white1000.wav", .3f);
            highlight = get("click.wav", .0f);
            snap = get("close.wav", .7f);
            correct = get("correct.wav", .8f);
            fanfare = get("fanfare.wav", 1);
        } catch(Throwable t) {
            audioBroken = true;
            t.printStackTrace();
        }
        if(audioBroken)
            return; // Don't bother listening for mute changes when we're not going to do anything.
        // Listen for changes to the "muted" property and cancel all sounds when true.
        PropertyManager.top.addPropertyListener(new PropertyListener() {
            @Override
            public void propertyChanged(String property, String newval) {
                if("true".equals(newval)) {
                    stop(Sound.TWISTING);
                    stop(Sound.HIGHLIGHT);
                    stop(Sound.SNAP);
                    stop(Sound.CORRECT);
                    stop(Sound.FANFARE);
                }
            }
        }, MagicCube.MUTED);
    }

    public static void play(Sound sound) {
        play(sound, false);
    }

    public static void loop(Sound sound) {
        play(sound, true);
    }

    public static void stop(Sound sound) {
        if(audioBroken)
            return;
        Clip clip = sound2clip(sound);
        if(clip != null) {
            try {
                clip.stop();
            } catch(Throwable t) {
                audioBroken = true;
                t.printStackTrace();
            }
        }
    }

    private static Clip sound2clip(Sound sound) {
        switch(sound) {
            case HIGHLIGHT:
                return highlight;
            case TWISTING:
                return twisting;
            case SNAP:
                return snap;
            case CORRECT:
                return correct;
            case FANFARE:
                return fanfare;
            default:
                return highlight;
        }
    }

    private static void play(Sound sound, boolean looped) {
        if(PropertyManager.getBoolean(MagicCube.MUTED, false) || audioBroken)
            return; // Don't start any sounds while muted or broken.
        Clip clip = sound2clip(sound);
        if(clip == null)
            return; // Just being defensive.
        try {
            clip.setFramePosition(0);
            clip.setLoopPoints(0, clip.getFrameLength() - 1);
            if(looped)
                clip.loop(Clip.LOOP_CONTINUOUSLY);
            else
                clip.start();
        } catch(Throwable t) {
            audioBroken = true;
            t.printStackTrace();
        }
    }

    private Clip get(String fname, float scale) {
        // Open an audio input stream.
        URL url = this.getClass().getClassLoader().getResource(fname);
        AudioInputStream audioIn = null;
        Clip clip = null;
        try {
            audioIn = AudioSystem.getAudioInputStream(url);
            // Get a sound clip resource.
            clip = AudioSystem.getClip();
            clip.open(audioIn);
            FloatControl volctrl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            float min = volctrl.getMinimum();
            float max = volctrl.getMaximum();
            volctrl.setValue(min + (max - min) * scale);// newVal - the value of volume slider
        } catch(Exception e) {
            audioBroken = true;
            e.printStackTrace();
        }
        return clip;
    }

}
