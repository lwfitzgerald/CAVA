package cava.miraje;

public interface ProgressListener {
    /**
     * Called by Mir when a search is started
     */
    public void searchStarted();
    
    /**
     * Called by Mir when the progress changes
     * @param progress Float between 0 and 1 representing progress
     */
    public void updateProgress(float progress);
}