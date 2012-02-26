package cava;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;

public class ControlListener implements ActionListener, ChangeListener {

    private ListenerFor listensTo;
    private AudioPlayer audioPlayer;

    public ControlListener(ListenerFor listensTo, AudioPlayer audioPlayer) {
        this.listensTo = listensTo;
        this.audioPlayer = audioPlayer;
    }

    @Override
    public void actionPerformed(ActionEvent arg0) {
        if (listensTo == ListenerFor.Pause) {
            switchPlayPause();
            audioPlayer.DoAudioAction(AudioAction.Pause);
        } else if (listensTo == ListenerFor.Play) {
            switchPlayPause();
            audioPlayer.DoAudioAction(AudioAction.Resume);
        } else if (listensTo == ListenerFor.SkipBack) {
            audioPlayer.DoAudioAction(AudioAction.SkipBack);
        } else if (listensTo == ListenerFor.SkipFoward) {
            audioPlayer.DoAudioAction(AudioAction.SkipForward);
        } else if (listensTo == ListenerFor.Stop) {
            audioPlayer.DoAudioAction(AudioAction.Stop);
        } else if (listensTo == ListenerFor.QuitProg) {
            System.exit(0);
        }

    }

    public void stateChanged(ChangeEvent e) {
        if (listensTo == ListenerFor.seekBar) {
            //audioPlayer.DoAudioAction(AudioAction.Pause);
            //audioPlayer.SeekbarResume();
        }
    }

    private void switchPlayPause() {
        if (listensTo == ListenerFor.Pause) {
            listensTo = ListenerFor.Play;
        } else if (listensTo == ListenerFor.Play) {
            listensTo = ListenerFor.Pause;
        }
    }
}
