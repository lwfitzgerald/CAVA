package cava;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.BoxLayout;

import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.metal.MetalSliderUI;

@SuppressWarnings("serial")
public class Volumebar extends JPanel {

    private JSlider slider;
    private int seekbarwidth = 300;
    private int seekbarheight = 50;
    private int muteVolume;
    public Boolean muted;
    private AudioPlayer audioPlayerVar;

    public class SliderStyle extends MetalSliderUI {

        ImageIcon arrow = new ImageIcon(this.getClass().getResource("images/arrow.gif"));
        //ImageIcon arrow = new ImageIcon(this.getClass().getResource("images/quit.png"));

        public void scrollDueToClickInTrack(int dir) {
            //System.out.println(valueForXPosition(slider.getMousePosition().x));
            slider.setValue(valueForXPosition(slider.getMousePosition().x));
        }

        public void paintTrack(Graphics g) {
            int trackX = trackRect.x;
            int trackY = trackRect.y + (trackRect.height - getTrackWidth()) / 2;
            int trackW = trackRect.width;
            int trackH = getTrackWidth();

            Graphics2D graphics2 = (Graphics2D) g;

            g.setColor(Color.BLACK);
            graphics2.drawRoundRect(trackX - 1, trackY - 1, trackW + 2, trackH + 2, 2, 2);
            graphics2.fillRoundRect(trackX - 1, trackY - 1, trackW + 2, trackH + 2, 2, 2);

            g.setColor(Color.DARK_GRAY);
            graphics2.drawRoundRect(trackX, trackY, trackW, trackH, 2, 2);
            graphics2.fillRoundRect(trackX, trackY, trackW, trackH, 2, 2);

            int xPos = xPositionForValue(slider.getValue());
            int w = xPos - trackRect.x;

            if (slider.getValue() > 0) {
                g.setColor(Color.LIGHT_GRAY);
                if (w < 3) {
                    g.drawOval(trackX, trackY, 3, trackH);
                    g.fillOval(trackX, trackY, 3, trackH);
                    g.setColor(Color.BLACK);
                    g.drawRect(w + 7, trackY, 1, 7);
                    g.fillRect(w + 7, trackY, 1, 7);
                    g.setColor(Color.DARK_GRAY);
                    g.drawRect(trackX + w + 1, trackY, 2, trackH);
                    g.fillRect(trackX + w + 1, trackY, 2, trackH);
                } else {
                    g.drawOval(trackX, trackY, 3, trackH);
                    g.fillOval(trackX, trackY, 3, trackH);
                    g.setColor(Color.BLACK);
                    g.drawRect(w + 7, trackY, 1, 7);
                    g.fillRect(w + 7, trackY, 1, 7);
                    g.setColor(Color.LIGHT_GRAY);

                    if (slider.getValue() < slider.getMaximum() - 3) {
                        g.drawRect(trackX + 3, trackY, w - 3, trackH);
                        g.fillRect(trackX + 3, trackY, w - 3, trackH);
                    } else {
                        g.drawRect(trackX + 3, trackY, xPositionForValue(slider.getMaximum()) - 6 - trackX, trackH);
                        g.fillRect(trackX + 3, trackY, xPositionForValue(slider.getMaximum()) - 6 - trackX, trackH);
                        g.drawOval(trackX + w - 3, trackY, 3, trackH);
                        g.fillOval(trackX + w - 3, trackY, 3, trackH);
                    }
                }
            }
        }

        public void paintThumb(Graphics g) {
            //arrow.paintIcon(slider, g, thumbRect.x + arrow.getIconWidth() - 1, thumbRect.y);
        }
    }

    //Final to keep inner class happy
    Volumebar(final AudioPlayer audioPlayer, ImageButton mButton) {

        // By default, the music player loads as unmuted, even if the user had it muted before
        muted = false;

        audioPlayerVar = audioPlayer;
	audioPlayerVar.setMuteButton(mButton);
        this.setSize(new Dimension(seekbarwidth, seekbarheight));
        slider = new JSlider(JSlider.HORIZONTAL, 0, seekbarwidth, 0);
        // Set the default volume
        slider.setValue(seekbarwidth);
        SliderStyle style = new SliderStyle();
        slider.setUI(style);


        slider.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                Object source = e.getSource();
                if (source instanceof JSlider) {
                    //System.out.println( "volume being adjusted   " + (slider.getValue() / (double) slider.getMaximum()));
                    audioPlayer.DoAudioAction(AudioAction.VolumeBarChange);
                }
            }
        });

        this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

        this.add(slider);
    }

    public void mute() {
        if (muted == false) {
            muted = true;
            muteVolume = slider.getValue();
            audioPlayerVar.DoAudioAction(AudioAction.VolumeBarChange);
        } else {
            muted = false;
            muteVolume = slider.getValue();
            audioPlayerVar.DoAudioAction(AudioAction.VolumeBarChange);
        }

    }

    public double getVolume() {
        if (muted == false) {
            return (double) slider.getValue() / (double) slider.getMaximum();
        } else {
            return 0;
        }
    }

    // This function is just so that we can check if the program needs to be unmuted because the slider has been moved.
    public Boolean sliderMoved() {
        if (muteVolume == slider.getValue()) {
            return false;
        } else {
            muted = false;
            return true;
        }


    }

    public void setVolume(final AudioPlayer audioPlayer, float value) {
        slider.setValue((int) (value * slider.getMaximum()));
        audioPlayer.DoAudioAction(AudioAction.VolumeBarChange);
    }
}
