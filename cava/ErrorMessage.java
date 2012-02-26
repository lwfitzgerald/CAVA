package cava;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class ErrorMessage {

	JLabel errorLabel;
	JFrame frame;
	String dialogTitle;
	int longestError;
	
	ArrayList<String> errorMessages;
	
	/**
	 * Create a new error message pop-up, with the default title "Error"
	 * @param errorMessage the error message to be displayed
	 */
	ErrorMessage(String errorMessage){
		createErrorMessage(errorMessage, "Error");
	}
	
	/**
	 * Create a new error message pop-up
	 * @param errorMessage the error message to be be displayed
	 * @param dialogTitle the title of the error message
	 */
	public ErrorMessage(String errorMessage,String dialogTitle) {
		createErrorMessage(errorMessage, dialogTitle);
	}
	
	/**
	 * Internal method actualy creates a new JFrame and JPanel add adds the text (via a JLabel)
	 * @param errorMessage the error message to be be displayed
	 * @param dialogTitle the title of the error message
	 */
	private void createErrorMessage(String errorMessage,String dialogTitle){
        // Create and set up the window.
		errorMessages = new ArrayList<String>();
		longestError=0;
		this.dialogTitle = dialogTitle;
        frame = new JFrame(dialogTitle);

        // Add content to the window.
        frame.add(getErrorPanel(errorMessage), BorderLayout.CENTER);
        frame.setAlwaysOnTop(true);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        frame.addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                errorLabel = null;
                errorMessages.clear();
            }
        });
	}
	
	/**
	 * Internal method returns the panel with the JLabel containing the error message
	 * @param errorMessage the error message to be displayed
	 * @return a JPanel with a JLabel added containing the error message
	 */
	private JPanel getErrorPanel(String errorMessage){
		JPanel errorPanel = new JPanel();
		errorMessages.add(errorMessage);
		longestError = errorMessage.length();
		setJLabelText();
		errorPanel.add(errorLabel);
		return errorPanel;
		
	}
	
	/**
	 * Add an error message to this error message object. If the 
	 * window has been closed, it will be re-opened with this message as the only
	 * error; otherwise this error message will be added as a new line.
	 * @param errorMessage
	 */
	public void addErrorMessage(String errorMessage){
		if(!frame.isVisible()){
			createErrorMessage(errorMessage, dialogTitle);
		}else{
			longestError = Math.max(longestError, errorMessage.length());
			errorMessages.add(errorMessage);
			setJLabelText();
		}
	}
	
	/**
	 * Internal method sets the text of the JLabel by iterating over the error
	 * messages to be displayed
	 */
	private void setJLabelText(){
		if (errorLabel==null){
			errorLabel = new JLabel();
		}
		
		StringBuilder text = new StringBuilder("<html>");
		for(String s: errorMessages){
			text.append(s+"<br>");
		}
		text.append("</html>");
		errorLabel.setText(text.toString());
		frame.setSize(new Dimension(longestError*5+40,(errorMessages.size()-1)*15+80));
	}
}
