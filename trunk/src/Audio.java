import java.net.URL;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;


/**
 * Services for playing audio clips.
 * 
 * @author Melinda Green
 */
public class Audio {
	public final static Audio instance = new Audio();
	public enum Sound { HIGHLIGHT, TWISTING, SNAP, CORRECT, FANFARE };
	private static Clip 
		highlight,
		twisting,
		snap,
		correct,
		fanfare;
	private static boolean muted = false;
	
	public Audio() {
		twisting = get("white1000.wav", .4f);
		highlight = get("click.wav", .0f);
		snap = get("close.wav", .7f);
		correct = get("correct.wav", .8f);
		fanfare = get("tada.wav", 1);
	}
	
	public static void play(Sound sound) {
		play(sound, false);
	}
	public static void loop(Sound sound) {
		play(sound, true);
	}
	public static void stop(Sound sound) {
		sound2clip(sound).stop();
	}
	
	public static void setMuted(boolean mute) {
		muted = mute;
		if(mute) {
			twisting.stop();
			highlight.stop();
			snap.stop();
			correct.stop();
			fanfare.stop();
		}
	}
	
	private static Clip sound2clip(Sound sound) {
		switch(sound) {
			case HIGHLIGHT: return highlight;
			case TWISTING: return twisting;
			case SNAP: return snap;
			case CORRECT: return correct;
			case FANFARE: return fanfare;
			default: return highlight;
		}
	}
	
	private static void play(Sound sound, boolean looped) {
		if(muted)
			return;
		Clip clip = sound2clip(sound);
		clip.setFramePosition(0);
		clip.setLoopPoints(0, clip.getFrameLength()-1);
		if(looped)
			clip.loop(Clip.LOOP_CONTINUOUSLY);
		else
			clip.start();
	}
	
	private Clip get(String fname, float scale){
		// Open an audio input stream.
	    URL url = this.getClass().getClassLoader().getResource(fname);
	    AudioInputStream audioIn = null;
	    Clip clip = null;
		try {
			audioIn = AudioSystem.getAudioInputStream(url);
			// Get a sound clip resource.
		    clip = AudioSystem.getClip();
			clip.open(audioIn);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		FloatControl volctrl=(FloatControl)clip.getControl(FloatControl.Type.MASTER_GAIN);
		float min = volctrl.getMinimum();
		float max = volctrl.getMaximum();
		volctrl.setValue(min + (max-min)*scale);// newVal - the value of volume slider
	    return clip;
	}

}
