package cava;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.metal.MetalSliderUI;

@SuppressWarnings("serial")
public class Seekbar extends JPanel {

    private int seekbarwidth = 300;
    private int seekbarheight = 50;
    private JLabel duration;
    private JLabel total;
    private JSlider slider;

    public class SliderStyle extends MetalSliderUI {

        ImageIcon arrow = new ImageIcon(this.getClass().getResource("images/arrow.gif"));

        public void scrollDueToClickInTrack(int dir) {
            slider.setValue(valueForXPosition(slider.getMousePosition().x));
        }

        public void paintTrack(Graphics g) {
            int trackX = trackRect.x;
            int trackY = trackRect.y + (trackRect.height - getTrackWidth()) / 2;
            int trackW = trackRect.width;
            int trackH = getTrackWidth();

            Graphics2D graphics2 = (Graphics2D) g;

            g.setColor(Color.BLACK);
            graphics2.drawRect(trackX - 1, trackY - 1, trackW + 2, trackH + 2);
            graphics2.fillRect(trackX - 1, trackY - 1, trackW + 2, trackH + 2);

            g.setColor(Color.DARK_GRAY);
            graphics2.drawRect(trackX, trackY, trackW, trackH);
            graphics2.fillRect(trackX, trackY, trackW, trackH);


            int xPos = xPositionForValue(slider.getValue());
            int w = xPos - trackRect.x;

            if (slider.getValue() > 0) {
                g.setColor(Color.LIGHT_GRAY);
                if (w < 3) {
                    g.drawRect(trackX, trackY, 3, trackH);
                    g.fillRect(trackX, trackY, 3, trackH);
                    g.setColor(Color.black);
                    g.drawRect(w+7, trackY, 1, 7);
                    g.fillRect(w+7, trackY, 1, 7);
                    g.setColor(Color.DARK_GRAY);
                    g.drawRect(trackX + w + 1, trackY, 2, trackH);
                    g.fillRect(trackX + w + 1, trackY, 2, trackH);
                } else {
                    g.drawRect(trackX, trackY, 3, trackH);
                    g.fillRect(trackX, trackY, 3, trackH);
                    g.setColor(Color.BLACK);
                    g.drawRect(w+7, trackY, 1, 7);
                    g.fillRect(w+7, trackY, 1, 7);
                    g.setColor(Color.LIGHT_GRAY);

                    if (slider.getValue() < slider.getMaximum() - 3) {
                        g.drawRect(trackX + 3, trackY, w - 3, trackH);
                        g.fillRect(trackX + 3, trackY, w - 3, trackH);
                    } else {
                        g.drawRect(trackX + 3, trackY, xPositionForValue(slider.getMaximum()) - 6 - trackX, trackH);
                        g.fillRect(trackX + 3, trackY, xPositionForValue(slider.getMaximum()) - 6 - trackX, trackH);
                        g.drawRect(trackX + w - 3, trackY, 3, trackH);
                        g.fillRect(trackX + w - 3, trackY, 3, trackH);
                    }
                }
            }
        }

        public void paintThumb(Graphics g) {
            arrow.paintIcon(slider, g, thumbRect.x + arrow.getIconWidth() - 1, thumbRect.y + 11);
        }
    }

    //Final to keep inner class happy
    Seekbar(final AudioPlayer audioPlayer) {
        this.setSize(new Dimension(seekbarwidth, seekbarheight));
        slider = new JSlider(JSlider.HORIZONTAL, 0, seekbarwidth, 0);
        SliderStyle style = new SliderStyle();
        slider.setUI(style);
        slider.setEnabled(false);

        slider.addChangeListener(new ChangeListener() {

            private long prevupdate = System.currentTimeMillis();
            private boolean finalupdated = true;
            private boolean prevadjusting = false;
            private int prevvalue;
            private Timer tim = new Timer();

            @Override
            public void stateChanged(ChangeEvent e) {
                Object source = e.getSource();
                if (source instanceof JSlider) {
                    JSlider slidersource = (JSlider) source;
                    boolean adjusting = slidersource.getValueIsAdjusting();
                    int value = slidersource.getValue();
                    //If there's no long playing, duration will be 0. Also check value to avoid infinite loop
                    if (duration.getText() == "0:00" && value != 0) {
                        slidersource.setValue(0);
                        return;
                    }
                    if (adjusting) {
                        if (adjusting == prevadjusting) {
                            if (finalupdated) {
                                finalupdated = false;
                            } else {
                                Calendar calendar = Calendar.getInstance();
                                calendar.setTimeInMillis(System.currentTimeMillis() + 400);

                                tim.cancel();
                                tim = new Timer();
                                tim.schedule(new TimerTask() {

                                    @Override
                                    public void run() {
                                        if (!finalupdated) {
                                            int value = slider.getValue();
                                            //System.out.println("Timer - Dragging - Update time position to: " + (double) value / slider.getMaximum());
                                            audioPlayer.DoAudioAction(AudioAction.SeekBarChange);

                                            prevupdate = System.currentTimeMillis();
                                            prevvalue = value;
                                        }
                                    }
                                }, calendar.getTime());

                                if (System.currentTimeMillis() - prevupdate >= 500) {
                                    if (value > (prevvalue + 3) || value < (prevvalue - 3)) {
                                        //System.out.println("Dragging - Update time position to: " + (double) value / slidersource.getMaximum());
                                        //Update the position
                                        audioPlayer.DoAudioAction(AudioAction.SeekBarChange);
                                    }

                                    prevupdate = System.currentTimeMillis();
                                    prevvalue = value;
                                }
                            }
                        }
                    } else {
                        if (!finalupdated) {
                            tim.cancel();

                            if (value > (prevvalue + 3) || value < (prevvalue - 3)) {
                                //System.out.println("Finished Drag - Update time position to: " + (double) value / slidersource.getMaximum());
                                //Update the position
                                audioPlayer.DoAudioAction(AudioAction.SeekBarChange);
                            }

                            finalupdated = true;
                        }
                    }
                    prevadjusting = adjusting;
                }
            }
        });


        this.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        duration = new JLabel();
        duration.setText("");
        duration.setForeground(Color.white);
        duration.setFont(Cava.MoviePosterSS);
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.FIRST_LINE_START;
        c.insets = new Insets(0, 8, 5, 0);
        c.gridx = 0;
        c.gridy = 0;
        c.ipadx = 0;
        c.ipady = 0;
        c.gridheight = 1;
        this.add(duration, c);

        total = new JLabel();
        total.setText("");
        total.setForeground(Color.white);
        total.setFont(Cava.MoviePosterSS);
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.FIRST_LINE_END;
        c.insets = new Insets(0, 0, 5, 8);
        c.gridx = 1;
        c.gridy = 0;
        c.ipadx = 0;
        c.ipady = 0;
        c.gridheight = 1;
        c.gridwidth = 1;
        this.add(total, c);

        c.anchor = GridBagConstraints.PAGE_END;
        c.insets = new Insets(0, 0, 0, 0);
        c.gridx = 0;
        c.gridy = 1;
        c.ipadx = 60;
        c.ipady = 0;
        c.gridheight = 1;
        c.gridwidth = 2;
        this.add(slider, c);
        this.setOpaque(false);

        slider.addChangeListener(new ControlListener(ListenerFor.seekBar, audioPlayer));
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
    }

    public int getDuration() {
        return this.slider.getValue();
    }

    public double getPosition() {
        return (double) slider.getValue() / (double) slider.getMaximum();
    }

    public boolean getValueIsAdjusting() {
        return slider.getValueIsAdjusting();
    }

    private void setMinutes(double duration, int tracklength) {
        int minutes = 0, seconds = 0;
        double tmp = duration * tracklength;
        while (tmp > 59) {
            tmp = tmp - 60;
            minutes = minutes + 1;
        }
        seconds = (int) tmp;

        if (seconds > 9) {
            this.duration.setText(minutes + ":" + seconds);
        }
        if (seconds < 10) {
            this.duration.setText(minutes + ":" + "0" + seconds);
        }

        tmp = (double) tracklength;
        minutes = 0;
        while (tmp > 59) {
            tmp = tmp - 60;
            minutes = minutes + 1;
        }
        seconds = (int) tmp;

        if (seconds > 9) {
            this.total.setText(minutes + ":" + seconds);
        }
        if (seconds < 10) {
            this.total.setText(minutes + ":" + "0" + seconds);
        }
    }

    public void setDuration(double duration, boolean isPlaying, int trackLength) {
        if (duration < 0 || duration > 1) {
            Dbg.syserr("Invalid duration:" + duration);
            return;
        }

        if (isPlaying == true) {
            double leftsize = duration * seekbarwidth;

            int left = (int) Math.round(leftsize);

            if (!slider.getValueIsAdjusting()) {
                slider.setValue(left);

            }
            setMinutes(duration, trackLength);
        } else {
            this.duration.setText("");
            this.total.setText("");
            slider.setValue(0);
        }
    }
    
    public void setEnabled(boolean enabled) {
        slider.setEnabled(enabled);
    }
}
