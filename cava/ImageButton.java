package cava;

// ImageButton.java
// $Id: ImageButton.java,v 1.13 2000/08/16 21:37:56 ylafon Exp $
// (c) COPYRIGHT MIT and INRIA, 1997.
// Please first read the full copyright statement in file COPYRIGHT.html
//package org.w3c.tools.widgets;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Shape;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

@SuppressWarnings("serial")
public class ImageButton extends Canvas {

	private enum ButtonState {
		Start, Switched;

		public ButtonState other() {
			if (this == Start) {
				return Switched;
			} else {
				return Start;
			}
		}
	}

	private enum ImagedState {
		Plain, Hold;

		public ImagedState other() {
			if (this == Plain) {
				return Hold;
			} else {
				return Plain;
			}
		}
	}

	private AudioPlayer audioPlayer;
	protected boolean switchable = false;
	protected Image startStatePlainImg = null;
	protected Image startStateHoldImg = null;
	protected Image switchedStatePlainImg = null;
	protected Image switchedStateHoldImg = null;
	protected Image currentImg = null;
	private int width = 0;
	private int height = 0;
	private AudioAction audioAction;
	transient ActionListener actionListener;
	private ButtonState currentButtonState = ButtonState.Start;
	private ImagedState currentImageState = ImagedState.Plain;
	public Boolean muted = false;

	/**
	 * Construct an ImageButton with the specified action command
	 * 
	 * @param startStatePlainImg
	 *            The image of this ImageButton
	 * @param startStateHoldImg
	 *            The image of this ImageButton
	 * @param audioPlayer
	 * @param command
	 *            The action command String
	 */

	public ImageButton(Image startStatePlainImg, Image startStateHoldImg) {

		this.startStatePlainImg = startStatePlainImg;
		this.startStateHoldImg = startStateHoldImg;
		this.currentImg = startStatePlainImg;
		addMouseListener(new ImageButtonListener());

		if (switchedStatePlainImg != null) {
			switchable = true;
		}

		prepareImage(startStatePlainImg, this);
		if (startStateHoldImg != null)
			prepareImage(startStateHoldImg, this);
		if (switchedStatePlainImg != null)
			prepareImage(switchedStatePlainImg, this);
		if (switchedStateHoldImg != null)
			prepareImage(switchedStateHoldImg, this);
		initSize();
	}

	public ImageButton(Image startStatePlainImg, Image startStateHoldImg,
			AudioPlayer audioPlayer, AudioAction audioAction) {
		this(startStatePlainImg, startStateHoldImg, null, null, audioPlayer,
				audioAction);
	}

	public ImageButton(Image startStatePlainImg, Image startStateHoldImg,
			Image switchedStatePlainImg, Image switchedStateHoldImg,
			AudioPlayer audioPlayer, AudioAction audioAction) {

		this.startStatePlainImg = startStatePlainImg;
		this.startStateHoldImg = startStateHoldImg;
		this.switchedStatePlainImg = switchedStatePlainImg;
		this.switchedStateHoldImg = switchedStateHoldImg;
		this.currentImg = startStatePlainImg;
		this.audioPlayer = audioPlayer;
		this.audioAction = audioAction;

		addMouseListener(new ImageButtonListener());
		if (switchedStatePlainImg != null) {
			switchable = true;
		}

		prepareImage(startStatePlainImg, this);
		if (startStateHoldImg != null)
			prepareImage(startStateHoldImg, this);
		if (switchedStatePlainImg != null)
			prepareImage(switchedStatePlainImg, this);
		if (switchedStateHoldImg != null)
			prepareImage(switchedStateHoldImg, this);
		initSize();
	}

	private int max(int a, int b) {
		return ((a < b) ? b : a);
	}

	/**
	 * Gets the size of the Image to calculate the minimum size of the Button
	 */
	protected void initSize() {
		if (switchable) {
			width = max(startStatePlainImg.getWidth(this), startStateHoldImg
					.getWidth(this));
			height = max(startStatePlainImg.getHeight(this), startStateHoldImg
					.getHeight(this));
		} else {
			width = currentImg.getWidth(this);
			height = currentImg.getHeight(this);
		}
	}

	public void switchImage() {
		currentImageState = currentImageState.other();
		setCurrentImageFromStates();
	}

	public void switchButtonState() {
		if (muted == true)
			muted = false;
		else
			muted = true;

		currentButtonState = currentButtonState.other();
		// Attempt to switch play and pause -- only happens if action is a play
		// or a pause
		audioAction = audioAction.SwapPlayPause();
		setCurrentImageFromStates();
	}

	public synchronized void setPlayPauseButton(AudioAction setTo) {
		//System.out.println("Setting play/pause button to: " + setTo);
		if ((this.audioAction == AudioAction.Resume && setTo == AudioAction.Pause)
				|| (this.audioAction == AudioAction.Pause && setTo == AudioAction.Resume)) {
			// System.out.println("Current action: " + audioAction);
			// System.out.println("New action    : " + setTo);
			switchButtonState();
			// System.out.println("Switching play/pause button");
		}
	}

	public synchronized void setMuteButton(AudioAction setTo) {
		
	}

	public Boolean getCurrentImage() {
		if (this.currentImg == startStatePlainImg)
			return true;
		else
			return false;

	}

	private void setCurrentImageFromStates() {
		if (this.currentButtonState == ButtonState.Start) {
			if (this.currentImageState == ImagedState.Plain) {
				currentImg = startStatePlainImg;
			} else {
				currentImg = startStateHoldImg;
			}
		} else {
			if (this.currentImageState == ImagedState.Plain) {
				currentImg = switchedStatePlainImg;
			} else {
				currentImg = switchedStateHoldImg;
			}
		}
		paintShadow(true);
	}

	/**
	 * paint the ImageButton in its initial shape
	 * 
	 * @param g
	 *            A Graphics
	 */
	public void paint(Graphics g) {
		paintShadow(true);
	}

	/**
	 * paints the ImageButton using double buffering
	 * 
	 * @param raised
	 *            A boolean which shows the state of the button
	 */
	protected void paintShadow(boolean raised) {
		Graphics g = getGraphics();
		Shape s = g.getClip();
		Image dbi;
		Graphics dbg;
		Color bg = getBackground();
		Dimension d = getSize();
		int dx;
		int dy;

		dbi = ImageCache.getImage(this, d.width, d.height);
		dbg = dbi.getGraphics();
		dbg.setClip(s);
		dbg.setColor(bg);
		dx = d.width - width;
		dy = d.height - height;
		dbg.clearRect(0, 0, d.width, d.height);
		dbg.fillRect(0, 0, d.width, d.height);
		dbg.drawImage(currentImg, dx / 2, dy / 2, this);
		g.drawImage(dbi, 0, 0, this);
	}

	/**
	 * called when more informations about the image are available. When the
	 * size is available, the ImageButton notifies its container that the size
	 * may have changed.
	 * 
	 * @see java.awt.image.ImageObserver
	 */
	public boolean imageUpdate(Image img, int flaginfo, int x, int y,
			int width, int height) {
		initSize();
		Container parent = getParent();
		if (parent != null) {
			parent.doLayout();
		}
		return super.imageUpdate(img, flaginfo, x, y, width, height);
	}

	/**
	 * Returns the minimum size of the ImageButton
	 */
	public Dimension getMinimumSize() {
		return new Dimension(width + 8, height + 8);
	}

	/**
	 * Returns the preferred size of the ImageButton
	 */
	public Dimension getPreferredSize() {
		return new Dimension(width + 8, height + 8);
	}

	/**
	 * This MouseListener is used to do all the paint operations and to generate
	 * ActionEvents when a click occured
	 */
	private class ImageButtonListener extends MouseAdapter {

		public void mousePressed(MouseEvent me) {
			switchImage();
		}

		public void mouseReleased(MouseEvent me) {
			switchImage();
		}

		public void mouseClicked(MouseEvent me) {
			// Attempt to perform the audio action
			if (audioAction == null) {
				
			} else {
				audioPlayer.DoAudioAction(audioAction);

				// If it was a resume or pause, check the result and switch
				// buttons if necessary
				if (audioAction == AudioAction.Resume) {
					if (audioPlayer.isPlaying() && !audioPlayer.isPaused()) {
						setPlayPauseButton(AudioAction.Pause);
					}
				} else if (audioAction == AudioAction.Pause) {
					if (audioPlayer.isPaused() && audioPlayer.isPaused()) {
						setPlayPauseButton(AudioAction.Resume);
					}
				} else if (audioAction == AudioAction.Mute) {
					// System.out.println( "mute mute mute mute mute" );
					setMuteButton(AudioAction.Mute);
				}
			}
		}
	}

}
