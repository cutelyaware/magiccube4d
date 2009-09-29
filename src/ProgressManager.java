import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JProgressBar;
import javax.swing.SwingWorker;

/*
 * Subclass of SwingWorker that manages a JProgressBar and includes
 * initialization and update methods that affect it.
 * Initialization will turn the progress bar to be visible,
 * and invisible when done.
 * Initialization can put the progress bar into determinant or indeterminant mode.
 * When in determinant mode, use the updateProgress method to set the bar position
 * between 0 and the supplied max values.
 * 
 * @author Melinda Green
 */
public abstract class ProgressManager extends SwingWorker<Void, Void> {
	private JProgressBar progressView;
	private int max;
	
	public ProgressManager(JProgressBar progress) {
		this.progressView = progress;
		this.addPropertyChangeListener(
  		     new PropertyChangeListener() {
   		    	 @Override
   		         public  void propertyChange(PropertyChangeEvent evt) {
   		    		 String eventName = evt.getPropertyName();
   		             if ("progress".equals(eventName)) {
   			        	 int newVal = ((Integer)evt.getNewValue()).intValue();
   		                 progressView.setValue(newVal);
   		                 progressView.repaint();
   		             }
   		         }
   		     }
   	    );
	}
	
	private void init(String string, boolean indeterminate, int max) {
		this.max = max;
		setProgress(0);
		progressView.setIndeterminate(indeterminate);
		progressView.setString(string);
		progressView.setVisible(true);
	}
	
	/*
	 * Initializes the progress bar in determinate mode.
	 */
	public void init(String string, int max) {
		init(string, false, max);
	}

	/*
	 * Initializes the progress bar in indeterminate mode.
	 */
	public void init(String string) {
		init(string, true, 1);
	}
	
	public void updateProgress(int progress) {
		int prog = (int)(100.0 * progress/max);
		//System.out.println(progressView.getString() + " " + progress + " out of " + max);
		super.setProgress(prog);
	}
	
	@Override
	public void done() {
		progressView.setVisible(false);
	}
}

