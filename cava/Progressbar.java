package cava;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.ImageIcon;

@SuppressWarnings("serial")
public class Progressbar extends JPanel {

    private static JProgressBar progressbar;
    private static int progress_SCMS = 100;
    private static int progress_Lyrics = 100;
    private static int progress_Playlist = 100;
    private static int progress_Import = 100;
    private static ActionListener actionL_SCMS;
    private static ActionListener actionL_Lyrics;
    private static ActionListener actionL_Playlist;
    private static ActionListener actionL_Import;
    private static ActionListener actionL_Current;
    private static JLabel progtext = new JLabel();
    private static JLabel progtextv = new JLabel();
    private static int currentPrio = 0;
    private static ProgressType currentType;
    private static JButton bCancel;
    private static ImageIcon imgCancel;
    private static ImageIcon imgCancelH;

    public Progressbar() {
        setupProgressbar();
    }

    public static void main(String args[]) {
        new Progressbar();
    }

    void setupProgressbar() {
        progressbar = new JProgressBar(0, 100);

        bCancel = new JButton();
        imgCancel = new ImageIcon(this.getClass().getResource("images/progress_cancel.png"));
        imgCancelH = new ImageIcon(this.getClass().getResource("images/progress_cancel_hold.png"));
        bCancel.setIcon(imgCancel);

        this.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();

        progressbar.setSize(100, 5);
        progressbar.setStringPainted(false);
        progressbar.setString("");
        progressbar.setBorderPainted(false);
        progressbar.setBackground(Color.darkGray);
        progressbar.setForeground(Color.GRAY);
        progressbar.setFont(Cava.Ari);
        progressbar.setOpaque(false);
        c.anchor = GridBagConstraints.FIRST_LINE_START;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(7, 0, 0, 0);
        c.gridx = 0;
        c.gridy = 0;
        c.gridheight = 1;
        c.gridwidth = 2;
        c.ipadx = 160;
        c.ipady = -10;
        this.add(progressbar, c);

        progtext.setHorizontalAlignment(JLabel.LEFT);
        progtext.setOpaque(false);
        progtext.setText("");
        progtext.setFont(Cava.Ari);
        progtext.setForeground(Cava.lightish);
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.FIRST_LINE_END;
        c.insets = new Insets(1, 0, 0, 0);
        c.gridx = 0;
        c.gridy = 1;
        c.gridheight = 1;
        c.gridwidth = 1;
        c.ipadx = 0;
        c.ipady = 0;
        this.add(progtext, c);

        progtextv.setOpaque(false);
        progtextv.setText("");
        progtextv.setFont(Cava.Ari);
        progtextv.setForeground(Cava.lightish);
        progtextv.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        c.anchor = GridBagConstraints.FIRST_LINE_END;
        c.insets = new Insets(1, 0, 0, 0);
        c.gridx = 1;
        c.gridy = 1;
        c.gridheight = 1;
        c.gridwidth = 1;
        c.ipadx = 0;
        c.ipady = 0;
        this.add(progtextv, c);

        bCancel.addMouseListener(new CancelListener());
        bCancel.setVisible(false);
        bCancel.setOpaque(false);
        bCancel.setBorderPainted(false);
        c.anchor = GridBagConstraints.FIRST_LINE_START;
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(0, 0, 0, 0);
        c.gridx = -5;
        c.gridy = -5;
        c.gridheight = 2;
        c.gridwidth = 1;
        c.ipadx = -10;
        c.ipady = -10;
        this.add(bCancel, c);
    }

    // Generate SCMS data --> Generating playlists --> Lyrics search --> Import
    // tracks
    public static void startWorking(ProgressType type) {

        int prio = 0;
        int progress = 0;
        String output = null;

        if (progress_Lyrics + progress_SCMS + progress_Playlist
                + progress_Import == 400) {
            bCancel.setVisible(true);
            progressbar.setOpaque(true);
        }

        if (type == ProgressType.SCMS) {
            prio = 1;
            output = "Generating SCMS Data";
            progress_SCMS = progress;
        } else if (type == ProgressType.Import) {
            prio = 2;
            output = "Importing Tracks";
            progress_Import = progress;
        } else if (type == ProgressType.Playlist) {
            prio = 3;
            output = "Generating Playlists";
            progress_Playlist = progress;
        } else if (type == ProgressType.Lyrics) {
            prio = 4;
            output = "Generating Lyrics";
            progress_Lyrics = progress;
        }

        if (currentPrio == 0) {
            currentPrio = prio;
        }

        if (prio >= currentPrio) {
            currentPrio = prio;
            progtext.setText(output);
            currentType = type;
            if (prio == 2) {
                progressbar.setIndeterminate(true);
            } else {
                progtextv.setText(progress + "%");
                progressbar.setValue(progress);
            }
        }
    }

    public static void setProgressValue(int progress, ProgressType type) {

        int prio = 0;
        String output = null;
        ActionListener actionL_this = null;

        if (type == ProgressType.SCMS) {
            prio = 1;
            output = "Generating SCMS Data";
            progress_SCMS = progress;
            actionL_this = actionL_SCMS;
        } else if (type == ProgressType.Import) {
            prio = 2;
            output = "Importing Tracks";
            progress_Import = progress;
            actionL_this = actionL_Import;
        } else if (type == ProgressType.Playlist) {
            prio = 3;
            output = "Generating Playlists";
            progress_Playlist = progress;
            actionL_this = actionL_Playlist;
        } else if (type == ProgressType.Lyrics) {
            prio = 4;
            output = "Generating Lyrics";
            progress_Lyrics = progress;
            actionL_this = actionL_Lyrics;
        }

        if (currentPrio == 0) {
            currentPrio = prio;
        }
        if (prio > currentPrio) {
            setCancelListener(actionL_this, type);
        }

        if (prio >= currentPrio) {
            if (prio == 2) {
                progressbar.setIndeterminate(true);
            } else {
                progressbar.setValue(progress);
                progtext.setText(output);
                progtextv.setText(progress + "%");
            }
        }
    }

    public static void finishedWorking(ProgressType type) {

        ActionListener actionL_New = null;
        int prio = 0;
        if (type == ProgressType.SCMS) {
            prio = 1;
            actionL_New = actionL_SCMS;
            progress_SCMS = 100;
        } else if (type == ProgressType.Import) {
            prio = 2;
            actionL_New = actionL_Import;
            progress_Import = 100;
        } else if (type == ProgressType.Playlist) {
            prio = 3;
            actionL_New = actionL_Playlist;
            progress_Playlist = 100;
        } else if (type == ProgressType.Lyrics) {
            prio = 4;
            actionL_New = actionL_Lyrics;
            progress_Lyrics = 100;
        }

        if(prio == currentPrio){
            currentPrio = 0;
        }


        if (progress_Lyrics + progress_SCMS + progress_Playlist
                + progress_Import == 400) {
            bCancel.setVisible(false);
            progressbar.setIndeterminate(false);
            progressbar.setOpaque(false);
            progressbar.setValue(0);
            progtext.setText("");
            progtextv.setText("");
            currentPrio = 0;
        }

        if (actionL_New == actionL_Current) {
            bCancel.removeActionListener(actionL_Current);
        }
    }

    public static void setCancelListener(ActionListener actionL_New, ProgressType type) {

        if (type == ProgressType.SCMS) {
            actionL_SCMS = actionL_New;
        } else if (type == ProgressType.Import) {
            actionL_Import = actionL_New;
        } else if (type == ProgressType.Playlist) {
            actionL_Playlist = actionL_New;
        } else if (type == ProgressType.Lyrics) {
            actionL_Lyrics = actionL_New;
        }

        if (type == currentType) {
            bCancel.removeActionListener(actionL_Current);
            bCancel.addActionListener(actionL_New);
            // bCancel.addMouseListener(new CancelListener(currentType));
        }
    }

    public static enum ProgressType {

        Lyrics, SCMS, Playlist, Import;
    }

    private static class CancelListener extends MouseAdapter {

        @Override
        public void mousePressed(MouseEvent me) {
            bCancel.setIcon(imgCancelH);
            bCancel.setOpaque(false);
        }

        @Override
        public void mouseReleased(MouseEvent me) {
            bCancel.setIcon(imgCancel);
            System.out.println("Cancel");
        }
    }
}
