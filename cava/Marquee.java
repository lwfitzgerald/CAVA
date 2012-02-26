package cava;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 *  This method makes a string scroll across a JPanel. In the case of Cava, this is done for any track name longer than 32 characters.
 *  This method also controls the display for shorter track names, but it just leaves them static.
 *
 *  @author     Dave Glencross
 *  @created    April 28th 2010
 */
public class Marquee {
	public static String marqueeText;
	private static Marquee myMarquee = new Marquee( "No Song Playing" );
	private static JLabel textOutput = new JLabel( myMarquee.toString() );
	private static Timer marquee;

	
	/**
	 *  Constructor for the Marquee object
	 *
	 *  @param  marquee  String passed is the desired marquee message.
	 */
	public Marquee( String marquee ) {
		marqueeText = marquee;
	}


	/**
	 *  The main program for the Marquee class
	 *
	 *  @param  args  The command line arguments
	 */
	public static JLabel movingText(JLabel label) {
		textOutput = label;
		marquee = new Timer( 200,
			new ActionListener() {
				public void actionPerformed( ActionEvent e ) {
					char firstChar  = marqueeText.charAt( 0 );
					String original = marqueeText;
					if (original.length() < 33) textOutput.setText( original.toString() );
					else {
						marqueeText = marqueeText.substring( 1, marqueeText.length() ) + firstChar;
						String subMarqueeText = marqueeText.substring( 1, 28 );
						textOutput.setText( subMarqueeText.toString() );
					}
				}
			} );
		
		textOutput.setSize(new Dimension(50, 500));

		marquee.start();
		
		return textOutput;

	}

	/**
	 *  Class extends toString.
	 *
	 *  @return    Returns a string format of the marquee object.
	 */
	public String toString() {
		return marqueeText;
	}
	
	public static void main(String[] args) {
		
		//Marquee m = new Marquee( "hello hello hello    " );
		//movingText();
		
	}
}